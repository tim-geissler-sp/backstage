/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.domain.subscription;

import com.google.common.collect.ImmutableList;
import com.sailpoint.ets.RepoTestConfig;
import com.sailpoint.ets.domain.EtsPostgreSQLContainer;
import com.sailpoint.ets.domain.OffsetBasedPageRequest;
import com.sailpoint.ets.domain.TenantId;
import com.sailpoint.ets.domain.status.SubscriptionStatus;
import com.sailpoint.ets.infrastructure.web.EtsFilter;
import lombok.extern.apachecommons.CommonsLog;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.containers.PostgreSQLContainer;

import javax.transaction.Transactional;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.jeasy.random.FieldPredicates.named;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * {@link SubscriptionRepo} integration test
 */
@RunWith(SpringRunner.class)
@DataJpaTest
@ContextConfiguration(classes = RepoTestConfig.class)
@ActiveProfiles("test")
@CommonsLog
public class SubscriptionRepoIntegrationTest {

	@ClassRule
	public static PostgreSQLContainer<EtsPostgreSQLContainer> _postgreSQLContainer = EtsPostgreSQLContainer.getInstance();

	private static EasyRandom _generator;

	@Autowired
	private SubscriptionRepo _subscriptionRepo;

	@BeforeClass
	public static void beforeClass() throws Exception {
		EasyRandomParameters parameters = new EasyRandomParameters();
		parameters.excludeField(named("config")); // Hibernate fails to serialize Map<String, Object> in some cases
		parameters.excludeField(named("created")); // "Created" timestamp is excluded, because it is set by the database

		_generator = new EasyRandom(parameters);
	}

	@Test
	@Transactional
	public void findByTenantIdAndTriggerId() throws Exception {
		final Subscription mockSubscription = _generator.nextObject(Subscription.class);

		insertSubscriptions(Collections.singleton(mockSubscription));

		Subscription actual = _subscriptionRepo.findByTenantIdAndTriggerId(mockSubscription.getTenantId(), mockSubscription.getTriggerId())
			.orElse(null);

		assertNotNull(actual);
		assertTrue(this.isEqual(mockSubscription, actual));
	}

	@Test
	@Transactional
	public void findAllByTenantIdAndTriggerId() throws Exception {
		final List<Subscription> mockSubscriptions = _generator.objects(Subscription.class, 2)
			.collect(Collectors.toList());
		final Subscription expected = mockSubscriptions.get(0);

		insertSubscriptions(mockSubscriptions);

		List<Subscription> actual = _subscriptionRepo.findAllByTenantIdAndTriggerId(expected.getTenantId(), expected.getTriggerId())
			.collect(Collectors.toList());

		assertEquals(1, actual.size());
		assertTrue(this.isEqual(expected, actual.get(0)));
	}

	@Test
	@Transactional
	public void findAllByTenantIdAndType() throws Exception {
		final List<Subscription> mockSubscriptions = _generator.objects(Subscription.class, 2)
			.collect(Collectors.toList());
		final Subscription expected = mockSubscriptions.get(0);

		insertSubscriptions(mockSubscriptions);

		List<Subscription> actual = _subscriptionRepo.findAllByTenantIdAndType(expected.getTenantId(), expected.getType())
			.collect(Collectors.toList());

		assertEquals(1, actual.size());
		assertTrue(this.isEqual(expected, actual.get(0)));
	}

	@Test
	@Transactional
	public void findAllByTenantId() {
		final List<Subscription> mockSubscriptions = _generator.objects(Subscription.class, 2)
			.collect(Collectors.toList());
		final Subscription expected = mockSubscriptions.get(0);

		insertSubscriptions(mockSubscriptions);

		List<Subscription> actual = _subscriptionRepo.findAllByTenantId(expected.getTenantId())
			.collect(Collectors.toList());

		assertEquals(1, actual.size());
		assertTrue(this.isEqual(expected, actual.get(0)));
	}

	@Test
	@Transactional
	public void deleteAllByTenantId() {
		final List<Subscription> mockSubscriptions = _generator.objects(Subscription.class, 2)
			.collect(Collectors.toList());
		final Subscription expected = mockSubscriptions.get(0);

		insertSubscriptions(mockSubscriptions);

		_subscriptionRepo.deleteAllByTenantId(expected.getTenantId());

		List<Subscription> actual = ImmutableList.copyOf(_subscriptionRepo.findAll());

		assertEquals(1, actual.size());
		assertNotEquals(expected, actual);
	}

	@Test
	@Transactional
	public void testFilterByTenantId() {
		final List<Subscription> mockSubscriptions = _generator.objects(Subscription.class, 2)
			.collect(Collectors.toList());

		final Subscription customSubscription = Subscription.builder()
			.id(UUID.randomUUID())
			.tenantId(new TenantId("echo-test"))
			.build();

		insertSubscriptions(mockSubscriptions);
		insertSubscriptions(Collections.singletonList(customSubscription));

		Specification<Subscription> spec =
			new SubscriptionSpecification(EtsFilter.eq("tenantId", customSubscription.getTenantId()));
		Pageable pageable = new OffsetBasedPageRequest(0, 250);

		List<Subscription> actual = _subscriptionRepo.findAll(spec, pageable)
			.stream()
			.collect(Collectors.toList());

		assertEquals(1, actual.size());
		assertTrue(this.isEqual(customSubscription, actual.get(0)));
	}

	@Test
	@Transactional
	public void testFilterById() {
		final List<Subscription> mockSubscriptions = _generator.objects(Subscription.class, 5)
			.collect(Collectors.toList());

		insertSubscriptions(mockSubscriptions);

		final Subscription expected = mockSubscriptions.get(3);

		Specification<Subscription> spec = new SubscriptionSpecification(EtsFilter.eq("id", expected.getId()));
		Pageable pageable = new OffsetBasedPageRequest(0, 250);

		List<Subscription> actual = _subscriptionRepo.findAll(spec, pageable)
			.stream()
			.collect(Collectors.toList());

		assertEquals(1, actual.size());
		assertTrue(this.isEqual(expected, actual.get(0)));
	}

	@Test
	@Transactional
	public void testFilterByTriggerId() {
		final List<Subscription> mockSubscriptions = _generator.objects(Subscription.class, 5)
			.collect(Collectors.toList());

		insertSubscriptions(mockSubscriptions);

		final Subscription expected = mockSubscriptions.get(4);

		Specification<Subscription> spec = new SubscriptionSpecification(EtsFilter.eq("triggerId", expected.getTriggerId()));
		Pageable pageable = new OffsetBasedPageRequest(0, 250);

		List<Subscription> actual = _subscriptionRepo.findAll(spec, pageable)
			.stream()
			.collect(Collectors.toList());

		assertEquals(1, actual.size());
		assertTrue(this.isEqual(expected, actual.get(0)));
	}

	@Test
	@Transactional
	public void testFilterByType() {
		final Subscription mockSubscription1 = Subscription.builder()
			.id(UUID.randomUUID())
			.type(SubscriptionType.HTTP)
			.build();

		final Subscription mockSubscription2 = Subscription.builder()
			.id(UUID.randomUUID())
			.type(SubscriptionType.SCRIPT)
			.build();

		insertSubscriptions(Arrays.asList(mockSubscription1, mockSubscription2));

		Specification<Subscription> spec = new SubscriptionSpecification(EtsFilter.eq("type", SubscriptionType.SCRIPT));
		Pageable pageable = new OffsetBasedPageRequest(0, 250);

		List<Subscription> actual = _subscriptionRepo.findAll(spec, pageable)
			.stream()
			.collect(Collectors.toList());

		assertEquals(1, actual.size());
		assertTrue(this.isEqual(mockSubscription2, actual.get(0)));
	}

	@Test
	@Transactional
	public void testPaginationOffset() {
		final Subscription mockSubscription1 = Subscription.builder()
			.id(UUID.randomUUID())
			.tenantId(new TenantId("mockTenant1"))
			.build();

		final Subscription mockSubscription2 = Subscription.builder()
			.id(UUID.randomUUID())
			.tenantId(new TenantId("mockTenant2"))
			.build();

		insertSubscriptions(Arrays.asList(mockSubscription1, mockSubscription2));

		Specification<Subscription> spec = new SubscriptionSpecification(null);
		Pageable pageable = new OffsetBasedPageRequest(1, 250);

		List<Subscription> actual = _subscriptionRepo.findAll(spec, pageable)
			.stream()
			.collect(Collectors.toList());

		assertEquals(1, actual.size());
		assertTrue(this.isEqual(mockSubscription2, actual.get(0)));
	}

	@Test
	@Transactional
	public void testPaginationLimit() {
		final List<Subscription> mockSubscriptions = _generator.objects(Subscription.class, 5)
			.collect(Collectors.toList());

		insertSubscriptions(mockSubscriptions);

		Specification<Subscription> spec = new SubscriptionSpecification(null);
		Pageable pageable = new OffsetBasedPageRequest(0, 2);

		List<Subscription> actual = _subscriptionRepo.findAll(spec, pageable)
			.stream()
			.collect(Collectors.toList());

		assertEquals(2, actual.size());
	}

	@Test
	@Transactional
	public void testFindAllSubscriptionCounts() {
		final Subscription mockSubscription1 = Subscription.builder()
			.id(UUID.randomUUID())
			.type(SubscriptionType.HTTP)
			.tenantId(new TenantId("acme-solar"))
			.build();

		final Subscription mockSubscription2 = Subscription.builder()
			.id(UUID.randomUUID())
			.type(SubscriptionType.SCRIPT)
			.tenantId(new TenantId("acme-solar"))
			.build();

		final Subscription mockSubscription3 = Subscription.builder()
			.id(UUID.randomUUID())
			.type(SubscriptionType.SCRIPT)
			.tenantId(new TenantId("acme-solar"))
			.build();

		final Subscription mockSubscription4 = Subscription.builder()
			.id(UUID.randomUUID())
			.type(SubscriptionType.SCRIPT)
			.tenantId(new TenantId("acme-lunar"))
			.build();

		insertSubscriptions(Arrays.asList(mockSubscription1, mockSubscription2, mockSubscription3, mockSubscription4));

		Stream<SubscriptionStatus> statusStream = _subscriptionRepo.findAllSubscriptionCounts();

		assertEquals(2,
			statusStream
				.filter(s -> s.getType() == SubscriptionType.SCRIPT && s.getTenant().toString().equals("acme-solar"))
				.findFirst()
				.get()
				.getCount());
	}

	private void insertSubscriptions(Iterable<Subscription> subscriptions) {
		_subscriptionRepo.saveAll(subscriptions);
	}

	private boolean isEqual(Subscription expected, Subscription actual) {
		// "Created" timestamp is not checked, because it is set by the database
		if (Objects.equals(expected.getId(), actual.getId())
			&& Objects.equals(expected.getTenantId(), actual.getTenantId())
			&& Objects.equals(expected.getTriggerId(), actual.getTriggerId())
			&& Objects.equals(expected.getType(), actual.getType())
			&& Objects.equals(expected.getResponseDeadline(), actual.getResponseDeadline())
			&& Objects.equals(expected.getConfig(), actual.getConfig())
			&& Objects.equals(expected.getFilter(), actual.getFilter())
			&& Objects.equals(expected.getScriptSource(), actual.getScriptSource())
			&& Objects.equals(expected.getName(), actual.getName())
			&& Objects.equals(expected.getDescription(), actual.getDescription())
			&& expected.isEnabled() == actual.isEnabled()) {
			return true;
		} else {
			log.error("Expected: \n" + expected + "\nActual: " + actual);
			return false;
		}
	}
}
