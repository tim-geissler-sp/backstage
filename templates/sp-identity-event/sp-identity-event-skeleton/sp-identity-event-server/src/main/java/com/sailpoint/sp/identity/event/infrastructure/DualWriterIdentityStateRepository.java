/*
 * Copyright (C) 2022 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.sp.identity.event.infrastructure;

import com.sailpoint.atlas.featureflag.FeatureFlagService;
import com.sailpoint.sp.identity.event.domain.IdentityId;
import com.sailpoint.sp.identity.event.domain.IdentityState;
import com.sailpoint.sp.identity.event.domain.IdentityStateRepository;
import com.sailpoint.sp.identity.event.domain.TenantId;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

/**
 * An IdentityStateRepository implementation that migrates implementation from
 * one implementation to another, over time.
 */
@RequiredArgsConstructor
public class DualWriterIdentityStateRepository implements IdentityStateRepository {

	private final IdentityStateRepository _oldRepo;
	private final IdentityStateRepository _newRepo;
	private final FeatureFlagService _featureFlagService;

	public static final String WRITE_TO_OLD_REPO_FLAG = "SP_IDENTITY_EVENT_POSTGRES_ENABLED";
	public static final String WRITE_TO_NEW_REPO_FLAG = "SP_IDENTITY_EVENT_DYNAMO_ENABLED";

	public static final String READ_FROM_OLD_REPO = "SP_IDENTITY_EVENT_POSTGRES_SOURCE_TRUTH";


	/**
	 * {@inheritDoc}
	 */
	@Override
	public Optional<IdentityState> findById(TenantId tenantId, IdentityId identityId)  {
		if (_featureFlagService.getBoolean(READ_FROM_OLD_REPO, false)) {
			return _oldRepo.findById(tenantId, identityId);
		}

		// use new repo if reading-from-old-repo flag is off. If it doesn't exist, fallback to old repo
		Optional<IdentityState> identityState = _newRepo.findById(tenantId, identityId);
		if (!identityState.isPresent()) {
			identityState = _oldRepo.findById(tenantId, identityId);
		}
		return identityState;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void save(TenantId tenantId, IdentityState identityState) {
		if (_featureFlagService.getBoolean(WRITE_TO_OLD_REPO_FLAG, false)) {
			_oldRepo.save(tenantId, identityState);
		}
		if (_featureFlagService.getBoolean(WRITE_TO_NEW_REPO_FLAG, false)) {
			_newRepo.save(tenantId, identityState);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void deleteAllByTenant(TenantId tenantId) {
		if (_featureFlagService.getBoolean(WRITE_TO_OLD_REPO_FLAG, false)) {
			_oldRepo.deleteAllByTenant(tenantId);
		}
		if (_featureFlagService.getBoolean(WRITE_TO_NEW_REPO_FLAG, false)) {
			_newRepo.deleteAllByTenant(tenantId);
		}
	}

	private boolean isWriteToOldEnabled() {
		return _featureFlagService.getBoolean(WRITE_TO_OLD_REPO_FLAG, false);
	}
}
