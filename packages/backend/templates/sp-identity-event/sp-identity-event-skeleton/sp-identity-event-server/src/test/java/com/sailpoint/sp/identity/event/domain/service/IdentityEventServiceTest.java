/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.sp.identity.event.domain.service;

import com.sailpoint.atlas.featureflag.FeatureFlagService;
import com.sailpoint.sp.identity.event.domain.Account;
import com.sailpoint.sp.identity.event.domain.AccountId;
import com.sailpoint.sp.identity.event.domain.App;
import com.sailpoint.sp.identity.event.domain.AppId;
import com.sailpoint.sp.identity.event.domain.IdentityId;
import com.sailpoint.sp.identity.event.domain.ReferenceType;
import com.sailpoint.sp.identity.event.domain.SourceId;
import com.sailpoint.sp.identity.event.domain.TenantId;
import com.sailpoint.sp.identity.event.domain.command.DeleteIdentityCommand;
import com.sailpoint.sp.identity.event.domain.command.UpdateIdentityCommand;
import com.sailpoint.sp.identity.event.domain.event.AccountReference;
import com.sailpoint.sp.identity.event.domain.event.AppReference;
import com.sailpoint.sp.identity.event.domain.event.AttributeChange;
import com.sailpoint.sp.identity.event.domain.event.DisplayableIdentityReference;
import com.sailpoint.sp.identity.event.domain.event.IdentityAccessAddedEvent;
import com.sailpoint.sp.identity.event.domain.event.IdentityAccessRemovedEvent;
import com.sailpoint.sp.identity.event.domain.event.IdentityAccountAttributesChangedEvent;
import com.sailpoint.sp.identity.event.domain.event.IdentityAccountCorrelatedEvent;
import com.sailpoint.sp.identity.event.domain.event.IdentityAccountDisabledEvent;
import com.sailpoint.sp.identity.event.domain.event.IdentityAccountEnabledEvent;
import com.sailpoint.sp.identity.event.domain.event.IdentityAccountLockedEvent;
import com.sailpoint.sp.identity.event.domain.event.IdentityAccountUncorrelatedEvent;
import com.sailpoint.sp.identity.event.domain.event.IdentityAccountUnlockedEvent;
import com.sailpoint.sp.identity.event.domain.event.IdentityAppAddedEvent;
import com.sailpoint.sp.identity.event.domain.event.IdentityAppRemovedEvent;
import com.sailpoint.sp.identity.event.domain.event.IdentityAttributesChangedEvent;
import com.sailpoint.sp.identity.event.domain.event.IdentityCreatedEvent;
import com.sailpoint.sp.identity.event.domain.event.IdentityDeletedEvent;
import com.sailpoint.sp.identity.event.domain.event.IdentityDisabledEvent;
import com.sailpoint.sp.identity.event.domain.event.IdentityEnabledEvent;
import com.sailpoint.sp.identity.event.domain.event.IdentityEvent;
import com.sailpoint.sp.identity.event.domain.event.IdentityManagerChangedEvent;
import com.sailpoint.sp.identity.event.domain.event.IdentityReference;
import com.sailpoint.sp.identity.event.domain.event.SourceReference;
import com.sailpoint.sp.identity.event.infrastructure.MemoryIdentityEventPublisher;
import com.sailpoint.sp.identity.event.infrastructure.MemoryIdentityStateRepository;
import com.sailpoint.sp.identity.event.infrastructure.sync.SyncUtil;
import org.junit.Before;
import org.junit.Test;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for IdentityEventService
 */
public class IdentityEventServiceTest {

	private MemoryIdentityStateRepository _identityStateRepository = new MemoryIdentityStateRepository();
	private MemoryIdentityEventPublisher _identityEventPublisher = new MemoryIdentityEventPublisher();
	private final FeatureFlagService _featureFlagService = mock(FeatureFlagService.class);
	private IdentityEventPublishService _identityEventPublishService = new IdentityEventPublishService(_identityEventPublisher, _featureFlagService);
	private IdentityEventService _identityEventService = new IdentityEventService(_identityStateRepository, _identityEventPublishService);

	@Before
	public void setUp() {
		_identityStateRepository = new MemoryIdentityStateRepository();
		_identityEventPublisher = new MemoryIdentityEventPublisher();

		when(_featureFlagService.getBoolean(IdentityEventPublishService.SP_IDENTITY_EVENT_ACCOUNT_HANDLERS_ENABLED, false)).thenReturn(true);
		when(_featureFlagService.getBoolean(IdentityEventPublishService.SP_IDENTITY_EVENT_DISABLED_LOCKED_CHANGED, false)).thenReturn(true);
		_identityEventPublishService = new IdentityEventPublishService(_identityEventPublisher, _featureFlagService);
		_identityEventService = new IdentityEventService(_identityStateRepository, _identityEventPublishService);
	}

	@Test
	public void testIdentityEvents() {
		{
			UpdateIdentityCommand cmd = defaultUpdateIdentityCommand()
				.attribute("foo", "bar")
				.attribute("foo2", "bar2")
				.attribute("foo3", "bar3")
				.build();

			_identityEventService.updateIdentity(cmd);

			List<IdentityEvent> events = _identityEventPublisher.getEvents();
			assertEquals(1, events.size());
			assertTrue(events.get(0) instanceof IdentityCreatedEvent);

			IdentityCreatedEvent event = (IdentityCreatedEvent)events.get(0);
			assertEquals("1234", event.getIdentity().getId());
			assertEquals("john.doe", event.getIdentity().getName());
			assertEquals("bar", event.getAttributes().get("foo"));
			assertEquals("bar2", event.getAttributes().get("foo2"));
			assertEquals("bar3", event.getAttributes().get("foo3"));

			_identityEventPublisher.clear();
		}

		{
			UpdateIdentityCommand cmd = defaultUpdateIdentityCommand()
				.attribute("foo", "bar")
				.attribute("foo3", "bar4")
				.build();

			_identityEventService.updateIdentity(cmd);

			List<IdentityEvent> events = _identityEventPublisher.getEvents();
			assertEquals(1, events.size());
			assertTrue(events.get(0) instanceof IdentityAttributesChangedEvent);

			IdentityAttributesChangedEvent event = (IdentityAttributesChangedEvent)events.get(0);
			assertEquals("1234", event.getIdentity().getId());
			assertEquals("john.doe", event.getIdentity().getName());

			List<AttributeChange> changes = event.getChanges();
			assertEquals(2, changes.size());

			AttributeChange foo2Change = event.getChange("foo2")
				.orElseThrow(() -> new IllegalStateException("expected foo2 change"));

			assertEquals("foo2", foo2Change.getAttribute());
			assertEquals("bar2", foo2Change.getOldValue());
			assertNull(foo2Change.getNewValue());

			AttributeChange foo3Change = event.getChange("foo3")
				.orElseThrow(() -> new IllegalStateException("expected foo3 change"));

			assertEquals("foo3", foo3Change.getAttribute());
			assertEquals("bar3", foo3Change.getOldValue());
			assertEquals("bar4", foo3Change.getNewValue());

			_identityEventPublisher.clear();
		}

		{
			UpdateIdentityCommand cmd = defaultUpdateIdentityCommand()
				.attribute("foo", "bar")
				.attribute("foo3", "bar5")
				.build();

			_identityEventService.updateIdentity(cmd);

			List<IdentityEvent> events = _identityEventPublisher.getEvents();
			assertEquals(1, events.size());
			assertTrue(events.get(0) instanceof IdentityAttributesChangedEvent);

			IdentityAttributesChangedEvent event = (IdentityAttributesChangedEvent)events.get(0);
			assertEquals("1234", event.getIdentity().getId());
			assertEquals("john.doe", event.getIdentity().getName());

			List<AttributeChange> changes = event.getChanges();
			assertEquals(1, changes.size());

			AttributeChange foo3Change = event.getChange("foo3")
				.orElseThrow(() -> new IllegalStateException("expected foo3 change"));

			assertEquals("foo3", foo3Change.getAttribute());
			assertEquals("bar4", foo3Change.getOldValue());
			assertEquals("bar5", foo3Change.getNewValue());

			_identityEventPublisher.clear();
		}

		{
			UpdateIdentityCommand cmd = defaultUpdateIdentityCommand()
				.attribute("foo", "bar")
				.attribute("foo3", "bar6")
				.attribute("manager", DisplayableIdentityReference.builder()
					.id("managerId")
					.name("managerName")
					.displayName("Manager Name")
					.type(ReferenceType.IDENTITY)
					.build().toAttributesMap())
				.build();

			_identityEventService.updateIdentity(cmd);

			List<IdentityEvent> events = _identityEventPublisher.getEvents();
			assertEquals(2, events.size());
			assertTrue(events.get(0) instanceof IdentityAttributesChangedEvent);
			assertTrue(events.get(1) instanceof IdentityManagerChangedEvent);

			IdentityAttributesChangedEvent event = (IdentityAttributesChangedEvent)events.get(0);
			assertEquals("1234", event.getIdentity().getId());
			assertEquals("john.doe", event.getIdentity().getName());

			List<AttributeChange> changes = event.getChanges();
			assertEquals(2, changes.size());

			AttributeChange foo3Change = event.getChange("foo3")
				.orElseThrow(() -> new IllegalStateException("expected foo3 change"));

			assertEquals("foo3", foo3Change.getAttribute());
			assertEquals("bar5", foo3Change.getOldValue());
			assertEquals("bar6", foo3Change.getNewValue());

			AttributeChange managerChange = event.getChange("manager")
				.orElseThrow(() -> new IllegalStateException("expected manager change"));

			assertEquals("manager", managerChange.getAttribute());
			assertNull(managerChange.getOldValue());
			assertEquals("managerId", ((Map<String, Object>)managerChange.getNewValue()).get("id"));

			IdentityManagerChangedEvent managerChangedEvent = (IdentityManagerChangedEvent) events.get(1);
			assertEquals("1234", event.getIdentity().getId());
			assertEquals("john.doe", event.getIdentity().getName());

			assertNull(managerChangedEvent.getOldManager());
			assertNotNull(managerChangedEvent.getNewManager());
			assertEquals("managerId", managerChangedEvent.getNewManager().getId());
			assertEquals("managerName", managerChangedEvent.getNewManager().getName());
			assertEquals("Manager Name", managerChangedEvent.getNewManager().getDisplayName());

			_identityEventPublisher.clear();
		}

		{
			Map<String, Object> access1 = new HashMap<>();
			access1.put("id", "test1");
			access1.put("type", "ROLE");
			Map<String, Object> access2 = new HashMap<>();
			access2.put("id", "test2");
			access2.put("type", "ACCESS_PROFILE");
			List<Map<String,Object>> accessList = new ArrayList<>();
			accessList.add(access1);
			accessList.add(access2);

			UpdateIdentityCommand cmd = defaultUpdateIdentityCommand()
				.attribute("foo", "bar")
				.attribute("foo3", "bar6")
				.attribute("manager", IdentityReference.builder()
					.id("managerId")
					.name("managerName")
					.type(ReferenceType.IDENTITY)
					.build().toAttributesMap())
				.access(accessList)
				.build();

			_identityEventService.updateIdentity(cmd);

			List<IdentityEvent> events = _identityEventPublisher.getEvents();
			assertEquals(2, events.size());
			assertTrue(events.get(0) instanceof IdentityAccessAddedEvent);
			assertTrue(events.get(1) instanceof IdentityAccessAddedEvent);

			IdentityAccessAddedEvent event = (IdentityAccessAddedEvent) events.get(0);
			assertEquals("test1", event.getAccess().getId());
			assertEquals("ROLE", event.getAccess().getType());

			event = (IdentityAccessAddedEvent) events.get(1);
			assertEquals("test2", event.getAccess().getId());
			assertEquals("ACCESS_PROFILE", event.getAccess().getType());

			_identityEventPublisher.clear();
		}

		{
			// Test that an access item without an ID does not cause problems.  See IDA-8749.
			Map<String, Object> access1 = new HashMap<>();
			access1.put("id", "test1");
			access1.put("type", "ROLE");
			Map<String, Object> access2 = new HashMap<>();
			access2.put("id", "test2");
			access2.put("type", "ACCESS_PROFILE");
			Map<String, Object> accessBoom = new HashMap<>();
			accessBoom.put("id", null);
			accessBoom.put("type", "ENTITLEMENT");
			List<Map<String,Object>> accessList = new ArrayList<>();
			accessList.add(access1);
			accessList.add(access2);
			accessList.add(accessBoom);

			UpdateIdentityCommand cmd = defaultUpdateIdentityCommand()
				.attribute("foo", "bar")
				.attribute("foo3", "bar6")
				.attribute("manager", IdentityReference.builder()
					.id("managerId")
					.name("managerName")
					.type(ReferenceType.IDENTITY)
					.build().toAttributesMap())
				.access(accessList)
				.build();

			_identityEventService.updateIdentity(cmd);

			List<IdentityEvent> events = _identityEventPublisher.getEvents();
			assertTrue("Found events: " + events, events.isEmpty());

			_identityEventPublisher.clear();
		}

		{
			Map<String, Object> access2 = new HashMap<>();
			access2.put("id", "test2");
			access2.put("type", "ACCESS_PROFILE");
			List<Map<String,Object>> accessList = new ArrayList<>();
			accessList.add(access2);

			UpdateIdentityCommand cmd = defaultUpdateIdentityCommand()
				.attribute("foo", "bar")
				.attribute("foo3", "bar6")
				.attribute("manager", IdentityReference.builder()
					.id("managerId")
					.name("managerName")
					.type(ReferenceType.IDENTITY)
					.build().toAttributesMap())
				.access(accessList)
				.build();

			_identityEventService.updateIdentity(cmd);

			List<IdentityEvent> events = _identityEventPublisher.getEvents();
			assertEquals(1, events.size());
			assertTrue(events.get(0) instanceof IdentityAccessRemovedEvent);

			IdentityAccessRemovedEvent event = (IdentityAccessRemovedEvent) events.get(0);
			assertEquals("test1", event.getAccess().getId());
			assertEquals("ROLE", event.getAccess().getType());

			_identityEventPublisher.clear();
		}

		{
			DeleteIdentityCommand cmd = DeleteIdentityCommand.builder()
				.timestamp(OffsetDateTime.now())
				.tenantId(new TenantId("acme-solar"))
				.identityId(new IdentityId("1234"))
				.build();

			_identityEventService.deleteIdentity(cmd);

			List<IdentityEvent> events = _identityEventPublisher.getEvents();
			assertEquals(2, events.size());
			assertTrue(events.get(0) instanceof IdentityAccessRemovedEvent);
			assertTrue(events.get(1) instanceof IdentityDeletedEvent);

			IdentityAccessRemovedEvent event = (IdentityAccessRemovedEvent) events.get(0);
			assertEquals("test2", event.getAccess().getId());
			assertEquals("ACCESS_PROFILE", event.getAccess().getType());

			IdentityDeletedEvent deletedEvent = (IdentityDeletedEvent)events.get(1);
			assertEquals("1234", deletedEvent.getIdentity().getId());
			assertEquals("john.doe", deletedEvent.getIdentity().getName());
			assertEquals("bar", deletedEvent.getAttributes().get("foo"));
			assertEquals("bar6", deletedEvent.getAttributes().get("foo3"));

			_identityEventPublisher.clear();
		}
	}

	@Test
	public void testAccountEvents() {
		List<Account> accounts = SyncUtil.getAccountsFromIdentityMap(validTestAccounts());

		//when create new identity should generate accounts correlated events as well.
		{
			UpdateIdentityCommand cmd = defaultUpdateIdentityCommand()
				.attribute("foo", "bar")
				.attribute("foo2", "bar2")
				.attribute("foo3", "bar3")
				.account(accounts.get(0))
				.build();

			_identityEventService.updateIdentity(cmd);

			List<IdentityEvent> events = _identityEventPublisher.getEvents();
			assertEquals(2, events.size());
			assertTrue(events.get(0) instanceof IdentityCreatedEvent);

			IdentityCreatedEvent event = (IdentityCreatedEvent)events.get(0);
			assertEquals("1234", event.getIdentity().getId());
			assertEquals("john.doe", event.getIdentity().getName());
			assertEquals("bar", event.getAttributes().get("foo"));
			assertEquals("bar2", event.getAttributes().get("foo2"));
			assertEquals("bar3", event.getAttributes().get("foo3"));

			assertTrue(events.get(1) instanceof IdentityAccountCorrelatedEvent);

			IdentityAccountCorrelatedEvent eventAccount = (IdentityAccountCorrelatedEvent)events.get(1);
			assertEquals("1234", eventAccount.getIdentity().getId());
			assertEquals("john.doe", eventAccount.getIdentity().getName());
			assertEquals(ReferenceType.IDENTITY, eventAccount.getIdentity().getType());

			assertEquals("E002", eventAccount.getAccount().getId());
			assertEquals("Algernop Krieger", eventAccount.getAccount().getName());
			assertEquals("E002", eventAccount.getAccount().getNativeIdentity());
			assertEquals("456", eventAccount.getAccount().getUuid());
			assertEquals(4, eventAccount.getEntitlementCount());
			assertEquals(ReferenceType.ACCOUNT, eventAccount.getAccount().getType());

			assertEquals("ff80818159182fec01591830983802a5", eventAccount.getSource().getId());
			assertEquals("HR", eventAccount.getSource().getName());
			assertEquals(ReferenceType.SOURCE, eventAccount.getSource().getType());

			assertNull(eventAccount.getAttributes().get("created"));
			assertNull(eventAccount.getAttributes().get("disabled"));

			_identityEventPublisher.clear();
		}

		//added new account to existing identity.
		{
			UpdateIdentityCommand cmd = defaultUpdateIdentityCommand()
				.attribute("foo", "bar")
				.attribute("foo2", "bar2")
				.attribute("foo3", "bar3")
				.accounts(accounts)
				.build();

			_identityEventService.updateIdentity(cmd);

			List<IdentityEvent> events = _identityEventPublisher.getEvents();
			assertEquals(1, events.size());
			assertTrue(events.get(0) instanceof IdentityAccountCorrelatedEvent);

			IdentityAccountCorrelatedEvent eventAccount = (IdentityAccountCorrelatedEvent)events.get(0);
			assertEquals("1234", eventAccount.getIdentity().getId());
			assertEquals("john.doe", eventAccount.getIdentity().getName());
			assertEquals(ReferenceType.IDENTITY, eventAccount.getIdentity().getType());

			assertEquals("E005", eventAccount.getAccount().getId());
			assertEquals("Cheryl Tunt", eventAccount.getAccount().getName());
			assertEquals("E005", eventAccount.getAccount().getNativeIdentity());
			assertEquals("456", eventAccount.getAccount().getUuid());
			assertEquals(ReferenceType.ACCOUNT, eventAccount.getAccount().getType());
			assertEquals(4, eventAccount.getEntitlementCount());

			assertEquals("cc80818159182fec01591830983802a5", eventAccount.getSource().getId());
			assertEquals("Mainframe", eventAccount.getSource().getName());
			assertEquals(ReferenceType.SOURCE, eventAccount.getSource().getType());

			_identityEventPublisher.clear();
		}

		//remove account from identity.
		{
			UpdateIdentityCommand cmd = defaultUpdateIdentityCommand()
				.attribute("foo", "bar")
				.attribute("foo2", "bar2")
				.attribute("foo3", "bar3")
				.account(accounts.get(1))
				.build();

			_identityEventService.updateIdentity(cmd);

			List<IdentityEvent> events = _identityEventPublisher.getEvents();
			assertEquals(1, events.size());
			assertTrue(events.get(0) instanceof IdentityAccountUncorrelatedEvent);

			IdentityAccountUncorrelatedEvent eventAccount = (IdentityAccountUncorrelatedEvent)events.get(0);
			assertEquals("1234", eventAccount.getIdentity().getId());
			assertEquals("john.doe", eventAccount.getIdentity().getName());
			assertEquals(ReferenceType.IDENTITY, eventAccount.getIdentity().getType());
			assertEquals(4, eventAccount.getEntitlementCount());

			assertEquals("E002", eventAccount.getAccount().getId());
			assertEquals("Algernop Krieger", eventAccount.getAccount().getName());
			assertEquals("E002", eventAccount.getAccount().getNativeIdentity());
			assertEquals("456", eventAccount.getAccount().getUuid());
			assertEquals(ReferenceType.ACCOUNT, eventAccount.getAccount().getType());

			assertEquals("ff80818159182fec01591830983802a5", eventAccount.getSource().getId());
			assertEquals("HR", eventAccount.getSource().getName());
			assertEquals(ReferenceType.SOURCE, eventAccount.getSource().getType());


			_identityEventPublisher.clear();
		}

		// update locked true -> false
		{
			Map<String,Object> updatedAccounts = new HashMap<>();
			Map<String, Object> e005 = (Map<String, Object>) ((List)validTestAccounts().get("accounts")).get(1);
			e005.put("locked", false);
			updatedAccounts.put("accounts", Collections.singletonList(e005));
			UpdateIdentityCommand cmd = defaultUpdateIdentityCommand()
				.attribute("foo", "bar")
				.attribute("foo2", "bar2")
				.attribute("foo3", "bar3")
				.account(SyncUtil.getAccountsFromIdentityMap(updatedAccounts).get(0))
				.build();


			_identityEventService.updateIdentity(cmd);

			List<IdentityEvent> events = _identityEventPublisher.getEvents();
			assertEquals(1, events.size());
			assertTrue(events.get(0) instanceof IdentityAccountUnlockedEvent);
			IdentityAccountUnlockedEvent eventAccount = (IdentityAccountUnlockedEvent) events.get(0);
			validateTestAccount(1, "1234", "john.doe", eventAccount.getIdentity(), eventAccount.getAccount(), eventAccount.getSource());

			assertEquals(true, eventAccount.isDisabled());
			assertEquals(false, eventAccount.isLocked());

			_identityEventPublisher.clear();
		}

		// update locked false -> true, string value
		{
			Map<String,Object> updatedAccounts = new HashMap<>();
			Map<String, Object> e005 = (Map<String, Object>) ((List)validTestAccounts().get("accounts")).get(1);
			e005.put("locked", "true");
			updatedAccounts.put("accounts", Collections.singletonList(e005));
			UpdateIdentityCommand cmd = defaultUpdateIdentityCommand()
				.attribute("foo", "bar")
				.attribute("foo2", "bar2")
				.attribute("foo3", "bar3")
				.account(SyncUtil.getAccountsFromIdentityMap(updatedAccounts).get(0))
				.build();


			_identityEventService.updateIdentity(cmd);

			List<IdentityEvent> events = _identityEventPublisher.getEvents();
			assertEquals(1, events.size());
			assertTrue(events.get(0) instanceof IdentityAccountLockedEvent);
			IdentityAccountLockedEvent eventAccount = (IdentityAccountLockedEvent) events.get(0);
			validateTestAccount(1, "1234", "john.doe", eventAccount.getIdentity(), eventAccount.getAccount(), eventAccount.getSource());

			assertEquals(true, eventAccount.isDisabled());
			assertEquals(true, eventAccount.isLocked());

			_identityEventPublisher.clear();
		}

		// update locked true -> true, string value to boolean value, no event
		{
			Map<String,Object> updatedAccounts = new HashMap<>();
			Map<String, Object> e005 = (Map<String, Object>) ((List)validTestAccounts().get("accounts")).get(1);
			e005.put("locked", true);
			updatedAccounts.put("accounts", Collections.singletonList(e005));
			UpdateIdentityCommand cmd = defaultUpdateIdentityCommand()
				.attribute("foo", "bar")
				.attribute("foo2", "bar2")
				.attribute("foo3", "bar3")
				.account(SyncUtil.getAccountsFromIdentityMap(updatedAccounts).get(0))
				.build();


			_identityEventService.updateIdentity(cmd);

			List<IdentityEvent> events = _identityEventPublisher.getEvents();
			assertEquals(0, events.size());

			_identityEventPublisher.clear();
		}

		// update disabled true -> false
		{
			Map<String,Object> updatedAccounts = new HashMap<>();
			Map<String, Object> e005 = (Map<String, Object>) ((List)validTestAccounts().get("accounts")).get(1);
			e005.put("disabled", false);
			updatedAccounts.put("accounts", Collections.singletonList(e005));
			UpdateIdentityCommand cmd = defaultUpdateIdentityCommand()
				.attribute("foo", "bar")
				.attribute("foo2", "bar2")
				.attribute("foo3", "bar3")
				.account(SyncUtil.getAccountsFromIdentityMap(updatedAccounts).get(0))
				.build();


			_identityEventService.updateIdentity(cmd);

			List<IdentityEvent> events = _identityEventPublisher.getEvents();
			assertEquals(1, events.size());
			assertTrue(events.get(0) instanceof IdentityAccountEnabledEvent);
			IdentityAccountEnabledEvent eventAccount = (IdentityAccountEnabledEvent) events.get(0);
			validateTestAccount(1, "1234", "john.doe", eventAccount.getIdentity(), eventAccount.getAccount(), eventAccount.getSource());

			assertEquals(false, eventAccount.isDisabled());
			assertEquals(true, eventAccount.isLocked());

			_identityEventPublisher.clear();
		}

		// update disabled false -> true
		{
			Map<String,Object> updatedAccounts = new HashMap<>();
			Map<String, Object> e005 = (Map<String, Object>) ((List)validTestAccounts().get("accounts")).get(1);
			e005.put("disabled", true);
			updatedAccounts.put("accounts", Collections.singletonList(e005));
			UpdateIdentityCommand cmd = defaultUpdateIdentityCommand()
				.attribute("foo", "bar")
				.attribute("foo2", "bar2")
				.attribute("foo3", "bar3")
				.account(SyncUtil.getAccountsFromIdentityMap(updatedAccounts).get(0))
				.build();


			_identityEventService.updateIdentity(cmd);

			List<IdentityEvent> events = _identityEventPublisher.getEvents();
			assertEquals(1, events.size());
			assertTrue(events.get(0) instanceof IdentityAccountDisabledEvent);
			IdentityAccountDisabledEvent eventAccount = (IdentityAccountDisabledEvent) events.get(0);
			validateTestAccount(1, "1234", "john.doe", eventAccount.getIdentity(), eventAccount.getAccount(), eventAccount.getSource());

			assertEquals(true, eventAccount.isDisabled());
			assertEquals(true, eventAccount.isLocked());

			_identityEventPublisher.clear();
		}

		//update attributes in account.
		{
			Map<String,Object> updatedAccounts = new HashMap<>();
			Map<String, Object> e007 = (Map<String, Object>) ((List)validTestAccounts().get("accounts")).get(1);
			e007.put("accountId", "E007");
			e007.put("name", "Cheryl Brown");
			e007.put("created", 1505922175456L);
			e007.put("entitlementAttributes", new HashMap<>());
			e007.put("accountAttributes", Collections.singletonMap("country", "UK"));
			updatedAccounts.put("accounts", Collections.singletonList(e007));

			UpdateIdentityCommand cmd = defaultUpdateIdentityCommand()
				.attribute("foo", "bar")
				.attribute("foo2", "bar2")
				.attribute("foo3", "bar3")
				.account(SyncUtil.getAccountsFromIdentityMap(updatedAccounts).get(0))
				.build();

			_identityEventService.updateIdentity(cmd);

			List<IdentityEvent> events = _identityEventPublisher.getEvents();
			assertEquals(1, events.size());
			assertTrue(events.get(0) instanceof IdentityAccountAttributesChangedEvent);

			IdentityAccountAttributesChangedEvent eventAccount = (IdentityAccountAttributesChangedEvent)events.get(0);
			assertEquals("1234", eventAccount.getIdentity().getId());
			assertEquals("john.doe", eventAccount.getIdentity().getName());
			assertEquals(ReferenceType.IDENTITY, eventAccount.getIdentity().getType());

			assertEquals("E005", eventAccount.getAccount().getId());
			assertEquals("Cheryl Brown", eventAccount.getAccount().getName());
			assertEquals("E005", eventAccount.getAccount().getNativeIdentity());
			assertEquals("456", eventAccount.getAccount().getUuid());
			assertEquals(ReferenceType.ACCOUNT, eventAccount.getAccount().getType());

			assertEquals("cc80818159182fec01591830983802a5", eventAccount.getSource().getId());
			assertEquals("Mainframe", eventAccount.getSource().getName());
			assertEquals(ReferenceType.SOURCE, eventAccount.getSource().getType());

			assertFalse(eventAccount.getChange("accountId").isPresent());
			assertFalse(eventAccount.getChange("name").isPresent());

			assertEquals("US", eventAccount.getChange("country").get().getOldValue());
			assertEquals("UK", eventAccount.getChange("country").get().getNewValue());

			assertFalse(eventAccount.getChange("created")
				.isPresent());

			_identityEventPublisher.clear();
		}

		//delete identity.
		{
			DeleteIdentityCommand cmd2 = DeleteIdentityCommand.builder()
				.timestamp(OffsetDateTime.now())
				.tenantId(new TenantId("acme-solar"))
				.identityId(new IdentityId("1234"))
				.build();

			_identityEventService.deleteIdentity(cmd2);

			List<IdentityEvent> events = _identityEventPublisher.getEvents();
			assertEquals(2, events.size());
			assertTrue(events.get(0) instanceof IdentityAccountUncorrelatedEvent);
			assertTrue(events.get(1) instanceof IdentityDeletedEvent);

			IdentityAccountUncorrelatedEvent eventAccount = (IdentityAccountUncorrelatedEvent)events.get(0);
			assertEquals("1234", eventAccount.getIdentity().getId());
			assertEquals("john.doe", eventAccount.getIdentity().getName());
			assertEquals(ReferenceType.IDENTITY, eventAccount.getIdentity().getType());

			assertEquals("E005", eventAccount.getAccount().getId());
			assertEquals("Cheryl Brown", eventAccount.getAccount().getName());
			assertEquals("E005", eventAccount.getAccount().getNativeIdentity());
			assertEquals("456", eventAccount.getAccount().getUuid());
			assertEquals(ReferenceType.ACCOUNT, eventAccount.getAccount().getType());

			assertEquals("cc80818159182fec01591830983802a5", eventAccount.getSource().getId());
			assertEquals("Mainframe", eventAccount.getSource().getName());
			assertEquals(ReferenceType.SOURCE, eventAccount.getSource().getType());

			_identityEventPublisher.clear();
		}
	}

	@Test
	public void testIdentityCreateWithNullAccessId() {
		// New identity, an access item with a null identity shouldn't could explosions.  See IDA-8832.
		Map<String, Object> access = new HashMap<>();
		access.put("type", "ENTITLEMENT");
		access.put("id", null);

		UpdateIdentityCommand cmd = defaultUpdateIdentityCommand()
			.access(access)
			.build();

		_identityEventService.updateIdentity(cmd);

		List<IdentityEvent> events = _identityEventPublisher.getEvents();
		assertEquals(1, events.size());
		assertTrue(events.get(0) instanceof IdentityCreatedEvent);

		_identityEventPublisher.clear();
	}

	@Test
	public void testIdentityDeleteWithNullAccessId() {
		{
			// Put an identity into the state that has a null ID for an access item.
			Map<String, Object> access = new HashMap<>();
			access.put("type", "ENTITLEMENT");
			access.put("id", null);

			UpdateIdentityCommand cmd = defaultUpdateIdentityCommand()
				.access(access)
				.build();

			_identityEventService.updateIdentity(cmd);

			// Clear it out ... we don't care about the creation.
			_identityEventPublisher.clear();
		}

		//delete identity.
		{
			DeleteIdentityCommand cmd = DeleteIdentityCommand.builder()
				.timestamp(OffsetDateTime.now())
				.tenantId(new TenantId("acme-solar"))
				.identityId(new IdentityId("1234"))
				.build();

			_identityEventService.deleteIdentity(cmd);

			// Check that there is just a single delete event and no access removed event.  And no explosion - see IDA-8832.
			List<IdentityEvent> events = _identityEventPublisher.getEvents();
			assertEquals(1, events.size());
			assertTrue(events.get(0) instanceof IdentityDeletedEvent);
		}
	}

	@Test
	public void testAppEvents() {
		final App appOne = App.builder()
			.appId(AppId.builder()
				.id("appOne")
				.name("App One")
				.build())
			.sourceId(SourceId.builder()
				.id("sourceOne")
				.name("Source One")
				.build())
			.accountId(AccountId.builder()
				.id("accountOne")
				.nativeIdentity("nativeOne")
				.name("Account One")
				.build())
			.build();
		final App appTwo = App.builder()
			.appId(AppId.builder()
				.id("appTwo")
				.name("App Two")
				.build())
			.sourceId(SourceId.builder()
				.id("sourceTwo")
				.name("Source Two")
				.build())
			.accountId(AccountId.builder()
				.id("accountTwo")
				.nativeIdentity("nativeTwo")
				.name("Account Two")
				.build())
			.build();

		{
			// New identity, get app added event
			UpdateIdentityCommand cmd = defaultUpdateIdentityCommand()
				.app(appOne)
				.build();

			_identityEventService.updateIdentity(cmd);

			List<IdentityEvent> events = _identityEventPublisher.getEvents();
			assertEquals(2, events.size());
			assertTrue(events.get(0) instanceof IdentityCreatedEvent);
			assertTrue(events.get(1) instanceof IdentityAppAddedEvent);
			IdentityAppAddedEvent event = (IdentityAppAddedEvent)events.get(1);
			assertEquals("1234", event.getIdentity().getId());
			assertEquals("john.doe", event.getIdentity().getName());
			assertApp(appOne, event.getApp(), event.getSource(), event.getAccount());

			_identityEventPublisher.clear();
		}
		{
			// New app
			UpdateIdentityCommand cmd = defaultUpdateIdentityCommand()
				.app(appOne)
				.app(appTwo)
				.build();

			_identityEventService.updateIdentity(cmd);

			List<IdentityEvent> events = _identityEventPublisher.getEvents();
			assertEquals(1, events.size());

			assertTrue(events.get(0) instanceof IdentityAppAddedEvent);

			IdentityAppAddedEvent event = (IdentityAppAddedEvent)events.get(0);
			assertEquals("1234", event.getIdentity().getId());
			assertEquals("john.doe", event.getIdentity().getName());
			assertApp(appTwo, event.getApp(), event.getSource(), event.getAccount());

			_identityEventPublisher.clear();
		}

		{
			// Remove appOne
			UpdateIdentityCommand cmd = defaultUpdateIdentityCommand()
				.app(appTwo)
				.build();

			_identityEventService.updateIdentity(cmd);

			List<IdentityEvent> events = _identityEventPublisher.getEvents();
			assertEquals(1, events.size());
			assertTrue(events.get(0) instanceof IdentityAppRemovedEvent);

			IdentityAppRemovedEvent event = (IdentityAppRemovedEvent)events.get(0);
			assertEquals("1234", event.getIdentity().getId());
			assertEquals("john.doe", event.getIdentity().getName());
			assertApp(appOne, event.getApp(), event.getSource(), event.getAccount());

			_identityEventPublisher.clear();
		}

		{
			// Delete identity, get app removed events
			DeleteIdentityCommand cmd = DeleteIdentityCommand.builder()
				.tenantId(new TenantId("acme-solar"))
				.identityId(new IdentityId("1234"))
				.timestamp(OffsetDateTime.now())
				.build();

			_identityEventService.deleteIdentity(cmd);
			List<IdentityEvent> events = _identityEventPublisher.getEvents();
			assertEquals(2, events.size());
			assertTrue(events.get(0) instanceof IdentityAppRemovedEvent);
			assertTrue(events.get(1) instanceof IdentityDeletedEvent);

			IdentityAppRemovedEvent appEvent = (IdentityAppRemovedEvent)events.get(0);
			assertEquals("1234", appEvent.getIdentity().getId());
			assertEquals("john.doe", appEvent.getIdentity().getName());
			assertEquals(ReferenceType.IDENTITY, appEvent.getIdentity().getType());

			assertApp(appTwo, appEvent.getApp(), appEvent.getSource(), appEvent.getAccount());
		}
	}

	@Test
	public void testIdentityStatusChanges() {
		{
			UpdateIdentityCommand cmd = defaultUpdateIdentityCommand().build();
			_identityEventService.updateIdentity(cmd);
			_identityEventPublisher.clear();
		}
		{
			UpdateIdentityCommand cmd = defaultUpdateIdentityCommand().disabled(true).build();
			_identityEventService.updateIdentity(cmd);
			List<IdentityEvent> events = _identityEventPublisher.getEvents();
			assertEquals(1, events.size());
			assertTrue(events.get(0) instanceof IdentityDisabledEvent);
			_identityEventPublisher.clear();
		}
		{
			UpdateIdentityCommand cmd = defaultUpdateIdentityCommand().disabled(false).build();
			_identityEventService.updateIdentity(cmd);
			List<IdentityEvent> events = _identityEventPublisher.getEvents();
			assertEquals(1, events.size());
			assertTrue(events.get(0) instanceof IdentityEnabledEvent);
			_identityEventPublisher.clear();
		}
	}

	private void assertApp(App app, AppReference appReference, SourceReference sourceReference, AccountReference accountReference) {
		assertEquals(app.getAppId().getId(), appReference.getId());
		assertEquals(app.getAppId().getName(), appReference.getName());
		assertEquals(app.getSourceId().getId(), sourceReference.getId());
		assertEquals(app.getSourceId().getName(), sourceReference.getName());
		assertEquals(app.getAccountId().getId(), accountReference.getId());
		assertEquals(app.getAccountId().getName(), accountReference.getName());
		assertEquals(app.getAccountId().getNativeIdentity(), accountReference.getNativeIdentity());
	}

	private static Map<String, Object> validTestAccounts() {
		Map<String, Object> identityMap = new HashMap<>();

		Map<String, Object> account1 = new HashMap<>();
		account1.put("id", "E002");
		account1.put("uuid", "456");
		account1.put("nativeIdentity", "E002");
		account1.put("accountId", "E002");
		account1.put("name", "Algernop Krieger");
		account1.put("created", 1505922175727L);
		account1.put("disabled", false);
		account1.put("locked", false);
		account1.put("entitlementAttributes", getEntitlementAttributeMap());
		account1.put("serviceId", "ff80818159182fec01591830983802a5");
		account1.put("serviceName", "HR");
		account1.put("serviceType", "DelimitedFile");

		Map<String, Object> account2 = new HashMap<>();
		account2.put("id", "E005");
		account2.put("uuid", "456");
		account2.put("nativeIdentity", "E005");
		account2.put("accountId", "E005");
		account2.put("name", "Cheryl Tunt");
		account2.put("created", 1505922175456L);
		account2.put("disabled", true);
		account2.put("locked", true);
		account2.put("entitlementAttributes", getEntitlementAttributeMap());
		account2.put("serviceId", "cc80818159182fec01591830983802a5");
		account2.put("serviceName", "Mainframe");
		account2.put("serviceType", "AD");
		account2.put("accountAttributes", Collections.singletonMap("country", "US"));

		ArrayList<Map<String, Object>> accounts = new ArrayList<>();
		accounts.add(account1);
		accounts.add(account2);

		identityMap.put("accounts", accounts);

		return identityMap;
	}

	private static Map getEntitlementAttributeMap() {
		Map<String, Object> attributeMap = new HashMap<>();
		attributeMap.put("attribute1", "value1");
		attributeMap.put("attribute2", null);
		List<String> list = Arrays.asList("group1", "group2", "group3");
		attributeMap.put("attribute3", list);
		return attributeMap;
	}

	private void validateTestAccount(int index, String identityId, String identityName, IdentityReference identityReference, AccountReference accountReference, SourceReference sourceReference) {
		Map<String, Object> account = ((List<Map<String, Object>>)validTestAccounts().get("accounts")).get(index);
		assertEquals(accountReference.getId(), account.get("id"));
		assertEquals(accountReference.getNativeIdentity(), account.get("nativeIdentity"));
		assertEquals(accountReference.getName(), account.get("name"));

		assertEquals(sourceReference.getId(), account.get("serviceId"));
		assertEquals(sourceReference.getName(), account.get("serviceName"));

		assertEquals(identityId, identityReference.getId());
		assertEquals(identityName, identityReference.getName());
	}

	private UpdateIdentityCommand.UpdateIdentityCommandBuilder defaultUpdateIdentityCommand() {
		return UpdateIdentityCommand.builder()
			.timestamp(OffsetDateTime.now())
			.tenantId(new TenantId("acme-solar"))
			.id(new IdentityId("1234"))
			.name("john.doe")
			.disabled(false);
	}
}
