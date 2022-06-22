/*
 * Copyright (C) 2019 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.domain.subscription;

import com.sailpoint.ets.domain.TenantId;
import com.sailpoint.ets.domain.status.SubscriptionStatus;
import com.sailpoint.ets.domain.trigger.TriggerId;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * SubscriptionRepo
 */
@Repository
public interface SubscriptionRepo extends PagingAndSortingRepository<Subscription, UUID>, JpaSpecificationExecutor<Subscription> {
	Optional<Subscription> findByTenantIdAndId(TenantId tenantId, UUID id);
	Optional<Subscription> findByTenantIdAndTriggerId(TenantId tenantId, TriggerId triggerId);
	Stream<Subscription> findAllByTenantIdAndTriggerId(TenantId tenantId, TriggerId triggerId);
	Stream<Subscription> findAllByTenantIdAndType(TenantId tenantId, SubscriptionType type);
	Stream<Subscription> findAllByTenantId(TenantId tenantId);
	void deleteAllByTenantId(TenantId tenantId);

	@Query("select new com.sailpoint.ets.domain.status.SubscriptionStatus(s.tenantId, s.type, count(s)) from Subscription s group by s.tenantId, s.type")
	Stream<SubscriptionStatus> findAllSubscriptionCounts();
}
