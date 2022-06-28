/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.interest.matcher.repository.impl.json;

import com.google.common.collect.ImmutableMap;
import com.sailpoint.atlas.AtlasConfig;
import com.sailpoint.utilities.JsonUtil;
import com.sailpoint.iris.client.Event;
import com.sailpoint.iris.client.EventBuilder;
import com.sailpoint.iris.client.EventHeaders;
import com.sailpoint.iris.client.OrgTopic;
import com.sailpoint.iris.client.Topic;
import com.sailpoint.iris.server.EventHandlerContext;
import com.sailpoint.notification.sender.common.event.interest.matching.dto.NotificationInterestMatched;
import com.sailpoint.notification.interest.matcher.interest.Interest;
import com.sailpoint.notification.interest.matcher.interest.InterestTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.sailpoint.notification.interest.matcher.event.InterestMatcherEventHandler.HERMES_SLACK_NOTIFICATION_ENABLED;
import static org.mockito.Mockito.when;

/**
 * Tests for class InterestRepositoryJsonImpl
 */
@RunWith(MockitoJUnitRunner.class)
public class InterestRepositoryJsonImplTest {

	@Mock
	EventHandlerContext _context;

	private Event _event;

	private Topic _topic;

	@Test
	public void interestRepositoryJsonImplTest() throws IOException {
		Map<String, String> config = ImmutableMap.of(InterestRepositoryJsonImpl.ATLAS_INTEREST_MATCHED_REPOSITORY_LOCATION,
				createTempJsonFile());
		InterestRepositoryJsonImpl repo = new InterestRepositoryJsonImpl(AtlasConfig.loadConfig(config));

		Interest interest = repo.getInterests().get(1);
		Assert.assertEquals("User Invite Request", interest.getInterestName());
		Assert.assertEquals("email", interest.getCategoryName());
		Assert.assertEquals("USER_INVITE", interest.getTopicName());
		Assert.assertEquals("USER_INVITE_REQUESTED", interest.getEventType());
		Assert.assertEquals("jsonPathDiscovery", interest.getDiscoveryType());
		Assert.assertEquals(InterestTest.CONFIG, interest.getDiscoveryConfig());
	}

	@Test
	public void interestRepositoryJsonImplErrorTest() {
		Map<String, String> config = ImmutableMap.of(InterestRepositoryJsonImpl.ATLAS_INTEREST_MATCHED_REPOSITORY_LOCATION,
				"empty.json");
		InterestRepositoryJsonImpl repo = new InterestRepositoryJsonImpl(AtlasConfig.loadConfig(config));
		Assert.assertEquals(0, repo.getInterests().size());
	}

	@Test
	public void interestRepositoryJsonImplMatchTest() {
		withAccessApprovalEventTopic();

		InterestRepositoryJsonImpl repo = new InterestRepositoryJsonImpl(AtlasConfig
				.loadConfig(Collections.EMPTY_MAP));
		Assert.assertEquals(8, repo.getInterests().size());
		Assert.assertTrue(repo.test(_context));

		withApprovalGeneratedEventTopic();
		Assert.assertTrue(repo.test(_context));
	}

	@Test
	public void interestRepositoryJsonImplprocessInterestMatchTestArrayEvent() {
		withAccessApprovalEventTopic();

		InterestRepositoryJsonImpl repo = new InterestRepositoryJsonImpl(AtlasConfig
				.loadConfig(Collections.EMPTY_MAP));
		Assert.assertEquals(8, repo.getInterests().size());

		Interest approved = repo.getInterests().stream().filter(i->i.getNotificationKey().equals("approval_request")).findAny().get();
		List<NotificationInterestMatched> interestsMatched = repo.processInterestMatch(_context, approved);

		//insure recipient discovery generate 2 events
		Assert.assertEquals(2, interestsMatched.size());

		//verify first event
		NotificationInterestMatched interestMatchedFirst = interestsMatched.get(0);
		Assert.assertNotNull(interestMatchedFirst.getNotificationId());
		Assert.assertEquals(_event.getContentJson(), interestMatchedFirst.getDomainEvent().getContentJson());
		Assert.assertEquals("314cf125-f892-4b16-bcbb-bfe4afb01f85", interestMatchedFirst.getRecipientId());
		Assert.assertEquals("Access Approval Request", interestMatchedFirst.getInterestName());
		Assert.assertEquals("email", interestMatchedFirst.getCategoryName());
		Assert.assertEquals("approval_request", interestMatchedFirst.getNotificationKey());

		//verify second event
		NotificationInterestMatched interestMatchedSecond = interestsMatched.get(1);
		Assert.assertNotNull(interestMatchedSecond.getNotificationId());
		Assert.assertEquals(_event.getContentJson(), interestMatchedSecond.getDomainEvent().getContentJson());
		Assert.assertEquals("70e7cde5-3473-46ea-94ea-90bc8c605a6c", interestMatchedSecond.getRecipientId());
		Assert.assertEquals("Access Approval Request", interestMatchedSecond.getInterestName());
		Assert.assertEquals("email", interestMatchedSecond.getCategoryName());
		Assert.assertEquals("approval_request", interestMatchedSecond.getNotificationKey());
	}

	@Test
	public void interestRepositoryJsonImplProcessInterestMatchTest() {
		withExtendedNotificationEventTopic();

		InterestRepositoryJsonImpl repo = new InterestRepositoryJsonImpl(AtlasConfig
				.loadConfig(Collections.EMPTY_MAP));
		Assert.assertEquals(8, repo.getInterests().size());

		Interest interest = repo.getInterests().stream().filter(i->i.getInterestName().equals("Extended Notification Event")).findAny().get();
		List<NotificationInterestMatched> interestsMatched = repo.processInterestMatch(_context, interest);

		//insure recipient discovery generate 1 events
		Assert.assertEquals(1, interestsMatched.size());

		//verify event
		NotificationInterestMatched interestMatched = interestsMatched.get(0);
		Assert.assertNotNull(interestMatched.getNotificationId());
		Assert.assertEquals(_event.getContentJson(), interestMatched.getDomainEvent().getContentJson());
		Assert.assertEquals("5", interestMatched.getRecipientId());
		Assert.assertEquals("vasil.shlapkou@sailpoint.com", interestMatched.getRecipientEmail());
		Assert.assertEquals("Extended Notification Event", interestMatched.getInterestName());
		Assert.assertEquals("email", interestMatched.getCategoryName());
		Assert.assertEquals("cloud_user_app_password_changed", interestMatched.getNotificationKey());
	}

	private static String createTempJsonFile() throws IOException {
		Path path = Files.createTempFile("interestsRepository" + UUID.randomUUID().toString(), ".json");
		File file = path.toFile();
		Files.write(path, JsonUtil.toJsonPretty(InterestTest.createInterests()).getBytes(StandardCharsets.UTF_8));
		file.deleteOnExit();
		return file.getAbsolutePath();
	}

	private void withAccessApprovalEventTopic() {
		_event = EventBuilder.withTypeAndContentJson("ACCESS_APPROVAL_REQUESTED", JSON_EVENT)
				.addHeader(EventHeaders.POD, "dev")
				.addHeader(EventHeaders.ORG, "acme-solar")
				.build();

		_topic = new OrgTopic("notification", "dev", "acme-solar");

		when(_context.getEvent())
				.thenReturn(_event);

		when(_context.getTopic())
				.thenReturn(_topic);
	}

	private void withApprovalGeneratedEventTopic() {
		_event = EventBuilder.withTypeAndContentJson("APPROVALS_GENERATED", JSON_APPROVALS_GENERATED_EVENT)
				.addHeader(EventHeaders.POD, "dev")
				.addHeader(EventHeaders.ORG, "acme-solar")
				.build();

		_topic = new OrgTopic("access_request", "dev", "acme-solar");

		when(_context.getEvent())
				.thenReturn(_event);

		when(_context.getTopic())
				.thenReturn(_topic);
	}

	private void withExtendedNotificationEventTopic() {
		_event = EventBuilder.withTypeAndContentJson("EXTENDED_NOTIFICATION_EVENT", JSON_EVENT_EXTENDED)
				.addHeader(EventHeaders.POD, "dev")
				.addHeader(EventHeaders.ORG, "acme-solar")
				.build();

		_topic = new OrgTopic("notification", "dev", "acme-solar");

		when(_context.getEvent())
				.thenReturn(_event);

		when(_context.getTopic())
				.thenReturn(_topic);
	}

	public final static String JSON_EVENT = "{\"content\": {  \n" +
			"        \"approvers\": [{  \n" +
			"            \"id\": \"314cf125-f892-4b16-bcbb-bfe4afb01f85\",  \n" +
			"            \"name\": \"james.smith\"  \n" +
			"        }, {  \n" +
			"            \"id\": \"70e7cde5-3473-46ea-94ea-90bc8c605a6c\",  \n" +
			"            \"name\": \"jane.doe\"  \n" +
			"        }],  \n" +
			"        \"requester_id\": \"46ec3058-eb0a-41b2-8df8-1c3641e4d771\",  \n" +
			"        \"requester_name\": \"boss.man\",  \n" +
			"        \"accessItems\": [{  \n" +
			"            \"type\": \"ROLE\",  \n" +
			"            \"name\": \"Engineering Administrator\"  \n" +
			"        }]  \n" +
			"    }  }";

	public final static String JSON_EVENT_EXTENDED = "{\n" +
			"  \"recipient\": {\n" +
			"    \"id\": \"5\",\n" +
			"    \"name\": null,\n" +
			"    \"phone\": null,\n" +
			"    \"email\": \"vasil.shlapkou@sailpoint.com\"\n" +
			"  },\n" +
			"  \"medium\": \"email\",\n" +
			"  \"from\": \"no-reply@sailpoint.com\",\n" +
			"  \"subject\": \"[Original recipient: cloud-support@sailpoint.com] ATTENTION: Your SailPoint password update was successful\",\n" +
			"  \"body\": \"\\u003cfont face\\u003d\\\"helvetica,arial,sans-serif\\\"\\u003e\\tDear Cloud Support,\\u003cbr /\\u003e\\t\\t\\t\\t\\t\\t\\t\\t\\t\\t\\t\\t\\t\\t\\t\\t\\t\\t\\t\\t\\t\\t\\t\\t\\t\\t\\t\\t\\t\\u003c/font\\u003e\\u003cp\\u003eIf you did not make this change please contact your IT administrator immediately.\\u003cbr /\\u003e\\u003c/p\\u003e\\t\\u003cp\\u003eThanks,\\u003cbr /\\u003eThe SailPoint Team\\u003c/p\\u003e\",\n" +
			"  \"replyTo\": \"no-reply@sailpoint.com\",\n" +
			"  \"orgId\": 3,\n" +
			"  \"org\": \"acme-solar\",\n" +
			"  \"notificationKey\": \"cloud_user_app_password_changed\",\n" +
			"  \"isTemplateEvaluated\": true,\n" +
			"  \"requestId\": \"570887cb-f4f0-41fb-8dd9-919abbbde619\"\n" +
			"}";

	public final static String JSON_APPROVALS_GENERATED_EVENT = "{\"id\":\"963c5fc8d2bb492a95ebed08642d049b\",\"requestedFor\":" +
			"{\"type\":\"IDENTITY\",\"id\":\"2c918087713167d001719981adb11528\",\"name\":\"SailPoint Support\"},\"requestedBy\":" +
			"{\"type\":\"IDENTITY\",\"id\":\"2c918087713167d001719981adb11528\",\"name\":\"SailPoint Support\"},\"requestType\":\"GRANT_ACCESS\",\"accessRequestItemsWithApprovals\"" +
			":[{\"accessRequestItem\":{\"id\":\"2c9180847551146301758a13af4a0a0d\",\"name\":\"test\",\"type\":\"ROLE\",\"removeDate\":null,\"requesterComment\":" +
			"{\"comment\":\"ss\",\"author\":{\"type\":\"IDENTITY\",\"id\":\"2c918087713167d001719981adb11528\",\"name\":\"SailPoint Support\"}" +
			",\"created\":\"2020-11-02T20:11:22.376Z\"},\"clientMetadata\":null}," +
			"\"approvals\":[{\"approvalSchema\":\"ROLE_OWNER\",\"owner\":" +
			"{\"type\":\"IDENTITY\",\"id\":\"2c918087713167d001719981adb11528\",\"name\":\"SailPoint Support\"}}]}]}";

	public final static String JSON_ACCESS_REQUEST_REVIEWED_EVENT_APPROVED =
			"{\"requestedFor\":{\"type\":\"IDENTITY\",\"id\":\"2c91808874ff9155017509a630101813\",\"name\":\"Ray Gillette\"}," +
					"\"requestedBy\":{\"type\":\"IDENTITY\",\"id\":\"2c91808874ff9155017509a630101813\",\"name\":\"E008\"}," +
					"\"accessRequestId\":\"ff7e5a8de3384660bf3ca92d3aa791bc\"," +
					"\"requestedItemsStatus\":[{\"approvalInfo\":[{\"approverName\":\"E001\",\"approvalComment\":null," +
					"\"approvalDecision\":\"APPROVED\"}],\"clientMetadata\":null,\"comment\":\"pls approve\"," +
					"\"operation\":\"Add\",\"type\":\"ROLE\",\"id\":\"2c91808675467cd70175497e12f30056\",\"name\":\"Test-Role\"}]}";

	public final static String JSON_ACCESS_REQUEST_REVIEWED_EVENT_DENIED =
			"{\"requestedFor\":{\"type\":\"IDENTITY\",\"id\":\"2c91808874ff9155017509a630101813\",\"name\":\"Ray Gillette\"}," +
					"\"requestedBy\":{\"type\":\"IDENTITY\",\"id\":\"2c91808874ff9155017509a630101813\",\"name\":\"E008\"}," +
					"\"accessRequestId\":\"ff7e5a8de3384660bf3ca92d3aa791bc\"," +
					"\"requestedItemsStatus\":[{\"approvalInfo\":[{\"approverName\":\"E001\",\"approvalComment\":null," +
					"\"approvalDecision\":\"REJECTED\"}],\"clientMetadata\":null,\"comment\":\"pls approve\"," +
					"\"operation\":\"Add\",\"type\":\"ROLE\",\"id\":\"2c91808675467cd70175497e12f30056\",\"name\":\"Test-Role\"}]}";

	public final static String JSON_IDENTITY_ATTRIBUTES_CHANGED_EVENT =
			"{\"identity\":{\"id\":\"test\",\"name\":\"john.doe\",\"type\":\"IDENTITY\"},\"changes\":[{\"attribute\":\"department\"," +
		"\"oldValue\":\"sales\",\"newValue\":\"marketing\"}]}";

	public final static String JSON_IDENTITY_ATTRIBUTES_CHANGED_NOISY =
			"{\"identity\":{\"id\":\"test\",\"name\":\"john.doe\",\"type\":\"IDENTITY\"},\"changes\":[{\"attribute\":\"lastLoginTimestamp\",\"oldValue\":\"sales\"," +
					"\"newValue\":\"marketing\"}]}";

	public final static String JSON_IDENTITY_ATTRIBUTES_CHANGED_NOT_SUPPORTED_ATTRIBUTES =
			"{\"identity\":{\"id\":\"test\",\"name\":\"john.doe\",\"type\":\"IDENTITY\"},\"changes\":[{\"attribute\":\"duoLoginTime\"," +
					"\"oldValue\":\"12345678\",\"newValue\":\"12345679\"}]}";

	public final static String JSON_IDENTITY_ATTRIBUTES_CHANGED_EVENT_NOT_STRING =
			"{\"identity\":{\"id\":\"test\",\"name\":\"john.doe\",\"type\":\"IDENTITY\"},\"changes\":[{\"attribute\":\"department\"," +
					"\"oldValue\":true,\"newValue\":false}]}";
}
