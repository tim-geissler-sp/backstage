package com.sailpoint.notification.sender.common.lifecycle;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sailpoint.atlas.AtlasConfig;
import com.sailpoint.atlas.RequestContext;
import com.sailpoint.atlas.event.AtlasEventPlugin;
import com.sailpoint.notification.api.event.EventType;
import com.sailpoint.notification.api.event.dto.Notification;
import com.sailpoint.utilities.JsonUtil;
import com.sailpoint.iris.client.Event;
import com.sailpoint.iris.client.EventHeaders;
import com.sailpoint.iris.server.EventHandlerContext;
import com.sailpoint.notification.api.event.dto.NotificationRendered;
import com.sailpoint.notification.sender.common.event.interest.matching.dto.NotificationInterestMatched;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Map;
import java.util.Optional;

/**
 * NotificationMetricsUtil helper class for reporting metrics.
 */
@Singleton
public class NotificationMetricsUtil {

	private static final Log _log = LogFactory.getLog(NotificationMetricsUtil.class);

	private final static String EVENT_TYPE = "eventType";
	private final static String NAME = "name";
	private final static String NOTIFICATION_KEY = "notificationKey";
	private final static String EXCEPTION = "exception_class";
	private final static String ENV = "env";
	private final static String ENV_PROD = "sp_prod";
	private final static String ENV_DEV = "sp_dev";

	private final boolean _isProduction;

	@Inject
	public NotificationMetricsUtil(AtlasConfig config) {
		_isProduction = config.isProduction() &&
				config.getBoolean(AtlasEventPlugin.ATLAS_IRIS_CONFIG_SERVER_IS_IN_PRODUCTION, false);
	}

	public Map<String, String> getTags(Optional<String> ex) {
		RequestContext context = RequestContext.ensureGet();
		return Map.of("org", context.getOrg(),
			"pod", context.getPod());
	}

	public Map<String, String> getTags(EventHandlerContext context, Optional<String> ex) {
		ImmutableMap.Builder<String, String> tagsBuilder = ImmutableMap.builder();
		addHeaderTag(context, EventHeaders.POD, tagsBuilder);
		addHeaderTag(context, EventHeaders.ORG, tagsBuilder);
		addHeaderTag(context, EventHeaders.ATTEMPT_NUMBER, tagsBuilder);
		addHeaderTag(context, EventHeaders.GROUP_ID, tagsBuilder);
		addEnvironmentTag(tagsBuilder);

		tagsBuilder.put(EVENT_TYPE, context.getEvent().getType());
		tagsBuilder.put(NAME, context.getTopic().getName());

		if (EventType.NOTIFICATION_RENDERED.equalsIgnoreCase(context.getEvent().getType())) {
			Optional<String> notificationKeyOpt = Optional.ofNullable(context.getEvent())
					.map(e -> e.getContent(NotificationRendered.class))
					.map(NotificationRendered::getNotificationKey);

			notificationKeyOpt.ifPresent(s -> tagsBuilder.put(NOTIFICATION_KEY, s));
		}

		ex.ifPresent(value -> tagsBuilder.put(EXCEPTION,  value));
		return tagsBuilder.build();
	}

	/**
	 * Get domain event from Notification
	 * @param notification Notification event.
	 * @return optional domainEvent.
	 */
	public static Optional<Event> getDomainEvent(Notification notification) {
		try {

			if (notification.getDomainEvent() == null) {
				return Optional.empty(); //Notification service M1 ignore for now.
			}

			Map<String, String> parentEvent = (Map<String, String>) notification.getDomainEvent();
			if (parentEvent == null) {
				_log.error("Error reporting notification latency metric. Domain event is empty.");
				return Optional.empty();
			}

			NotificationInterestMatched interestMatched = JsonUtil.parse(NotificationInterestMatched.class,
					parentEvent.get("contentJson"));
			return Optional.of(interestMatched.getDomainEvent());

		} catch (Exception e) {
			_log.error("Error extracting domainEvent", e);
		}
		return Optional.empty();
	}

	private void addHeaderTag(EventHandlerContext context, String headerName,
									 ImmutableMap.Builder<String, String> tagsBuilder) {
		Event event = context.getEvent();
		event.getHeader(headerName).ifPresent(value -> tagsBuilder.put(headerName, value));
	}

	private void addEnvironmentTag(ImmutableMap.Builder<String, String> tagsBuilder) {
		if(_isProduction) {
			tagsBuilder.put(ENV, ENV_PROD);
		} else {
			tagsBuilder.put(ENV, ENV_DEV);
		}
	}
}
