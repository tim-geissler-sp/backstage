/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.audit;

import com.amazonaws.services.dynamodbv2.document.Item;
import com.sailpoint.atlas.AtlasConfig;
import com.sailpoint.atlas.OrgData;
import com.sailpoint.mantis.platform.db.MantisDynamoOrgDataProvider;

public class AuditOrgDataProvider extends MantisDynamoOrgDataProvider {
	public AuditOrgDataProvider(AtlasConfig config) {
		super(config);
	}

	/**
	 * {@inheritDoc}
	 */

	@Override
	protected void customizeOrgData(Item org, OrgData orgData) {
		super.customizeOrgData(org, orgData);

		orgData.setAttribute("auditFirehose", org.getString("audit_firehose"));
		orgData.setAttribute("auditS3Bucket", org.getString("audit_bucket"));
	}

}
