/*
 * Copyright (C) 2021 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.audit.service.mapping;

import com.sailpoint.audit.event.model.EventCatalog;
import com.sailpoint.audit.event.model.EventTemplates;
import com.sailpoint.audit.event.util.ResourceUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DomainAuditEventsUtilTest {

    ResourceUtils _resourceUtils = new ResourceUtils();

    EventCatalog _eventCatalog = new EventCatalog(_resourceUtils, new EventTemplates(_resourceUtils));

    DomainAuditEventsUtil _sut = new DomainAuditEventsUtil(_eventCatalog);

    @Test
    public void testDomainEventActionLookup() {
        Assert.assertTrue(_sut.isDomainAuditEvent("NON_EMPLOYEE_REQUEST_CREATED"));
    }

    @Test
    public void testDomainEventActionSet() {
        Assert.assertTrue(_sut.getDomainEventActions().size() > 0);
    }

    @Test
    public void testGetTypeFromAction() {
        Assert.assertEquals("NON_EMPLOYEE", _sut.getDomainType("NON_EMPLOYEE_APPROVAL_ACTION_COMPLETED"));
    }
}
