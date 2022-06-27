/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.sp.identity.event.infrastructure;

import com.sailpoint.atlas.RequestContext;
import com.sailpoint.iris.client.Event;
import com.sailpoint.iris.client.EventHeaders;
import com.sailpoint.iris.server.EventHandler;
import com.sailpoint.iris.server.EventHandlerContext;
import com.sailpoint.metrics.PrometheusMetricsUtil;
import com.sailpoint.sp.identity.event.IdentityEventConfig;
import com.sailpoint.sp.identity.event.domain.Account;
import com.sailpoint.sp.identity.event.domain.App;
import com.sailpoint.sp.identity.event.domain.IdentityId;
import com.sailpoint.sp.identity.event.domain.TenantId;
import com.sailpoint.sp.identity.event.domain.command.DeleteIdentityCommand;
import com.sailpoint.sp.identity.event.domain.command.UpdateIdentityCommand;
import com.sailpoint.sp.identity.event.domain.service.IdentityEventDebugService;
import com.sailpoint.sp.identity.event.domain.service.IdentityEventService;
import com.sailpoint.sp.identity.event.infrastructure.sync.SyncUtil;
import com.sailpoint.utilities.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.kafka.common.utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.sailpoint.sp.identity.event.IdentityEventConfig.IDENTITY_CHANGED;
import static com.sailpoint.sp.identity.event.IdentityEventConfig.IDENTITY_DELETED;

/**
 * IdentityEventHandlers
 *
 * @see IdentityEventConfig#registerEventHandlers()
 */
@Component
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@CommonsLog
public class IdentityEventHandlers implements EventHandler {

	/**
	 * An event header in the input event that denotes a debug event
	 */
	public static final String DEBUG_EVENT_HEADER = "debug";

	private static final String SP_IDENTITY_EVENT_IDENTITY_GAUGE = "sp_identity_event_identity";

	private static final String SP_IDENTITY_EVENT_IDENTITY_GAUGE_HELP = "Measures the identity shape";

	private static final String SP_IDENTITY_EVENT_IDENTITY_GAUGE_TAG_FOR_KEY = "key";

	private static final String SP_IDENTITY_EVENT_IDENTITY_GAUGE_TAG_FOR_POD = "pod";

	private final IdentityEventService _identityEventService;
	private final IdentityEventDebugService _identityEventDebugService;

	@Override
	public void handleEvent(EventHandlerContext context) {
		if (IDENTITY_CHANGED.equals(context.getEvent().getType())) {
			onIdentityChanged(context);
		} else if (IDENTITY_DELETED.equals(context.getEvent().getType())) {
			onIdentityDeleted(context);
		}
	}

	private void onIdentityChanged(EventHandlerContext ctx) {
		final String pod = RequestContext.ensureGet().getPod();
		Event event = ctx.getEvent();
		final OffsetDateTime eventTimestamp = ctx.getEvent().getTimestamp();
		log.info(String.format("Handling %s from partition %d", event.getType(), ctx.getPartition()));

		Map<String, Object> identityMap = SyncUtil.getIdentityMapFromIdentiyChangedEventEvent(event);
		if (identityMap == null || identityMap.size() == 0) {
			// This is possible when IDENTITY_CHANGED event is for uncorrelated identities
			return;
		}

		String identityId = (String) identityMap.get(SyncUtil.IDENTITY_CHANGED_EXTERNAL_ID_KEY);
		if (StringUtil.isNullOrEmpty(identityId)) {
			log.warn("Identity Id is null or empty");
			return;
		}

		Map<String, Object> attributes = SyncUtil.getIdentityAttributesFromIdentityMap(identityMap);
		if (attributes == null || attributes.size() == 0) {
			log.warn(String.format("Identity %s does not have attributes", identityId));
			return;
		}
		publishIdentityGaugeMetrics("attributes", pod, attributes != null? attributes.size() : 0);

		List<Account> accounts = SyncUtil.getAccountsFromIdentityMap(identityMap);
		publishIdentityGaugeMetrics("accounts", pod, accounts != null? accounts.size() : 0);

		List<Map<String, Object>> access = SyncUtil.getAccessFromIdentityMap(identityMap);
		publishIdentityGaugeMetrics("access", pod, access != null? access.size() : 0);

		List<App> apps = SyncUtil.getAppsFromIdentityMap(identityMap);
		publishIdentityGaugeMetrics("apps", pod, apps != null? apps.size() : 0);

		UpdateIdentityCommand cmd = UpdateIdentityCommand.builder()
			.timestamp(eventTimestamp)
			.tenantId(new TenantId(ctx.getEvent().ensureHeader(EventHeaders.TENANT_ID)))
			.id(new IdentityId(identityId))
			.name((String) identityMap.get(SyncUtil.IDENTITY_CHANGED_EXTERNAL_ALIAS_KEY))
			.attributes(attributes)
			.accounts(accounts)
			.access(access)
			.apps(apps)
			.disabled(SyncUtil.getDisabledFromIdentityMap(identityMap).orElse(false))
			.build();

		if (getDebugHeader(event).isPresent()) {
			_identityEventDebugService.updateIdentity(cmd);
		} else {
			_identityEventService.updateIdentity(cmd);
		}
	}

	private void onIdentityDeleted(EventHandlerContext ctx) {
		IdentityDeletedEvent event = ctx.getEvent().getContent(IdentityDeletedEvent.class);
		if (event.deletedIds != null) {
			for (String identityId : event.deletedIds) {
				DeleteIdentityCommand cmd = DeleteIdentityCommand.builder()
					.timestamp(ctx.getEvent().getTimestamp())
					.tenantId(getTenantId(ctx))
					.identityId(new IdentityId(identityId))
					.build();

				if (getDebugHeader(ctx.getEvent()).isPresent()) {
					_identityEventDebugService.deleteIdentity(cmd);
				} else {
					_identityEventService.deleteIdentity(cmd);
				}
			}
		}
	}

	/**
	 * Get TenantId from current RequestContext.
	 *
	 * @param ctx EventHandlerContext.
	 * @return TenantId.
	 */
	private TenantId getTenantId(EventHandlerContext ctx) {
		return RequestContext.get()
			.flatMap(RequestContext::getTenantId)
			.map(TenantId::new)
			.orElseThrow(() -> new IllegalStateException("No TenantId found for org " + ctx.getEvent().getHeader(EventHeaders.ORG)));
	}

	/**
	 * Reads the debug event header from input events to the Identity topic
	 *
	 * @param event the event
	 * @return Optional header
	 */
	private static Optional<String> getDebugHeader(Event event) {
		return Optional.ofNullable(event)
			.flatMap(e -> e.getHeader(DEBUG_EVENT_HEADER));
	}

	/**
	 * Utility Class holds lists of deleted ids.
	 */
	private static class IdentityDeletedEvent {
		List<String> deletedIds;
	}

	/**
	 * Helps publish Identity Gauge metrics
	 * @param tag the tag to use for the metric
	 * @param pod the pod
	 * @param val the value of metric
	 */
	private void publishIdentityGaugeMetrics(String tag, String pod, int val) {
		PrometheusMetricsUtil
			.gauge(SP_IDENTITY_EVENT_IDENTITY_GAUGE,
				SP_IDENTITY_EVENT_IDENTITY_GAUGE_HELP,
				Map.of(SP_IDENTITY_EVENT_IDENTITY_GAUGE_TAG_FOR_KEY, tag,
					SP_IDENTITY_EVENT_IDENTITY_GAUGE_TAG_FOR_POD, pod))
			.set(val);
	}
}
