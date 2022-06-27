/*
 * Copyright (C) 2022 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.audit.integration;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClient;
import com.amazonaws.services.sqs.buffered.AmazonSQSBufferedAsyncClient;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.sailpoint.atlas.AtlasConfig;
import com.sailpoint.atlas.OrgData;
import com.sailpoint.atlas.RequestContext;
import com.sailpoint.atlas.event.AtlasEventPlugin;
import com.sailpoint.atlas.event.EventService;
import com.sailpoint.atlas.event.idn.IdnTopic;
import com.sailpoint.atlas.idn.IdnMessageScope;
import com.sailpoint.atlas.messaging.client.MessagePriority;
import com.sailpoint.atlas.messaging.client.Payload;
import com.sailpoint.atlas.messaging.client.SendMessageOptions;
import com.sailpoint.atlas.plugin.AtlasPlugin;
import com.sailpoint.atlas.plugin.PluginConfigurationContext;
import com.sailpoint.atlas.security.AdministratorSecurityContext;
import com.sailpoint.atlas.service.AtomicMessageService;
import com.sailpoint.atlas.service.ServiceFactory;
import com.sailpoint.atlas.test.integration.kafka.EnableKafkaServer;
import com.sailpoint.audit.AuditEventPlugin;
import com.sailpoint.audit.event.DomainEventPlugin;
import com.sailpoint.audit.service.SQSClientProvider;
import com.sailpoint.audit.verification.AuditVerificationService;
import com.sailpoint.iris.client.Event;
import com.sailpoint.mantis.event.MantisEventHandlerModule;
import com.sailpoint.mantis.platform.MantisApplication;
import com.sailpoint.mantis.test.integration.IntegrationTestApplication;
import com.sailpoint.mantis.test.integration.MantisIntegrationTest;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import sailpoint.object.AuditEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Integration tests for audit verification
 */
@EnableKafkaServer(topics = {"search"}, useTestcontainers = true)
public class VerificationIntegrationTest extends MantisIntegrationTest {

	@ClassRule
	public static LocalStackContainer _localstack =
		new LocalStackContainer(DockerImageName.parse("406205545357.dkr.ecr.us-east-1.amazonaws.com/mirror/localstack:0.14.2")
			.asCompatibleSubstituteFor("localstack/localstack:0.14.2"))
			.withServices(LocalStackContainer.Service.SQS);

	private static AmazonSQS _sqsClient;

	private String _verificationQueueUrl;
	private EventService _eventService;
	private AtomicMessageService _atomicMessageService;
	private String _queueName;

	@Override
	protected MantisApplication createMantisApplication() {
		return new IntegrationTestApplication() {{
			registerPlugin(new AtlasEventPlugin());
			registerPlugin(new AuditEventPlugin());
			registerPlugin(new DomainEventPlugin());
			registerPlugin(new AtlasPlugin() {
				@Override
				public void configure(PluginConfigurationContext context) {
					context.addGuiceModule(new MantisEventHandlerModule());
				}
			});
		}};
	}

	@Override
	public void initializeApplication() throws Exception {
		super.initializeApplication();
		_eventService = ServiceFactory.getService(EventService.class);
		_atomicMessageService = ServiceFactory.getService(AtomicMessageService.class);
		getFeatureFlagClient().setBoolean(AuditVerificationService.Flags.PLAT_SUBMIT_AUDIT_VERIFICATION, true);
		final AtlasConfig config = ServiceFactory.getService(AtlasConfig.class);
		_queueName = config.getString(AuditVerificationService.AUDIT_VERIFICATION_QUEUE_KEY, AuditVerificationService.AUDIT_VERIFICATION_QUEUE_DEFAULT);

		installDummyRequestContext();
	}

	@BeforeClass
	public static void setup() {
		final AmazonSQSAsync asyncClient = AmazonSQSAsyncClient.asyncBuilder()
			.withEndpointConfiguration(_localstack.getEndpointConfiguration(LocalStackContainer.Service.SQS))
			.withCredentials(_localstack.getDefaultCredentialsProvider())
			.build();
		_sqsClient = new AmazonSQSBufferedAsyncClient(asyncClient);

		// Override the SQS client instance that will be injected into the test application
		SQSClientProvider._amazonSQS = _sqsClient;
	}

	@Before
	public void setupQueue() {
		// Give us a new queue for each run
		_verificationQueueUrl = _sqsClient
			.createQueue(new CreateQueueRequest(_queueName))
			.getQueueUrl();
		waitForAWS();
	}

	@After
	public void tearDownQueue() {
		// tear down the queue created
		_sqsClient.deleteQueue(_verificationQueueUrl);
		waitForAWS();
	}

	@Test
	public void shouldPublishVerificationRequestToSqsForRedisSourcedAudits() throws Exception {
		whenAuditMessageIsReceived();
		waitForAWS();
		verifyMessageAddedToSQS();
	}

	@Test
	public void shouldPublishVerificationRequestToSqsForDomainEventSourcedAudits() {
		whenDomainEventIsReceived();
		waitForAWS();
		verifyMessageAddedToSQS();
	}

	private void waitForAWS() {
		// Locally this was not needed but adding for PRB. Async buffers for 200ms so wait a little longer than that.
		try {
			Thread.sleep(250);
		} catch (InterruptedException e) {
		}
	}
	private void whenAuditMessageIsReceived() throws Exception {
		// Dummy up minimal audit event payload and submit via atomic message
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("auditEventXml", new AuditEvent().toXml());
		data.put("useAerStorage", true);
		Payload auditPayload = new Payload("AUDIT_EVENT", data);
		_atomicMessageService.send(IdnMessageScope.AUDIT, auditPayload, new SendMessageOptions(MessagePriority.LOW));
	}

	private void whenDomainEventIsReceived() {
		final Event event = new Event("SAVED_SEARCH_CREATE_PASSED", "{}");
		_eventService.publish(IdnTopic.SEARCH, event);
	}

	private void verifyMessageAddedToSQS() {
		final ReceiveMessageResult receiveMessageResult = _sqsClient.receiveMessage(new ReceiveMessageRequest()
			.withQueueUrl(_verificationQueueUrl).withWaitTimeSeconds(20));
		Assert.assertNotNull(receiveMessageResult);
		Assert.assertEquals(1, receiveMessageResult.getMessages().size());
	}

	private void installDummyRequestContext() {
		// Atomic message sender barfs without a request context
		RequestContext requestContext = RequestContext.create();
		requestContext.setOrgData(new OrgData("dev", "acme-solar"));
		requestContext.getOrgData().setTenantId("0001");
		requestContext.setSecurityContext(new AdministratorSecurityContext());
	}
}
