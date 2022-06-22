/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.sp.identity.event.infrastructure;

import com.sailpoint.sp.identity.event.domain.Account;
import com.sailpoint.sp.identity.event.domain.App;
import com.sailpoint.sp.identity.event.domain.Identity;
import com.sailpoint.sp.identity.event.domain.IdentityId;
import com.sailpoint.sp.identity.event.domain.IdentityState;
import com.sailpoint.sp.identity.event.domain.IdentityStateEntity;
import com.sailpoint.sp.identity.event.domain.IdentityStateJpaRepository;
import com.sailpoint.sp.identity.event.domain.IdentityStateRepository;
import com.sailpoint.sp.identity.event.domain.ReferenceType;
import com.sailpoint.sp.identity.event.domain.TenantId;
import com.sailpoint.utilities.JsonUtil;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * PostgresIdentityStateRepository
 */
@RequiredArgsConstructor
public class PostgresIdentityStateRepository implements IdentityStateRepository {

	private final IdentityStateJpaRepository _identityStateJpaRepository;

	@Override
	public Optional<IdentityState> findById(TenantId tenantId, IdentityId identityId) {
		return _identityStateJpaRepository.findByTenantIdAndIdentityId(tenantId, identityId)
			.map(PostgresIdentityStateRepository::fromEntity);
	}

	@Override
	public void save(TenantId tenantId, IdentityState identityState) {
		_identityStateJpaRepository.save(toEntity(tenantId, identityState));
	}

	@Override
	public void deleteAllByTenant(TenantId tenantId) {
		_identityStateJpaRepository.deleteByTenantId(tenantId);
	}

	@Override
	public void deleteById(TenantId tenantId, IdentityId identityId) {
		_identityStateJpaRepository.deleteByTenantIdAndIdentityId(tenantId, identityId);
	}

	/**
	 * Returns IdentityState domain object from JPA entity.
	 *
	 * @param identityStateEntity The JPA entity.
	 * @return The IdentityState.
	 */
	public static IdentityState fromEntity(IdentityStateEntity identityStateEntity) {
		Identity identity = Identity.builder()
			.id(identityStateEntity.getIdentityId())
			.name(identityStateEntity.getName())
			.type(ReferenceType.valueOf(identityStateEntity.getType()))
			.access(JsonUtil.parse(List.class, identityStateEntity.getAccess()))
			.accounts(JsonUtil.parseList(Account.class, identityStateEntity.getAccounts()))
			.attributes(JsonUtil.parse(Map.class, identityStateEntity.getAttributes()))
			.apps(JsonUtil.parseList(App.class, identityStateEntity.getApps()))
			.disabled(identityStateEntity.isDisabled())
			.build();
		return IdentityState.builder()
			.identity(identity)
			.expiration(identityStateEntity.getExpiration())
			.lastEventTime(identityStateEntity.getLastEventTime())
			.deleted(identityStateEntity.isDeleted())
			.build();
	}

	/**
	 * Returns the JPA entity from the IdentityState domain object and tenantId
	 *
	 * @param tenantId The tenantId.
	 * @param identityState The IdentityState.
	 * @return
	 */
	public static IdentityStateEntity toEntity(TenantId tenantId, IdentityState identityState) {
		//Todo: Add error handling
		return IdentityStateEntity.builder()
			.access(JsonUtil.toJson(identityState.getIdentity().getAccess()))
			.accounts(JsonUtil.toJson(identityState.getIdentity().getAccounts()))
			.attributes(JsonUtil.toJson(identityState.getIdentity().getAttributes()))
			.apps(JsonUtil.toJson(identityState.getIdentity().getApps()))
			.disabled(identityState.getIdentity().isDisabled())
			.deleted(identityState.isDeleted())
			.expiration(identityState.getExpiration())
			.identityId(identityState.getIdentity().getId())
			.name(identityState.getIdentity().getName())
			.type(identityState.getIdentity().getType().toString())
			.tenantId(tenantId)
			.lastEventTime(identityState.getLastEventTime())
			.build();
	}
}
