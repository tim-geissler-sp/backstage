/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.sp.identity.event.domain.command;

import com.sailpoint.iris.client.kafka.internal.NumberGauge;
import com.sailpoint.metrics.MetricsUtil;
import com.sailpoint.sp.identity.event.domain.Account;
import com.sailpoint.sp.identity.event.domain.App;
import com.sailpoint.sp.identity.event.domain.IdentityId;
import com.sailpoint.sp.identity.event.domain.IdentityStateRepository;
import com.sailpoint.sp.identity.event.domain.ReferenceType;
import com.sailpoint.sp.identity.event.domain.event.AttributeChange;
import com.sailpoint.sp.identity.event.domain.Identity;
import com.sailpoint.sp.identity.event.domain.IdentityState;
import com.sailpoint.sp.identity.event.domain.TenantId;
import com.sailpoint.sp.identity.event.domain.service.IdentityEventPublishService;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;
import lombok.extern.apachecommons.CommonsLog;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * UpdateIdentityCommand is a command invoked in response to an IdentityChanged event. It is primarily responsible
 * for computing the difference from the current IdentityState and incoming command, potentially publishing events
 * as a side-effect.
 */
@Value
@Builder
@CommonsLog
public class UpdateIdentityCommand {
	public static final String ENTITLEMENT_COUNT_ATTRIBUTE = "entitlementCount";
	public static final String MANAGER_ATTRIBUTE = "manager";

	@NonNull TenantId _tenantId;
	@NonNull IdentityId _id;
	@NonNull String _name;
	@NonNull @Singular Map<String, Object> _attributes;
	@NonNull @Singular List<Account> _accounts;
	@NonNull @Singular("access") List<Map<String, Object>> _access;
	@NonNull OffsetDateTime _timestamp;
	@NonNull @Singular List<App> _apps;
	@NonNull Boolean _disabled;

	/**
	 * Handle uses the current state of the related Identity and compares with the incoming event. Delta events are
	 * published to the event publisher if any significant changes are detected.
	 *
	 * @param identityStateRepository The IdentityStateRepository implementation.
	 * @param identityEventPublishService The IdentityEventPublishService.
	 */
	public void handle(IdentityStateRepository identityStateRepository, IdentityEventPublishService identityEventPublishService) {
		Identity newIdentity = Identity.builder()
			.id(_id)
			.name(getName())
			.type(ReferenceType.IDENTITY)
			.attributes(_attributes)
			.accounts(_accounts)
			.access(_access)
			.apps(_apps)
			.disabled(_disabled)
			.build();

		IdentityState newState = IdentityState.builder()
			.identity(newIdentity)
			.lastEventTime(_timestamp)
			.build();

		Optional<IdentityState> optOldState = identityStateRepository.findById(_tenantId, newIdentity.getId());
		if (!optOldState.isPresent()) {
			identityEventPublishService.publishIdentityCreatedEvent(newIdentity, _timestamp);
			if (_accounts.size() > 0) {
				_accounts.forEach(a -> identityEventPublishService.publishAccountCorrelatedEvent(a, newIdentity, _timestamp));
			}
			if (_access.size() > 0) {
				_access.stream()
					.filter(a -> null != getId(a))
					.forEach(a -> identityEventPublishService.publishAccessAddedEvent(a, newIdentity, _timestamp));
			}
			if (_apps.size() > 0) {
				_apps.forEach(a -> identityEventPublishService.publishAppAddedEvent(a, newIdentity, _timestamp));
			}

			identityStateRepository.save(_tenantId, newState);
			return;
		}

		IdentityState oldState = optOldState.get();

		if (oldState.getLastEventTime().isAfter(_timestamp)) {
			log.warn(String.format("ignoring out of order event for identity %s", oldState.getIdentity().toString()));
			return;
		}

		if (oldState.isDeleted()) {
			log.warn(String.format("ignoring change event for deleted identity %s", oldState.getIdentity().toString()));
			return;
		}

		if (oldState.getIdentity().isDisabled() != newIdentity.isDisabled()) {
			identityEventPublishService.publishIdentityDisabledChangedEvent(newIdentity, _timestamp);
		}

		List<AttributeChange> changes = getAttributeChanges(oldState.getIdentity(), newIdentity);
		if (!changes.isEmpty()) {
			identityEventPublishService.publishAttributesChangedEvent(newIdentity, changes, _timestamp);
		}

		processManagerChange(oldState.getIdentity(), newIdentity, identityEventPublishService);
		processAccessChanges(oldState.getIdentity(), newIdentity, identityEventPublishService);
		processAppChanges(oldState.getIdentity(), newIdentity, identityEventPublishService);
		processAccountsChanges(oldState.getIdentity(), newIdentity, identityEventPublishService);

		identityStateRepository.save(_tenantId, newState);
	}

	/**
	 * Computes the set of attribute changes between an old and new identity state.
	 *
	 * @param oldIdentity The old identity state.
	 * @param newIdentity The new identity state.
	 * @return The, potentially empty, list of changed attributes.
	 */
	private List<AttributeChange> getAttributeChanges(Identity oldIdentity, Identity newIdentity) {
		return getAttributeChanges(oldIdentity.getAttributes(), newIdentity.getAttributes());
	}

	/**
	 * Computes the set of attribute changes between an old and new state.
	 * @param oldAttributes old attributes map.
	 * @param newAttributes new attributes map.
	 * @return - attributes changes.
	 */
	private List<AttributeChange> getAttributeChanges(Map<String, Object> oldAttributes, Map<String, Object> newAttributes) {
		Set<String> attributes = new HashSet<>();
		attributes.addAll(oldAttributes.keySet());
		attributes.addAll(newAttributes.keySet());

		List<AttributeChange> changes = new ArrayList<>();
		for (String attribute : attributes) {
			Object oldValue = oldAttributes.get(attribute);
			Object newValue = newAttributes.get(attribute);

			if (isAttributeChanged(attribute, oldValue, newValue)) {
				changes.add(AttributeChange.builder()
					.attribute(attribute)
					.oldValue(oldValue)
					.newValue(newValue)
					.build());
			}
		}

		return changes;
	}

	private boolean isAttributeChanged(String attribute, Object oldValue, Object newValue) {
		// Manager can be stored as a map representation of DisplayableIdentityReference if it is set, however we do not want to
		// consider the manager display name in comparison. If ID matches then no change.
		if (MANAGER_ATTRIBUTE.equals(attribute) && (oldValue instanceof Map || newValue instanceof Map)) {
			Object oldManagerId = (oldValue == null) ? null : ((Map<String, Object>)oldValue).get("id");
			Object newManagerId = (newValue == null) ? null : ((Map<String, Object>)newValue).get("id");
			return !Objects.equals(oldManagerId, newManagerId);
		} else {
			return !Objects.equals(oldValue, newValue);
		}
	}

	private void processManagerChange(Identity oldIdentity, Identity newIdentity, IdentityEventPublishService identityEventPublishService) {
		Map<String, Object> oldManager = (Map<String, Object>)oldIdentity.getAttributes().get(MANAGER_ATTRIBUTE);
		Map<String, Object> newManager = (Map<String, Object>)newIdentity.getAttributes().get(MANAGER_ATTRIBUTE);
		if (isAttributeChanged(MANAGER_ATTRIBUTE, oldManager, newManager)) {
			identityEventPublishService.publishManagerChangedEvent(newIdentity, oldManager, newManager, _timestamp);
		}
	}

	/**
	 * Process account changes and publish events if any.
	 * @param oldIdentity old Identity.
	 * @param newIdentity new Identity.
	 * @param identityEventPublishService event publish service.
	 */
	void processAccountsChanges(Identity oldIdentity, Identity newIdentity, IdentityEventPublishService identityEventPublishService) {
		Map<String, Account> oldAccounts = oldIdentity.getAccountsMap();
		Map<String, Account> newAccounts = newIdentity.getAccountsMap();
		Set<String> accountIds = new HashSet<>();
		accountIds.addAll(oldAccounts.keySet());
		accountIds.addAll(newAccounts.keySet());
		for (String id : accountIds) {
			if(!oldAccounts.containsKey(id)) { //new account added.
				identityEventPublishService.publishAccountCorrelatedEvent(newAccounts.get(id), newIdentity, _timestamp);
			} else if(!newAccounts.containsKey(id)) { //account was removed
				identityEventPublishService.publishAccountUnCorrelatedEvent(oldAccounts.get(id), newIdentity, _timestamp);
			} else { //verify if any account attributes changes.
				Map<String, Object> oldAccountAttributes = new HashMap<>();
				Map<String, Object> newAccountAttributes = new HashMap<>();

				Account oldAccount = oldAccounts.get(id);
				Account newAccount = newAccounts.get(id);

				if (oldAccount.getAccountAttributes() != null) {
					oldAccountAttributes.putAll(oldAccount.getAccountAttributes());
				}
				if (newAccount.getAccountAttributes() != null) {
					newAccountAttributes.putAll(newAccount.getAccountAttributes());
				}

				List<AttributeChange> attributeChanges = getAttributeChanges(oldAccountAttributes, newAccountAttributes);
				if (!attributeChanges.isEmpty()) {
					identityEventPublishService.publishAccountAttributesChangedEvent(newAccount, newIdentity, attributeChanges, _timestamp);
				}

				if (oldAccount.isLocked() != newAccount.isLocked()) {
					identityEventPublishService.publishAccountLockedChangedEvent(newAccount, newIdentity, _timestamp);
				}

				if (oldAccount.isDisabled() != newAccount.isDisabled()) {
					identityEventPublishService.publishAccountDisabledChangedEvent(newAccounts.get(id), newIdentity, _timestamp);
				}
			}
		}
	}

	void processAppChanges(Identity oldIdentity, Identity newIdentity, IdentityEventPublishService identityEventPublishService) {
		Map<String, App> oldApps = oldIdentity.getAppsMap();
		Map<String, App> newApps = newIdentity.getAppsMap();
		Set<String> appIds = new HashSet<>();
		appIds.addAll(oldApps.keySet());
		appIds.addAll(newApps.keySet());
		appIds.forEach(appId -> {
			if(!oldApps.containsKey(appId)) { // app added
				identityEventPublishService.publishAppAddedEvent(newApps.get(appId), newIdentity, _timestamp);
			} else if(!newApps.containsKey(appId)) { // app removed
				identityEventPublishService.publishAppRemovedEvent(oldApps.get(appId), newIdentity, _timestamp);
			}
		});
	}

	/**
	 * Process access changes and publish events if any.
	 * @param oldIdentity old Identity.
	 * @param newIdentity new Identity.
	 * @param identityEventPublishService event publish service.
	 */
	void processAccessChanges(Identity oldIdentity, Identity newIdentity, IdentityEventPublishService identityEventPublishService) {
		List<Map<String, Object>> oldAccess = oldIdentity.getAccess() == null ? Collections.emptyList() : oldIdentity.getAccess();
		List<Map<String, Object>> newAccess = newIdentity.getAccess() == null ? Collections.emptyList() : newIdentity.getAccess();
		Set<String> oldAccessKeys = getAccessIds(oldAccess);
		Set<String> newAccessKeys = getAccessIds(newAccess);
		Set<Map<String, Object>> uniqueAccessList = new HashSet<>(oldAccess);
		uniqueAccessList.addAll(newAccess);
		for (Map<String, Object> access : uniqueAccessList) {
			String accessId = getId(access);
			if (null != accessId) {
				if (!oldAccessKeys.contains(accessId)) { //new access added.
					identityEventPublishService.publishAccessAddedEvent(access, newIdentity, _timestamp);
				} else if (!newAccessKeys.contains(accessId)) { //access was removed
					identityEventPublishService.publishAccessRemovedEvent(access, newIdentity, _timestamp);
				}
			}
		}
	}

	/**
	 * Return a Set of IDs from the given access maps, filtering out any items that don't have IDs.
	 *
	 * @param access A non-null List of Maps that have the access items.
	 *
	 * @return A non-null Set with the non-null IDs from the given access list.
	 */
	private static Set<String> getAccessIds(List<Map<String, Object>> access) {
		return access.stream()
			.map(UpdateIdentityCommand::getId)
			.filter(Objects::nonNull)
			.collect(Collectors.toSet());
	}

	/**
	 * Return the ID (or null) from the given access item map.
	 *
	 * @param map The non-null access item map.
	 *
	 * @return The ID (of null) of the given access item.
	 */
	static String getId(Map<String, Object> map) {
		Object id = map.get("id");
		return null != id ? id.toString() : null;
	}
}
