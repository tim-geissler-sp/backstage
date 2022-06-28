/*
 *
 *  * Copyright (c) 2020.  SailPoint Technologies, Inc. All rights reserved.
 *
 */

package com.sailpoint.audit.service.util;

import com.amazonaws.regions.Regions;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.sailpoint.atlas.event.EventService;
import com.sailpoint.atlas.search.model.event.Event;
import com.sailpoint.atlas.search.util.JsonUtils;
import com.sailpoint.atlas.service.RemoteFileService;
import com.sailpoint.audit.event.model.EventCatalog;
import com.sailpoint.audit.event.model.EventTemplates;
import com.sailpoint.audit.event.util.ResourceUtils;
import com.sailpoint.audit.service.mapping.AuditEventTypeToAction;
import com.sailpoint.audit.service.mapping.DomainAuditEventsUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import sailpoint.object.Attributes;
import sailpoint.object.AuditEvent;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Arrays;

import static com.sailpoint.audit.service.AuditEventService.FIREHOSE_RECORD_LIMIT_KB;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AuditUtilTest {
    @Mock
    EventService _eventService;

    @Mock
    RemoteFileService _remoteFileService;

    @Mock
    DomainAuditEventsUtil _domainAuditEventsUtil;

    AuditUtil _sut;

    @Before
    public void setUp() {
        _sut = new AuditUtil();
        _sut._eventService = _eventService;
        _sut._remoteFileService = _remoteFileService;
        _sut._domainEventActions = _domainAuditEventsUtil;
    }

    @Test
	public void testToDate() {
		Map<String, Object> arguments = new HashMap<>();
		arguments.put("date", "2020-01-01");
		LocalDate localDate = AuditUtil.toDate(arguments, "date");
		assertEquals(2020, localDate.getYear());
		assertEquals(1, localDate.getMonthValue());
		assertEquals(1, localDate.getDayOfMonth());
	}

    @Test
    public void testFirehoseLimit() {
        Event bigEvent = null;
        try {
            bigEvent = JsonUtils.parse(Event.class,
                    Resources.toString(Resources.getResource("big-audit-event.json"), Charsets.UTF_8));
        } catch (IOException e) {

        }

        Event newEvent = _sut.checkEventSizeAndFieldLimits(bigEvent);

        assertTrue(JsonUtils.toJson(newEvent).getBytes().length < FIREHOSE_RECORD_LIMIT_KB);

        List<String> newErrors = new ArrayList<>();
        for(int i = 0; i < 10; i++) {
            newErrors.addAll((List) newEvent.getAttributes().get("errors"));
        }
        newEvent.getAttributes().put("errors", newErrors);
        newEvent = _sut.checkEventSizeAndFieldLimits(bigEvent);

        assertTrue(JsonUtils.toJson(newEvent).getBytes().length < FIREHOSE_RECORD_LIMIT_KB);
    }

    @Test
	public void testPublishAuditEvent() {
		try {
			Event event = new Event();
			_sut.publishAuditEvent(event, true);
		} catch (Exception ignored) {
			verify(_eventService, times(1)).publishAsync(any(), any());
		}
	}

    @Test
    public void testGetOrgAuditAthenaTableName() {
        Assert.assertEquals("mock_123_table_audit_data", AuditUtil.getOrgAuditAthenaTableName("mock-123.table"));
        Assert.assertEquals("mock_123_table_audit_data", AuditUtil.getOrgAuditAthenaTableName("MOCK-123.table"));
        Assert.assertEquals("mock_123_table_audit_data", AuditUtil.getOrgAuditAthenaTableName("MOCK#123_table"));
        Assert.assertEquals("mock_123_table_audit_data", AuditUtil.getOrgAuditAthenaTableName("MOCK*123_table"));
    }

    @Test
    public void testGetS3Paths(){
        List<String> mockResult = Arrays.asList(
                "parquet/org=mock/MOCK1/date=2000-12-12/test1.parquet",
                "parquet/org=mock/MOCK2/date=2000-12-12/test2.parquet",
                "parquet/org=mock/MOCK2/date=2000-12-12/tes3t.parquet",
                "parquet/org=mock/MOCK3/date=2000-11-12/tes3t.parquet");
        when(_remoteFileService.listFiles(anyString(), anyString())).thenReturn(mockResult);

        Set<String> result = _sut.getS3AuditPartitionPaths("s3mock",
                "parquet/org=mock/", "date=2000-12-12");
        Assert.assertEquals(2, result.size());
        Assert.assertTrue(result.contains("s3://s3mock/parquet/org=mock/MOCK1/date=2000-12-12/"));
        Assert.assertFalse(result.contains("s3://notexists/parquet/org=mock/MOCK1/date=2000-12-12/"));

        result = _sut.getS3AuditPartitionPaths("s3mock",
                "parquet/org=mock/", "date=2000-11-12");
        Assert.assertEquals(1, result.size());
    }

    @Test
    public void testGetCurrentRegion(){
        String defaultRegion = "us-east-1";
       String region = AuditUtil.getCurrentRegion(defaultRegion);
       String expectedRegion = Regions.getCurrentRegion() == null ? defaultRegion : Regions.getCurrentRegion().getName();
       Assert.assertEquals(region, expectedRegion);
    }

    @Test
    public void testAllDefaultAllowAuditEvents()  {

        _domainAuditEventsUtil = new DomainAuditEventsUtil(new EventCatalog(new ResourceUtils(), new EventTemplates(new ResourceUtils())));
        _sut._domainEventActions = _domainAuditEventsUtil;

        AuditEventTypeToAction.ACTION_TO_TYPE_MAPPING.keySet().forEach(auditEventAction -> {
            AuditEvent ae = new AuditEvent("Actor", auditEventAction);
            ae.setCreated(new Date());
            ae.setApplication("[tpe] foobar [source-12345]");
            Map<String,Object> attrs = new HashMap<>();
            attrs.put("oldValue",123);
            attrs.put("newValue",true);
            attrs.put("reviewer.comments", "I like turtles");
            ae.setAttributes(new Attributes<>(attrs));

            Assert.assertTrue(_sut.isAlwaysAllowAudit(ae));
        });

		_domainAuditEventsUtil.getDomainEventActions().forEach(auditEventAction -> {
			AuditEvent ae = new AuditEvent("Actor", auditEventAction);
			ae.setCreated(new Date());
			ae.setApplication("[tpe] foobar [source-12345]");
			Map<String,Object> attrs = new HashMap<>();
			attrs.put("oldValue",123);
			attrs.put("newValue",true);
			attrs.put("reviewer.comments", "I like turtles");
			ae.setAttributes(new Attributes<>(attrs));

            Assert.assertTrue(_sut.isAlwaysAllowAudit(ae));
		});

    }
}
