/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.notification.context.service;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sailpoint.notification.sender.common.exception.persistence.StaleElementException;
import com.sailpoint.notification.context.common.model.GlobalContext;
import com.sailpoint.notification.context.common.repository.GlobalContextRepository;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

/**
 * GlobalContextService returns the global context as a Map of flattened attributes.
 */
@Singleton
public class GlobalContextService {

	public static final String BRANDING_CONFIGS = "brandingConfigs";
	public static final String PRODUCT_NAME = "productName";
	public static final String EMAIL_FROM_ADDRESS = "emailFromAddress";
	public static final String DEFAULT_PRODUCT_NAME = "IdentityNow";
	public static final String DEFAULT_EMAIL_FROM_ADDRESS = "no-reply@sailpoint.com";
	public static final String DEFAULT_BRAND = "default";
	public static final String EMAIL_OVERRIDE = "emailOverride";

	private final static Map<String, Object> DEFAULTS_ATTRIBUTES;

	static {
		DEFAULTS_ATTRIBUTES = ImmutableMap.of(PRODUCT_NAME,DEFAULT_PRODUCT_NAME,
				EMAIL_FROM_ADDRESS,DEFAULT_EMAIL_FROM_ADDRESS);
	}

	private GlobalContextRepository _globalContextRepository;

	private Log _log = LogFactory.getLog(GlobalContextService.class);

	public static final int MAX_ATTEMPTS = 2;

	@Inject
	@VisibleForTesting
	public GlobalContextService(GlobalContextRepository globalContextRepository) {
		_globalContextRepository = globalContextRepository;
	}

	public Optional<GlobalContext> findOneByTenant(String tenant) {
		requireNonNull(tenant);
		return _globalContextRepository.findOneByTenant(tenant);
	}

	public void deleteByTenant(String tenant) {
		requireNonNull(tenant);
		withRetries((gcr) -> gcr.deleteByTenant(tenant));
	}

	/**
	 * This will set any missing or null value default attributes
	 * @param attributes
	 */
	private void verifyDefaultAttributes(Map<String, Object> attributes) {
		DEFAULTS_ATTRIBUTES.entrySet().stream().forEach(defaultAttribute ->
				attributes.compute(defaultAttribute.getKey(), (k,v) -> v == null ? defaultAttribute.getValue() : v));
	}

	/**
	 * Returns the global context for the "default" brand.
	 * @param tenant the tenant
	 * @return the global context
	 */
	public Map<String, Object> getDefaultContext(String tenant) {
		return getContext(tenant, DEFAULT_BRAND);
	}

	/**
	 * Returns the global context for the given tenant and brand. If the brand is not found,
	 * it returns the "default" brand. If no brands are found then it returns some defaults.
	 * @param tenant the tenant
	 * @param brand the brand associated with the identity
	 * @return the global context
	 */
	public Map<String, Object> getContext(String tenant, String brand) {
		//get all from db
		Map<String, Object> attributes = new HashMap<>();
		Optional<GlobalContext> optional = findOneByTenant(tenant);
		if(optional.isPresent()) {
			GlobalContext gc = optional.get();
			if (gc.getAttributes() != null) {
				attributes.putAll(gc.getAttributes());
			}
		} else {
			return DEFAULTS_ATTRIBUTES; //nothing found use default
		}

		//search for brand provided
		if (attributes.containsKey(BRANDING_CONFIGS)) {
			Map<String, Object> brandingAttributes = (Map<String, Object>) attributes.get(BRANDING_CONFIGS);
			if (brandingAttributes != null) {

				if (brandingAttributes.get(brand) != null) {
					attributes.putAll((Map<String, Object>) brandingAttributes.get(brand));
				} else if (brandingAttributes.get(DEFAULT_BRAND) != null) {
					attributes.putAll((Map<String, Object>) brandingAttributes.get(DEFAULT_BRAND));
				}
				verifyDefaultAttributes(attributes);
				return attributes;
			}
		}
		//no brand found in db return default
		return DEFAULTS_ATTRIBUTES;
	}

	/**
	 * Saves the given attribute in the global context repository. This will overwrite, therefore should be used only if
	 * you are the authoritative source of the attribute
	 * @param tenant The tenant
	 * @param key the attribute key
	 * @param value the attribute value
	 */
	public void saveAttribute(String tenant, String key, Object value) {
		requireNonNull(tenant);
		requireNonNull(key);

		withRetries((gcr) -> {
			GlobalContext context = gcr.findOneByTenant(tenant).orElse(new GlobalContext(tenant));
			Map<String, Object> attributes = context.getAttributes() != null ? context.getAttributes() : new HashMap<>();
			attributes.put(key, value);
			context.setAttributes(attributes);
			gcr.save(context);
		});
	}

	/**
	 * Removes the given attribute from the global context repository.
	 * @param tenant The tenant
	 * @param key the attribute key
	 */
	public void removeAttribute(String tenant, String key) {
		requireNonNull(tenant);
		requireNonNull(key);

		withRetries((gcr) -> {
			GlobalContext context = gcr.findOneByTenant(tenant).orElse(new GlobalContext(tenant));
			Map<String, Object> attributes = context.getAttributes() != null ? context.getAttributes() : new HashMap<>();
			attributes.remove(key);
			context.setAttributes(attributes);
			gcr.save(context);
		});
	}

	/**
	 * Saves the given attributes in the global context repository. This will overwrite, therefore should be used only if
	 * you are the authoritative source of the attributes
	 * @param tenant tenant name.
	 * @param values attributes
	 */
	public void saveAttributes(String tenant, Map<String, Object> values) {
		requireNonNull(tenant);
		if (values == null) {
			return;
		}
		withRetries((gcr) -> {
			GlobalContext context = gcr.findOneByTenant(tenant).orElse(new GlobalContext(tenant));
			Map<String, Object> attributes = context.getAttributes() != null ? context.getAttributes() : new HashMap<>();
			attributes.putAll(values);
			context.setAttributes(attributes);
			gcr.save(context);
		});
	}

	/**
	 * The branding attributes are a nested map in the attributes under "brandingConfigs". The key will be the name of the brand config
	 * and the value will be a map of key value pairs.
	 */
	public void saveBrandingAttributes(String tenant, Map<String, Object> values) {
		requireNonNull(tenant);
		withRetries((gcr) -> {
			GlobalContext context = gcr.findOneByTenant(tenant).orElse(new GlobalContext(tenant));
			Map<String, Object> attributes = context.getAttributes() != null ? context.getAttributes() : new HashMap<>();
			Map<String, Object> brandingAttributes = attributes.get(BRANDING_CONFIGS) != null ? (Map<String, Object>) attributes.get(BRANDING_CONFIGS) : new HashMap();
			brandingAttributes.putAll(values);
			attributes.put(BRANDING_CONFIGS, brandingAttributes);
			context.setAttributes(attributes);
			gcr.save(context);
		});
	}

	public void save(GlobalContext globalContext) throws StaleElementException {
		requireNonNull(globalContext);
		_globalContextRepository.save(globalContext);
	}

	public void deleteBranding(String tenant, String name) {
		requireNonNull(tenant);
		withRetries((gcr) -> {
			Optional<GlobalContext> contextOptional = _globalContextRepository.findOneByTenant(tenant);

			if(!contextOptional.isPresent()) {
				return;
			}

			GlobalContext context = contextOptional.get();
			if(context.getAttributes() == null) {
				return;
			}

			Map<String, Object> attributes = context.getAttributes();
			Map<String, Object> brandingAttributes = (Map<String, Object>)attributes.get(BRANDING_CONFIGS);
			if(brandingAttributes == null) {
				return;
			}

			if(!brandingAttributes.containsKey(name)) {
				return;
			}

			brandingAttributes.remove(name);
			attributes.put(BRANDING_CONFIGS, brandingAttributes);
			context.setAttributes(attributes);
			 gcr.save(context);
		});
	}

	private void withRetries(Consumer<GlobalContextRepository> consumer) {
		int attempt = 0;
		do {
			_log.info("Updating global context. Attempt: " + ++attempt);
			try {
				consumer.accept(_globalContextRepository);
				return;
			} catch (StaleElementException e) {
				_log.warn("Update attempt: " + attempt + " failed", e);
			}
		} while (attempt < MAX_ATTEMPTS);
		throw new IllegalStateException("Reached max retries: " + MAX_ATTEMPTS);
	}
}
