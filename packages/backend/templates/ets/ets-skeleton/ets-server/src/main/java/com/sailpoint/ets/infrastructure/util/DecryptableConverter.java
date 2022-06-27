/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.infrastructure.util;

import com.sailpoint.atlas.util.AwsEncryptionServiceUtil;
import org.apache.commons.codec.binary.Base64;

import java.util.Map;

/**
 * Abstract class that provides decryption functionality for model mapper converters
 */
abstract class DecryptableConverter {

	public final static String SUBSCRIPTION_ID = "SubscriptionID";
	private final AwsEncryptionServiceUtil _awsEncryptionServiceUtil;

	DecryptableConverter(AwsEncryptionServiceUtil awsEncryptionServiceUtil) {
		_awsEncryptionServiceUtil = awsEncryptionServiceUtil;
	}

	/**
	 * Helper function for Decrypt string value
	 * @param encryptedValue encrypted value.
	 * @param encryptionContext encryptionContext used for encrypting httpConfig.
	 * @return decrypted string.
	 */
	public String decrypt(String encryptedValue, Map<String, String> encryptionContext) {
		return new String(_awsEncryptionServiceUtil
			.decryptDataWithoutCheckKey(Base64.decodeBase64(encryptedValue), encryptionContext));
	}
}
