/*
 * Copyright (c) 2020. SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.notification.sender.email.impl;

import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityRequest;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityResult;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.sailpoint.atlas.AtlasConfig;
import com.sailpoint.atlas.featureflag.FeatureFlagService;
import com.sailpoint.notification.sender.email.MailClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;

import java.util.Optional;

/**
 * SESMailClientProvider provides a MailClient.
 */
@RequiredArgsConstructor(onConstructor_={@Inject})
@CommonsLog
public class SESMailClientProvider implements Provider<MailClient> {

	private final AtlasConfig _atlasConfig;

	private final FeatureFlagService _featureFlagService;

	/**
	 * Environment variable name for specifying the region for SES client.
	 */
	private static final String AWS_SES_REGION = "AWS_SES_REGION";

	private static final String AWS_SES_PARTITION = "AWS_SES_PARTITION";

	/**
	 * Environment variable name for specifying SOURCE ARN
	 */
	private static final String AWS_SES_SOURCE_ARN = "AWS_SES_SOURCE_ARN";

	private static final String DEFAULT_AWS_SES_PARTITION_VALUE = "aws";

	private static final String DEFAULT_AWS_SES_REGION_VALUE = "us-east-1";

	private static final String DEFAULT_AWS_SES_SOURCE_ARN = "arn:%s:ses:%s:%s:identity/sailpoint.com";

	@Override
	public MailClient get() {
		String sesRegion = _atlasConfig.getString(AWS_SES_REGION, DEFAULT_AWS_SES_REGION_VALUE);
		String sesPartition = _atlasConfig.getString(AWS_SES_PARTITION, DEFAULT_AWS_SES_PARTITION_VALUE);
		Optional<String> sourceArn = getSourceArn(sesRegion, sesPartition);
		SESMailClient mailClient = new SESMailClient(
				AmazonSimpleEmailServiceClientBuilder.standard()
						.withRegion(sesRegion)
						.build(),
				_featureFlagService,
				sourceArn);
		return mailClient;
	}

	/**
	 * Gets the sourceArn to be used in sendMail requests for no-reply@sailpoint.com from address.
	 *
	 * @param sesRegion The region where sourceArn is configured.
	 * @return
	 */
	private Optional<String> getSourceArn(String sesRegion, String sesPartition) {
		if (_atlasConfig.hasProperty(AWS_SES_SOURCE_ARN)) {
			return Optional.ofNullable(_atlasConfig.getString(AWS_SES_SOURCE_ARN));
		} else {
			try {
				AWSSecurityTokenService securityTokenService = AWSSecurityTokenServiceClientBuilder.defaultClient();
				GetCallerIdentityResult result = securityTokenService.getCallerIdentity(new GetCallerIdentityRequest());
				String accountId = result.getAccount();
				return Optional.of(String.format(DEFAULT_AWS_SES_SOURCE_ARN, sesPartition, sesRegion, accountId));
			} catch (Exception e) {
				log.error("Failed to create default source ARN", e);
			}
		}

		return Optional.empty();
	}
}
