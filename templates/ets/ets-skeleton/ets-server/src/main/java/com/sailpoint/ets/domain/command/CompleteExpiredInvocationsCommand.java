/*
 * Copyright (C) 2019 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.ets.domain.command;

import com.sailpoint.ets.domain.event.EventPublisher;
import com.sailpoint.ets.domain.event.InvocationFailedEvent;
import com.sailpoint.ets.domain.invocation.Invocation;
import com.sailpoint.ets.domain.invocation.InvocationRepo;
import com.sailpoint.metrics.annotation.Metered;
import lombok.Builder;
import lombok.Value;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.OffsetDateTime;
import java.util.List;

import static com.sailpoint.ets.infrastructure.util.MetricsReporter.reportInvocationComplete;
import static com.sailpoint.ets.infrastructure.util.MetricsReporter.reportTotalInvocationTime;
import static com.sailpoint.ets.infrastructure.util.TriggerEventLogUtil.logInvocationFailedEvent;

/**
 * CompleteExpiredInvocationsCommand
 */
@Value
@Builder
@CommonsLog
public class CompleteExpiredInvocationsCommand {

	private final int _maxInvocations;
	private final String _requestId;
	/**
	 * Completes expired invocations in the repository, returning the number of invocations that were completed.
	 * This method will expire at most _maxInvocations Invocations.
	 *
	 * @param invocationRepo The InvocationRepo implementation.
	 * @param eventPublisher The EventPublisher implementation.
	 * @return The number of invocations that were expired.
	 */
	public int handle(InvocationRepo invocationRepo, EventPublisher eventPublisher) {
		List<Invocation> invocations = invocationRepo.findByDeadlineBefore(OffsetDateTime.now(),
				PageRequest.of(0, _maxInvocations, Sort.by("deadline")));

		for (Invocation invocation : invocations) {
			InvocationFailedEvent event = invocation.newFailedEvent("invocation timed out");

			eventPublisher.publish(event);

			reportTotalInvocationTime(invocation, true);
			reportInvocationComplete(true);
			log.error(logInvocationFailedEvent(event));

			deleteInvocation(invocationRepo, invocation);
		}

		return invocations.size();
	}

	@Metered
	private void deleteInvocation(InvocationRepo invocationRepo, Invocation invocation) {
		invocationRepo.delete(invocation);
	}
}
