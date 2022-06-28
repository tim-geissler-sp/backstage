/*
 * Copyright (C) 2019 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.audit.event;

import com.google.common.base.MoreObjects;
import com.sailpoint.atlas.event.idn.IdnTopic;
import com.sailpoint.iris.client.Topic;
import com.sailpoint.iris.server.EventHandlerContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;

public class EventContext {
	private static Log _log = LogFactory.getLog(EventContext.class);

	static final String REQUEST_ID = "requestId";

	private final Topic _topic;

	private final IdnTopic _idnTopic;

	private final String _requestId;

	private final String _eventType;

	private final String _eventId;

	private final OffsetDateTime _timestamp;

	private final Map<?, ?> _domainEvent;

	EventContext(EventHandlerContext context) {

		_topic = context.getTopic();
		_idnTopic = toIdnTopic(_topic);

		_requestId = context.getEvent().getHeader(REQUEST_ID).orElse(null);
		_eventType = context.getEvent().getType();
		_eventId = context.getEvent().getId();
		_timestamp = Optional.ofNullable(context.getEvent().getTimestamp()).orElseGet(() -> {
			_log.warn("Timestamp was null on event: " + context.getEvent().getId());
			return OffsetDateTime.now();
		});
		_domainEvent = context.getEvent().getContent(Map.class);
	}

	private static IdnTopic toIdnTopic(Topic topic) {
		return IdnTopic.valueOf(topic.getName().toUpperCase());
	}

	public IdnTopic getIdnTopic() {

		return _idnTopic;
	}

	public String getRequestId() {

		return _requestId;
	}

	public String getEventType() {

		return _eventType;
	}

	public String getEventId() {
		return _eventId;
	}

	public Map<?, ?> getDomainEvent() {

		return _domainEvent;
	}

	public OffsetDateTime getTimestamp() {
		return _timestamp;
	}

	public String toString() {

		return MoreObjects.toStringHelper(this)
			.add("topic", _topic)
			.add("idnTopic", _idnTopic)
			.add("requestId", _requestId)
			.add("eventType", _eventType)
			.add("domainEvent", _domainEvent)
			.toString();
	}
}
