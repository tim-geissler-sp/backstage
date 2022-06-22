package com.sailpoint.sp.identity.event.domain.event;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class IdentityAccountEnabledEvent implements IdentityEvent {
	@NonNull IdentityReference _identity;
	@NonNull AccountReference _account;
	@NonNull SourceReference _source;
	boolean _locked;
	boolean _disabled = false;

	@Override
	public IdentityReference getIdentity() {
		return _identity;
	}
}
