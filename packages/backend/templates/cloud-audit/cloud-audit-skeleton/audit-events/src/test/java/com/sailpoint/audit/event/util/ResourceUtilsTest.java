/*
 * Copyright (C) 2019 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.audit.event.util;

import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class ResourceUtilsTest {

    ResourceUtils _resourceUtils = new ResourceUtils();

    @Test
    public void loadError() {

        assertEquals(Collections.emptyMap(), _resourceUtils.loadMap("notFound.json", "Not Found"));
    }
}
