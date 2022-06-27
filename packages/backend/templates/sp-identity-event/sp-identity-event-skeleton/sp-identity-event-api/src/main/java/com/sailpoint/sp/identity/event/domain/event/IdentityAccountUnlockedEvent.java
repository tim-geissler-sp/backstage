package com.sailpoint.sp.identity.event.domain.event;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class IdentityAccountUnlockedEvent implements IdentityEvent {
	@NonNull IdentityReference _identity;
	@NonNull AccountReference _account;
	@NonNull SourceReference _source;
	boolean _locked = false;
	boolean _disabled;

	@Override
	public IdentityReference getIdentity() {
		return _identity;
	}
}
