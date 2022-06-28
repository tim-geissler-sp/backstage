/*
 * Copyright (C) 2019 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.domain.invocation;

import com.sailpoint.ets.domain.Secret;
import com.sailpoint.ets.domain.TenantId;
import com.sailpoint.ets.domain.event.InvocationCompletedEvent;
import com.sailpoint.ets.domain.event.InvocationCompletedTestEvent;
import com.sailpoint.ets.domain.event.InvocationFailedEvent;
import com.sailpoint.ets.domain.event.InvocationFailedTestEvent;
import com.sailpoint.ets.domain.status.InvocationType;
import com.sailpoint.ets.domain.trigger.TriggerId;
import com.vladmihalcea.hibernate.type.json.JsonStringType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Invocation
 */
@Entity
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
@TypeDef(name="json", typeClass= JsonStringType.class)
public class Invocation {

	@Id
	@Type(type="pg-uuid")
	private UUID id;

	@CreationTimestamp
	private OffsetDateTime created;

	@Column(name="tenant_id")
	@Type(type="com.sailpoint.ets.infrastructure.hibernate.TenantIdType")
	private TenantId tenantId;

	@Column(name="trigger_id")
	@Type(type="com.sailpoint.ets.infrastructure.hibernate.TriggerIdType")
	private TriggerId triggerId;

	@Column(name="context", columnDefinition="TEXT")
	@Type(type="json")
	private Map<String, Object> context;

	@Column(name="deadline", nullable=false)
	private OffsetDateTime deadline;

	@Column(name="secret", nullable=false)
	@Type(type="com.sailpoint.ets.infrastructure.hibernate.SecretType")
	private Secret secret;

	@Column(name="type", nullable=false)
	@Enumerated(EnumType.STRING)
	private InvocationType type;

	/**
	 * Generate InvocationFailedEvent.
	 * @param reason error string.
	 * @return InvocationFailedEvent.
	 */
	public InvocationFailedEvent newFailedEvent(String reason) {
		if (this.getType() == InvocationType.REAL_TIME) {
			return InvocationFailedEvent.builder()
				.invocationId(this.getId().toString())
				.tenantId(this.getTenantId().toString())
				.triggerId(this.getTriggerId().toString())
				.context(this.getContext())
				.reason(reason)
				.build();
		} else {
			return InvocationFailedTestEvent.testBuilder()
				.invocationId(this.getId().toString())
				.tenantId(this.getTenantId().toString())
				.triggerId(this.getTriggerId().toString())
				.context(this.getContext())
				.reason(reason)
				.build();
		}
	}

	/**
	 * Generate InvocationCompletedEvent
	 * @param output output map.
	 * @param requestId requestId.
	 * @return InvocationCompletedEvent.
	 */
	public InvocationCompletedEvent newCompletedEvent(Map<String, Object> output, String requestId) {
		if (this.getType() == InvocationType.REAL_TIME) {
			return InvocationCompletedEvent.builder()
				.invocationId(this.getId().toString())
				.tenantId(this.getTenantId().toString())
				.requestId(requestId)
				.triggerId(this.getTriggerId().toString())
				.context(this.getContext())
				.output(output)
				.build();
		} else {
			return InvocationCompletedTestEvent.testBuilder()
				.invocationId(this.getId().toString())
				.tenantId(this.getTenantId().toString())
				.requestId(requestId)
				.triggerId(this.getTriggerId().toString())
				.context(this.getContext())
				.output(output)
				.build();
		}
	}

}
