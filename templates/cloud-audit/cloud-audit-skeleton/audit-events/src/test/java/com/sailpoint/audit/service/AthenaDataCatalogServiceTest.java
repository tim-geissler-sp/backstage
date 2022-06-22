/*
 *
 *  * Copyright (c) 2021.  SailPoint Technologies, Inc. All rights reserved.
 *
 */

package com.sailpoint.audit.service;

import com.amazonaws.services.athena.AmazonAthena;
import com.amazonaws.services.athena.model.GetQueryExecutionRequest;
import com.amazonaws.services.athena.model.GetQueryExecutionResult;
import com.amazonaws.services.athena.model.QueryExecution;
import com.amazonaws.services.athena.model.QueryExecutionState;
import com.amazonaws.services.athena.model.QueryExecutionStatus;
import com.amazonaws.services.athena.model.QueryExecutionContext;
import com.amazonaws.services.athena.model.ResultConfiguration;
import com.amazonaws.services.athena.model.StartQueryExecutionRequest;
import com.amazonaws.services.athena.model.StartQueryExecutionResult;
import com.amazonaws.services.athena.model.GetQueryResultsRequest;
import com.amazonaws.services.athena.model.GetQueryResultsResult;
import com.amazonaws.services.athena.model.ColumnInfo;
import com.amazonaws.services.athena.model.ResultSet;
import com.amazonaws.services.athena.model.ResultSetMetadata;
import com.amazonaws.services.athena.model.Row;
import com.amazonaws.services.athena.model.Datum;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.sailpoint.atlas.AtlasConfig;
import com.sailpoint.audit.event.util.ResourceUtils;
import com.sailpoint.audit.service.util.AuditUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.eq;

@RunWith(MockitoJUnitRunner.class)
public class AthenaDataCatalogServiceTest {
	private static final String ORG_NAME = "ORG_MOCK";
	private static final String ORG_NAME_NEXT = "ORG_MOCK2";

	private static final String DB_NAME = "DB_MOCK";
	private static final String S3_BUCKET_NAME = "S3_MOCK";

	@Mock
	AmazonAthena _amazonAthena;

	@Mock
	AtlasConfig _atlasConfig;

	@Mock
	AuditUtil _util;

	@Rule
	public ExpectedException exceptionRule = ExpectedException.none();

	AthenaDataCatalogService _dataService;
	
	@Before
	public void setUp() throws Exception {
		_dataService = new AthenaDataCatalogService(new ResourceUtils());
		_dataService._athenaClient = _amazonAthena;
		_dataService._auditUtil = _util;
		when(_atlasConfig.getInt("AUDIT_ATHENA_POLL_SLEEP_TIME")).thenReturn(10);
		_dataService._atlasConfig = _atlasConfig;
	}

	@Test
	public void testCreateTable() throws Exception {

		QueryExecutionContext queryExecutionContext = new QueryExecutionContext().withDatabase(DB_NAME);
		ResultConfiguration resultConfiguration = new ResultConfiguration()
				.withOutputLocation("s3://"+S3_BUCKET_NAME+"/results/"+AuditUtil.getOrgAuditAthenaTableName(ORG_NAME));
		StartQueryExecutionRequest startQueryExecutionRequest = new StartQueryExecutionRequest()
				.withQueryString(_dataService.getAthenaAuditTableCreateSchema(
						AuditUtil.getOrgAuditAthenaTableName(ORG_NAME), S3_BUCKET_NAME, AuditUtil.PARQUET_DATA_S3_PREFIX))
				.withQueryExecutionContext(queryExecutionContext)
				.withResultConfiguration(resultConfiguration).
						withSdkClientExecutionTimeout(60000);

		StartQueryExecutionResult result = new StartQueryExecutionResult();
		result.setQueryExecutionId("mock-execution-id");

		when(_amazonAthena.startQueryExecution(startQueryExecutionRequest)).thenReturn(result);
		setQueryExecutionStatus(QueryExecutionState.SUCCEEDED);

		_dataService.createTable(DB_NAME, AuditUtil.getOrgAuditAthenaTableName(ORG_NAME),
				S3_BUCKET_NAME, AuditUtil.PARQUET_DATA_S3_PREFIX);
		GetQueryExecutionRequest getQueryExecutionRequest = new GetQueryExecutionRequest()
				.withQueryExecutionId("mock-execution-id");
		verify(_amazonAthena, times(1)).startQueryExecution(eq(startQueryExecutionRequest));
		verify(_amazonAthena, times(1)).getQueryExecution(eq(getQueryExecutionRequest));

		ResultConfiguration resultConfigurationNext = new ResultConfiguration()
				.withOutputLocation("s3://"+S3_BUCKET_NAME+"/results/"+AuditUtil.getOrgAuditAthenaTableName(ORG_NAME_NEXT));
		StartQueryExecutionRequest startQueryExecutionRequestNext = new StartQueryExecutionRequest()
				.withQueryString(_dataService.getAthenaAuditTableCreateSchema(
						AuditUtil.getOrgAuditAthenaTableName(ORG_NAME_NEXT), S3_BUCKET_NAME, AuditUtil.PARQUET_DATA_S3_PREFIX))
				.withQueryExecutionContext(queryExecutionContext)
				.withResultConfiguration(resultConfigurationNext).
						withSdkClientExecutionTimeout(60000);

		StartQueryExecutionResult resultNext = new StartQueryExecutionResult();
		resultNext.setQueryExecutionId("mock-execution-next-id");

		when(_amazonAthena.startQueryExecution(startQueryExecutionRequestNext)).thenReturn(resultNext);
		setQueryExecutionStatus(QueryExecutionState.SUCCEEDED);

		_dataService.createTable(DB_NAME, AuditUtil.getOrgAuditAthenaTableName(ORG_NAME_NEXT),
				S3_BUCKET_NAME, AuditUtil.PARQUET_DATA_S3_PREFIX);
		GetQueryExecutionRequest getQueryExecutionRequestNext = new GetQueryExecutionRequest()
				.withQueryExecutionId("mock-execution-next-id");
		verify(_amazonAthena, times(1)).startQueryExecution(eq(startQueryExecutionRequestNext));
		verify(_amazonAthena, times(1)).getQueryExecution(eq(getQueryExecutionRequestNext));
	}

	@Test
	public void testDeleteTable() throws Exception {

		QueryExecutionContext queryExecutionContext = new QueryExecutionContext().withDatabase(DB_NAME);
		ResultConfiguration resultConfiguration = new ResultConfiguration()
				.withOutputLocation("s3://"+S3_BUCKET_NAME+"/results/"+AuditUtil.getOrgAuditAthenaTableName(ORG_NAME));
		StartQueryExecutionRequest startQueryExecutionRequest = new StartQueryExecutionRequest()
				.withQueryString("DROP TABLE IF EXISTS " + AuditUtil.getOrgAuditAthenaTableName(ORG_NAME))
				.withQueryExecutionContext(queryExecutionContext)
				.withResultConfiguration(resultConfiguration).
						withSdkClientExecutionTimeout(60000);

		StartQueryExecutionResult result = new StartQueryExecutionResult();
		result.setQueryExecutionId("mock-execution-id");

		setQueryExecutionStatus(QueryExecutionState.SUCCEEDED);
		when(_amazonAthena.startQueryExecution(startQueryExecutionRequest)).thenReturn(result);

		_dataService.deleteTable(DB_NAME, AuditUtil.getOrgAuditAthenaTableName(ORG_NAME), S3_BUCKET_NAME);
		GetQueryExecutionRequest getQueryExecutionRequest = new GetQueryExecutionRequest()
				.withQueryExecutionId("mock-execution-id");
		verify(_amazonAthena, times(1)).startQueryExecution(startQueryExecutionRequest);
		verify(_amazonAthena, times(1)).getQueryExecution(getQueryExecutionRequest);
	}

	@Test
	public void testTetAthenaAuditTableSchema() throws IOException {
		String resultSchema = _dataService.getAthenaAuditTableCreateSchema(
				AuditUtil.getOrgAuditAthenaTableName(ORG_NAME), S3_BUCKET_NAME, AuditUtil.PARQUET_DATA_S3_PREFIX);
		String expectedOutput = Resources.toString(Resources.getResource
				("create_audit_table.txt"), Charsets.UTF_8);
		Assert.assertEquals(expectedOutput.toLowerCase(), resultSchema.toLowerCase());
	}

	@Test
	public void testSubmitAthenaQuery(){
		String createAthenaTableScript = _dataService.getAthenaAuditTableCreateSchema(
				AuditUtil.getOrgAuditAthenaTableName(ORG_NAME), S3_BUCKET_NAME, AuditUtil.PARQUET_DATA_S3_PREFIX);
		_dataService.submitAthenaQuery(DB_NAME,createAthenaTableScript, S3_BUCKET_NAME, AuditUtil.PARQUET_DATA_S3_PREFIX);
		verify(_amazonAthena, times(1)).startQueryExecution(any());
	}

	@Test
	public void testWaitForQueryToComplete() throws InterruptedException {
		setQueryExecutionStatus(QueryExecutionState.SUCCEEDED);

		_dataService.waitForQueryToComplete("mock-execution-id");

		verify(_amazonAthena, times(1)).getQueryExecution(any());
	}

	@Test
	public void testWaitForQueryToCompleteRunningState() throws InterruptedException {
		setQueryExecutionStatus(QueryExecutionState.RUNNING);
		exceptionRule.expect(RuntimeException.class);
		exceptionRule.expectMessage("Query failed to execute");
		_dataService.waitForQueryToComplete("mock-execution-id");
		verify(_amazonAthena, times(6)).getQueryExecution(any());
	}

	@Test
	public void testWaitForQueryToCompleteCancelledState() throws InterruptedException {
		setQueryExecutionStatus(QueryExecutionState.CANCELLED);
		exceptionRule.expect(RuntimeException.class);
		exceptionRule.expectMessage("Query was cancelled.");
		_dataService.waitForQueryToComplete("mock-execution-id");
		verify(_amazonAthena, times(1)).getQueryExecution(any());
	}

	@Test
	public void testWaitForQueryToCompleteFailedState() throws InterruptedException {
		GetQueryExecutionResult getQueryExecutionResult = new GetQueryExecutionResult();
		QueryExecution execution = new QueryExecution();
		QueryExecutionStatus status = new QueryExecutionStatus();
		status.setState(QueryExecutionState.FAILED);
		status.setStateChangeReason("Mock Failure");
		execution.setStatus(status);
		getQueryExecutionResult.setQueryExecution(execution);
		exceptionRule.expect(RuntimeException.class);
		exceptionRule.expectMessage("Query Failed to run with Error Message: Mock Failure");
		when(_amazonAthena.getQueryExecution(any())).thenReturn(getQueryExecutionResult);
		_dataService.waitForQueryToComplete("mock-execution-id");
		verify(_amazonAthena, times(1)).getQueryExecution(any());
	}

	@Test
	public void testWaitForQueryToCompleteQueuedState() throws InterruptedException {
		setQueryExecutionStatus(QueryExecutionState.QUEUED);
		exceptionRule.expect(RuntimeException.class);
		exceptionRule.expectMessage("Query failed to execute");
		_dataService.waitForQueryToComplete("mock-execution-id");
		verify(_amazonAthena, times(1)).getQueryExecution(any());
	}

	@Test
	public void testBuildAddPartitionsQuery(){
		Set<String> s3PartitionPaths = new HashSet<>(Arrays.asList( "s3://s3mock/parquet/org=mock/type=MOCK1/date=2000-12-12/",
				"s3://s3mock/parquet/org=mock/type=MOCK1/date=2000-11-12/"));
		when(_util.getS3AuditPartitionPaths(anyString(), anyString(), anyString())).thenReturn(s3PartitionPaths);

		String result = _dataService.buildAddPartitionsQuery("mock_db", "mock_audit_table", "mocks3",
				"mock-org", "2020-12-10");

		String expectedQuery = "ALTER TABLE mock_audit_table ADD IF NOT EXISTS " +
				" PARTITION (org=\"mock-org\", type=\"MOCK1\", date=\"2020-12-10\") " +
				"LOCATION 's3://s3mock/parquet/org=mock/type=MOCK1/date=2000-12-12/'  " +
				"PARTITION (org=\"mock-org\", type=\"MOCK1\", date=\"2020-12-10\") " +
				"LOCATION 's3://s3mock/parquet/org=mock/type=MOCK1/date=2000-11-12/' ";

		Assert.assertEquals(result, expectedQuery);
	}

	@Test
	public void testAddPartitions() throws InterruptedException {

		final String PARTITION_DATE = "2020-12-10";
		Set<String> partitions = new HashSet<>();
		partitions.add("s3://"+S3_BUCKET_NAME+"/parquet/org="+ORG_NAME+"/type=mock/date="+PARTITION_DATE);
		when(_util.getS3AuditPartitionPaths(anyString(), anyString(), anyString())).thenReturn(partitions);
		QueryExecutionContext queryExecutionContext = new QueryExecutionContext().withDatabase(DB_NAME);
		ResultConfiguration resultConfiguration = new ResultConfiguration()
				.withOutputLocation("s3://"+S3_BUCKET_NAME+"/results/"+AuditUtil.getOrgAuditAthenaTableName(ORG_NAME));
		StartQueryExecutionRequest startQueryExecutionRequest = new StartQueryExecutionRequest()
				.withQueryString(_dataService.buildAddPartitionsQuery(DB_NAME,
						AuditUtil.getOrgAuditAthenaTableName(ORG_NAME), S3_BUCKET_NAME, ORG_NAME, PARTITION_DATE ))
				.withQueryExecutionContext(queryExecutionContext)
				.withResultConfiguration(resultConfiguration).
						withSdkClientExecutionTimeout(60000);

		StartQueryExecutionResult result = new StartQueryExecutionResult();
		result.setQueryExecutionId("mock-execution-id");

		when(_amazonAthena.startQueryExecution(startQueryExecutionRequest)).thenReturn(result);
		setQueryExecutionStatus(QueryExecutionState.SUCCEEDED);

		_dataService.addPartitions(DB_NAME, AuditUtil.getOrgAuditAthenaTableName(ORG_NAME), S3_BUCKET_NAME,
				ORG_NAME, PARTITION_DATE);

		GetQueryExecutionRequest getQueryExecutionRequest = new GetQueryExecutionRequest()
				.withQueryExecutionId("mock-execution-id");
		verify(_amazonAthena, times(1)).startQueryExecution(startQueryExecutionRequest);
		verify(_amazonAthena, times(1)).getQueryExecution(getQueryExecutionRequest);
	}

	@Test
	public void testGetAuditEventsCount() throws Exception {
		final String DATE = "2020-12-10";
		final String QUERY_EXECUTION_ID = "mock-execution-id";
		final String COUNT_QUERY_EXECUTION_ID = "count-mock-execution-id";

		final String TABLE_NAME = AuditUtil.getOrgAuditAthenaTableName(ORG_NAME);

		//Mock to return the table exists query results
		String checkTableExistsQuery = "SHOW TABLES LIKE '" + TABLE_NAME +"'";

		QueryExecutionContext queryExecutionContext = new QueryExecutionContext().withDatabase(DB_NAME);
		ResultConfiguration resultConfiguration = new ResultConfiguration()
				.withOutputLocation("s3://"+S3_BUCKET_NAME+"/results/"+TABLE_NAME);
		StartQueryExecutionRequest startQueryExecutionRequest = new StartQueryExecutionRequest()
				.withQueryString(checkTableExistsQuery)
				.withQueryExecutionContext(queryExecutionContext)
				.withResultConfiguration(resultConfiguration).
						withSdkClientExecutionTimeout(60000);

		StartQueryExecutionResult result = new StartQueryExecutionResult();
		result.setQueryExecutionId(QUERY_EXECUTION_ID);

		when(_amazonAthena.startQueryExecution(eq(startQueryExecutionRequest))).thenReturn(result);
		setQueryExecutionStatus(QueryExecutionState.SUCCEEDED);

		GetQueryResultsRequest resultsRequest = new GetQueryResultsRequest().
				withQueryExecutionId(QUERY_EXECUTION_ID);

		List<ColumnInfo> columnInfo = new ArrayList<ColumnInfo>(){
			{
				add(new ColumnInfo()
						.withName("tab_name")
						.withLabel("tab_name")
						.withType("string"));
			}
		};

		ResultSet resultSet = new ResultSet()
				.withResultSetMetadata(new ResultSetMetadata()
					.withColumnInfo(columnInfo))
				.withRows(new Row().withData(new Datum()
				.withVarCharValue(TABLE_NAME)));
		GetQueryResultsResult queryResults = new GetQueryResultsResult()
				.withResultSet(resultSet);
		when(_amazonAthena.getQueryResults(eq(resultsRequest))).thenReturn(queryResults);

		//Mock to return the audit event count from athena table
		String auditEventCountQuery = "SELECT COUNT(DISTINCT(id)) AS count FROM " + TABLE_NAME + " WHERE date = DATE('" + DATE + "')";

		StartQueryExecutionRequest startCountQueryExecutionRequest = new StartQueryExecutionRequest()
				.withQueryString(auditEventCountQuery)
				.withQueryExecutionContext(queryExecutionContext)
				.withResultConfiguration(resultConfiguration).
						withSdkClientExecutionTimeout(60000);

		StartQueryExecutionResult countStartQueryResult = new StartQueryExecutionResult();
		countStartQueryResult.setQueryExecutionId(COUNT_QUERY_EXECUTION_ID);
		when(_amazonAthena.startQueryExecution(eq(startCountQueryExecutionRequest))).thenReturn(countStartQueryResult);

		setQueryExecutionStatus(QueryExecutionState.SUCCEEDED);

		GetQueryResultsRequest countResultsRequest = new GetQueryResultsRequest().
				withQueryExecutionId(COUNT_QUERY_EXECUTION_ID);
		List<ColumnInfo> countColumnInfo = new ArrayList<ColumnInfo>(){
			{
				add(new ColumnInfo()
						.withName("count")
						.withLabel("count")
						.withType("bigint"));
			}
		};
		final int EXPECTED_ROW_COUNT = 50;
		ResultSet countResultSet = new ResultSet()
				.withResultSetMetadata(new ResultSetMetadata()
						.withColumnInfo(countColumnInfo))
				.withRows(new Row().withData(new Datum()
						.withVarCharValue("count")),
						new Row().withData(new Datum()
								.withVarCharValue(String.valueOf(EXPECTED_ROW_COUNT))));
		StartQueryExecutionResult countQueryResult = new StartQueryExecutionResult();
		countQueryResult.setQueryExecutionId(COUNT_QUERY_EXECUTION_ID);
		GetQueryResultsResult countQueryResults = new GetQueryResultsResult()
				.withResultSet(countResultSet);
		when(_amazonAthena.getQueryResults(eq(countResultsRequest))).thenReturn(countQueryResults);

		Assert.assertEquals(EXPECTED_ROW_COUNT, _dataService.getAuditEventsCount(DB_NAME, AuditUtil.getOrgAuditAthenaTableName(ORG_NAME), DATE, S3_BUCKET_NAME));

	}

	private void setQueryExecutionStatus(QueryExecutionState state){
		GetQueryExecutionResult getQueryExecutionResult = new GetQueryExecutionResult();
		QueryExecution execution = new QueryExecution();
		QueryExecutionStatus status = new QueryExecutionStatus();
		status.setState(state);
		execution.setStatus(status);
		getQueryExecutionResult.setQueryExecution(execution);
		when(_amazonAthena.getQueryExecution(any())).thenReturn(getQueryExecutionResult);
	}
}
