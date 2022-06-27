/*
 * Copyright (C) 2019 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.ets.infrastructure.event;

import com.sailpoint.atlas.ApplicationInfo;
import com.sailpoint.atlas.boot.core.web.TenantIdentifier;
import com.sailpoint.atlas.event.idn.IdnTopic;
import com.sailpoint.ets.domain.event.AckEvent;
import com.sailpoint.ets.domain.event.DomainEvent;
import com.sailpoint.ets.domain.event.EventPublisher;
import com.sailpoint.iris.client.Event;
import com.sailpoint.iris.client.EventBuilder;
import com.sailpoint.iris.client.EventHeaders;
import com.sailpoint.iris.client.PodTopic;
import com.sailpoint.iris.client.Topic;
import com.sailpoint.metrics.annotation.Metered;
import com.sailpoint.utilities.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.transaction.Transactional;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.sailpoint.ets.infrastructure.util.MetricsReporter.reportEventsTableCount;

/**
 * JpaEventPublisher
 */
@Component
@CommonsLog
@RequiredArgsConstructor(onConstructor_={@Autowired})
@Profile("!test")
public class JpaEventPublisher implements EventPublisher {

	private static final String POLL_SQL = "DELETE FROM event WHERE id=(SELECT id FROM event ORDER BY id FOR UPDATE SKIP LOCKED LIMIT 1) RETURNING id, topic, event_json";

	private final com.sailpoint.iris.client.EventPublisher _irisEventPublisher;
	private final PersistedEventRepo _persistedEventRepo;
	private final JdbcTemplate _jdbcTemplate;
	private final ApplicationInfo _applicationInfo;

	private AtomicBoolean _sendingEvents = new AtomicBoolean(false);

	@Override
	@Metered
	public void publish(DomainEvent domainEvent) {
		TenantIdentifier identifier = TenantIdentifier.parse(domainEvent.getTenantId().toString());

		EventBuilder builder = EventBuilder.withTypeAndContent(domainEvent.getClass().getSimpleName(), domainEvent)
			.addHeader(EventHeaders.POD, identifier.getPod())
			.addHeader(EventHeaders.ORG, identifier.getOrg())
			.addHeader(EventHeaders.REQUEST_ID, domainEvent.getRequestId())
			.addHeader(EventHeaders.ORIGIN_SERVICE_ID, _applicationInfo.getStack());

		Map<String, String> headers = domainEvent.getHeaders();
		if (headers != null) {
			String tenantId = domainEvent.getHeaders().get(EventHeaders.TENANT_ID);
			if (tenantId != null) {
				builder.addHeader(EventHeaders.TENANT_ID, tenantId);
			}
		}

		domainEvent.getPartitionKey().ifPresent(key ->
			builder.addHeader(EventHeaders.PARTITON_KEY, key));

		PersistedEvent persistedEvent = PersistedEvent.builder()
			.topic(new PodTopic(getTopic(domainEvent), identifier.getPod()).getId())
			.eventJson(builder.build().toJson())
			.build();

		_persistedEventRepo.save(persistedEvent);

		// Trigger event sending when this transaction commits successfully...
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
			@Override
			public void afterCommit() {
				sendEvents();
			}
		});
	}

	/**
	 * Run every once in a while to pick up and send events that failed to send.
	 */
	@Scheduled(fixedDelay = 30000)
	public void scheduledSendEvents() {
		try {
			long count = _persistedEventRepo.count();
			reportEventsTableCount(count);

			if (count > 0) {
				sendEvents();
			}
		} catch (Exception e) {
			log.error("error send events", e);
			throw e;
		}
	}

	private void sendEvents() {
		if (_sendingEvents.getAndSet(true)) {
			return;
		}
		try {
			int count = 0;
			while (sendOneEvent()) {
				++count;
			}
			if (count > 0) {
				log.info("sent " + count + " events");
			}
		} finally {
			_sendingEvents.set(false);
		}
	}

	/**
	 * Return topic name based on type of domain event.
	 * @param event domain event.
	 * @return name of the topic.
	 */
	private String getTopic(DomainEvent event) {
		if (event instanceof AckEvent) {
			return IdnTopic.TRIGGER_ACK.getName();
		} else {
			return IdnTopic.TRIGGER.getName();
		}
	}

	@Transactional
	public boolean sendOneEvent() {
		AtomicBoolean sent = new AtomicBoolean(false);
		_jdbcTemplate.query(POLL_SQL, rs -> {
			long id = rs.getLong(1);
			String rawTopic = rs.getString(2);
			String json = rs.getString(3);

			Topic topic = Topic.parse(rawTopic);
			Event event = JsonUtil.parse(Event.class, json);

			try {
				Future<?> future = _irisEventPublisher.publish(event, topic);
				future.get();
			} catch (Exception ex) {
				throw new RuntimeException("error to publish event to kafka", ex);
			}

			log.info("published event: " + id + ": " + event.getType() + " to topic '"  + rawTopic);
			sent.set(true);
		});
		return sent.get();
	}

}
