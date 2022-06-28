/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.interest.matcher.event;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.sailpoint.atlas.RequestContext;
import com.sailpoint.atlas.event.EventService;
import com.sailpoint.atlas.event.idn.IdnTopic;
import com.sailpoint.atlas.featureflag.FeatureFlagService;
import com.sailpoint.iris.client.Event;
import com.sailpoint.iris.server.EventHandler;
import com.sailpoint.iris.server.EventHandlerContext;
import com.sailpoint.notification.api.event.EventType;
import com.sailpoint.notification.interest.matcher.interest.Interest;
import com.sailpoint.notification.sender.common.event.interest.matching.dto.NotificationInterestMatched;
import com.sailpoint.notification.interest.matcher.repository.InterestRepository;
import com.sailpoint.notification.interest.matcher.service.InterestMatcherDebugService;
import com.sailpoint.notification.sender.slack.service.SlackService;
import com.sailpoint.notification.sender.teams.service.TeamsService;
import com.sailpoint.utilities.StringUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 *  Interest Matcher EventHandler. Class for handle iris events during interest matcher phase.
 */
public class InterestMatcherEventHandler implements EventHandler {

	private static final Log _log = LogFactory.getLog(InterestMatcherEventHandler.class);

	private static final List<String> SLACK_TEAMS_RELEVANT_ATTRIBUTES = Arrays.asList("id", "displayName", "firstName", "lastName",
			"manager", "idnAccountName", "email", "workPhone", "isManager", "phone", "personalEmail", "country",
			"department", "location", "cloudLifecycleState");

	private final InterestMatcherDebugService _interestMatcherDebugService;

	private final InterestRepository _interestRepository;

	private Interest _interest;

	private final Provider<EventService> _eventService;

	private final FeatureFlagService _featureFlagService;

	private final Supplier<List<String>> _teamsTenantsSupplier;

	private final Supplier<List<String>> _slackTenantsSupplier;

	public static final String HERMES_SLACK_NOTIFICATION_ENABLED = "HERMES_SLACK_NOTIFICATION_ENABLED";
	public static final String HERMES_TEAMS_NOTIFICATION_ENABLED = "HERMES_TEAMS_NOTIFICATION_ENABLED";

	@Inject
	@VisibleForTesting
	InterestMatcherEventHandler(InterestMatcherDebugService interestMatcherDebugService,
								InterestRepository interestRepository,
								Provider<EventService> eventService,
								FeatureFlagService featureFlagService,
								TeamsService teamsService,
								SlackService slackService) {
		_interestMatcherDebugService = interestMatcherDebugService;
		_interestRepository = interestRepository;
		_eventService = eventService;
		_featureFlagService = featureFlagService;
		_teamsTenantsSupplier = Suppliers.memoizeWithExpiration(
				teamsService::getTeamsTenants,
				10,
				TimeUnit.MINUTES);
		_slackTenantsSupplier = Suppliers.memoizeWithExpiration(
				slackService::getSlackTenants,
				10,
				TimeUnit.MINUTES);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void handleEvent(EventHandlerContext context) {
		try {
			boolean isSlackEnabled = _featureFlagService.getBoolean(HERMES_SLACK_NOTIFICATION_ENABLED, false);
			boolean isTeamsEnabled = _featureFlagService.getBoolean(HERMES_TEAMS_NOTIFICATION_ENABLED, false);
			if (interestEnabled(isSlackEnabled, isTeamsEnabled, _interest, context)) {
				Event event = context.getEvent();
				_log.info("Handling " + event.getType() + " event " + event.getId());
				List<NotificationInterestMatched> events = _interestRepository.processInterestMatch(context, _interest);
				processDebugEvent(event, events);

				events.forEach( e ->publishNotificationMatchedEvent(event, e));
			}
		} catch (Exception e) {
			_log.error("Error processing event " + context.getEvent().getType(), e);
		}
	}

	public void setInterest(Interest interest) {
		_interest = interest;
	}

	private boolean interestEnabled(boolean isSlackEnabled, boolean isTeamsEnabled, Interest interest, EventHandlerContext context) {
		//TODO: Remove during removing FF for slack, teams.
		try {
			if (interest.getCategoryName() != null && interest.getCategoryName().contains("slack")) {
				return interest.test(context) && isSlackEnabled(isSlackEnabled) && containsSlackAndTeamsRelevantChanges(context);
			} else if (interest.getCategoryName() != null && interest.getCategoryName().contains("teams")) {
				return interest.test(context) && isTeamsEnabled(isTeamsEnabled) && containsSlackAndTeamsRelevantChanges(context);
			} else {
				return interest.test(context);
			}
		} catch (Exception e) {
			_log.error("Error determining if interest is enabled", e);
			return false;
		}
	}

	/**
	 * Checks if IDENTITY_ATTRIBUTES_CHANGED event contains only attributes changes
	 * that are specified in the {@link #SLACK_TEAMS_RELEVANT_ATTRIBUTES} list.
	 *
	 * This method is used only by Slack/Teams services and their events.
	 * If passed event is not IDENTITY_ATTRIBUTES_CHANGED event, it'll always return true.
	 *
	 * @param context - context of the event.
	 * @return true - if eventType is not IDENTITY_ATTRIBUTES_CHANGED,
	 * true - if event content contains attributes changes that are specified in the {@link #SLACK_TEAMS_RELEVANT_ATTRIBUTES} list,
	 * false - if event content does not contain attributes changes that are specified in the {@link #SLACK_TEAMS_RELEVANT_ATTRIBUTES} list,
	 * false - if event does not contain 'identity' key,
	 * false - if event does not contain 'changes' key.
	 */
    private boolean containsSlackAndTeamsRelevantChanges(EventHandlerContext context) {
        Event event = context.getEvent();
        if (!event.getType().equals(EventType.IDENTITY_ATTRIBUTE_CHANGED)) {
            return true;
        }

        Map<String, Object> contentMap = event.getContent(HashMap.class);

        if (contentMap != null &&
                contentMap.containsKey("identity") && contentMap.containsKey("changes")) {
            Map<Object, Object> identity = (Map<Object, Object>) contentMap.get("identity");
            List<Object> changes = (List<Object>) contentMap.get("changes");
            if (identity == null || !identity.containsKey("id") || changes == null) {
                return false;
            }
            return changes.stream().anyMatch(o -> {
                Map<String, Object> changeMap = (Map<String, Object>) o;
                return changeMap.containsKey("attribute") &&
                        StringUtil.isNotNullOrEmpty(String.valueOf(changeMap.get("attribute"))) &&
                        (StringUtil.isNotNullOrEmpty(String.valueOf(changeMap.get("oldValue")))
                                && StringUtil.isNotNullOrEmpty(String.valueOf(changeMap.get("newValue")))) &&
                        SLACK_TEAMS_RELEVANT_ATTRIBUTES.contains(changeMap.get("attribute"));
            });
        } else {
            return false;
        }
    }

	private boolean isTeamsEnabled(boolean flag) {
		try {
			RequestContext context = RequestContext.ensureGet();
			return flag && _teamsTenantsSupplier.get().contains(context.ensureOrg());
		} catch (Exception e) {
			return false;
		}
	}

	private boolean isSlackEnabled(boolean flag) {
		try {
			RequestContext context = RequestContext.ensureGet();
			return flag && _slackTenantsSupplier.get().contains(context.ensureOrg());
		} catch (Exception e) {
			return false;
		}
	}

	private void publishNotificationMatchedEvent(Event event, NotificationInterestMatched interestMatched) {
		if(!event.getHeader(InterestMatcherDebugService.REDIS_INTEREST_DEBUG_KEY).isPresent()) {
			_eventService.get().publish(IdnTopic.NOTIFICATION, EventType.NOTIFICATION_INTEREST_MATCHED,
					interestMatched);
		}
	}

	private void processDebugEvent(Event event, List<NotificationInterestMatched> events) {
		Optional<String> debugHeader = event.getHeader(InterestMatcherDebugService.REDIS_INTEREST_DEBUG_KEY);
		if (debugHeader.isPresent()) {
			_log.info("Handling debug " + event.getType() + " event id " + event.getId());
			StringBuilder ids = new StringBuilder();
			events.forEach(e -> ids.append(e.getRecipientId()));
			events.forEach(e -> ids.append(e.getRecipientEmail()));
			_interestMatcherDebugService.writeToStore(debugHeader.get(), ids.toString());
		}
	}
}
