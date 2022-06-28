/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.notification.context.service;

import com.sailpoint.notification.sender.common.exception.persistence.StaleElementException;
import com.sailpoint.notification.context.common.model.GlobalContext;
import com.sailpoint.notification.context.common.repository.GlobalContextRepository;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.sailpoint.notification.context.event.OrgLifecycleEventHandler.PRODUCT_URL;
import static com.sailpoint.notification.context.service.GlobalContextService.BRANDING_CONFIGS;
import static com.sailpoint.notification.context.service.GlobalContextService.DEFAULT_EMAIL_FROM_ADDRESS;
import static com.sailpoint.notification.context.service.GlobalContextService.EMAIL_FROM_ADDRESS;
import static com.sailpoint.notification.context.service.GlobalContextService.MAX_ATTEMPTS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for GlobalContextService
 */
@RunWith(MockitoJUnitRunner.class)
public class GlobalContextServiceTest {

	private GlobalContextService _globalContextService;

	@Mock
	GlobalContextRepository _globalContextRepository;

	@Before
	public void setup() {
		_globalContextService = new GlobalContextService(_globalContextRepository);
	}

	@Test(expected = StaleElementException.class)
	public void saveFailure() {
		Mockito.doThrow(StaleElementException.class).when(_globalContextRepository).save(any());

		_globalContextService.save(new GlobalContext("acme-solar"));
		verify(_globalContextRepository, times(MAX_ATTEMPTS)).save(any());
	}

	@Test
	public void saveSuccess() {
		Mockito.doNothing().when(_globalContextRepository)
				.save(any());

		_globalContextService.save(new GlobalContext("acme-solar"));

		verify(_globalContextRepository, times(1)).save(any());
	}

	@Test(expected = IllegalStateException.class)
	public void deleteLockingFailure() {
		Mockito.doThrow(StaleElementException.class)
				.when(_globalContextRepository)
				.deleteByTenant(any());

		_globalContextService.deleteByTenant("acme-solar");
		verify(_globalContextRepository, times(MAX_ATTEMPTS)).deleteByTenant(any());

	}

	@Test
	public void deleteValidFailure() {
		Mockito.doThrow(StaleElementException.class)
				.doReturn(false)
				.when(_globalContextRepository)
				.deleteByTenant(any());

		_globalContextService.deleteByTenant("acme-solar");
		verify(_globalContextRepository, times(2)).deleteByTenant(any());
	}

	@Test
	public void deleteValidSuccess() {
		Mockito.doReturn(true)
				.when(_globalContextRepository)
				.deleteByTenant(any());

		_globalContextService.deleteByTenant("acme-solar");
		verify(_globalContextRepository, times(1)).deleteByTenant(any());
	}

	@Test
	public void find() {
		Mockito.doReturn(Optional.of(new GlobalContext("acme-solar")))
				.when(_globalContextRepository)
				.findOneByTenant(any());

		Assert.assertNotNull(_globalContextService.findOneByTenant("acme-solar"));
		verify(_globalContextRepository, times(1)).findOneByTenant(any());
	}

	@Test
	public void findEmpty() {
		Mockito.doReturn(Optional.empty())
				.when(_globalContextRepository)
				.findOneByTenant(any());

		Assert.assertNotNull(_globalContextService.findOneByTenant("acme-solar"));
		verify(_globalContextRepository, times(1)).findOneByTenant(any());
	}

	@Test
	public void saveAttribute() {
		Mockito.doReturn(Optional.of(new GlobalContext("acme-solar")))
				.when(_globalContextRepository)
				.findOneByTenant(any());
		Mockito.doThrow(StaleElementException.class)
				.doNothing()
				.when(_globalContextRepository)
				.save(any());

		_globalContextService.saveAttribute("acme-solar", "key", "value");
		verify(_globalContextRepository, times(2)).findOneByTenant(any());
		verify(_globalContextRepository, times(2)).save(any());
	}


	@Test(expected = IllegalStateException.class)
	public void saveAttributeFailure() {
		Mockito.doReturn(Optional.of(new GlobalContext("acme-solar")))
				.when(_globalContextRepository)
				.findOneByTenant(any());
		Mockito.doThrow(StaleElementException.class)
				.when(_globalContextRepository)
				.save(any());

		_globalContextService.saveAttribute("acme-solar", "key", "value");
		verify(_globalContextRepository, times(MAX_ATTEMPTS)).findOneByTenant(any());
		verify(_globalContextRepository, times(MAX_ATTEMPTS)).save(any());
	}

	@Test
	public void saveAttributes() {
		Mockito.doReturn(Optional.of(new GlobalContext("acme-solar")))
				.when(_globalContextRepository)
				.findOneByTenant(any());
		Mockito.doThrow(StaleElementException.class)
				.doNothing()
				.when(_globalContextRepository)
				.save(any());

		_globalContextService.saveAttributes("acme-solar", new HashMap<>());
		verify(_globalContextRepository, times(2)).findOneByTenant(any());
		verify(_globalContextRepository, times(2)).save(any());
	}

	@Test
	public void saveAttributesNull() {
		_globalContextService.saveAttributes("acme-solar", null);
		verify(_globalContextRepository, times(0)).findOneByTenant(any());
		verify(_globalContextRepository, times(0)).save(any());
	}

	@Test
	public void saveBrandingAttributes() {
		Mockito.doReturn(Optional.of(new GlobalContext("acme-solar")))
				.when(_globalContextRepository)
				.findOneByTenant(any());
		Mockito.doNothing()
				.when(_globalContextRepository)
				.save(any());

		_globalContextService.saveBrandingAttributes("acme-solar", new HashMap<>());
		verify(_globalContextRepository, times(1)).findOneByTenant(any());
		verify(_globalContextRepository, times(1)).save(any());
	}

	@Test
	public void getAttributesFlattened() {
		Map<String, Object> brand1 = new HashMap<>();
		brand1.put("productName", "Acme Polar Express");
		Map<String, Object> brands = new HashMap<>();
		brands.put("brand1", brand1);
		Map<String, Object> attributes = new HashMap<>();
		attributes.put("productUrl", "http://localhost");
		attributes.put(BRANDING_CONFIGS, brands);
		GlobalContext context = new GlobalContext("acme-solar");
		context.setAttributes(attributes);

		Mockito.doReturn(Optional.of(context))
				.when(_globalContextRepository)
				.findOneByTenant(any());

		Map<String, Object> flatAttributes = _globalContextService.getContext("acme-solar", "brand1");
		Assert.assertNotNull(flatAttributes);
		assertEquals("Acme Polar Express", flatAttributes.get("productName"));
		assertEquals("http://localhost",  flatAttributes.get("productUrl"));

		verify(_globalContextRepository, times(1)).findOneByTenant(any());

		Mockito.doReturn(Optional.empty())
				.when(_globalContextRepository)
				.findOneByTenant(any());

		flatAttributes = _globalContextService.getContext("acme-solar", "brand2");
		Assert.assertNotNull(flatAttributes);
		assertEquals("IdentityNow",  flatAttributes.get("productName"));
	}

	@Test
	public void defaultBrand() {

		//should return hardcoded brand values
		Map<String, Object> brands = new HashMap<>();
		Map<String, Object> attributes = new HashMap<>();
		attributes.put(BRANDING_CONFIGS, brands);
		attributes.put(PRODUCT_URL, "http://localhost");
		GlobalContext context = new GlobalContext("acme-solar");
		context.setAttributes(attributes);

		Mockito.doReturn(Optional.of(context))
				.when(_globalContextRepository)
				.findOneByTenant(any());

		Map<String, Object> allAttributes = _globalContextService.getDefaultContext("acme-solar");
		assertEquals(4, allAttributes.size());
		assertEquals("IdentityNow",  allAttributes.get("productName"));
		assertEquals("http://localhost", allAttributes.get(PRODUCT_URL));

		//test db default brand
		Map<String, Object> brand1 = new HashMap<>();
		brand1.put("productName", "Acme Polar Express");
		Map<String, Object> defaultBrand = new HashMap<>();
		defaultBrand.put("productName", "Acme Polar Express Default");
		defaultBrand.put("emailFromAddress", "no-replay@acme.com");
		brands = new HashMap<>();
		brands.put("brand1", brand1);
		brands.put("default", defaultBrand);

		attributes = new HashMap<>();
		attributes.put(BRANDING_CONFIGS, brands);
		context = new GlobalContext("acme-solar");
		context.setAttributes(attributes);

		Mockito.doReturn(Optional.of(context))
				.when(_globalContextRepository)
				.findOneByTenant(any());

		//should return value for existing brand
		allAttributes = _globalContextService.getContext("acme-solar", "brand1");
		//the existing brand did not have a emailFromAddress but it is set to the default because it is required
		assertEquals(3, allAttributes.size());
		assertTrue(allAttributes.get(EMAIL_FROM_ADDRESS).equals(DEFAULT_EMAIL_FROM_ADDRESS));
		//should return value for db default brand
		allAttributes = _globalContextService.getContext("acme-solar", "brand2");
		assertEquals(3, allAttributes.size());
		assertEquals("Acme Polar Express Default",  allAttributes.get("productName"));
		assertEquals("no-replay@acme.com",  allAttributes.get("emailFromAddress"));
	}

	@Test
	public void deleteBrand() {
		Map<String, Object> brand1 = new HashMap<>();
		brand1.put("productName", "Acme Polar Express");
		Map<String, Object> brands = new HashMap<>();
		brands.put("brand1", brand1);
		Map<String, Object> attributes = new HashMap<>();
		attributes.put(BRANDING_CONFIGS, brands);
		GlobalContext context = new GlobalContext("acme-solar");
		context.setAttributes(attributes);

		Mockito.doReturn(Optional.of(context))
				.when(_globalContextRepository)
				.findOneByTenant(any());

		_globalContextService.deleteBranding("acme-solar", "brand1");

		verify(_globalContextRepository, times(1)).findOneByTenant(any());
		verify(_globalContextRepository, times(1)).save(any());

		brand1 = new HashMap<>();
		brand1.put("productName", "Acme Polar Express");
		brands = new HashMap<>();
		brands.put("brand2", brand1);
		attributes = new HashMap<>();
		attributes.put(BRANDING_CONFIGS, brands);
		context = new GlobalContext("acme-solar");
		context.setAttributes(attributes);

		Mockito.doReturn(Optional.of(context))
				.when(_globalContextRepository)
				.findOneByTenant(any());

		_globalContextService.deleteBranding("acme-solar", "brand1");

		verify(_globalContextRepository, times(2)).findOneByTenant(any());
		verify(_globalContextRepository, times(1)).save(any());

		attributes = new HashMap<>();
		context = new GlobalContext("acme-solar");
		context.setAttributes(attributes);

		Mockito.doReturn(Optional.of(context))
				.when(_globalContextRepository)
				.findOneByTenant(any());

		_globalContextService.deleteBranding("acme-solar", "brand1");

		verify(_globalContextRepository, times(3)).findOneByTenant(any());
		verify(_globalContextRepository, times(1)).save(any());

		context = new GlobalContext("acme-solar");

		Mockito.doReturn(Optional.of(context))
				.when(_globalContextRepository)
				.findOneByTenant(any());

		_globalContextService.deleteBranding("acme-solar", "brand1");

		verify(_globalContextRepository, times(4)).findOneByTenant(any());
		verify(_globalContextRepository, times(1)).save(any());

		Mockito.doReturn(Optional.empty())
				.when(_globalContextRepository)
				.findOneByTenant(any());

		_globalContextService.deleteBranding("acme-solar", "brand1");

		verify(_globalContextRepository, times(5)).findOneByTenant(any());
		verify(_globalContextRepository, times(1)).save(any());
	}

	@Test
	public void getAttributesFlattenedWithDefaultAttributes() {
		GlobalContext context = new GlobalContext("acme-solar");
		context.setAttributes(null);

		Mockito.doReturn(Optional.of(context))
				.when(_globalContextRepository)
				.findOneByTenant(any());
		Mockito.doReturn(Optional.of(context))
				.when(_globalContextRepository)
				.findOneByTenant(any());

		//no brand found should return default
		Map<String, Object> flatAttributes = _globalContextService.getContext("acme-solar", "brand1");
		Assert.assertNotNull(flatAttributes);
		assertEquals("IdentityNow",  flatAttributes.get("productName"));
	}

	@Test
	public void getAttributesFlattenedWithEmptyAttributes() {
		GlobalContext context = new GlobalContext("acme-solar");
		context.setAttributes(new HashMap<>());

		Mockito.doReturn(Optional.of(context))
				.when(_globalContextRepository)
				.findOneByTenant(any());
		Mockito.doReturn(Optional.of(context))
				.when(_globalContextRepository)
				.findOneByTenant(any());

		Map<String, Object> flatAttributes = _globalContextService.getContext("acme-solar", "brand1");
		Assert.assertNotNull(flatAttributes);
		assertEquals("IdentityNow",  flatAttributes.get("productName"));
	}

	@Test
	public void getAttributesFlattenedWithNoBranding() {
		Map<String, Object> attributes = new HashMap<>();
		attributes.put(BRANDING_CONFIGS, null);
		GlobalContext context = new GlobalContext("acme-solar");
		context.setAttributes(attributes);

		Mockito.doReturn(Optional.of(context))
				.when(_globalContextRepository)
				.findOneByTenant(any());
		Mockito.doReturn(Optional.of(context))
				.when(_globalContextRepository)
				.findOneByTenant(any());

		Map<String, Object> flatAttributes = _globalContextService.getContext("acme-solar", "brand1");
		Assert.assertNotNull(flatAttributes);
	}

	@Test
	public void getAttributesFlattenedWithNoBrandingWithDefault() {
		Map<String, Object> brands = new HashMap<>();
		Map<String, Object> defaultBrand = new HashMap<>();
		defaultBrand.put("productName", "Acme Solar");
		defaultBrand.put("emailFromAddress", "no-replay@acme.com");
		brands.put("default", defaultBrand);

		Map<String, Object> attributes = new HashMap<>();
		attributes.put(BRANDING_CONFIGS, brands);
		GlobalContext context = new GlobalContext("acme-solar");
		context.setAttributes(attributes);

		Mockito.doReturn(Optional.empty())
				.when(_globalContextRepository)
				.findOneByTenant(Matchers.contains("acme"));

		Map<String, Object> flatAttributes = _globalContextService.getDefaultContext("acme");
		Assert.assertNotNull(flatAttributes);

		Mockito.doReturn(Optional.of(context))
				.when(_globalContextRepository)
				.findOneByTenant(Matchers.contains("acme-solar"));

		flatAttributes = _globalContextService.getDefaultContext("acme-solar");
		Assert.assertNotNull(flatAttributes);
		assertEquals("Acme Solar",  flatAttributes.get("productName"));
		assertEquals("no-replay@acme.com",  flatAttributes.get("emailFromAddress"));
	}

	@Test
	public void getAttributesFlattenedWithNullBranding() {
		Map<String, Object> brands = new HashMap<>();
		brands.put("brand1", null);
		Map<String, Object> attributes = new HashMap<>();
		attributes.put(BRANDING_CONFIGS, brands);
		GlobalContext context = new GlobalContext("acme-solar");
		context.setAttributes(attributes);

		Mockito.doReturn(Optional.of(context))
				.when(_globalContextRepository)
				.findOneByTenant(any());
		Mockito.doReturn(Optional.of(context))
				.when(_globalContextRepository)
				.findOneByTenant(any());

		Map<String, Object> flatAttributes = _globalContextService.getContext("acme-solar", "brand1");
		Assert.assertNotNull(flatAttributes);
	}

	@Test
	public void removeAttribute() {
		Map<String, Object> attributes = new HashMap<>();
		attributes.put("test", "123");
		attributes.put("rest", "124");
		GlobalContext context = new GlobalContext("acme-solar");
		context.setAttributes(attributes);

		ArgumentCaptor<GlobalContext> globalContextArgumentCaptor = ArgumentCaptor.forClass(GlobalContext.class);

		Mockito.doReturn(Optional.of(context))
				.when(_globalContextRepository)
				.findOneByTenant(anyString());
		Mockito.doNothing()
				.when(_globalContextRepository)
				.save(globalContextArgumentCaptor.capture());
		_globalContextService.removeAttribute("acme-solar", "test");

		Map<String, Object> output = globalContextArgumentCaptor.getValue().getAttributes();
		assertEquals(1, output.size());
		assertTrue(attributes.containsKey("rest"));
	}
}
