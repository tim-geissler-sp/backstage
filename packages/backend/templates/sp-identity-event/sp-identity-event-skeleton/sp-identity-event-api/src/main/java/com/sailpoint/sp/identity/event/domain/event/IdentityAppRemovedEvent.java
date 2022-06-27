package com.sailpoint.sp.identity.event.domain.event;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class IdentityAppRemovedEvent implements IdentityEvent {
	@NonNull IdentityReference identity;
	@NonNull SourceReference source;
	@NonNull AccountReference account;
	@NonNull AppReference app;
}
