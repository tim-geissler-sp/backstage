/*
 * Copyright (C) 2019 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.domain.invocation;

import com.sailpoint.ets.domain.TenantId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * InvocationRepo
 */
@Repository
public interface InvocationRepo extends CrudRepository<Invocation, UUID> {
	List<Invocation> findByDeadlineBefore(OffsetDateTime limit, Pageable pageable);
	Stream<Invocation> findAllByTenantId(TenantId tenantId);
	void deleteAllByTenantId(TenantId tenantId);
}
