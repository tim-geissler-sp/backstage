/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.sp.identity.event.domain;

import com.sailpoint.sp.identity.event.domain.event.IdentityReference;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Identity
 */
@Value
@Builder(toBuilder = true)
public class Identity {
	@NonNull IdentityId _id;
	@NonNull String _name;
	@NonNull ReferenceType _type;
	@Singular Map<String, Object> _attributes;
	List<Account> _accounts;
	List<Map<String, Object>> _access;
	List<App> _apps;
	boolean disabled;

	/**
	 * Builds a reference to this Identity.
	 *
	 * @return A reference to this Identity.
	 */
	public IdentityReference getReference() {
		return IdentityReference.builder()
			.id(_id.getId())
			.name(_name)
			.type(_type)
			.build();
	}

	/**
	 * Return map representation for accounts.
	 * @return accounts map.
	 */
	public Map<String, Account> getAccountsMap() {
		Map<String, Account> result = new HashMap<>();
		if(_accounts != null && _accounts.size() > 0) {
			_accounts.forEach(a->result.put(a.getAccountId().getId(), a));
		}
		return result;
	}

	public Map<String, App> getAppsMap() {
		Map<String, App> result = new HashMap<>();
		if (_apps != null && _apps.size() > 0) {
			_apps.forEach(a -> result.put(a.getAppId().getId(), a));
		}
		return result;
	}
}
