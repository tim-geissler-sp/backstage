/*
 * Copyright (c) 2020. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.event;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sailpoint.iris.server.EventHandler;
import com.sailpoint.iris.server.EventHandlerContext;
import com.sailpoint.notification.api.event.EventType;
import com.sailpoint.notification.service.VerifiedFromAddressService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *  Event handler for ORG_DELETED for the purpose of deleting Email From Address
 */
@Singleton
public class OrgDeletedEventHandler implements EventHandler {
	private Log _log = LogFactory.getLog(OrgDeletedEventHandler.class);

	private VerifiedFromAddressService _verifiedFromAddressService;

	@Inject
	OrgDeletedEventHandler(VerifiedFromAddressService verifiedFromAddressService) {
		_verifiedFromAddressService = verifiedFromAddressService;
	}

	@Override
	public void handleEvent(EventHandlerContext eventHandlerContext) {
		_log.info("Handling " + EventType.ORG_DELETED);
		handleDelete();
	}

	private void handleDelete() {
		_verifiedFromAddressService.deleteAll();
	}
}
