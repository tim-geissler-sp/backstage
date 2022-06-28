/*
 * Copyright (C) 2022 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.audit.verification;

import com.google.gson.annotations.JsonAdapter;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.util.Date;
import java.util.List;

@Builder
@Data
/**
 * Class encapsulating a request to verify an audit's presence in various targets
 */
public final class AuditVerificationRequest {
	@NonNull
	/**
	 * The id of the audit
	 */
	private String _id;

	@NonNull
	/**
	 * The id of the tenant the audit is for
	 */
	private String _tenantId;

	@NonNull
	/**
	 * The name of the pod for the org.
	 */
	private String _pod;

	@NonNull
	/**
	 * The name of the org the audit is for
	 */
	private String _org;

	@NonNull
	/**
	 * List of targets to verify the audits presence in
	 */
	private List<VerificationTarget> _verifyIn;

	@NonNull
	@JsonAdapter(IsoOffsetDateTimeAdapter.class)
	/**
	 * Created timestamp
	 */
	private Date _created;

	/**
	 * Available verification targets
	 */
	public enum VerificationTarget {
		S3,
		SEARCH
	}
}
