/*
 * Copyright (C) 2021 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.sp.identity.event.domain.event;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class AccessReference {
	@NonNull String id;
	@NonNull String type;
}
