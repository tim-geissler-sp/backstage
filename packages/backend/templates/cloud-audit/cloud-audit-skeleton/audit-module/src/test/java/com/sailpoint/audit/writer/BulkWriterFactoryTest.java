package com.sailpoint.audit.writer;

import com.sailpoint.atlas.service.FeatureFlagService;
import com.sailpoint.mantis.core.service.ConfigService;
import com.sailpoint.mantis.platform.service.search.SyncTransformer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;

public class BulkWriterFactoryTest {

	@Mock
	ConfigService _configService;

	@Mock
	FeatureFlagService _featureFlagService;

	@Mock
	SyncTransformer _syncTransformer;

	BulkWriterFactory _bulkWriterFactory;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		_bulkWriterFactory = new BulkWriterFactory();
		_bulkWriterFactory._featureFlagService = _featureFlagService;
		_bulkWriterFactory._configService = _configService;
	}

	@Test
	public void testGetWriterSyncToSearch() throws IOException {
		BulkWriter bulkWriter = _bulkWriterFactory.getWriter(_syncTransformer, true);
		Assert.assertTrue(bulkWriter instanceof BulkSearchWriter);
	}

	@Test
	public void testGetWriterFirehose() throws IOException {
		BulkWriter bulkWriter = _bulkWriterFactory.getWriter(_syncTransformer, false);
		Assert.assertTrue(bulkWriter instanceof BulkFirehoseWriter);
	}
}
