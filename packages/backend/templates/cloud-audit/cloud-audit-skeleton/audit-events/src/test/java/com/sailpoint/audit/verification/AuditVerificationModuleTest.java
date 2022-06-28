package com.sailpoint.audit.verification;

import com.amazonaws.services.sqs.AmazonSQS;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.sailpoint.atlas.AtlasConfig;
import com.sailpoint.atlas.featureflag.FeatureFlagService;
import com.sailpoint.atlas.service.provider.AtlasConfigProvider;
import com.sailpoint.atlas.util.GuiceUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AuditVerificationModuleTest {

	private Injector _injector;

	@Mock
	FeatureFlagService _featureFlagService;

	@Before
	public void setUp() throws Exception {
		_injector = Guice.createInjector(new AbstractModule() {
			@Override
			protected void configure() {
				bind(AtlasConfig.class).toProvider(AtlasConfigProvider.class);
				bind(FeatureFlagService.class).toInstance(_featureFlagService);
				install(new AuditVerificationModule());
			}
		});
	}

	@Test
	public void testInjector() throws Exception {
		Assert.assertTrue(GuiceUtil.hasBinding(_injector, AuditVerificationService.class));
		Assert.assertTrue(GuiceUtil.hasBinding(_injector, AmazonSQS.class));
	}
}
