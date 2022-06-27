/*
 * Copyright (C) 2020 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.sp.identity.event;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;


/**
 * Service Properties
 */
@Data
@ConfigurationProperties(prefix="identity.event")
public class IdentityEventProperties {

	private String identityStateBucketName;
	private String identityStateDynamoTable;

}
