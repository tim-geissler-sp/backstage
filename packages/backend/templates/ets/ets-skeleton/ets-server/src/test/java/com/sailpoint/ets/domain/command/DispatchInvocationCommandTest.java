/*
 * Copyright (C) 2019 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.ets.domain.command;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.sailpoint.atlas.util.AwsEncryptionServiceUtil;
import com.sailpoint.ets.EtsProperties;
import com.sailpoint.ets.domain.Secret;
import com.sailpoint.ets.domain.TenantId;
import com.sailpoint.ets.domain.event.DomainEvent;
import com.sailpoint.ets.domain.event.EventPublisher;
import com.sailpoint.ets.domain.event.TriggerWorkflowEvent;
import com.sailpoint.ets.domain.invocation.InvocationCallbackUrlProvider;
import com.sailpoint.ets.domain.subscription.ScriptLanguageType;
import com.sailpoint.ets.domain.subscription.SubscriptionType;
import com.sailpoint.ets.domain.trigger.EtsFeatureStore;
import com.sailpoint.ets.domain.trigger.TriggerType;
import com.sailpoint.ets.infrastructure.aws.HttpInvocationPayload;
import com.sailpoint.ets.infrastructure.aws.InvocationPayload;
import com.sailpoint.ets.infrastructure.aws.Invoker;
import com.sailpoint.ets.infrastructure.aws.ScriptInvocationPayload;
import com.sailpoint.ets.infrastructure.util.HTTPConfigConverter;
import com.sailpoint.ets.infrastructure.util.ScriptConfigConverter;
import com.sailpoint.ets.infrastructure.util.WorkflowConfigConverter;
import com.sailpoint.ets.infrastructure.web.dto.ResponseMode;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.apache.commons.codec.binary.Base64;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.UUID;

import static com.sailpoint.ets.infrastructure.util.ScriptConfigConverter.RESPONSE_MODE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for DispatchInvocationCommand
 */
@RunWith(MockitoJUnitRunner.class)
public class DispatchInvocationCommandTest {

	@Captor
	ArgumentCaptor<String> _functionNameCaptor;

	@Captor
	ArgumentCaptor<InvocationPayload> _functionArgumentCaptor;

	@Mock
	Invoker _invoker;

	@Mock
	InvocationCallbackUrlProvider _invocationCallbackUrlProvider;

	@Mock
	AwsEncryptionServiceUtil _awsEncryptionServiceUtil;

	@Mock
	CircuitBreaker _circuitBreaker;

	@Mock
	ObjectMapper _objectMapper;

	@Mock
	EventPublisher _publisher;

	@Mock
	EtsFeatureStore _flagEtsFeatureStore;

	private DispatchInvocationCommand _cmd;
	private EtsProperties _properties;
	private HTTPConfigConverter _httpConfigConverter;
	private ScriptConfigConverter _scriptConfigConverter;

	@Before
	public void setUp() {
		when(_invocationCallbackUrlProvider.getCallbackUrl(any(), any()))
			.thenReturn("http://invocation-callback");
		_properties = new EtsProperties();
		_properties.setDeadlineMinutes(1);
		_properties.setLambdaNamePrefix("ets-handler-");
		_httpConfigConverter = new HTTPConfigConverter(_awsEncryptionServiceUtil);
		_scriptConfigConverter = new ScriptConfigConverter(_awsEncryptionServiceUtil);
		when(_awsEncryptionServiceUtil.decryptDataWithoutCheckKey(eq(Base64.decodeBase64(Base64.encodeBase64String("aladdin".getBytes()))), any()))
			.thenReturn("aladdin".getBytes());
		when(_awsEncryptionServiceUtil.decryptDataWithoutCheckKey(eq(Base64.decodeBase64(Base64.encodeBase64String("opensesame".getBytes()))), any()))
			.thenReturn("opensesame".getBytes());
		when(_awsEncryptionServiceUtil.decryptDataWithoutCheckKey(eq(Base64.decodeBase64(Base64.encodeBase64String("eyJhbGciOi".getBytes()))), any()))
			.thenReturn("eyJhbGciOi".getBytes());
		when(_awsEncryptionServiceUtil.decryptDataWithoutCheckKey(eq(Base64.decodeBase64(Base64.encodeBase64String("dummyJavascript".getBytes()))), any()))
			.thenReturn("dummyJavascript".getBytes());
		when(_circuitBreaker.getState())
			.thenReturn(CircuitBreaker.State.CLOSED);
	}

	@Test
	public void dispatchesToHttpFunctionWithBasicAuthConfig() {
		givenCommandWithBasicAuthConfig();
		whenTheCommandIsHandled();
		thenTheFunctionIsInvoked("ets-handler-http");

		InvocationPayload payload = _functionArgumentCaptor.getValue();

		assertTrue(payload instanceof HttpInvocationPayload);
		assertEquals(((HttpInvocationPayload)payload).getUrl(), "http://sample-url.com");
		assertNotNull(payload.getHeaders());
		assertEquals(payload.getHeaders().get("Authorization"), "Basic YWxhZGRpbjpvcGVuc2VzYW1l");
	}

	@Test
	public void dispatchesToHttpFunctionWithBearerAuthConfig() {
		givenCommandWithBearerAuthConfig();
		whenTheCommandIsHandled();
		thenTheFunctionIsInvoked("ets-handler-http");

		InvocationPayload payload = _functionArgumentCaptor.getValue();

		assertTrue(payload instanceof HttpInvocationPayload);
		assertEquals(((HttpInvocationPayload)payload).getUrl(), "http://sample-url.com");
		assertNotNull(payload.getHeaders());
		assertEquals(payload.getHeaders().get("Authorization"), "Bearer eyJhbGciOi");
	}

	@Test
	public void testDispatchScriptInvocation() {
		givenScriptInvocationCommand();
		whenTheCommandIsHandled();
		thenTheFunctionIsInvoked("ets-handler-script-js");

		InvocationPayload payload = _functionArgumentCaptor.getValue();

		assertTrue(payload instanceof ScriptInvocationPayload);
		assertEquals(((ScriptInvocationPayload)payload).getScriptCode(), java.util.Base64.getEncoder().encodeToString("dummyJavascript".getBytes()));
	}

	@Test
	public void testDispatchWorkflowInvocationUsingPublisher() {
		givenWorkflowInvocationCommand();

		//When the command is handled
		_cmd.handle(_invoker, _invocationCallbackUrlProvider, _properties, _httpConfigConverter,
			_scriptConfigConverter, _circuitBreaker, _objectMapper, _publisher,  _flagEtsFeatureStore);

		//Then event publisher is invoked
		ArgumentCaptor<TriggerWorkflowEvent> workflowExecutedEventArgumentCaptor = ArgumentCaptor.forClass(TriggerWorkflowEvent.class);
		verify(_publisher, times(1)).publish(workflowExecutedEventArgumentCaptor.capture());

		TriggerWorkflowEvent workflowExecutionEvent = workflowExecutedEventArgumentCaptor.getValue();
		assertNotNull(workflowExecutionEvent);
		assertTrue(workflowExecutionEvent instanceof DomainEvent);
		assertEquals("dev#acme-solar", workflowExecutionEvent.getTenantId());
		assertEquals("test1", workflowExecutionEvent.getWorkflowId());
		assertEquals("test:request-response", workflowExecutionEvent.getTriggerId());
	}

	private void givenCommandWithBasicAuthConfig() {
		_cmd = getCommandBuilder()
				.subscriptionConfig(ImmutableMap.of("url", "http://sample-url.com",
					"httpAuthenticationType", "BASIC_AUTH",
					"basicAuthConfig", ImmutableMap.of("userName", Base64.encodeBase64String("aladdin".getBytes()),
						"password", Base64.encodeBase64String("opensesame".getBytes())))
				)
				.build();
	}

	private void givenCommandWithBearerAuthConfig() {
		_cmd = getCommandBuilder()
			.subscriptionConfig(ImmutableMap.of("url", "http://sample-url.com",
				"httpAuthenticationType", "BEARER_TOKEN",
				"bearerTokenAuthConfig", ImmutableMap.of("bearerToken", Base64.encodeBase64String("eyJhbGciOi".getBytes())))
			)
			.build();
	}

	private void givenScriptInvocationCommand() {
		_cmd = DispatchInvocationCommand.builder()
			.input(Collections.singletonMap("input1", "value1"))
			.tenantId(new TenantId("dev#acme-solar"))
			.triggerId("test:request-response")
			.requestId(UUID.randomUUID().toString())
			.invocationId(UUID.fromString("0612a993-a2f8-4365-9dcc-4b5d620a64f0"))
			.secret(new Secret("12345"))
			.triggerType(TriggerType.FIRE_AND_FORGET)
			.subscriptionId(UUID.randomUUID().toString())
			.subscriptionType(SubscriptionType.SCRIPT)
			.subscriptionConfig(ImmutableMap.of(ScriptConfigConverter.LANGUAGE, ScriptLanguageType.JAVASCRIPT.name(),
				RESPONSE_MODE, ResponseMode.SYNC))
			.scriptSource(Base64.encodeBase64String("dummyJavascript".getBytes()))
			.headers(ImmutableMap.of("tenantId", UUID.randomUUID().toString()))
			.build();
	}

	private void givenWorkflowInvocationCommand() {
		_cmd = DispatchInvocationCommand.builder()
			.input(Collections.singletonMap("input1", "value1"))
			.tenantId(new TenantId("dev#acme-solar"))
			.triggerId("test:request-response")
			.requestId(UUID.randomUUID().toString())
			.invocationId(UUID.fromString("0612a993-a2f8-4365-9dcc-4b5d620a64f0"))
			.secret(new Secret("12345"))
			.triggerType(TriggerType.REQUEST_RESPONSE)
			.subscriptionId(UUID.randomUUID().toString())
			.subscriptionType(SubscriptionType.WORKFLOW)
			.subscriptionConfig(ImmutableMap.of(WorkflowConfigConverter.WORKFLOW_ID, "test1"))
			.headers(ImmutableMap.of("tenantId", UUID.randomUUID().toString()))
			.build();
	}

	private DispatchInvocationCommand.DispatchInvocationCommandBuilder getCommandBuilder() {
		return DispatchInvocationCommand.builder()
			.input(Collections.singletonMap("input1", "value1"))
			.tenantId(new TenantId("dev#acme-solar"))
			.triggerId("test:request-response")
			.requestId(UUID.randomUUID().toString())
			.invocationId(UUID.fromString("0612a993-a2f8-4365-9dcc-4b5d620a64f0"))
			.secret(new Secret("12345"))
			.triggerType(TriggerType.REQUEST_RESPONSE)
			.subscriptionId(UUID.randomUUID().toString())
			.subscriptionType(SubscriptionType.HTTP)
			.headers(ImmutableMap.of("tenantId", UUID.randomUUID().toString()));
	}

	private void whenTheCommandIsHandled() {
		_cmd.handle(_invoker, _invocationCallbackUrlProvider, _properties, _httpConfigConverter,
			_scriptConfigConverter, _circuitBreaker, _objectMapper, _publisher, _flagEtsFeatureStore);
		verify(_invoker).invokeLambdaFunction(_functionNameCaptor.capture(), _functionArgumentCaptor.capture());
	}

	private void thenTheFunctionIsInvoked(String name) {
		assertEquals(name, _functionNameCaptor.getValue());
	}
}
