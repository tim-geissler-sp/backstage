/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.context.event;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sailpoint.atlas.RequestContext;
import com.sailpoint.atlas.discovery.ServiceLocator;
import com.sailpoint.atlas.idn.IdnOrgData;
import com.sailpoint.atlas.idn.RestClientProvider;
import com.sailpoint.atlas.idn.ServiceNames;
import com.sailpoint.utilities.JsonUtil;
import com.sailpoint.iris.client.Event;
import com.sailpoint.iris.client.EventHeaders;
import com.sailpoint.iris.server.EventHandler;
import com.sailpoint.iris.server.EventHandlerContext;
import com.sailpoint.mantisclient.BaseRestClient;
import com.sailpoint.notification.api.event.EventType;
import com.sailpoint.notification.context.common.model.BrandConfig;
import com.sailpoint.notification.context.common.model.GlobalContext;
import com.sailpoint.notification.context.common.util.BrandConfigMapper;
import com.sailpoint.notification.context.dto.ListResult;
import com.sailpoint.notification.context.service.GlobalContextService;
import com.sailpoint.utilities.JsonParameterizedType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.lang.reflect.Type;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 *  Event handler for ORG_CREATED and ORG_UPGRADED event for the purpose of seeding and updating vanity domain
 *  Event handler for ORG_DELETED for the purpose of deleting ALL global context
 */
@Singleton
public class OrgLifecycleEventHandler implements EventHandler {

	public static final String PRODUCT_URL = "productUrl";

	private Log _log = LogFactory.getLog(OrgLifecycleEventHandler.class);

	private GlobalContextService _globalContextService;

	private ServiceLocator _serviceLocator;

	private RestClientProvider _restClientProvider;

	private static final String BRANDING_LIST = "/branding/branding";


	@Inject
	OrgLifecycleEventHandler(GlobalContextService globalContextService, ServiceLocator serviceLocator, RestClientProvider restClientProvider) {
		_globalContextService = globalContextService;
		_serviceLocator = serviceLocator;
		_restClientProvider = restClientProvider;
	}

	@VisibleForTesting
	OrgLifecycleEventHandler(GlobalContextService globalContextService, ServiceLocator serviceLocator, RestClientProvider restClientProvider, Log log) {
		this(globalContextService, serviceLocator, restClientProvider);
		_log = log;
	}

	@Override
	public void handleEvent(EventHandlerContext eventHandlerContext) {
		Event event = eventHandlerContext.getEvent();
		_log.info("Handling " + event.getType());
		final String org = requireNonNull(event.getHeader(EventHeaders.ORG).get());

		switch (event.getType()) {
			case EventType.ORG_DELETED:
				handleDelete(org);
				break;
			case EventType.ORG_CREATED:
				handleCreateOrUpgrade(org, EventType.ORG_CREATED);
				break;
			case EventType.ORG_UPGRADED:
				handleCreateOrUpgrade(org, EventType.ORG_UPGRADED);
				break;
		}
	}

	private void handleDelete(String org) {
		_globalContextService.deleteByTenant(org);
	}

	private void handleCreateOrUpgrade(String org, String eventType) {
		reconcileProductURL(org, eventType);
		reconcileBrandingConfiguration(org);
	}

	/**
	 * Gets the vanity domain from the org data provider
	 * @return the optional vanity domain
	 */
	private Optional<String> getProductURLFromOrgData() {
		RequestContext rc = RequestContext.ensureGet();
		Optional<String> toReturn = IdnOrgData.getVanityDomain(rc.getOrgData());
		if (toReturn.isPresent()) {
			URI uri = URI.create(toReturn.get());
			if (uri.getScheme() == null) {
				toReturn = Optional.of(addSchemeIfMissing(toReturn.get()));
			}
		}
		_log.info("Product URL from Org Data:" + toReturn);
		return toReturn;
	}

	private String getProductURLFromCCServiceLocator(String org) {
		String toReturn = addSchemeIfMissing(_serviceLocator.ensureFind(org, ServiceNames.CC).getRawUrl()) + "/" + org;
		_log.info("Product URL from CC Service Locator:" + toReturn);
		return toReturn;
	}

	private String addSchemeIfMissing(String str) {
		if (str != null) {
			URI uri = URI.create(str);
			if (uri.getScheme() == null) {
				return "https://" + str;
			}
			return str;
		}
		return null;
	}

	/**
	 * Running this method will overwrite the product URL
	 */
	public void reconcileProductURL(String org, String eventType) {
		//Get the vanity domain from org data provider. The vanity domain can be empty in which case use the
		//service locator. Note: If there is no org data for the org, something is very wrong and we won't
		//fall back to the service locator.
		String vanityDomain = getProductURLFromOrgData()
				.orElseGet(() -> getProductURLFromCCServiceLocator(org));

		//Look for existing global context for the tenant. If it already exists, raise a warning.
		GlobalContext globalContext = _globalContextService.findOneByTenant(org).orElse(null);

		if (globalContext != null && eventType.equals("ORG_CREATED")) {
			_log.warn("Global context already exists for tenant " + org + ". Attempting to overwrite vanity domain: " + vanityDomain);
		}

		_globalContextService.saveAttribute(org, PRODUCT_URL, vanityDomain);
	}

	/**
	 * Running this method will overwrite any existing branding
	 */
	@VisibleForTesting
	public void reconcileBrandingConfiguration(String org) {
		try {
			BaseRestClient client = _restClientProvider.getInternalRestClient("PIGS");
			Type type = new JsonParameterizedType<>(ListResult.class, BrandConfig.class);
			ListResult<BrandConfig> result = JsonUtil.parse(type, client.get(BRANDING_LIST));

			if (result != null && result.getItems() != null) {
				List<BrandConfig> brandConfigList = result.getItems();

				Map toSave = brandConfigList.stream().map(BrandConfigMapper::brandingConfigToMap).flatMap(e -> e.entrySet().stream())
						.collect(Collectors.toMap(
								entry -> entry.getKey(),
								entry -> entry.getValue()
						));
				_globalContextService.saveBrandingAttributes(org, toSave);
				_log.info("Reconciling branding attributes: " + toSave.toString());
			}
		} catch (Exception e) {
			_log.error("Reconciling branding attributes failed", e);
		}
	}
}
