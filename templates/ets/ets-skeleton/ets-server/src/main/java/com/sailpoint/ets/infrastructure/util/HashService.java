/*
 * Copyright (C) 2019 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.ets.infrastructure.util;

import com.sailpoint.atlas.boot.core.AtlasBootCoreProperties;
import com.sailpoint.atlas.jwt.JwtSigningKey;
import com.sailpoint.atlas.util.AwsParameterStoreUtil;
import com.sailpoint.atlas.util.HashingUtil;
import com.sailpoint.ets.infrastructure.hibernate.SecretJavaDescriptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;


/**
 * Utility class used for hashing in database.
 */
@Component
@CommonsLog
@RequiredArgsConstructor(onConstructor_=@Autowired)
public class HashService {
	private static final Log _log = LogFactory.getLog(HashService.class);

	private final AtlasBootCoreProperties _atlasBootCoreProperties;

	private JwtSigningKey _jwtSigningKey;

	@PostConstruct
	public void init() {
		try {
			_jwtSigningKey = JwtSigningKey.fromAwsParameterStore(new AwsParameterStoreUtil(),
				_atlasBootCoreProperties.getJwtKeyAwsParamName(), _atlasBootCoreProperties.getJwtKey());
			SecretJavaDescriptor.initHashService(this);
		}
		catch (Exception e) {
			_log.error(String.format("%s: %s", "HashService initialization error", e.getMessage()));
		}
	}

    /**
     * Return hash.
     *
     * @return hashed secret.
     */
    public String encode(String secret) {
        return HashingUtil.getHashString(_jwtSigningKey.getBytes(), secret.getBytes());
    }

	/**
	 * Match secret with hash.
	 * @param hash hash.
	 * @param secret secret.
	 * @return true if match.
	 */
	public boolean matches(String hash, String secret) {
		if (hash == null || secret == null) {
			return false;
		}

		return HashingUtil.getHashString(_jwtSigningKey.getBytes(),
			secret.getBytes()).equals(hash);
	}
}
