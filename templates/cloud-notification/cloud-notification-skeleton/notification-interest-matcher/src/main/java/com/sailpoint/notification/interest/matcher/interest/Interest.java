/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.interest.matcher.interest;

import com.google.common.annotations.VisibleForTesting;
import com.sailpoint.atlas.util.JsonPathUtil;
import com.sailpoint.iris.client.Event;
import com.sailpoint.iris.client.Topic;
import com.sailpoint.iris.server.EventHandlerContext;
import com.sailpoint.notification.sender.common.event.discovery.FieldsDiscovery;
import com.sailpoint.notification.sender.common.event.discovery.FieldsDiscoveryFactory;
import com.sailpoint.utilities.StringUtil;

import java.util.function.Predicate;

/**
 * Class represent Interest during interest matching phase.
 */
public class Interest implements Predicate<EventHandlerContext> {

	private final String _interestName;

	private final String _categoryName;

	private final String _topicName;

	private final String _eventType;

	private final String _notificationKey;

	private final String _discoveryType;

	private final String _discoveryConfig;

	private final String _filter;

	private final boolean _enabled;

	private transient FieldsDiscovery _fieldsDiscovery;

	/**
	 * Initialing with default values.
	 */
	public Interest() {
		_interestName = "";
		_categoryName = "";
		_topicName = "";
		_eventType = "";
		_notificationKey = "";
		_discoveryType = "";
		_discoveryConfig = "";
		_filter = null;
		_enabled = true;
	}

	@VisibleForTesting
	public Interest(String interestName, String categoryName, String topicName, String eventType,
					String discoveryType, String notificationKey, String discoveryConfig, String filter, boolean enabled) {
		_interestName = interestName;
		_categoryName = categoryName;
		_topicName = topicName;
		_eventType = eventType;
		_discoveryType = discoveryType;
		_discoveryConfig = discoveryConfig;
		_notificationKey = notificationKey;
		_filter = filter;
		_enabled = enabled;
		init();
	}

	public String getInterestName() {
		return _interestName;
	}

	public String getCategoryName() {
		return _categoryName;
	}

	public String getTopicName() {
		return _topicName;
	}

	public String getNotificationKey() {
		return _notificationKey;
	}

	public boolean isEnabled() {
		return _enabled;
	}

	public String getEventType() {
		return _eventType;
	}

	public String getDiscoveryType() {
		return _discoveryType;
	}

	public String getDiscoveryConfig() {
		return _discoveryConfig;
	}

	public String getFilter() {
		return _filter;
	}

	public FieldsDiscovery getFieldsDiscovery() {
		return _fieldsDiscovery;
	}

	public void init() {
		_fieldsDiscovery = FieldsDiscoveryFactory
				.getFieldsDiscovery(_discoveryType, _discoveryConfig);
	}

	@Override
	public boolean test(EventHandlerContext context) {
		final Topic topic = context.getTopic();
		final Event event = context.getEvent();

		return _eventType.equalsIgnoreCase(event.getType()) &&
				_topicName.equalsIgnoreCase(topic.getName()) &&
				filter(event.getContentJson());
	}

	/**
	 * If a JsonPath filter exists, then apply the filter on the event json
	 * @param eventJson The event json
	 * @return true if a JsonPath filter exists and evaluates to true
	 */
	private boolean filter(String eventJson) {
		if (StringUtil.isNotNullOrEmpty(_filter)) {
			return JsonPathUtil.isPathExist(eventJson, _filter);
		}
		return true;
	}
}
