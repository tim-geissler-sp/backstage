package com.sailpoint.sp.identity.event.domain.event;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class IdentityEnabledEvent implements IdentityEvent {
	@NonNull IdentityReference _identity;
	boolean _disabled = false;

	@Override
	public IdentityReference getIdentity() {
		return _identity;
	}
}
