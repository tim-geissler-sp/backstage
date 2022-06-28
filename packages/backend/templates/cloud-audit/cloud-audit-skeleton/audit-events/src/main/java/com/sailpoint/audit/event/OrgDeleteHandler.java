/*
 * Copyright (C) 2019 SailPoint Technologies, Inc.  All rights reserved.
 */

package com.sailpoint.audit.event;

import com.google.inject.Inject;
import com.sailpoint.atlas.AtlasConfig;
import com.sailpoint.atlas.RequestContext;
import com.sailpoint.atlas.service.FeatureFlagService;
import com.sailpoint.atlas.service.RemoteFileService;
import com.sailpoint.audit.persistence.S3DeletionManager;
import com.sailpoint.audit.persistence.S3PersistenceManager;
import com.sailpoint.audit.service.DataCatalogService;
import com.sailpoint.audit.service.DeletedOrgsCacheService;
import com.sailpoint.audit.service.FeatureFlags;
import com.sailpoint.audit.service.util.AuditUtil;
import com.sailpoint.iris.server.EventHandler;
import com.sailpoint.iris.server.EventHandlerContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.inject.Named;

public class OrgDeleteHandler implements EventHandler {

	private static Log _log = LogFactory.getLog(OrgDeleteHandler.class);

	@Inject
	DeletedOrgsCacheService _deletedOrgsCache;

	@Inject @Named("Athena")
	DataCatalogService _athenaService;

	@Inject
	FeatureFlagService _featureFlagService;

	@Inject
	AtlasConfig _atlasConfig;

	@Inject
	RemoteFileService _s3FileService;

	@Override
	public void handleEvent(EventHandlerContext eventHandlerContext) throws InterruptedException {

		RequestContext.ensureGet().getTenantId().ifPresent(_deletedOrgsCache::cacheDeletedOrg);

		RequestContext.ensureGet().getTenantId().ifPresent((tenantId) -> {
			_log.info("Deleting S3 persisted Audit Event records for tenantId:" + tenantId);
			S3DeletionManager.queueTenantForDeletion(tenantId); // PLTDP-1495: decouple Org delete from Kafka.
		});

		if( _featureFlagService.getBoolean(FeatureFlags.WRITE_AUDIT_DATA_IN_PARQUET, false) ) {
			String orgName = RequestContext.ensureGet().getOrg();
			_athenaService.deleteTable(_atlasConfig.getString("AER_AUDIT_ATHENA_DATABASE"),
					AuditUtil.getOrgAuditAthenaTableName(orgName),
					_atlasConfig.getString("AER_AUDIT_PARQUET_DATA_S3_BUCKET"));

			String[] prefixStrings = {"parquet/org="+orgName};

			_s3FileService.deleteMultipleObjects(_atlasConfig.getString("AER_AUDIT_PARQUET_DATA_S3_BUCKET"),
					prefixStrings, AuditUtil.getCurrentRegion(_atlasConfig.getAwsRegion().getName()));
		}
	}
}
