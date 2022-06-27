package com.sailpoint.sp.identity.event.domain.event;

import com.sailpoint.sp.identity.event.domain.ReferenceType;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/**
 * A value-type reference to an Identity with displayable name
 */
@Value
@Builder
public class DisplayableIdentityReference {
	@NonNull String _id;
	@NonNull String _name;
	String _displayName;
	@NonNull ReferenceType _type;

	public String getDisplayableName() {
		return Optional.ofNullable(_displayName).orElse(_name);
	}

	/**
	 * Get fields as attributes Map.
	 * @return Attributes map.
	 */
	public Map<String, Object> toAttributesMap() {
		Map<String, Object> attributes = new HashMap<>();
		attributes.put("id", _id);
		attributes.put("name", _name);
		attributes.put("type", _type);
		if (_displayName != null) {
			attributes.put("displayName", _displayName);
		}
		return attributes;
	}

	public static DisplayableIdentityReference fromAttributesMap(@NonNull Map<String, Object> attributes) {
		return DisplayableIdentityReference.builder()
			.id((String)attributes.get("id"))
			.name((String)attributes.get("name"))
			.displayName((String)attributes.get("displayName"))
			.type(ReferenceType.valueOf(attributes.get("type").toString()))
			.build();
	}
}
