/*
 * Copyright (C) 2021 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.infrastructure.util;

import com.sailpoint.ets.domain.subscription.Subscription;
import com.sailpoint.ets.domain.subscription.SubscriptionType;
import com.sailpoint.ets.infrastructure.web.dto.WorkflowConfigDto;
import org.modelmapper.Converter;
import org.modelmapper.spi.MappingContext;

import java.util.Map;

/**
 * Utility class for converting WorkflowConfigDto map to WorkflowConfigDto .
 */
public class WorkflowConfigConverter implements Converter<Map<String, Object>, WorkflowConfigDto> {
	public final static String WORKFLOW_ID = "workflowId";

	/**
	 * {@inheritDoc}
	 */
	@Override
	public WorkflowConfigDto convert(MappingContext<Map<String, Object>, WorkflowConfigDto> context) {
		Map<String, Object> workflowConfig = context.getSource();
		Subscription subscription = (Subscription) context.getParent().getSource();
		if (subscription.getType() == SubscriptionType.WORKFLOW) {
			WorkflowConfigDto workflowConfigDto = new WorkflowConfigDto();
			workflowConfigDto.setWorkflowId((String)workflowConfig.get(WORKFLOW_ID));
			return workflowConfigDto;
		} else {
			return null;
		}
	}
}
