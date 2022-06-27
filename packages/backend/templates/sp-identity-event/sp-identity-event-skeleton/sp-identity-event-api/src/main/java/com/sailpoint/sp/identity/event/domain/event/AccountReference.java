/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.sp.identity.event.domain.event;

import com.sailpoint.sp.identity.event.domain.ReferenceType;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.util.HashMap;
import java.util.Map;

/**
 * A value-type reference to an Account.
 */
@Value
@Builder
public class AccountReference {
	@NonNull String _id;
	@NonNull String _name;
	@NonNull String _nativeIdentity;
	@NonNull ReferenceType _type;
	String _uuid;

	/**
	 * Get fields as attributes Map.
	 * @return Attributes map.
	 */
	public Map<String, Object> toAttributesMap() {
		Map<String, Object> attributes = new HashMap<>();
		attributes.put("id", _uuid);
		attributes.put("uuid", _uuid);
		attributes.put("name", _name);
		attributes.put("nativeIdentity", _nativeIdentity);
		attributes.put("type", _type);
		return attributes;
	}
}
