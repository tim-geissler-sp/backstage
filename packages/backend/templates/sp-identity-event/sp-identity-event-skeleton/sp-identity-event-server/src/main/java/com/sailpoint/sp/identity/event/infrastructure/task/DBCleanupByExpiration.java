package com.sailpoint.sp.identity.event.infrastructure.task;

import com.sailpoint.atlas.ApplicationInfo;
import com.sailpoint.atlas.boot.core.AtlasBootCoreProperties;
import com.sailpoint.atlas.boot.core.util.RedisDistributedLock;
import com.sailpoint.atlas.messaging.client.impl.redis.RedisPool;
import com.sailpoint.metrics.annotation.Timed;
import com.sailpoint.sp.identity.event.domain.IdentityStateJpaRepository;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.time.Duration;
import java.time.OffsetDateTime;

/**
 * DBCleanupByExpiration
 */
@Component
@ConditionalOnProperty("expiration-task")
@CommonsLog
public class DBCleanupByExpiration {

	private final IdentityStateJpaRepository _identityStateJpaRepository;
	private final RedisDistributedLock _redisDistributedLock;

	@Autowired
	public DBCleanupByExpiration(IdentityStateJpaRepository identityStateJpaRepository,
								 RedisPool redisPool,
								 ApplicationInfo applicationInfo,
								 AtlasBootCoreProperties atlasBootCoreProperties) {
		_identityStateJpaRepository = identityStateJpaRepository;
		_redisDistributedLock = new RedisDistributedLock(
			redisPool,
			applicationInfo.getStack() + "/expiration-task-lock/" + StringUtils.join(atlasBootCoreProperties.getPods(), '-'),
			Duration.ofMinutes(2L).toMillis()
		);
	}

	/**
	 * Scheduled to go off every hour on the 30th minute
	 */
	@Scheduled(cron = "0 30 * * * *")
	@Transactional
	@Timed
	public void execute() {
		if (_redisDistributedLock.acquireLock()) {
			try {
				log.debug("Executing delete by expiration.");
				_identityStateJpaRepository.deleteByExpirationBefore(OffsetDateTime.now());
			} catch (Exception e) {
				log.error("Failed to delete by expiration.", e);
			} finally {
				_redisDistributedLock.releaseLock();
			}
		}
	}
}
