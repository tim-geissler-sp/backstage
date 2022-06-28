/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.interest.matcher.interest;


import com.sailpoint.utilities.JsonUtil;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;


public class InterestTest {

	@Test
	public void interestMatcherRepositoryTest() {

		List<Interest> interests = createInterests();
		String interestJson = JsonUtil.toJsonPretty(interests);
		interests = JsonUtil.parseList(Interest.class, interestJson);
		Assert.assertEquals(2, interests.size());

		Interest interest = interests.get(0);
		Assert.assertEquals("Access Approval Request", interest.getInterestName());
		Assert.assertEquals("email", interest.getCategoryName());
		Assert.assertEquals("notification", interest.getTopicName());
		Assert.assertEquals("ACCESS_APPROVAL_REQUESTED", interest.getEventType());
		Assert.assertEquals("jsonPathDiscovery", interest.getDiscoveryType());
		Assert.assertEquals("approval_request", interest.getNotificationKey());
		Assert.assertEquals(CONFIG, interest.getDiscoveryConfig());
		Assert.assertTrue(interest.isEnabled());
		Assert.assertNull(interest.getFieldsDiscovery());

		interest = interests.get(1);
		Assert.assertFalse(interest.isEnabled());
	}

	public static List<Interest> createInterests() {
		List<Interest> interests = new ArrayList<>();
		interests.add(new Interest("Access Approval Request", "email", "notification",
				"ACCESS_APPROVAL_REQUESTED", "jsonPathDiscovery",
				"approval_request",  CONFIG, null, true));
		interests.add(new Interest("User Invite Request", "email", "USER_INVITE",
				"USER_INVITE_REQUESTED", "jsonPathDiscovery",
				"invite_request", CONFIG, null, false));
		return interests;
	}

	public final static String CONFIG = "[\n" +
			"  {\n" +
			"    \"jsonPath\": \"$.content.approvers[*].id\",\n" +
			"    \"valueName\": \"recipientId\"\n" +
			"  },\n" +
			"  {\n" +
			"    \"jsonPath\": \"$.content.approvers[*].name\",\n" +
			"    \"valueName\": \"recipientName\"\n" +
			"  }\n" +
			"]";
}
