/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.context.common.model;

import org.junit.Assert;
import org.junit.Test;

public class BrandConfigTest {

	@Test
	public void newBrandConfigTest() {
		BrandConfig config = new BrandConfig();
		config.setActionButtonColor("#efefef");
		config.setActiveLinkColor("#ffffff");
		config.setNavigationColor("#000000");
		config.setEmailFromAddress("no-reply@sailpoint.com");
		config.setName("name");
		config.setNarrowLogoURL("narrowLogoUrl");
		config.setProductName("Acme Solar");
		config.setStandardLogoURL("standardLogoUrl");

		Assert.assertEquals("#efefef", config.getActionButtonColor());
		Assert.assertEquals("#ffffff", config.getActiveLinkColor());
		Assert.assertEquals("#000000", config.getNavigationColor());
		Assert.assertEquals("no-reply@sailpoint.com", config.getEmailFromAddress());
		Assert.assertEquals("name", config.getName());
		Assert.assertEquals("narrowLogoUrl", config.getNarrowLogoURL());
		Assert.assertEquals("Acme Solar", config.getProductName());
		Assert.assertEquals("standardLogoUrl", config.getStandardLogoURL());
	}
}
