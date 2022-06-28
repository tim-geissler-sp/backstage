/*
 * Copyright (c) 2021. SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.audit.service;

public interface DataCatalogService {

    void createTable(String dbName, String tableName, String s3Bucket, String s3Prefix) throws InterruptedException;
    void deleteTable(String dbName, String tableName, String s3Bucket) throws InterruptedException;
    void addPartitions(String dbName, String tableName, String s3Bucket, String orgName, String date) throws InterruptedException;
    int getAuditEventsCount(String dbName, String tableName, String date, String s3Bucket) throws Exception;

}
