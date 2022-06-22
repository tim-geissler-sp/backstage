/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.sp.identity.event.domain.command;

import static com.sailpoint.sp.identity.event.domain.command.UpdateIdentityCommand.getId;

import com.sailpoint.sp.identity.event.domain.Identity;
import com.sailpoint.sp.identity.event.domain.IdentityStateRepository;
import com.sailpoint.sp.identity.event.domain.IdentityId;
import com.sailpoint.sp.identity.event.domain.IdentityState;
import com.sailpoint.sp.identity.event.domain.TenantId;
import com.sailpoint.sp.identity.event.domain.service.IdentityEventPublishService;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.apachecommons.CommonsLog;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Deletes the specified Identity from the system.
 */
@Value
@Builder
@CommonsLog
public class DeleteIdentityCommand {
	@NonNull TenantId _tenantId;
	@NonNull IdentityId _identityId;
	@NonNull OffsetDateTime _timestamp;

	/**
	 * Deletes the specified identity from the system.
	 *
	 * @param identityStateRepository The IdentityStateRepository implementation.
	 * @param identityEventPublishService The IdentityEventPublishService.
	 */
	public void handle(IdentityStateRepository identityStateRepository, IdentityEventPublishService identityEventPublishService) {
		Optional<IdentityState> optOldState = identityStateRepository.findById(_tenantId, _identityId);
		if (!optOldState.isPresent()) {
			return;
		}

		IdentityState oldState = optOldState.get();
		if (oldState.isDeleted()) {
			return;
		}

		if (oldState.getLastEventTime().isAfter(_timestamp)) {
			log.warn("ignoring out-of-order deleted event");
			return;
		}

		Identity identity = oldState.getIdentity();
		if (identity.getAccounts() != null) {
			identity.getAccounts().forEach(account -> identityEventPublishService.publishAccountUnCorrelatedEvent(account, identity, _timestamp));
		}
		if (identity.getAccess() != null) {
			identity.getAccess().stream()
				.filter(access -> null != getId(access))
				.forEach(access -> identityEventPublishService.publishAccessRemovedEvent(access, identity, _timestamp));
		}
		if (identity.getApps() != null) {
			identity.getApps().forEach(app -> identityEventPublishService.publishAppRemovedEvent(app, identity, _timestamp));
		}

		identityEventPublishService.publishIdentityDeletedEvent(identity, _timestamp);

		IdentityState newState = oldState.toBuilder()
			.deleted(true)
			.expiration(OffsetDateTime.now().plusHours(1))
			.build();

		identityStateRepository.save(_tenantId, newState);
	}
}
