/*
 * Copyright (C) 2019 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.domain.subscription;

import com.sailpoint.ets.domain.Secret;
import com.sailpoint.ets.domain.TenantId;
import com.sailpoint.ets.domain.invocation.Invocation;
import com.sailpoint.ets.domain.status.InvocationType;
import com.sailpoint.ets.domain.trigger.TriggerId;
import com.vladmihalcea.hibernate.type.json.JsonStringType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@TypeDef(name="json", typeClass= JsonStringType.class)
public class Subscription {

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

	@Column(name="type")
	@Enumerated(EnumType.STRING)
	private SubscriptionType type;

	@Column(name="response_dead_line")
	@Type(type="com.sailpoint.ets.infrastructure.hibernate.DurationType")
	private Duration responseDeadline;

	@Column(name="config", columnDefinition="TEXT")
	@Type(type="json")
	private Map<String, Object> config;

	@Column(name="filter")
	private String filter;

	@Column(name="script_source")
	private String scriptSource;

	@Column(name="name")
	private String name;

	@Column(name="description")
	private String description;

	@Column(name="enabled")
	private boolean enabled;

	public Invocation createInvocation(Map<String, Object> context, int deadline, InvocationType type) {
		long duration = deadline;
		if(deadline != Integer.MIN_VALUE && responseDeadline != null) {
			duration = responseDeadline.toMinutes();
		}
		return Invocation.builder()
				.id(UUID.randomUUID())
				.tenantId(getTenantId())
				.triggerId(getTriggerId())
				.context(context)
				.deadline(OffsetDateTime.now().plusMinutes(duration))
				.secret(Secret.generate())
				.type(type)
				.build();
	}
}
