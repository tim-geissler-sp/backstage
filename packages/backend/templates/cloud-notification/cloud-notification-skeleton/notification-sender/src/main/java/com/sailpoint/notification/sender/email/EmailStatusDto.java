/*
 * Copyright (c) 2020. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.sender.email;

import com.sailpoint.cloud.api.client.model.BaseDto;
import com.sailpoint.notification.sender.email.dto.VerificationStatus;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Dto for email sender status
 */
@Data
@Builder(toBuilder = true)
@EqualsAndHashCode(callSuper = false)
public class EmailStatusDto extends BaseDto {

	private String _id;
	private String _email;
	private VerificationStatus _verificationStatus;

}
