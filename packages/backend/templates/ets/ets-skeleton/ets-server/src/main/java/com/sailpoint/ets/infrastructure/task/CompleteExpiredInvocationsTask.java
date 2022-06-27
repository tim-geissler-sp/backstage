/*
 * Copyright (C) 2019 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.ets.infrastructure.task;

import com.sailpoint.ets.domain.command.CompleteExpiredInvocationsCommand;
import com.sailpoint.ets.service.TriggerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * CompleteExpiredInvocationsTask
 */
@CommonsLog
@Component
@RequiredArgsConstructor(onConstructor_={@Autowired})
public class CompleteExpiredInvocationsTask {

	private final TriggerService _triggerService;

	@Scheduled(fixedDelay = 60000L)
	public void execute() {
		while (true) {
			CompleteExpiredInvocationsCommand cmd = CompleteExpiredInvocationsCommand.builder()
					.maxInvocations(100)
					.requestId(UUID.randomUUID().toString())
					.build();

			int expiredCount = _triggerService.completeExpiredInvocations(cmd);

			if (expiredCount > 0) {
				log.info("expired " + expiredCount + " invocations");
			} else {
				break;
			}
		}
	}

}
