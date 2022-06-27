/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.sp.identity.event.domain.service;

import com.sailpoint.sp.identity.event.domain.IdentityStateRepository;
import lombok.extern.apachecommons.CommonsLog;

@CommonsLog
public class IdentityEventDebugService extends IdentityEventService {

	public IdentityEventDebugService(IdentityStateRepository identityStateRepository, IdentityEventPublishService identityEventPublishService) {
		super(identityStateRepository, identityEventPublishService);
	}
}
