/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.notification.integration.ses;

import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.ConfigurationSet;
import com.amazonaws.services.simpleemail.model.ConfigurationSetAlreadyExistsException;
import com.amazonaws.services.simpleemail.model.CreateConfigurationSetEventDestinationRequest;
import com.amazonaws.services.simpleemail.model.CreateConfigurationSetRequest;
import com.amazonaws.services.simpleemail.model.EventDestination;
import com.amazonaws.services.simpleemail.model.EventType;
import com.amazonaws.services.simpleemail.model.SNSDestination;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sns.model.CreateTopicRequest;
import com.amazonaws.services.sns.model.CreateTopicResult;
import com.amazonaws.services.sns.util.Topics;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.CreateQueueResult;
import com.amazonaws.services.sqs.model.Message;
import com.sailpoint.atlas.AtlasApplication;
import com.sailpoint.atlas.dynamodb.DynamoDBService;
import com.sailpoint.atlas.dynamodb.DynamoDBServiceModule;
import com.sailpoint.atlas.event.AtlasDefaultEventHandlerModule;
import com.sailpoint.atlas.event.AtlasEventPlugin;
import com.sailpoint.atlas.health.AtlasHealthPlugin;
import com.sailpoint.atlas.idn.DevOrgDataProvider;
import com.sailpoint.atlas.plugin.AtlasPlugin;
import com.sailpoint.atlas.plugin.PluginConfigurationContext;
import com.sailpoint.atlas.service.ServiceFactory;
import com.sailpoint.atlas.test.integration.IdnAtlasIntegrationTest;
import com.sailpoint.atlas.test.integration.IdnAtlasIntegrationTestApplication;
import com.sailpoint.atlas.test.integration.dynamodb.EnableInMemoryDynamoDB;
import com.sailpoint.atlas.test.integration.kafka.EnableKafkaServer;
import com.sailpoint.featureflag.impl.MockFeatureFlagClient;
import com.sailpoint.mantisclient.HttpResponseException;
import com.sailpoint.mantisclient.IdnAtlasClient;
import com.sailpoint.mantisclient.Params;
import com.sailpoint.notification.NotificationPlugin;
import com.sailpoint.notification.api.event.RecipientBuilder;
import com.sailpoint.notification.api.event.dto.NotificationRendered;
import com.sailpoint.notification.context.GlobalContextPlugin;
import com.sailpoint.notification.sender.NotificationSenderPlugin;
import com.sailpoint.notification.sender.email.EmailStatusDto;
import com.sailpoint.notification.sender.email.domain.TenantSenderEmail;
import com.sailpoint.notification.sender.email.dto.VerificationStatus;
import com.sailpoint.notification.template.NotificationTemplatePlugin;
import com.sailpoint.utilities.JsonUtil;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static com.sailpoint.notification.sender.email.impl.SESMailClient.DEFAULT_EMAIL_FROM_ADDRESS;
import static com.sailpoint.notification.sender.email.impl.SESMailClient.HERMES_SOURCE_ARN_ENABLED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@EnableKafkaServer(topics="notification")
@EnableInMemoryDynamoDB
@Ignore
public class SESMailIntegrationTest extends IdnAtlasIntegrationTest {

	public static final String SES_CONFIG_SET_NAME = "HermesDevTestEventDesitination";
	public static final String SNS_TOPIC_NAME = "HermesDevTestTopic";
	public static final String SQS_QUEUE_NAME_PREFIX = "HermesDevTestQueue";
	private static final String SENDER_FROM_EMAIL_ENDPOINT = "/notification/verified-from-addresses";

	IdnAtlasClient _idnAtlasClient;

	@Override
	protected AtlasApplication createApplication() {
		return new IdnAtlasIntegrationTestApplication() {{
			registerPlugin(new AtlasHealthPlugin());
			registerPlugin(new AtlasEventPlugin());
			registerPlugin(new NotificationSenderPlugin());
			registerPlugin(new GlobalContextPlugin());
			registerPlugin(new NotificationPlugin());
			registerPlugin(new NotificationTemplatePlugin());

			addServiceModule(new AtlasDefaultEventHandlerModule());
			registerPlugin(new AtlasPlugin() {
				@Override
				public void configure(PluginConfigurationContext context) {
					context.addGuiceModule(new DynamoDBServiceModule());
				}
			});
		}};
	}

	@Test
	public void emailSenderTest() {
		DynamoDBService dynamoDBService = ServiceFactory.getService(DynamoDBService.class);
		dynamoDBService.createTable(TenantSenderEmail.class, DynamoDBService.PROJECTION_ALL);

		// Create and verify the first email. The email should be case insensitive
		String response = _idnAtlasClient.post(SENDER_FROM_EMAIL_ENDPOINT, EmailStatusDto.builder()
				.email("success@simulator.amazonses.com")
				.build());
		EmailStatusDto emailStatusDto = JsonUtil.parse(EmailStatusDto.class, response);
		assertEquals(emailStatusDto.getEmail(), "success@simulator.amazonses.com");

		// Create the second email for testing
		_idnAtlasClient.post(SENDER_FROM_EMAIL_ENDPOINT, EmailStatusDto.builder()
				.email("Badly formatted <          bounce@simulator.amazonses.com>")
				.build());

		// Verify that we got two emails
		List<EmailStatusDto> emailStatusDtoList = JsonUtil.parseList(EmailStatusDto.class, _idnAtlasClient.get(SENDER_FROM_EMAIL_ENDPOINT));
		assertEquals(emailStatusDtoList.size(), 3);
		assertEquals(emailStatusDtoList.get(0).getVerificationStatus(), VerificationStatus.PENDING);
		assertEquals(emailStatusDtoList.get(1).getVerificationStatus(), VerificationStatus.PENDING);
		assertEquals(emailStatusDtoList.get(2).getVerificationStatus(), VerificationStatus.SUCCESS);

		// Verify limit and offset
		emailStatusDtoList = JsonUtil.parseList(EmailStatusDto.class, _idnAtlasClient.get(SENDER_FROM_EMAIL_ENDPOINT,
				new Params().query("limit", 1).query("offset", 1)));

		assertEquals(emailStatusDtoList.size(), 1);

		// Verify filter
		emailStatusDtoList = JsonUtil.parseList(EmailStatusDto.class, _idnAtlasClient.get(SENDER_FROM_EMAIL_ENDPOINT,
				new Params().query("filters", "email eq \"success@simulator.amazonses.com\"")));
		assertEquals(emailStatusDtoList.size(), 1);
		assertEquals(emailStatusDtoList.get(0).getEmail(), "success@simulator.amazonses.com");

		// Verify sorter
		emailStatusDtoList = JsonUtil.parseList(EmailStatusDto.class, _idnAtlasClient.get(SENDER_FROM_EMAIL_ENDPOINT,
				new Params().query("sorters", "-email")));
		assertEquals(emailStatusDtoList.size(), 3);
		assertEquals(emailStatusDtoList.get(0).getEmail(), "success@simulator.amazonses.com");
		assertEquals(emailStatusDtoList.get(1).getEmail(), DEFAULT_EMAIL_FROM_ADDRESS);
		assertEquals(emailStatusDtoList.get(2).getEmail(), "Badly formatted <bounce@simulator.amazonses.com>");

		// Delete the bounce email address
		response = _idnAtlasClient.delete(SENDER_FROM_EMAIL_ENDPOINT + "/" + emailStatusDtoList.get(2).getId());
		assertNull(response);

		// Start testing unhappy path
		// Create with empty email
		try {
			_idnAtlasClient.post(SENDER_FROM_EMAIL_ENDPOINT, EmailStatusDto.builder().build());
			fail("should not be able to create empty email");
		} catch (HttpResponseException e) {
			assertEquals(e.getStatusCode(), 400);
		}

		// Create the same email with different case and expect for 400 response
		try {
			_idnAtlasClient.post(SENDER_FROM_EMAIL_ENDPOINT, EmailStatusDto.builder()
					.email("success@simulator.amazonses.com")
					.build());
			fail("should not be able to create same email");
		} catch (HttpResponseException e) {
			assertEquals(e.getStatusCode(), 400);
		}

		// Delete with not existed id
		try {
			_idnAtlasClient.delete(SENDER_FROM_EMAIL_ENDPOINT + "/notExisted");
			fail("should not be able to create same email");
		} catch (HttpResponseException e) {
			assertEquals(e.getStatusCode(), 400);
		}

		// Assign email that is not verified
		try {
			_idnAtlasClient.post("/notification/assign-verified-from-addresses", EmailStatusDto.builder()
					.email("notVerified@simulator.amazonses.com")
					.build());
			fail("should not be able to assign not verified email");
		} catch (HttpResponseException e) {
			assertEquals(e.getStatusCode(), 400);
		}

	}

	@Override
	public void initializeApplication() throws Exception {
		super.initializeApplication();

		_application.getRestPort().ifPresent(restPort -> {
			_idnAtlasClient = new IdnAtlasClient(
					String.format("http://localhost:%d", restPort),
					"dev",
					"acme-solar",
					DevOrgDataProvider.DEV_API_KEY,
					null,
					getHttpClient());
		});
	}

	/**
	 * Sends an email to AWS SES mailbox simulator and confirms mail was sent using AWS' event notification. This test
	 * may fail with a MailClient other than the SESMailClient.
	 */
	@Test
	public void SNSMailAlertTest() throws Exception{
		MockFeatureFlagClient mockFeatureFlagClient = getFeatureFlagClient();
		mockFeatureFlagClient.setBoolean(HERMES_SOURCE_ARN_ENABLED, false);

		AmazonSQS sqs = AmazonSQSClientBuilder.defaultClient();
		String timeStamp = Long.toString(new Date().getTime());
		final String QUEUE_NAME = SQS_QUEUE_NAME_PREFIX + timeStamp;

		try {
			//create an SQS client and queue
			CreateQueueResult createQueueResult = sqs.createQueue(QUEUE_NAME);

			//create a new SNS client and set endpoint
			AmazonSNS sns = AmazonSNSClientBuilder.defaultClient();

			//create a new SNS topic
			CreateTopicRequest createTopicRequest = new CreateTopicRequest(SNS_TOPIC_NAME);
			CreateTopicResult createTopicResult = sns.createTopic(createTopicRequest);

			Topics.subscribeQueue(sns, sqs, createTopicResult.getTopicArn(), createQueueResult.getQueueUrl());

			AmazonSimpleEmailService ses = AmazonSimpleEmailServiceClientBuilder.defaultClient();

			try {
				CreateConfigurationSetRequest createConfigurationSetRequest = new CreateConfigurationSetRequest().withConfigurationSet(new ConfigurationSet().withName(SES_CONFIG_SET_NAME));
				ses.createConfigurationSet(createConfigurationSetRequest);
				CreateConfigurationSetEventDestinationRequest request = new CreateConfigurationSetEventDestinationRequest()
						.withConfigurationSetName(createConfigurationSetRequest.getConfigurationSet().getName())
						.withEventDestination(new EventDestination().withSNSDestination(new SNSDestination().withTopicARN(createTopicResult.getTopicArn()))
								.withName(SES_CONFIG_SET_NAME)
								.withMatchingEventTypes(EventType.Send)
								.withEnabled(true));
				ses.createConfigurationSetEventDestination(request);
			} catch (Exception ex) {
				if (!(ex instanceof ConfigurationSetAlreadyExistsException)) {
					throw ex;
				}
			}

			NotificationRendered notificationRendered = NotificationRendered.builder()
					.recipient(new RecipientBuilder()
							.withEmail("success@simulator.amazonses.com")
							.build())
					.from("no-reply@sailpoint.com")
					.subject("test" + timeStamp)
					.body("<body>hello<br />html</body>")
					.replyTo("paul.mccartney@identitysoon.com")
					.build();
			_idnAtlasClient.post("/sender/debug/sendmail?configset=" + SES_CONFIG_SET_NAME, notificationRendered);

			int maxRetries = 5;
			int attempts = 0;
			boolean recd = false;
			do {
				Thread.sleep(10000);
				List<Message> messages = sqs.receiveMessage(createQueueResult.getQueueUrl()).getMessages();
				if (messages.size() != 0) {
					HashMap<String, Object> map = JsonUtil.parse(HashMap.class, messages.get(0).getBody());
					if (map.containsKey("Message")) {
						String message = map.get("Message").toString();
						recd = message.contains("{\"name\":\"Subject\",\"value\":\"test" + timeStamp + "\"}");
					}
				}
			} while (attempts++ < maxRetries && !recd); //Polls for a total of one minute
			assertTrue(recd);
		} finally {
			sqs.deleteQueue(QUEUE_NAME);
		}
	}

}
