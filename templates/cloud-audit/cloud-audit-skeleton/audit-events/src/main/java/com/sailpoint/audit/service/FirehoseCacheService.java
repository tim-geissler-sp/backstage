/*
 * Copyright (C) 2021 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.audit.service;

import com.amazonaws.services.resourcegroupstaggingapi.AWSResourceGroupsTaggingAPIClient;
import com.amazonaws.services.resourcegroupstaggingapi.AWSResourceGroupsTaggingAPIClientBuilder;
import com.amazonaws.services.resourcegroupstaggingapi.model.GetResourcesRequest;
import com.amazonaws.services.resourcegroupstaggingapi.model.GetResourcesResult;
import com.amazonaws.services.resourcegroupstaggingapi.model.ResourceTagMapping;
import com.amazonaws.services.resourcegroupstaggingapi.model.TagFilter;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sailpoint.atlas.AtlasConfig;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Singleton
public class FirehoseCacheService {

    static Log _log = LogFactory.getLog(FirehoseCacheService.class);

    private int _firehoseCount;

    @VisibleForTesting
    static final String FIREHOSE_TAGNAME = "FIREHOSE_TAG";

    public int getFirehoseCount() {
        return _firehoseCount;
    }

    private LoadingCache<Integer, String> _loadingCache;

    AWSResourceGroupsTaggingAPIClient _awsTaggingClient
            = (AWSResourceGroupsTaggingAPIClient) AWSResourceGroupsTaggingAPIClientBuilder.defaultClient();

    @Inject
    AtlasConfig _atlasConfig;

    public FirehoseCacheService() {
        _loadingCache = CacheBuilder.newBuilder()
                .maximumSize(100)
                .build(
                        new CacheLoader<Integer, String>() {
                            //Initial loading of the cache.
                            @Override
                            public String load(Integer firehoseId)  {
                                return null;
                            }
                        }
                );
    }

    /**
     * Method builds cache during service startup
     * Each AER deployment cluster will have an environment variable caled FIREHOSE_TAG.
     * FIREHOSE_TAG's value will be used to get all firehoses dedicated for this aer deployment cluster
     */
    public void primeFirehoseCache() {
        try {
            final String firehoseTagName = _atlasConfig.getString(FIREHOSE_TAGNAME);
            if (null == firehoseTagName) {
                throw new IllegalArgumentException(FIREHOSE_TAGNAME + " environment variable not found in aer");
            }

            TagFilter tagFilter = new TagFilter().withKey(firehoseTagName);
            GetResourcesRequest resourcesRequest = new GetResourcesRequest().withTagFilters(tagFilter);
            GetResourcesResult result = _awsTaggingClient.getResources(resourcesRequest);
            List<ResourceTagMapping> tagMappingList = result.getResourceTagMappingList();

            int index = 0;
            for (ResourceTagMapping resourceTagMapping : tagMappingList) {
                _loadingCache.put(index++, extractFirehoseNameFromArn(resourceTagMapping));
            }

            _firehoseCount = (int) _loadingCache.size();

            _log.info("Firehose count:" + _firehoseCount);
        } catch (Exception e) {
            _log.error("Exception encountered in loading firehose cache", e);
        }
    }

    private String extractFirehoseNameFromArn(ResourceTagMapping resourceTagMapping) {
        final String resourceArn = resourceTagMapping.getResourceARN();
        return resourceArn.substring(resourceArn.indexOf("/") + 1);
    }

    public String get(Integer firehoseId) {
        String firehoseName = null;
        try {
            firehoseName = _loadingCache.get(firehoseId);
        } catch (ExecutionException e) {
            _log.error("Exception retrieving firehose. Firehose count: " + _firehoseCount, e);
        }
        return firehoseName;
    }
}
