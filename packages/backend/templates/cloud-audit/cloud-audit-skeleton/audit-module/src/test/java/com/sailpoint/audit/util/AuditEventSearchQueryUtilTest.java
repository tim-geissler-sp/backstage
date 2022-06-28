/*
 * Copyright (c) 2022 SailPoint Technologies, Inc.  All rights reserved
 */

package com.sailpoint.audit.util;

import com.sailpoint.mantis.core.service.CrudService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import sailpoint.object.Identity;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AuditEventSearchQueryUtilTest {
    @InjectMocks
    private AuditEventSearchQueryUtil auditEventSearchQueryUtil;

    @Mock
    private CrudService _crudService;

    @Mock
    private Identity _identity;

    @Before
    public void setup() {
        when(_crudService.findByName(eq(Identity.class), anyString())).thenReturn(Optional.of(_identity));
        when(_identity.getAttribute(anyString())).thenReturn("999999");
    }

    @Test
    public void testBuildingSearchQuery() {

        String actualQuery = auditEventSearchQueryUtil.buildSearchQuery("user.name", "export");
        String expectQuery = "(  actor.name:\"999999\" OR target.name:\"999999\"  OR  actor.name:\"user.name\" OR target.name:\"user.name\"  )  AND created:[now-7d/d TO now]  AND *export*";
        assertEquals(actualQuery, expectQuery);
    }

    @Test
    public void testBuildSearchQueryWithoutSearchText() {
        String actualQuery = auditEventSearchQueryUtil.buildSearchQuery("user.name", null);
        String expectQuery = "(  actor.name:\"999999\" OR target.name:\"999999\"  OR  actor.name:\"user.name\" OR target.name:\"user.name\"  )  AND created:[now-7d/d TO now] ";
        assertEquals(actualQuery, expectQuery);
    }

    @Test
    public void testGettingAllUserName() {
        Set<String> actualUserNames = auditEventSearchQueryUtil.getAllUserNames("user.name");
        Set<String> expectedUserName = new HashSet<>();
        expectedUserName.add("999999");
        expectedUserName.add("user.name");
        assertEquals(actualUserNames, expectedUserName);
    }
}
