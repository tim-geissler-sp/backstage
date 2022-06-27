package com.sailpoint.sp.identity.event.domain.event;

import com.sailpoint.sp.identity.event.domain.ReferenceType;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class AppReference {
	@NonNull String id;
	@NonNull String name;
	@NonNull ReferenceType type;
}
