/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.sp.identity.event.domain;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * IdentityStateJpaRepository
 */
@Repository
public interface IdentityStateJpaRepository extends CrudRepository<IdentityStateEntity, String> {

	Optional<IdentityStateEntity> findByTenantIdAndIdentityId(TenantId tenantId, IdentityId identityId);

	@Modifying
	@Query("delete from IdentityStateEntity i where i.identityId=:identityId AND i.tenantId=:tenantId")
	void deleteByTenantIdAndIdentityId(@Param("tenantId") TenantId tenantId, @Param("identityId") IdentityId identityId);

	@Modifying
	@Query("delete from IdentityStateEntity i where i.expiration<=:offsetDateTime")
	void deleteByExpirationBefore(OffsetDateTime offsetDateTime);

	@Modifying
	@Query("delete from IdentityStateEntity i where i.tenantId=:tenantId")
	void deleteByTenantId(@Param("tenantId") TenantId tenantId);
}
