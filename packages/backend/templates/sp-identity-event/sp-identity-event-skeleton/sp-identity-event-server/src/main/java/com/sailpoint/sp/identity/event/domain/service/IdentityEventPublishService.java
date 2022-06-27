package com.sailpoint.sp.identity.event.domain.service;

import com.sailpoint.atlas.featureflag.FeatureFlagService;
import com.sailpoint.sp.identity.event.domain.Account;
import com.sailpoint.sp.identity.event.domain.App;
import com.sailpoint.sp.identity.event.domain.Identity;
import com.sailpoint.sp.identity.event.domain.IdentityEventPublisher;
import com.sailpoint.sp.identity.event.domain.event.AccessReference;
import com.sailpoint.sp.identity.event.domain.event.AttributeChange;
import com.sailpoint.sp.identity.event.domain.event.DisplayableIdentityReference;
import com.sailpoint.sp.identity.event.domain.event.IdentityAccessAddedEvent;
import com.sailpoint.sp.identity.event.domain.event.IdentityAccessRemovedEvent;
import com.sailpoint.sp.identity.event.domain.event.IdentityAccountAttributesChangedEvent;
import com.sailpoint.sp.identity.event.domain.event.IdentityAccountCorrelatedEvent;
import com.sailpoint.sp.identity.event.domain.event.IdentityAccountDisabledEvent;
import com.sailpoint.sp.identity.event.domain.event.IdentityAccountEnabledEvent;
import com.sailpoint.sp.identity.event.domain.event.IdentityAccountLockedEvent;
import com.sailpoint.sp.identity.event.domain.event.IdentityAccountUncorrelatedEvent;
import com.sailpoint.sp.identity.event.domain.event.IdentityAccountUnlockedEvent;
import com.sailpoint.sp.identity.event.domain.event.IdentityAppAddedEvent;
import com.sailpoint.sp.identity.event.domain.event.IdentityAppRemovedEvent;
import com.sailpoint.sp.identity.event.domain.event.IdentityAttributesChangedEvent;
import com.sailpoint.sp.identity.event.domain.event.IdentityCreatedEvent;
import com.sailpoint.sp.identity.event.domain.event.IdentityDeletedEvent;
import com.sailpoint.sp.identity.event.domain.event.IdentityDisabledEvent;
import com.sailpoint.sp.identity.event.domain.event.IdentityEnabledEvent;
import com.sailpoint.sp.identity.event.domain.event.IdentityEvent;
import com.sailpoint.sp.identity.event.domain.event.IdentityManagerChangedEvent;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.MapUtils;

@RequiredArgsConstructor
public class IdentityEventPublishService {
	public static final String SP_IDENTITY_EVENT_ACCOUNT_HANDLERS_ENABLED = "SP_IDENTITY_EVENT_ACCOUNT_HANDLERS_ENABLED";
	public static final String SP_IDENTITY_EVENT_DISABLED_LOCKED_CHANGED = "SP_IDENTITY_EVENT_DISABLED_LOCKED_CHANGED";

	@NonNull private final IdentityEventPublisher _identityEventPublisher;
	@NonNull private final FeatureFlagService _featureFlagService;

	/**
	 * Publish IdentityCreatedEvent for the given identity
	 * @param identity Identity
	 * @param timestamp Timestamp of the source event
	 */
	public void publishIdentityCreatedEvent(Identity identity, OffsetDateTime timestamp) {
		IdentityCreatedEvent event = IdentityCreatedEvent.builder()
			.identity(identity.getReference())
			.attributes(identity.getAttributes())
			.build();

		_identityEventPublisher.publish(event, timestamp);
	}

	/**
	 * Publish IdentityDeletedEvent for the given identity
	 * @param identity Identity
	 * @param timestamp Timestamp of the source event
	 */
	public void publishIdentityDeletedEvent(Identity identity, OffsetDateTime timestamp) {
		IdentityDeletedEvent event = IdentityDeletedEvent.builder()
			.identity(identity.getReference())
			.attributes(identity.getAttributes())
			.build();

		_identityEventPublisher.publish(event, timestamp);
	}

	/**
	 * Publish either IdentityDisabledEvent or IdentityEnabledEvent for the given identity
	 * @param identity Identity
	 * @param timestamp Timestamp of the source event
	 */
	public void publishIdentityDisabledChangedEvent(Identity identity, OffsetDateTime timestamp) {
		if (!_featureFlagService.getBoolean(SP_IDENTITY_EVENT_DISABLED_LOCKED_CHANGED, false)) {
			return;
		}

		IdentityEvent event = identity.isDisabled() ?
			IdentityDisabledEvent.builder()
				.identity(identity.getReference())
				.build() :
			IdentityEnabledEvent.builder()
				.identity(identity.getReference())
				.build();

		_identityEventPublisher.publish(event, timestamp);
	}

	/**
	 * Publish IdentityAttributesChangedEvent for the given identity
	 * @param identity Identity
	 * @param changes List of AttributeChanges
	 * @param timestamp Timestamp of the source event
	 */
	public void publishAttributesChangedEvent(Identity identity, List<AttributeChange> changes, OffsetDateTime timestamp) {
		IdentityAttributesChangedEvent event = IdentityAttributesChangedEvent.builder()
			.identity(identity.getReference())
			.changes(changes)
			.build();

		_identityEventPublisher.publish(event, timestamp);
	}

	/**
	 * Publish IdentityAccessAddedEvent.
	 * @param access Map representation of access.
	 * @param identity Identity.
	 * @param timestamp Timestamp of the source event
	 */
	public void publishAccessAddedEvent(Map<String, Object> access, Identity identity, OffsetDateTime timestamp) {
		IdentityAccessAddedEvent.IdentityAccessAddedEventBuilder accessAddedEventBuilder = IdentityAccessAddedEvent
			.builder()
			.access(createAccessReferenceFromMap(access))
			.identity(identity.getReference())
			.attributes(access);

		_identityEventPublisher.publish(accessAddedEventBuilder.build(), timestamp);
	}

	/**
	 * Publish IdentityAccessAddedEvent.
	 * @param access Map representation of access.
	 * @param identity Identity.
	 * @param timestamp Timestamp of the source event
	 */
	public void publishAccessRemovedEvent(Map<String, Object> access, Identity identity, OffsetDateTime timestamp) {
		IdentityAccessRemovedEvent.IdentityAccessRemovedEventBuilder accessRemovedEventBuilder = IdentityAccessRemovedEvent
			.builder()
			.access(createAccessReferenceFromMap(access))
			.identity(identity.getReference())
			.attributes(access);

		_identityEventPublisher.publish(accessRemovedEventBuilder.build(), timestamp);

	}

	/**
	 * Publish IdentityAccountCorrelatedEvent.
	 * @param account Account.
	 * @param identity Identity.
	 * @param timestamp Timestamp of the source event
	 */
	public void publishAccountCorrelatedEvent(Account account, Identity identity, OffsetDateTime timestamp) {
		if (!_featureFlagService.getBoolean(SP_IDENTITY_EVENT_ACCOUNT_HANDLERS_ENABLED, false)) {
			return;
		}
		IdentityAccountCorrelatedEvent.IdentityAccountCorrelatedEventBuilder accountEventBuilder = IdentityAccountCorrelatedEvent.builder()
			.account(account.getAccountReference())
			.source(account.getSourceReference())
			.identity(identity.getReference())
			.entitlementCount(getEntitlementCount(account));

		if (account.getAccountAttributes() != null) {
			accountEventBuilder.attributes(account.getAccountAttributes());
		}

		_identityEventPublisher.publish(accountEventBuilder.build(), timestamp);
	}

	/**
	 * Publish IdentityAccountUnCorrelatedEvent.
	 * @param account Account.
	 * @param identity Identity.
	 * @param timestamp Timestamp of the source event
	 */
	public void publishAccountUnCorrelatedEvent(Account account, Identity identity, OffsetDateTime timestamp) {
		if (!_featureFlagService.getBoolean(SP_IDENTITY_EVENT_ACCOUNT_HANDLERS_ENABLED, false)) {
			return;
		}
		IdentityAccountUncorrelatedEvent accountEvent = IdentityAccountUncorrelatedEvent.builder()
			.account(account.getAccountReference())
			.source(account.getSourceReference())
			.identity(identity.getReference())
			.entitlementCount(getEntitlementCount(account))
			.build();
		_identityEventPublisher.publish(accountEvent, timestamp);
	}

	/**
	 * Publish either IdentityAccountLockedEvent or IdentityAccountUnlockedEvent for the account
	 * @param account Account
	 * @param identity Identity
	 * @param timestamp Timestamp of the source event
	 */
	public void publishAccountLockedChangedEvent(Account account, Identity identity, OffsetDateTime timestamp) {
		if (!_featureFlagService.getBoolean(SP_IDENTITY_EVENT_ACCOUNT_HANDLERS_ENABLED, false)) {
			return;
		}
		IdentityEvent event = account.isLocked() ?
			IdentityAccountLockedEvent.builder()
				.account(account.getAccountReference())
				.identity(identity.getReference())
				.source(account.getSourceReference())
				.disabled(account.isDisabled())
				.build() :
			IdentityAccountUnlockedEvent.builder()
				.account(account.getAccountReference())
				.identity(identity.getReference())
				.source(account.getSourceReference())
				.disabled(account.isDisabled())
				.build();

		_identityEventPublisher.publish(event, timestamp);
	}

	/**
	 * Publish either IdentityAccountDisabledEvent or IdentityAccountEnabledEvent for the account
	 * @param account Account
	 * @param identity Identity
	 * @param timestamp Timestamp of the source event
	 */
	public void publishAccountDisabledChangedEvent(Account account, Identity identity, OffsetDateTime timestamp) {
		if (!_featureFlagService.getBoolean(SP_IDENTITY_EVENT_ACCOUNT_HANDLERS_ENABLED, false)) {
			return;
		}
		IdentityEvent event = account.isDisabled() ?
			IdentityAccountDisabledEvent.builder()
				.account(account.getAccountReference())
				.identity(identity.getReference())
				.source(account.getSourceReference())
				.locked(account.isLocked())
				.build() :
			IdentityAccountEnabledEvent.builder()
				.account(account.getAccountReference())
				.identity(identity.getReference())
				.source(account.getSourceReference())
				.locked(account.isLocked())
				.build();

		_identityEventPublisher.publish(event, timestamp);
	}

	/**
	 * Publish IdentityAccountAttributesChangedEvent for the given account attributes
	 * @param account Account
	 * @param identity Identity
	 * @param attributeChanges List of AttributeChanges
	 * @param timestamp Timestamp of the source event
	 */
	public void publishAccountAttributesChangedEvent(Account account, Identity identity, List<AttributeChange> attributeChanges, OffsetDateTime timestamp) {
		if (!_featureFlagService.getBoolean(SP_IDENTITY_EVENT_ACCOUNT_HANDLERS_ENABLED, false)) {
			return;
		}
		IdentityAccountAttributesChangedEvent event = IdentityAccountAttributesChangedEvent.builder()
			.account(account.getAccountReference())
			.identity(identity.getReference())
			.source(account.getSourceReference())
			.changes(attributeChanges)
			.build();
		_identityEventPublisher.publish(event, timestamp);
	}

	/**
	 * Publish IdentityAppAddedEvent
	 * @param app App
	 * @param identity Identity
	 * @param timestamp Timestamp of the source event
	 */
	public void publishAppAddedEvent(App app, Identity identity, OffsetDateTime timestamp) {
		IdentityAppAddedEvent event = IdentityAppAddedEvent.builder()
			.app(app.getAppReference())
			.source(app.getSourceReference())
			.account(app.getAccountReference())
			.identity(identity.getReference())
			.build();

		_identityEventPublisher.publish(event, timestamp);
	}

	/**
	 * Publish IdentityAppRemovedEvent
	 * @param app App
	 * @param identity Identity
	 * @param timestamp Timestamp of the source event
	 */
	public void publishAppRemovedEvent(App app, Identity identity, OffsetDateTime timestamp) {
		IdentityAppRemovedEvent event = IdentityAppRemovedEvent.builder()
			.app(app.getAppReference())
			.source(app.getSourceReference())
			.account(app.getAccountReference())
			.identity(identity.getReference())
			.build();

		_identityEventPublisher.publish(event, timestamp);
	}

	/**
	 * Publish the IdentityManagerChangedEvent
	 * @param identity Identity
	 * @param oldManager Map representation of DisplayableIdentityReference for previous manager
	 * @param newManager Map representation of DisplayableIdentityReference for new manager
	 * @param timestamp Timestamp of the source event
	 */
	public void publishManagerChangedEvent(Identity identity, Map<String, Object> oldManager, Map<String, Object> newManager, OffsetDateTime timestamp) {
		IdentityManagerChangedEvent event = IdentityManagerChangedEvent.builder()
			.identity(identity.getReference())
			.oldManager(oldManager == null ? null : DisplayableIdentityReference.fromAttributesMap(oldManager))
			.newManager(newManager == null ? null : DisplayableIdentityReference.fromAttributesMap(newManager))
			.build();

		_identityEventPublisher.publish(event, timestamp);
	}


	/***
	 * Gets the entitlementCount from the entitlementAttributes of Account model
	 *
	 * @param account the account model
	 * @return the entitlement count
	 */
	private int getEntitlementCount(Account account) {
		int entitlementCount = 0;
		if (MapUtils.isNotEmpty(account.getEntitlementAttributes())) {
			for (Map.Entry<String, Object> att : account.getEntitlementAttributes().entrySet()) {
				if (att.getValue() instanceof Collection) {
					entitlementCount += ((Collection) att.getValue()).size();
				} else if (null != att.getValue()) {
					entitlementCount++;
				}
			}
		}
		return entitlementCount;
	}

	private AccessReference createAccessReferenceFromMap(Map<String, Object> accessMap) {
		return AccessReference.builder()
			.type(accessMap.get("type").toString())
			.id(accessMap.get("id").toString())
			.build();
	}
}
