/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.userpreferences.event.dto;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.util.HashMap;
import java.util.Map;

/**
 * A value-type reference to an Identity.
 */
@Value
@Builder
public class IdentityReference {
	@NonNull String _id;
	@NonNull String _name;
	@NonNull ReferenceType _type;

	/**
	 * Get fields as attributes Map.
	 * @return Attributes map.
	 */
	public Map<String, Object> toAttributesMap() {
		Map<String, Object> attributes = new HashMap<>();
		attributes.put("id", _id);
		attributes.put("name", _name);
		attributes.put("type", _type);
		return attributes;
	}
}
