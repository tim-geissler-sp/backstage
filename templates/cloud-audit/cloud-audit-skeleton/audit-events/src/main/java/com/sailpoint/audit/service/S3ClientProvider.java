/*
 * Copyright (c) 2021. SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.audit.service;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class S3ClientProvider implements Provider<AmazonS3> {

    public static final int CLIENT_EXECUTION_TIMEOUT = 60000;

    // Tuned by needs of: AuditEventService calling S3PersistenceManager + SyncCisToS3Worker CIS Sync + Bulk Export.
    // The AWS SDK had a DEFAULT_MAX_CONNECTIONS = 50, which was too little for our S3 IO worker pools during Sync.
    // 128 + 32 comes from the size of the s3io worker pool for bulk sync'ing plus 32 for real-time processing.
    public static final int MAX_CONCURRENT_S3_CONNECTIONS = 128 + 32;

    // From AWS's documents:
    //   By default, the SDK will attempt to reuse HTTP connections as long as possible. In failure situations where a
    //   connection is established to a server that has been brought out of service, having a finite TTL can help with
    //   application recovery. For example, setting a 15 minute TTL will ensure that even if you have a connection
    //   established to a server that is experiencing issues, you’ll reestablish a connection to a new server within
    //   15 minutes. ... Tuning this setting down (together with an appropriately-low setting for Java's DNS cache TTL)
    //   ensures that your application will quickly rotate over to new IP addresses when the service begins announcing
    //   them through DNS, at the cost of having to reestablish new connections more frequently. By default, it is
    //   set to {@code -1], i.e. connections do not expire.
    // Setting this to 2 minutes to support recovery of any kind and to allow open TCP sockets to loosely scale with
    // load being processed by the Java process.
    public static final int MAX_S3_CONNECTION_TTL = 120000;

    @Override
    public AmazonS3 get() {
        return AmazonS3ClientBuilder.standard()
                .withClientConfiguration(
                        new ClientConfiguration()
                                .withClientExecutionTimeout(CLIENT_EXECUTION_TIMEOUT)
                                .withMaxConnections(MAX_CONCURRENT_S3_CONNECTIONS)
                                .withConnectionTTL(MAX_S3_CONNECTION_TTL)
                ).build();
    }
}
