/*
 * Copyright (c) 2022. SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.audit.util;

/**
 * Will hold the name of all index used with AER service.
 * todo: Add other index name to this list and refactor its usage.
 */
public enum IndexNames {
    EVENTS("events");

    private final String indexName;

    IndexNames(String indexName) {
        this.indexName = indexName;
    }

    public String getIndexName() {
        return indexName;
    }
}
