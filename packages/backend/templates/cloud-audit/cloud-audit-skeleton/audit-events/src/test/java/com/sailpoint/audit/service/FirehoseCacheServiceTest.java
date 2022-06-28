/*
 * Copyright (C) 2021 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.audit.service;

import com.amazonaws.services.resourcegroupstaggingapi.AWSResourceGroupsTaggingAPIClient;
import com.amazonaws.services.resourcegroupstaggingapi.model.GetResourcesRequest;
import com.amazonaws.services.resourcegroupstaggingapi.model.GetResourcesResult;
import com.amazonaws.services.resourcegroupstaggingapi.model.ResourceTagMapping;
import com.sailpoint.atlas.AtlasConfig;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.sailpoint.audit.service.FirehoseCacheService.FIREHOSE_TAGNAME;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FirehoseCacheServiceTest {

    @Mock
    AtlasConfig _atlasConfig;

    @Mock
    AWSResourceGroupsTaggingAPIClient _awsTaggingClient;

    FirehoseCacheService _sut;

    @Before
    public void setup() {
        _sut = new FirehoseCacheService();
        _sut._awsTaggingClient = _awsTaggingClient;
        _sut._atlasConfig = _atlasConfig;

        when(_atlasConfig.getString(FIREHOSE_TAGNAME)).thenReturn("FIREHOSE_TEST_TAG");

        GetResourcesResult result = mock(GetResourcesResult.class);
        List<ResourceTagMapping> tagMappingList = new ArrayList<>();
        ResourceTagMapping tagMapping = mock(ResourceTagMapping.class);
        tagMappingList.addAll(Arrays.asList(tagMapping, tagMapping, tagMapping));

        when(_awsTaggingClient.getResources(any(GetResourcesRequest.class))).thenReturn(result);
        when(result.getResourceTagMappingList()).thenReturn(tagMappingList);

        String firehoseArnPrefix = "arn:aws:firehose:us-east-1:406205545357:deliverystream/firehose";
        when(tagMapping.getResourceARN()).thenReturn(firehoseArnPrefix + 0, firehoseArnPrefix + 1, firehoseArnPrefix + 2);
    }

    @Test
    public void testFirehoseCache() {
        Assert.assertEquals(0, _sut.getFirehoseCount());
    }

    @Test
    public void testCache() {
        _sut.primeFirehoseCache();

        Assert.assertEquals(3, _sut.getFirehoseCount());

        Assert.assertEquals("firehose0", _sut.get(0));
    }

    @Test
    public void testException() {
        when(_atlasConfig.getString(FIREHOSE_TAGNAME)).thenReturn(null);

        _sut.primeFirehoseCache();

        Assert.assertEquals(0, _sut.getFirehoseCount());
    }

}