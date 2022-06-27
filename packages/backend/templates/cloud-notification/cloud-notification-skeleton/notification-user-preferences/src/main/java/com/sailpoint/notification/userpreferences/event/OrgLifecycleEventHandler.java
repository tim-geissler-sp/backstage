/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.userpreferences.event;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sailpoint.iris.client.Event;
import com.sailpoint.iris.client.EventHeaders;
import com.sailpoint.iris.server.EventHandler;
import com.sailpoint.iris.server.EventHandlerContext;
import com.sailpoint.notification.api.event.EventType;
import com.sailpoint.notification.orgpreferences.repository.TenantPreferencesRepository;
import com.sailpoint.notification.orgpreferences.repository.TenantUserPreferencesRepository;
import com.sailpoint.notification.userpreferences.mapper.UserPreferencesMapper;
import com.sailpoint.notification.userpreferences.repository.UserPreferencesRepository;
import com.sailpoint.notification.userpreferences.service.OrgLifecycleDebugService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Optional;

/**
 *  Org Lifecycle EventHandler. Class for handle iris events for org lifecycle changes.
 */
@Singleton
public class OrgLifecycleEventHandler implements EventHandler {

	private static final Log _log = LogFactory.getLog(OrgLifecycleEventHandler.class);

	private UserPreferencesRepository _userPreferencesRepository;
	private TenantPreferencesRepository _tenantPreferencesRepository;
	private TenantUserPreferencesRepository _tenantUserPreferencesRepository;

	@Inject
	@VisibleForTesting
	OrgLifecycleEventHandler(UserPreferencesRepository userPreferencesRepository, TenantPreferencesRepository tenantPreferencesRepository, TenantUserPreferencesRepository tenantUserPreferencesRepository) {
		_userPreferencesRepository = userPreferencesRepository;
		_tenantPreferencesRepository = tenantPreferencesRepository;
		_tenantUserPreferencesRepository = tenantUserPreferencesRepository;
	}

	@Override
	public void handleEvent(EventHandlerContext context) {
		try {
			Event event = context.getEvent();
			_log.info("Handling " + event.getType() + " event " + event.getId());

			processOrgLifecycleEvent(event);
		} catch (Exception e) {
			_log.error("Error processing event " + context.getEvent().getType(), e);
		}
	}

	private boolean isDebugEvent(Event event) {
		Optional<String> debugHeader = event.getHeader(OrgLifecycleDebugService.REDIS_ORG_LIFECYCLE_DEBUG_KEY);
		return debugHeader.isPresent();
	}

	private void processOrgLifecycleEvent(Event event) {
		Optional<String> maybeOrg = event.getHeader(EventHeaders.ORG);
		Optional<String> maybePod = event.getHeader(EventHeaders.POD);

		if (isDebugEvent(event)) {
			maybeOrg = Optional.of(OrgLifecycleDebugService.DEBUG_ORG);
		}

		if(maybeOrg.isPresent() && maybePod.isPresent()) {
			switch(event.getType()) {
				case EventType.ORG_DELETED : handleOrgDelete(maybePod.get(), maybeOrg.get());
					break;
			}
		}
	}

	private void handleOrgDelete(String pod, String org) {
		final String tenant = UserPreferencesMapper.toHashKey(pod, org);
		_userPreferencesRepository.deleteByTenant(tenant);

		_tenantPreferencesRepository.bulkDeleteForTenant(tenant);
		_tenantUserPreferencesRepository.bulkDeleteForTenant(tenant);

		_log.info("Users, Org and User preferences from org " + org + " has been deleted.");
	}

}