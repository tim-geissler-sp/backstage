/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.sp.identity.event.infrastructure;

import com.sailpoint.sp.identity.event.domain.IdentityStateRepository;
import com.sailpoint.sp.identity.event.domain.IdentityId;
import com.sailpoint.sp.identity.event.domain.IdentityState;
import com.sailpoint.sp.identity.event.domain.TenantId;
import lombok.Value;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory IdentityStateRepository implementation.
 */
public class MemoryIdentityStateRepository implements IdentityStateRepository {

	private final Map<Key, IdentityState> _stateMap = new ConcurrentHashMap<>();

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Optional<IdentityState> findById(TenantId tenantId, IdentityId identityId) {
		return Optional.ofNullable(_stateMap.get(new Key(tenantId, identityId)))
			.filter(s -> !s.isExpired());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void save(TenantId tenantId, IdentityState identityState) {
		_stateMap.put(new Key(tenantId, identityState.getIdentity().getId()), identityState);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void deleteAllByTenant(TenantId tenantId) {
		for (Key key : _stateMap.keySet()) {
			if (key.getTenantId().equals(tenantId)) {
				_stateMap.remove(key);
			}
		}
	}

	/**
	 * Clears all data from the IdentityStateRepository.
	 */
	public void clear() {
		_stateMap.clear();
	}

	@Value
	static class Key {
		TenantId _tenantId;
		IdentityId _identityId;
	}

}
