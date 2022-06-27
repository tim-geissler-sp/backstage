package com.sailpoint.sp.identity.event.domain.event;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class IdentityManagerChangedEvent implements IdentityEvent {
	@NonNull IdentityReference _identity;
	DisplayableIdentityReference _oldManager;
	DisplayableIdentityReference _newManager;

	@Override
	public IdentityReference getIdentity() {
		return _identity;
	}
}
