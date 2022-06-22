/*
 * Copyright (C) 2019 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.domain.command;

import com.sailpoint.atlas.RequestContext;
import com.sailpoint.ets.EtsProperties;
import com.sailpoint.ets.domain.subscription.Subscription;
import com.sailpoint.ets.domain.subscription.SubscriptionRepo;
import com.sailpoint.ets.domain.subscription.SubscriptionType;
import com.sailpoint.ets.domain.TenantId;
import com.sailpoint.ets.domain.trigger.Trigger;
import com.sailpoint.ets.domain.trigger.EtsFeatureStore;
import com.sailpoint.ets.domain.trigger.TriggerId;
import com.sailpoint.ets.domain.trigger.TriggerRepo;
import com.sailpoint.ets.domain.trigger.TriggerType;
import com.sailpoint.ets.exception.DuplicatedSubscriptionException;
import com.sailpoint.ets.exception.IllegalSubscriptionTypeException;
import com.sailpoint.ets.exception.NotFoundException;
import com.sailpoint.ets.exception.LimitExceededException;
import com.sailpoint.ets.exception.ValidationException;
import com.sailpoint.ets.infrastructure.aws.EventBridge;
import lombok.Builder;
import lombok.Value;
import lombok.extern.apachecommons.CommonsLog;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static com.sailpoint.ets.infrastructure.util.EventBridgeConfigConverter.AWS_ACCOUNT_NUMBER;
import static com.sailpoint.ets.infrastructure.util.EventBridgeConfigConverter.AWS_PARTNER_EVENT_SOURCE_ARN;
import static com.sailpoint.ets.infrastructure.util.EventBridgeConfigConverter.AWS_PARTNER_EVENT_SOURCE_NAME;
import static com.sailpoint.ets.infrastructure.util.EventBridgeConfigConverter.AWS_REGION;

/**
 * SubscribeCommand
 */
@Value
@Builder
@CommonsLog
public class SubscribeCommand {

	private final UUID _id;
	private final TenantId _tenantId;
	private final TriggerId _triggerId;
	private final SubscriptionType _type;
	private final Duration _responseDeadline;
	private final Map<String, Object> _config;
	private final String _filter;
	private final String _scriptSource;
	private final String _name;
	private final String _description;
	private final boolean _enabled;

	private final String GOVCLOUD_PREFIX = "us-gov-";

	public Subscription handle(TriggerRepo triggerRepo, SubscriptionRepo subscriptionRepo, EventBridge eventBridge, EtsFeatureStore etsFeatureStore, EtsProperties properties) {
		Trigger trigger = triggerRepo.findById(_triggerId).filter(t -> t.isEnabledForTenant(etsFeatureStore))
				.orElseThrow(() -> new NotFoundException("trigger", _triggerId.toString()));

		// For fire and forget trigger type, verify if this subscription will exceed the subscription limit.
		// For other trigger type, verify if there is duplication.
		if (trigger.getType() != null && TriggerType.FIRE_AND_FORGET == trigger.getType())
		{
			long subscriptionCount = subscriptionRepo.findAllByTenantIdAndTriggerId(_tenantId, _triggerId).count();
			if (subscriptionCount >= properties.getSubscriptionLimit()) {
				throw new LimitExceededException("subscription", String.valueOf(properties.getSubscriptionLimit()));
			}

		} else {
			subscriptionRepo.findByTenantIdAndTriggerId(_tenantId, _triggerId)
					.ifPresent(s -> {
						throw new DuplicatedSubscriptionException(_triggerId.toString());
					});
		}

		// For script subscription, verify if this subscription will exceed the script subscription limit.
		if (_type == SubscriptionType.SCRIPT) {
			if (subscriptionRepo.findAllByTenantIdAndType(_tenantId, _type).count() >= properties.getScriptSubscriptionLimit()) {
				throw new LimitExceededException("script subscription", String.valueOf(properties.getScriptSubscriptionLimit()));
			}
		} else if(_type == SubscriptionType.EVENTBRIDGE) {
			String account = (String) _config.get(AWS_ACCOUNT_NUMBER);
			String region = (String) _config.get(AWS_REGION);
			if(region.startsWith(GOVCLOUD_PREFIX))
			{
				log.warn("Attempting to create AWS Partner Event Source in US GovCloud.  This might not be supported by AWS.");
			}

			String prefix = properties.getEventBridgePartnerEventSourcePrefix();
			String[] fields = _triggerId.getValue().split(":");
			String partnerEventSourceName = prefix + "/" + _id.toString() + "/" + fields[0] + "/" + fields[1];

			String eventSourceArn = eventBridge.createPartnerEventSource(account, region, partnerEventSourceName);
			_config.put(AWS_PARTNER_EVENT_SOURCE_NAME, partnerEventSourceName);
			_config.put(AWS_PARTNER_EVENT_SOURCE_ARN, eventSourceArn);
		} else if(_type == SubscriptionType.WORKFLOW) {
			if (!RequestContext.ensureGet().getJwt().isInternal()) {
				throw new IllegalSubscriptionTypeException("Subscription type WORKFLOW is not allowed.");
			}
		}

		Subscription subscription = Subscription.builder()
				.id(_id)
				.tenantId(_tenantId)
				.triggerId(_triggerId)
				.responseDeadline(_responseDeadline)
				.type(_type)
				.config(_config)
				.filter(_filter)
				.scriptSource(_scriptSource)
				.name(_name)
				.description(_description)
				.enabled(_enabled)
				.build();
		subscriptionRepo.save(subscription);

		return subscription;
	}

}
