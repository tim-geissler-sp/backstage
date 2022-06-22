/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.sp.identity.event.domain.service;

import com.sailpoint.atlas.featureflag.FeatureFlagService;
import com.sailpoint.sp.identity.event.domain.IdentityEventPublisher;
import com.sailpoint.sp.identity.event.domain.IdentityStateRepository;
import com.sailpoint.sp.identity.event.domain.command.DeleteTenantCommand;
import com.sailpoint.sp.identity.event.domain.command.UpdateIdentityCommand;
import com.sailpoint.sp.identity.event.domain.command.DeleteIdentityCommand;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import javax.transaction.Transactional;

/**
 * IdentityEventService
 */
@RequiredArgsConstructor
public class IdentityEventService {
	@NonNull private final IdentityStateRepository _identityStateRepository;
	@NonNull private final IdentityEventPublishService _identityEventPublishService;

	/**
	 * Handle an incoming identity deleted event.
	 *
	 * @param cmd The command.
	 */
	@Transactional
	public void deleteIdentity(DeleteIdentityCommand cmd) {
		cmd.handle(_identityStateRepository, _identityEventPublishService);
	}

	/**
	 * Handle an incoming identity changed event.
	 *
	 * @param cmd The command.
	 */
	@Transactional
	public void updateIdentity(UpdateIdentityCommand cmd) {
		cmd.handle(_identityStateRepository, _identityEventPublishService);
	}

	/**
	 * Deletes all data related to the specified tenant.
	 *
	 * @param cmd The command.
	 */
	@Transactional
	public void deleteTenant(DeleteTenantCommand cmd) {
		cmd.handle(_identityStateRepository);
	}
}
