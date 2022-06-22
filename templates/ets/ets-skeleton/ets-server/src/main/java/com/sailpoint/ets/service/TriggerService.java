/*
 * Copyright (C) 2019 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.sailpoint.atlas.util.AwsEncryptionServiceUtil;
import com.sailpoint.ets.EtsProperties;
import com.sailpoint.ets.domain.TenantId;
import com.sailpoint.ets.domain.command.CompleteExpiredInvocationsCommand;
import com.sailpoint.ets.domain.command.CompleteInvocationCommand;
import com.sailpoint.ets.domain.command.DeleteTenantCommand;
import com.sailpoint.ets.domain.command.DispatchInvocationCommand;
import com.sailpoint.ets.domain.command.InvokeTestTriggerCommand;
import com.sailpoint.ets.domain.command.InvokeTriggerCommand;
import com.sailpoint.ets.domain.command.SubscribeCommand;
import com.sailpoint.ets.domain.command.UnsubscribeCommand;
import com.sailpoint.ets.domain.command.UpdateSubscriptionCommand;
import com.sailpoint.ets.domain.command.status.CompleteInvocationStatusCommand;
import com.sailpoint.ets.domain.command.status.CreateInvocationStatusCommand;
import com.sailpoint.ets.domain.event.EventPublisher;
import com.sailpoint.ets.domain.invocation.Invocation;
import com.sailpoint.ets.domain.invocation.InvocationCallbackUrlProvider;
import com.sailpoint.ets.domain.invocation.InvocationRepo;
import com.sailpoint.ets.domain.status.InvocationStatus;
import com.sailpoint.ets.domain.subscription.Subscription;
import com.sailpoint.ets.domain.subscription.SubscriptionRepo;
import com.sailpoint.ets.domain.trigger.EtsFeatureStore;
import com.sailpoint.ets.domain.trigger.Trigger;
import com.sailpoint.ets.domain.trigger.TriggerId;
import com.sailpoint.ets.domain.trigger.TriggerRepo;
import com.sailpoint.ets.infrastructure.aws.Invoker;
import com.sailpoint.ets.infrastructure.event.PersistedEvent;
import com.sailpoint.ets.infrastructure.event.PersistedEventRepo;
import com.sailpoint.ets.infrastructure.status.DynamoDBInvocationStatusRepo;
import com.sailpoint.ets.infrastructure.util.HTTPConfigConverter;
import com.sailpoint.ets.infrastructure.util.HashService;
import com.sailpoint.ets.infrastructure.util.ScriptConfigConverter;
import com.sailpoint.ets.service.breaker.CircuitBreakerService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Service layer for the entire application. Each top-level method
 * in this service represents a capability of the system.
 */
@Component
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class TriggerService {

	private final TriggerRepo _triggerRepo;
	private final SubscriptionRepo _subscriptionRepo;
	private final InvocationRepo _invocationRepo;
	private final PersistedEventRepo _persistedEventRepo;
	private final EventPublisher _eventPublisher;
	private final Invoker _invoker;
	private final InvocationCallbackUrlProvider _invocationCallbackUrlProvider;
	private final HashService _hashService;
	private final EtsProperties _properties;
	private final AwsEncryptionServiceUtil _awsEncryptionServiceUtil;
	private final CircuitBreakerService _circuitBreakerService;
	private final EtsFeatureStore _etsFeatureStore;
	private final DynamoDBInvocationStatusRepo _dynamoDBInvocationStatusRepo;
	private final ObjectMapper _objectMapper;

	private HTTPConfigConverter _httpConfigconverter;
	private ScriptConfigConverter _scriptConfigconverter;

	@PostConstruct
	public void init() {
		_httpConfigconverter = new HTTPConfigConverter(_awsEncryptionServiceUtil);
		_scriptConfigconverter = new ScriptConfigConverter(_awsEncryptionServiceUtil);
	}

	/**
	 * Lists all triggers in the system.
	 *
	 * @return The list of all triggers.
	 */
	@Transactional
	public List<Trigger> listTriggers() {
		return _triggerRepo.findAll()
			.collect(Collectors.toList());
	}

	@Transactional
	public Optional<Trigger> findByTriggerId(TriggerId triggerId){
		return _triggerRepo.findById(triggerId);
	}

	/**
	 * Lists the subscriptions for the specified tenant.
	 *
	 * @param tenantId The tenant ID.
	 * @return The list of subscriptions by the specified tenant.
	 */
	@Transactional
	public List<Subscription> listSubscriptions(TenantId tenantId) {
		return _subscriptionRepo.findAllByTenantId(tenantId)
			.collect(Collectors.toList());
	}

	/**
	 * Get a {@link Subscription} based on tenantId and id.
	 *
	 * @param tenantId     Tenant
	 * @param id subscription id
	 * @return {@code Optional<Subscription>}
	 */
	@Transactional
	public Optional<Subscription> getSubscription(TenantId tenantId, UUID id ) {
		return _subscriptionRepo.findByTenantIdAndId(tenantId, id);
	}

	/**
	 * Get a {@link Page} of {@link Subscription} based on specified filter and pagination options.
	 *
	 * @param spec     Specification/filter option.
	 * @param pageable Pagination/sort option.
	 * @return {@code Page<Subscription>}
	 */
	@Transactional
	public Page<Subscription> listSubscriptions(Specification<Subscription> spec, Pageable pageable) {
		return _subscriptionRepo.findAll(spec, pageable);
	}

	/**
	 * Lists all invocations.
	 *
	 * @param tenantId The tenant ID.
	 * @return The list of invocations.
	 */
	@Transactional
	public List<Invocation> listActiveInvocations(TenantId tenantId) {
		return _invocationRepo.findAllByTenantId(tenantId)
			.collect(Collectors.toList());
	}

	/**
	 * Lists all persisted events.
	 *
	 * @return The list of persisted events.
	 */
	@Transactional
	public List<PersistedEvent> listEvents() {
		return ImmutableList.copyOf(_persistedEventRepo.findAll());
	}

	/**
	 * Subscribes to a trigger.
	 *
	 * @param cmd The input command.
	 * @return The created subscription.
	 */
	@Transactional
	public Subscription subscribe(SubscribeCommand cmd) {
		return cmd.handle(_triggerRepo, _subscriptionRepo, _invoker, _etsFeatureStore, _properties);
	}

	/**
	 * Update subscription to a trigger.
	 *
	 * @param cmd The input command.
	 * @return The updated subscription.
	 */
	@Transactional
	public Subscription updateSubscription(UpdateSubscriptionCommand cmd) {
		return cmd.handle(_triggerRepo, _subscriptionRepo, _etsFeatureStore);
	}

	/**
	 * Unsubscribes from a trigger.
	 *
	 * @param cmd The input command.
	 */
	@Transactional
	public void unsubscribe(UnsubscribeCommand cmd) {
		cmd.handle(_subscriptionRepo, _invoker);
	}

	/**
	 * Invokes a trigger.
	 *
	 * @param cmd The input command.
	 * @return The optional Invocation. An empty optional is returned if no subscription exists.
	 */
	@Transactional
	public List<Invocation> invokeTrigger(InvokeTriggerCommand cmd) {
		return cmd.handle(_triggerRepo, _subscriptionRepo, _invocationRepo, _eventPublisher, _properties, _etsFeatureStore);
	}

	/**
	 * Invokes a test trigger.
	 *
	 * @param cmd The input test command.
	 * @return The optional Invocation. An empty optional is returned if no subscription exists.
	 */
	@Transactional
	public List<Invocation> invokeTestTrigger(InvokeTestTriggerCommand cmd) {
		return cmd.handle(_triggerRepo, _subscriptionRepo, _invocationRepo, _eventPublisher, _properties, _etsFeatureStore);
	}

	/**
	 * Completes an invocation.
	 *
	 * @param cmd The input command.
	 */
	@Transactional
	public void completeInvocation(CompleteInvocationCommand cmd) {
		cmd.handle(_triggerRepo, _subscriptionRepo, _invocationRepo, _eventPublisher, _hashService, _circuitBreakerService);
	}

	/**
	 * Dispatches an invocation.
	 *
	 * @param cmd The input command.
	 */
	@Transactional
	public void dispatchInvocation(DispatchInvocationCommand cmd) {
		CircuitBreaker circuitBreaker = _circuitBreakerService.getCircuitBreaker(cmd.getTenantId().toString() + "_" + cmd.getTriggerId());
		cmd.handle(_invoker, _invocationCallbackUrlProvider, _properties, _httpConfigconverter,
			_scriptConfigconverter, circuitBreaker, _objectMapper, _eventPublisher, _etsFeatureStore);
	}

	/**
	 * Deletes all invocations that haven't been completed before their deadline, broadcasting
	 * a failure event for each.
	 *
	 * @param cmd The input command.
	 * @return The number of invocations that were expired.
	 */
	@Transactional
	public int completeExpiredInvocations(CompleteExpiredInvocationsCommand cmd) {
		return cmd.handle(_invocationRepo, _eventPublisher);
	}

	/**
	 * Delete tenant and clean up subscription, invocation repo
	 *
	 * @param cmd delete tenant command.
	 */
	@Transactional
	public void deleteTenant(DeleteTenantCommand cmd) {
		cmd.handle(_subscriptionRepo, _invocationRepo);
	}

	/**
	 * Get a {@link Stream} of {@link InvocationStatus} for specified tenant.
	 * @param tenantId Tenant ID
	 * @return {@code Stream<InvocationStatus>}
	 */
	public Stream<InvocationStatus> listInvocationStatuses(TenantId tenantId) {
		return _dynamoDBInvocationStatusRepo.findByTenantId(tenantId);
	}

	public Optional<InvocationStatus> listInvocationStatusesById(TenantId tenantId, UUID invocationStatusId) {
		return _dynamoDBInvocationStatusRepo.findByTenantIdAndId(tenantId, invocationStatusId);
	}

	/**
	 * Create InvocationStatus in invocation status table.
	 * @param cmd create command.
	 */
	public void createCreateInvocationStatus(CreateInvocationStatusCommand cmd) {
		cmd.handle(_dynamoDBInvocationStatusRepo);
	}

	/**
	 * Complete InvocationStatus in invocation status table.
	 * @param cmd create command.
	 */
	public void completeInvocationStatus(CompleteInvocationStatusCommand cmd) {
		cmd.handle(_dynamoDBInvocationStatusRepo);
	}
}
