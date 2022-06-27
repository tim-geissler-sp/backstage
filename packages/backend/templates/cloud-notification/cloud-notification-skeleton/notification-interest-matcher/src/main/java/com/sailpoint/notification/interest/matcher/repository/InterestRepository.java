/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.interest.matcher.repository;

import com.sailpoint.iris.server.EventHandlerContext;
import com.sailpoint.notification.sender.common.event.interest.matching.dto.NotificationInterestMatched;
import com.sailpoint.notification.interest.matcher.interest.Interest;

import java.util.List;
import java.util.function.Predicate;

/**
 * Interface represent access to Interest Repository
 */
public interface InterestRepository extends Predicate<EventHandlerContext> {

	/**
	 * Get all Interests registered with notification service
	 *
	 * @return list of Interest
	 */
	List<Interest> getInterests();

	/**
	 * Perform interest matching phase
	 *
	 * @param context iris EventHandlerContext
	 * @return list of Interest Matched Notifications
	 */
	List<NotificationInterestMatched> processInterestMatch(EventHandlerContext context, Interest interest);
}
