/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.sp.identity.event.domain;

import com.sailpoint.sp.identity.event.domain.event.AccountReference;
import com.sailpoint.sp.identity.event.domain.event.SourceReference;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.util.Map;
import lombok.extern.apachecommons.CommonsLog;

/**
 * Account
 */
@Value
@Builder
@CommonsLog
public class Account {
	public static final String ACCOUNT_ATTRIBUTES = "accountAttributes";
	public static final String ACCOUNT_LOCKED = "locked";
	public static final String ACCOUNT_DISABLED = "disabled";

	@NonNull AccountId _accountId;
	@NonNull SourceId _sourceId;
	Map<String, Object> attributes;
	Map<String, Object> entitlementAttributes;

	/**
	 * Builds a reference to this Account.
	 *
	 * @return A reference to this Account.
	 */
	public AccountReference getAccountReference() {
		return AccountReference.builder()
			.id(_accountId.getId())
			.uuid(_accountId.getUuid())
			.nativeIdentity(_accountId.getNativeIdentity())
			.name(_accountId.getName())
			.type(ReferenceType.ACCOUNT)
			.build();
	}

	/**
	 * Builds a Source reference for this Account.
	 *
	 * @return A Source reference for this Account.
	 */
	public SourceReference getSourceReference() {
		return SourceReference.builder()
			.id(_sourceId.getId())
			.name(_sourceId.getName())
			.type(ReferenceType.SOURCE)
			.build();
	}

	public boolean getBooleanAttribute(String key) {
		if (attributes == null || !attributes.containsKey(key)) {
			return false;
		}

		Object boolValue = attributes.get(key);
		if (boolValue == null) {
			return false;
		} else if (boolValue instanceof Boolean) {
			return (Boolean)boolValue;
		} else if (boolValue instanceof String) {
			return Boolean.parseBoolean((String)boolValue);
		} else {
			// Is this right? Not sure what else to do here, we should always have boolean but don't
			// really want to throw either
			log.warn("Unexpected non-boolean type for attribute " + key + ": " + boolValue);
			return false;
		}
	}

	public boolean isLocked() {
		return getBooleanAttribute(ACCOUNT_LOCKED);
	}

	public boolean isDisabled() {
		return getBooleanAttribute(ACCOUNT_DISABLED);
	}

	public Map<String, Object> getAccountAttributes() {
		return attributes == null ? null : (Map<String, Object>)attributes.get(ACCOUNT_ATTRIBUTES);
	}
}
