/*
 * Copyright (C) 2020 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.ets.infrastructure.task;

import com.sailpoint.atlas.ApplicationInfo;
import com.sailpoint.atlas.boot.core.AtlasBootCoreProperties;
import com.sailpoint.atlas.boot.core.util.RedisDistributedLock;
import com.sailpoint.atlas.messaging.client.impl.redis.RedisPool;
import com.sailpoint.ets.domain.status.SubscriptionStatus;
import com.sailpoint.ets.domain.subscription.SubscriptionRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.transaction.Transactional;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import static com.sailpoint.ets.infrastructure.util.MetricsReporter.reportTenantSubscriptions;

/**
 * Task that reports subscription status for each tenant
 */
@Component
@CommonsLog
@RequiredArgsConstructor(onConstructor_={@Autowired})
public class ReportSubscriptionUsageTask {
	private final SubscriptionRepo _subscriptionRepo;
	private final RedisPool _redisPool;
	private final ApplicationInfo _applicationInfo;
	private final AtlasBootCoreProperties _atlasBootCoreProperties;

	private RedisDistributedLock _redisDistributedLock;

	@PostConstruct
	public void setup() {
		_redisDistributedLock = new RedisDistributedLock(_redisPool,
			_applicationInfo.getStack() + "/tenant-subscription-count-lock/" + StringUtils.join(_atlasBootCoreProperties.getPods(), '-'),
			Duration.ofMinutes(2L).toMillis()
		);
	}

	/**
	 * Scheduled to go off every hour on the 30th minute to report tenant subscriptions
	 */
	@Scheduled(cron = "0 30 * * * *")
	@Transactional
	public void reportSubscriptions() {
		if (_redisDistributedLock.acquireLock()) {
			try {
				List<SubscriptionStatus> subscriptionCountList = _subscriptionRepo.findAllSubscriptionCounts().collect(Collectors.toList());
				reportTenantSubscriptions(subscriptionCountList);
			} catch (Exception e) {
				log.error("Failed to report tenant subscription count.", e);
			} finally {
				_redisDistributedLock.releaseLock();
			}
		}
	}

}
