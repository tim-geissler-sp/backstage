/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.sp.identity.event.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.OffsetDateTime;

@Data
@Entity
@Table(name = "identity_state")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdentityStateEntity {

	@Id
	@Column(name="id")
	@Type(type="com.sailpoint.sp.identity.event.infrastructure.hibernate.IdentityIdType")
	IdentityId identityId;

	@Column(name="tenant_id")
	@Type(type="com.sailpoint.sp.identity.event.infrastructure.hibernate.TenantIdType")
	TenantId tenantId;

	String name;

	String type;

	String attributes;

	String accounts;

	String access;

	String apps;

	@Column(name="last_event_time")
	OffsetDateTime lastEventTime;

	OffsetDateTime expiration;

	boolean deleted;

	boolean disabled;
}
