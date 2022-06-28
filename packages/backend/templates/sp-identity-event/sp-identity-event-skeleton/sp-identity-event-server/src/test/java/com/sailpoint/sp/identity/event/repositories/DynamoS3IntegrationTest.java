/*
 * Copyright (c) 2022 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.sp.identity.event.repositories;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.sailpoint.atlas.OrgData;
import com.sailpoint.atlas.RequestContext;
import com.sailpoint.atlas.boot.event.EventService;
import com.sailpoint.featureflag.FeatureFlagClient;
import com.sailpoint.featureflag.impl.MockFeatureFlagClient;
import com.sailpoint.iris.client.Event;
import com.sailpoint.iris.client.EventBuilder;
import com.sailpoint.iris.client.PodTopic;
import com.sailpoint.sp.identity.event.domain.IdentityEventPostgreSQLContainer;
import com.sailpoint.sp.identity.event.domain.IdentityState;
import com.sailpoint.sp.identity.event.domain.IdentityStateRepository;
import com.sailpoint.sp.identity.event.domain.event.IdentityAttributesChangedEvent;
import com.sailpoint.sp.identity.event.infrastructure.dynamos3.DynamoS3IdentityStateRepository;
import com.sailpoint.sp.identity.event.infrastructure.s3.S3Client;
import com.sailpoint.utilities.JsonUtil;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;
import org.testcontainers.utility.DockerImageName;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Consumer;

import static com.sailpoint.sp.identity.event.infrastructure.DualWriterIdentityStateRepository.READ_FROM_OLD_REPO;
import static com.sailpoint.sp.identity.event.infrastructure.DualWriterIdentityStateRepository.WRITE_TO_NEW_REPO_FLAG;
import static com.sailpoint.sp.identity.event.infrastructure.DualWriterIdentityStateRepository.WRITE_TO_OLD_REPO_FLAG;
import static com.sailpoint.sp.identity.event.infrastructure.KafkaIdentityEventPublisher.SP_IDENTITY_EVENT_PUBLISHER_LOGGING;
import static com.sailpoint.sp.identity.event.infrastructure.dynamos3.DynamoS3IdentityStateRepository.HASH_KEY_NAME;
import static com.sailpoint.sp.identity.event.infrastructure.dynamos3.DynamoS3IdentityStateRepository.URL_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * Integration test for DynamoS3IdentityStateRepository
 */
@ContextConfiguration(initializers = DynamoS3IntegrationTest.Initializer.class)
@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("integration")
public class DynamoS3IntegrationTest {

	static DockerImageName localstackImage = DockerImageName.parse("406205545357.dkr.ecr.us-east-1.amazonaws.com/mirror/localstack:0.14.2").asCompatibleSubstituteFor("localstack/localstack:0.14.2");

	public static Consumer<CreateContainerCmd> localStackBinding = e -> e.withPortBindings(
		new PortBinding(Ports.Binding.bindPort(4566), new ExposedPort(4566))
	);

	@ClassRule
	public static LocalStackContainer localStack = new LocalStackContainer(localstackImage)
		.withServices(LocalStackContainer.Service.S3, LocalStackContainer.Service.DYNAMODB)
		.withCreateContainerCmdModifier(localStackBinding);

	public static Consumer<CreateContainerCmd> kafkaBinding = e -> e.withPortBindings(
		new PortBinding(Ports.Binding.bindPort(9092), new ExposedPort(9092)),
		new PortBinding(Ports.Binding.bindPort(9093), new ExposedPort(9093)),
		new PortBinding(Ports.Binding.bindPort(2181), new ExposedPort(2181))
	);

	@ClassRule
	public static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:5.5.2"))
		.withCreateContainerCmdModifier(kafkaBinding);

	@ClassRule
	public static PostgreSQLContainer<IdentityEventPostgreSQLContainer> _container = IdentityEventPostgreSQLContainer.getInstance();

	static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
		@Override
		public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
			Properties properties = new Properties();
			properties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());

			AdminClient adminClient = KafkaAdminClient.create(properties);
			adminClient.createTopics(Collections.singletonList(new NewTopic("identity__dev", 2, (short) 1)));
		}
	}

	@Autowired
	private IdentityStateRepository _identityStateRepo;

	@Autowired
	private FeatureFlagClient _featureFlagClient;

	@Autowired
	private AmazonDynamoDB _amazonDynamoDB;

	@Autowired
	private EventService _eventService;

	@Autowired
	private TransferManager _transferManager;

	private String TABLE_NAME = "identity_event_state";

	private KafkaConsumer<String, String> _kafkaConsumer;

	private DynamoDB _dynamoDB;

	private Table _dynamoTable;

	@Before
	public void setUp() {
		RequestContext requestContext = new RequestContext();
		requestContext.setOrg("acme-solar");
		requestContext.setPod("dev");
		RequestContext.set(requestContext);

		if (!_amazonDynamoDB.listTables().getTableNames().contains(TABLE_NAME)) {
			final CreateTableRequest tableRequest = new CreateTableRequest()
				.withTableName(TABLE_NAME)
				.withKeySchema(new KeySchemaElement("identity_id", KeyType.HASH))
				.withAttributeDefinitions(
					new AttributeDefinition("identity_id", "S")
				)
				.withProvisionedThroughput(new ProvisionedThroughput(5L, 5L));

			_amazonDynamoDB.createTable(tableRequest);
		}

		_dynamoDB = new DynamoDB(_amazonDynamoDB);
		_dynamoTable = _dynamoDB.getTable(TABLE_NAME);

		_transferManager.getAmazonS3Client().createBucket("spt-identity-event-state-repo");
		_transferManager.getAmazonS3Client().createBucket("large-event-bucket");

		Properties properties = new Properties();
		properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
		properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		properties.put(ConsumerConfig.GROUP_ID_CONFIG, RandomStringUtils.randomAlphanumeric(17).toUpperCase());
		properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);

		_kafkaConsumer = new org.apache.kafka.clients.consumer.KafkaConsumer<>(properties);
		_kafkaConsumer.subscribe(Collections.singleton("identity_event__dev"));

	}

	@Test
	public void testDynamoS3Impl() throws InterruptedException, IOException {
		assertEquals("DualWriterIdentityStateRepository", _identityStateRepo.getClass().getSimpleName());

		givenRequestContext();
		givenFeatureFlag(SP_IDENTITY_EVENT_PUBLISHER_LOGGING, true);
		givenFeatureFlag(READ_FROM_OLD_REPO, false);
		givenFeatureFlag(WRITE_TO_OLD_REPO_FLAG, false);
		givenFeatureFlag(WRITE_TO_NEW_REPO_FLAG, true);

		// When an IDENTITY_CHANGED input event is encountered for a new identity, we should expect an IdentityCreatedEvent output event (among others) from sp-identity-event
		whenIdentityEventIsPublished(new File("./src/test/resources/IDENTITY_CHANGED_1.json"));

		List<Event> events = listenForEvents(1000, _kafkaConsumer);
		Event event = events.get(0);
		assertEquals("IdentityCreatedEvent", event.getType());

		Item item = null;
		int attempts = 0;
		while (item == null && attempts++ < 10) {
			try {
				item =_dynamoTable.getItem(HASH_KEY_NAME,"2c9180835c60517f015c645836811e11");
			} catch (ResourceNotFoundException ignore) {
			}
			Thread.sleep(1000);
		}

		if (item == null) {
			fail();
		}

		IdentityState identityState = DynamoS3IdentityStateRepository.fromItem(item);
		identityState.getIdentity();

		// When some attribute changes, we should expect IdentityAttributeChangedEvent
		whenIdentityEventIsPublished(new File("./src/test/resources/IDENTITY_CHANGED_2.json"));

		attempts = 0;
		Event identityAttributeChangedEvent = null;
		while (identityAttributeChangedEvent == null && attempts++ < 20) {
			events = listenForEvents(1000, _kafkaConsumer);
			Optional<Event> optional = events.stream().filter(e -> e.getType().equals("IdentityAttributesChangedEvent"))
				.findFirst();
			if (!optional.isPresent()) {
				Thread.sleep(1000);
			} else {
				identityAttributeChangedEvent = optional.get();
				break;
			}
		}

		if (identityAttributeChangedEvent == null) {
			fail();
		}

		IdentityAttributesChangedEvent iac = identityAttributeChangedEvent.getContent(IdentityAttributesChangedEvent.class);
		assertEquals("phone", iac.getChanges().get(0).getAttribute());

		// When a bunch of access items are added to the event causing it to be larger than "dynamoMaxSizeBytes", then the identity state should be persisted in S3
		whenIdentityEventIsPublished(new File("./src/test/resources/IDENTITY_CHANGED_3.json"));

		attempts = 0;
		Event accessAddedEvent = null;
		while (accessAddedEvent == null && attempts++ < 20) {
			events = listenForEvents(1000, _kafkaConsumer);
			Optional<Event> optional = events.stream().filter(e -> e.getType().equals("IdentityAccessAddedEvent"))
				.findFirst();
			if (!optional.isPresent()) {
				Thread.sleep(1000);
			} else {
				accessAddedEvent = optional.get();
				break;
			}
		}

		if (accessAddedEvent == null) {
			fail();
		}

		String s3Url = null;
		attempts = 0;
		while (s3Url == null && attempts++ < 10) {
			try {
				item = _dynamoTable.getItem(HASH_KEY_NAME,"2c9180835c60517f015c645836811e11");
				s3Url = item.getString(URL_KEY);
			} catch (ResourceNotFoundException ignore) {
			}
			Thread.sleep(1000);
		}

		if (s3Url == null || item.get("compressed_identity_state") != null) {
			fail();
		}

		S3Client s3Client = new S3Client("spt-identity-event-state-repo", _transferManager);
		identityState = DynamoS3IdentityStateRepository.decodeIdentityState(s3Client.findByURL(s3Url));

		assertEquals(23, identityState.getIdentity().getAccess().size());

		// When the identity state shrinks again, it should be persisted in dynamo and not be in S3 any longer
		whenIdentityEventIsPublished(new File("./src/test/resources/IDENTITY_CHANGED_2.json"));

		String oldS3Url = s3Url;

		attempts = 0;
		while (s3Url != null && attempts++ < 10) {
			try {
				item =_dynamoTable.getItem(HASH_KEY_NAME,"2c9180835c60517f015c645836811e11");
				s3Url = item.getString(URL_KEY);
			} catch (ResourceNotFoundException ignore) {
			}
			Thread.sleep(1000);
		}

		if (s3Url != null) {
			fail();
		}

		identityState = DynamoS3IdentityStateRepository.fromItem(item);
		identityState.getIdentity();

		assertEquals(3, identityState.getIdentity().getAccess().size());
		assertNull(s3Client.findByURL(oldS3Url));
	}

	private void whenIdentityEventIsPublished(String json) {
		_eventService.publish(new PodTopic("identity", "dev"), EventBuilder.withTypeAndContentJson("IDENTITY_CHANGED", json).build());
	}

	private void whenIdentityEventIsPublished(File jsonFile) throws IOException {
		whenIdentityEventIsPublished(Files.asCharSource(jsonFile, Charsets.UTF_8)
			.read());
	}

	private void givenFeatureFlag(String key, boolean val) {
		((MockFeatureFlagClient) _featureFlagClient).setBoolean(key, val);
	}

	/**
	 * List for events on a topic
	 *
	 * @param ms:            how long to listen for event
	 * @param kafkaConsumer: which consumer to use for event listening
	 * @return
	 */
	private List<Event> listenForEvents(int ms, KafkaConsumer<String, String> kafkaConsumer) {
		final ConsumerRecords<String, String> records = kafkaConsumer.poll(Duration.ofMillis(ms));
		final List<Event> events = new ArrayList<>();

		records.forEach(r -> {
			Event event = JsonUtil.parse(Event.class, r.value());
			events.add(event);
		});

		return events;
	}

	private void givenRequestContext() {
		RequestContext requestContext = new RequestContext();
		requestContext.setOrg("acme-solar");
		requestContext.setPod("dev");
		OrgData orgData = new OrgData();
		orgData.setOrg("acme-solar");
		orgData.setPod("dev");
		orgData.setTenantId("tenantId");
		requestContext.setOrgData(orgData);
		RequestContext.set(requestContext);
	}
}
