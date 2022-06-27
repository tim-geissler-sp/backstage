/*
 * Copyright (c) 2021. SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.audit.service;

import com.amazonaws.services.athena.AmazonAthena;

import com.amazonaws.services.athena.model.QueryExecutionContext;
import com.amazonaws.services.athena.model.ResultConfiguration;
import com.amazonaws.services.athena.model.StartQueryExecutionRequest;
import com.amazonaws.services.athena.model.StartQueryExecutionResult;
import com.amazonaws.services.athena.model.GetQueryExecutionRequest;
import com.amazonaws.services.athena.model.GetQueryExecutionResult;
import com.amazonaws.services.athena.model.QueryExecutionState;
import com.amazonaws.services.athena.model.GetQueryResultsRequest;
import com.amazonaws.services.athena.model.ResultSet;
import com.amazonaws.services.athena.model.ColumnInfo;
import com.amazonaws.services.athena.model.Row;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.sailpoint.atlas.AtlasConfig;
import com.sailpoint.audit.event.model.EventCatalog;
import com.sailpoint.audit.event.util.ResourceUtils;
import com.sailpoint.audit.service.util.AuditUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Set;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

@Singleton
public class AthenaDataCatalogService implements DataCatalogService {

    private static final int CLIENT_EXECUTION_TIMEOUT = 60000;
    private static final String ATHENA_AUDIT_TABLE_SCHEMA_FILE = "scripts/athena/create_audit_table.txt";
    private static final String TABLE_NAME_PLACEHOLDER = "$AUDIT_ATHENA_ORG_TABLE_NAME";
    private static final String S3_PATH_PALCEHOLDER = "$S3_TENANT_PARQUET_STORAGE_LOCATION";
    private static final int QUERY_RETRY_COUNT = 5;
    private static final Log _log = LogFactory.getLog(EventCatalog.class);

    private final String athenaAuditTableSchema;

    @Inject @Named("AthenaClient")
    AmazonAthena _athenaClient;

    @Inject
    AtlasConfig _atlasConfig;

    @Inject
    AuditUtil _auditUtil;

    @Inject
    public AthenaDataCatalogService(ResourceUtils resourceUtils) throws Exception{
        athenaAuditTableSchema = resourceUtils.toString(ATHENA_AUDIT_TABLE_SCHEMA_FILE);
    }

    /**
     *
     * @param dbName - athena database on which athena queries to be executed
     * @param tableName - table name to be created
     * @param s3Bucket - s3 bucket where audit data of tenant is written
     * @param s3Prefix - prefix of s3 where parquet data is written
     * @throws InterruptedException
     */
    @Override
    public void createTable(String dbName, String tableName, String s3Bucket, String s3Prefix) throws InterruptedException {

        String auditTableCreateSchema = getAthenaAuditTableCreateSchema(tableName, s3Bucket, s3Prefix);

        _log.debug("Athena table create script: "+ auditTableCreateSchema);

        String queryExecutionId = submitAthenaQuery(dbName, auditTableCreateSchema, s3Bucket, "/results/"+tableName);

        waitForQueryToComplete(queryExecutionId);

        _log.info(String.format("Athena table created, dbName: %s, tableName: %s, queryExecutionId: %s", dbName, tableName, queryExecutionId));
    }

    public String getAthenaAuditTableCreateSchema(String tableName, String s3Bucket, String s3Prefix){
        String athenaAuditTableCreateSchema = athenaAuditTableSchema.replace(TABLE_NAME_PLACEHOLDER, tableName);
        String s3Path = "s3://"+s3Bucket+"/"+s3Prefix;
        return athenaAuditTableCreateSchema.replace(S3_PATH_PALCEHOLDER, s3Path);
    }

    /**
     *
     * @param dbName - athena database on which athena queries to be executed
     * @param tableName - table name to be deleted
     * @param s3Bucket - s3 bucket where query results to be persisted
     * @throws InterruptedException
     */
    @Override
    public void deleteTable(String dbName, String tableName, String s3Bucket) throws InterruptedException {

        String dropTableQuery = "DROP TABLE IF EXISTS " + tableName;

        _log.debug("Athena drop table query: "+ dropTableQuery);

        String queryExecutionId = submitAthenaQuery(dbName, dropTableQuery, s3Bucket, "/results/"+tableName);

        waitForQueryToComplete(queryExecutionId);

        _log.info(String.format("Athena table deleted, dbName: %s, tableName: %s, queryExecutionId: %s", dbName, tableName, queryExecutionId));
    }

    /**
     *
     * @param dbName - athena database on which athena queries to be executed
     * @param tableName - athena table name
     * @param s3Bucket - s3 bucket where audit data is written, same bucket where query results are persisted
     * @param orgName - tenant name
     * @param date - date for which partitions to be added
     * @throws InterruptedException
     */
    @Override
    public void addPartitions(String dbName, String tableName, String s3Bucket, String orgName, String date)
            throws InterruptedException {

        String addPartitionsQuery = buildAddPartitionsQuery(dbName, tableName, s3Bucket, orgName, date);

        if(StringUtils.isEmpty(addPartitionsQuery)){
            _log.info(String.format("No partitions found to be added for tableName: %s, date: %s in s3 bucket: %s",
                    tableName, date, s3Bucket));
            return;
        }

        String queryExecutionId = submitAthenaQuery(dbName, addPartitionsQuery, s3Bucket, "/results/"+tableName);

        waitForQueryToComplete(queryExecutionId);

        _log.info(String.format("Athena partitions add query : %s, queryExecutionId: %s completed.",
                addPartitionsQuery, queryExecutionId));
    }

    /**
     * Submits given input query to Athena and returns the execution ID of the query.
     * Use this execution id to track status and get query results.
     */
    public String submitAthenaQuery(String dbName, String athenaQuery, String s3Bucket, String outputPrefix )
    {
        _log.info(String.format("Athena query being executed: %s on db: %s, query results will persisted to %s",
                athenaQuery, dbName, "s3://" + s3Bucket + outputPrefix));
        // The QueryExecutionContext allows us to set the Database.
        QueryExecutionContext queryExecutionContext = new QueryExecutionContext().withDatabase(dbName);

        ResultConfiguration resultConfiguration = new ResultConfiguration().withOutputLocation("s3://" + s3Bucket + outputPrefix);

        // Create the StartQueryExecutionRequest to send to Athena which will start the query.
        StartQueryExecutionRequest startQueryExecutionRequest = new StartQueryExecutionRequest()
                .withQueryString(athenaQuery)
                .withQueryExecutionContext(queryExecutionContext)
                .withResultConfiguration(resultConfiguration).
                        withSdkClientExecutionTimeout(CLIENT_EXECUTION_TIMEOUT);

        StartQueryExecutionResult startQueryExecutionResult = _athenaClient.startQueryExecution(startQueryExecutionRequest);

        return startQueryExecutionResult == null ? null : startQueryExecutionResult.getQueryExecutionId();
    }

    /**
     * Track the status of Athena query and wait to complete, fail or to be cancelled.
     * This is done by polling Athena over an interval of time.
     * If a query fails or is cancelled, then it will throw an exception.
     */
    public void waitForQueryToComplete(String queryExecutionId) throws InterruptedException
    {
        GetQueryExecutionRequest getQueryExecutionRequest = new GetQueryExecutionRequest()
                .withQueryExecutionId(queryExecutionId);

        GetQueryExecutionResult getQueryExecutionResult;
        int retryCounter = 0;
        while (retryCounter <= QUERY_RETRY_COUNT) {
            getQueryExecutionResult = _athenaClient.getQueryExecution(getQueryExecutionRequest);
            String queryState = getQueryExecutionResult.getQueryExecution().getStatus().getState();
            if (queryState.equals(QueryExecutionState.FAILED.toString())) {
                throw new RuntimeException("Query Failed to run with Error Message: " + getQueryExecutionResult.getQueryExecution().getStatus().getStateChangeReason());
            }
            else if (queryState.equals(QueryExecutionState.CANCELLED.toString())) {
                throw new RuntimeException("Query was cancelled.");
            }
            else if (queryState.equals(QueryExecutionState.SUCCEEDED.toString())) {
                _log.info(String.format("Athena query with id %s completed successfully", queryExecutionId));
                return;
            }
            else {
                // Sleep an amount of time before retrying again.
                Thread.sleep(_atlasConfig.getInt("AUDIT_ATHENA_POLL_SLEEP_TIME", 15000));
            }
            retryCounter++;
            _log.debug(String.format("Current Status of query: %s is: %s", queryExecutionId, queryState));
        }
        throw new RuntimeException(String.format("Query failed to execute, queryExecutionId: %s", queryExecutionId));
    }

    /**
     *
     * @param dbName - athena database name
     * @param tableName - table name partitions to be added to
     * @param s3Bucket - s3 bucket where audit data is persisted
     * @param orgName -  org name partitions to be added
     * @param date - date of the partition to be added to
     *             Sample query: ALTER TABLE $AUDIT_ATHENA_ORG_TABLE_NAME add partition (org="test-org", type="SYSTEM_CONFIG", date="2020-11-02")
     *         //      location "s3://athena-parquet-data-poc/parquet/org=test-org/type=SYSTEM_CONFIG/date=2020-11-02/";
     */
    public String buildAddPartitionsQuery(String dbName, String tableName, String s3Bucket, String orgName, String date){

        Set<String> partitionPaths = _auditUtil.getS3AuditPartitionPaths(s3Bucket, "parquet/org="+orgName+"/",
                "date="+date);

        if(partitionPaths == null || partitionPaths.size() == 0){
            return null;
        }
        StringBuilder addPartitionsQuery = new StringBuilder("ALTER TABLE " + tableName + " ADD IF NOT EXISTS " );

        for(String partitionPath: partitionPaths){
            String type = partitionPath.substring(partitionPath.indexOf("/type=")+6, partitionPath.indexOf("/date="));
            addPartitionsQuery.append(" PARTITION (org=\"" + orgName +"\", type=\"" + type+   "\", date=\"" + date + "\") LOCATION '" +partitionPath +"' ");
        }

        return addPartitionsQuery.toString();
    }

    /**
     *
     * @param dbName - athena database on which athena queries to be executed
     * @param tableName - athena table name
     * @param date - date for which audit events to be published
     * @param s3Bucket - s3 bucket where audit data is persisted
     * @return returns count of audit events for given criterion
     * @throws Exception
     */
    @Override
    public int getAuditEventsCount(String dbName, String tableName, String date, String s3Bucket) throws Exception{
        String checkTableExistsQuery = "SHOW TABLES LIKE '" + tableName +"'";
        String queryExecutionId = submitAthenaQuery(dbName, checkTableExistsQuery, s3Bucket, "/results/"+tableName);

        waitForQueryToComplete(queryExecutionId);

        List<Map<String, String>> rows = showQueryResults(queryExecutionId);

        if(rows.size() == 0){
            _log.warn(String.format("Table: %s, in database:%s not found", tableName, dbName));
            return 0;
        }

        String auditEventCountQuery = "SELECT COUNT(DISTINCT(id)) AS count FROM " + tableName + " WHERE date = DATE('" + date + "')";

        queryExecutionId = submitAthenaQuery(dbName, auditEventCountQuery, s3Bucket, "/results/"+tableName);
        waitForQueryToComplete(queryExecutionId);
        List<Map<String, String>> auditCountRows = queryResults(queryExecutionId);

        if(auditCountRows.size() == 0){
            _log.warn(String.format("Table: %s, in database:%s not found", tableName, dbName));
            return 0;
        }

        return Integer.parseInt(auditCountRows.get(0).get("count"));
    }

    /**
     *
     * @param queryExecutionId - get results using query execution id
     * @return - returns show query results - show query results doesn't have header row.
     */
    public List<Map<String, String>> showQueryResults(String queryExecutionId){
        _log.info(String.format("Getting query results for query id: %s", queryExecutionId));

        GetQueryResultsRequest resultsRequest = new GetQueryResultsRequest().
                withQueryExecutionId(queryExecutionId);

        ResultSet results = _athenaClient.getQueryResults(resultsRequest).getResultSet();
        List<ColumnInfo> columnInfoList = results.getResultSetMetadata().getColumnInfo();
        return getRows(results, columnInfoList, results.getRows());
    }

    /**
     *
     * @param queryExecutionId - get results using query execution id
     * @return - returns query results
     */
    public List<Map<String, String>> queryResults(String queryExecutionId){
        _log.info(String.format("Getting query results for query id: %s", queryExecutionId));

        GetQueryResultsRequest resultsRequest = new GetQueryResultsRequest().
                withQueryExecutionId(queryExecutionId);

        ResultSet results = _athenaClient.getQueryResults(resultsRequest).getResultSet();
        List<ColumnInfo> columnInfoList = results.getResultSetMetadata().getColumnInfo();
        List<Row> rows = results.getRows();
        rows.remove(0);
        return getRows(results, columnInfoList, rows);
    }

    private List<Map<String, String>> getRows(ResultSet results, List<ColumnInfo> columnInfoList, List<Row> rows) {
        List<Map<String, String>> auditEvents = new ArrayList<>();

        List<ColumnInfo> columnInfo = results.getResultSetMetadata().getColumnInfo();
        for(Row row: rows){
            Map<String, String> rowMap = new HashMap<>();
            for(int index=0; index<columnInfoList.size();index++){
                rowMap.put(columnInfo.get(index).getName(), row.getData().get(index).getVarCharValue());
            }
            auditEvents.add(rowMap);
        }
        return auditEvents;
    }
}
