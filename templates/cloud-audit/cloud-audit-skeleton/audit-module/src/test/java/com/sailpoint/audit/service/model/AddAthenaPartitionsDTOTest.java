/*
 * Copyright (C) 2021 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.audit.service.model;

import org.junit.Assert;
import org.junit.Test;

import java.time.LocalDate;

public class AddAthenaPartitionsDTOTest {

    @Test
    public void testPartitionDate() {
        AddAthenaPartitionsDTO dto = new AddAthenaPartitionsDTO();
        Assert.assertEquals(LocalDate.now().toString(), dto.getPartitionDate());
    }

    @Test
    public void testCustomPartitionDate() {
        AddAthenaPartitionsDTO dto = new AddAthenaPartitionsDTO("2021-09-01");
        Assert.assertEquals("2021-09-01", dto.getPartitionDate());
    }

}
