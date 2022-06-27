/*
 * Copyright (c) 2021. SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.audit.service;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.services.athena.AmazonAthena;
import com.amazonaws.services.athena.AmazonAthenaClientBuilder;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class AthenaClientProvider implements Provider<AmazonAthena> {

    private static final int CLIENT_EXECUTION_TIMEOUT = 60000;

    @Override
    public AmazonAthena get() {
        return AmazonAthenaClientBuilder.standard()
                .withClientConfiguration(new ClientConfiguration().withClientExecutionTimeout(CLIENT_EXECUTION_TIMEOUT)).build();
    }
}
