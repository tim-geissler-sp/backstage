package com.sailpoint.audit.message;

import com.sailpoint.atlas.OrgData;

/**
 * Simple payload to allow HTTP POSTs to request individual orgs get CIS->S3 bulk sync'ed.
 */
public class CisToS3Payload {

	// The orgData for the org to bulk-sync from CIS to S3.
	String orgName;

	public String getOrgName() {
		return orgName;
	}

	public void setOrgName(String orgName) {
		this.orgName = orgName;
	}

}
