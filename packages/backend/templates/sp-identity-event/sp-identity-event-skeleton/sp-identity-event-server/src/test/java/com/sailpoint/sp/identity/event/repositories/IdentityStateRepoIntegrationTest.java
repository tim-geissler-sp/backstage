/*
 * Copyright (c) 2020 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.sp.identity.event.repositories;
import com.sailpoint.sp.identity.event.RepoTestConfig;
import com.sailpoint.sp.identity.event.domain.Account;
import com.sailpoint.sp.identity.event.domain.App;
import com.sailpoint.sp.identity.event.domain.IdentityEventPostgreSQLContainer;
import com.sailpoint.sp.identity.event.domain.IdentityId;
import com.sailpoint.sp.identity.event.domain.IdentityState;
import com.sailpoint.sp.identity.event.domain.IdentityStateEntity;
import com.sailpoint.sp.identity.event.domain.IdentityStateJpaRepository;
import com.sailpoint.sp.identity.event.domain.ReferenceType;
import com.sailpoint.sp.identity.event.domain.TenantId;
import com.sailpoint.sp.identity.event.infrastructure.PostgresIdentityStateRepository;
import com.sailpoint.utilities.JsonUtil;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.shaded.com.google.common.collect.ImmutableList;

import javax.transaction.Transactional;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Integration test for PostgresIdentityStateRepository
 */
@RunWith(SpringRunner.class)
@DataJpaTest
@ContextConfiguration(classes = RepoTestConfig.class)
@ActiveProfiles("integration")
@Ignore
public class IdentityStateRepoIntegrationTest {

	@ClassRule
	public static PostgreSQLContainer<IdentityEventPostgreSQLContainer> _container = IdentityEventPostgreSQLContainer.getInstance();

	@Autowired
	private IdentityStateJpaRepository _identityStateJpaRepo;

	IdentityStateEntity _identityStateEntity;
	IdentityState _identityState;

	private static final TenantId _tenantId = new TenantId("acem-solar");
	private static final IdentityId _identityId1 = new IdentityId("1234");
	private static final IdentityId _identityId2 = new IdentityId("4321");

	@Before
	public void setUp() {
		_identityStateEntity = getTestIdentityStateEntity();
		_identityState = PostgresIdentityStateRepository.fromEntity(_identityStateEntity);
	}

	@Test
	@Transactional
	public void testFindById() {
		_identityStateJpaRepo.save(PostgresIdentityStateRepository.toEntity( _identityStateEntity.getTenantId(), _identityState));
		assertEquals(_identityStateJpaRepo.findByTenantIdAndIdentityId(_tenantId, _identityId1).get(), _identityStateEntity);
		assertFalse(_identityStateJpaRepo.findByTenantIdAndIdentityId(_tenantId, _identityId2).isPresent());
	}

	@Test
	@Transactional
	public void testDeleteAllByTenant() {
		_identityStateJpaRepo.save(PostgresIdentityStateRepository.toEntity( _identityStateEntity.getTenantId(), _identityState));
		List<IdentityStateEntity> actual = ImmutableList.copyOf(_identityStateJpaRepo.findAll());
		assertEquals(1, actual.size());
		_identityStateJpaRepo.deleteByTenantId(_tenantId);
		actual = ImmutableList.copyOf(_identityStateJpaRepo.findAll());
		assertEquals(0, actual.size());

		for(int i =0; i<10; i++) {
			_identityStateJpaRepo.save(
				getTestIdentityStateEntity(i));
		}

		actual = ImmutableList.copyOf(_identityStateJpaRepo.findAll());
		assertEquals(10, actual.size());
		_identityStateJpaRepo.deleteByTenantId(_tenantId);
		actual = ImmutableList.copyOf(_identityStateJpaRepo.findAll());
		assertEquals(0, actual.size());
	}

	private static IdentityStateEntity getTestIdentityStateEntity(){
		IdentityStateEntity entry = new IdentityStateEntity();
		entry.setIdentityId(_identityId1);
		entry.setTenantId(_tenantId);
		entry.setName("john.doe");
		entry.setType(ReferenceType.IDENTITY.toString());
		entry.setAttributes(JsonUtil.toJson(new HashMap<String, String>()));
		entry.setAccounts(JsonUtil.toJson(new ArrayList<Account>()));
		entry.setAccess(JsonUtil.toJson(new ArrayList<HashMap<String, Object>>()));
		entry.setApps(JsonUtil.toJson(new ArrayList<App>()));
		entry.setLastEventTime(OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC));
		entry.setExpiration(OffsetDateTime.now().plusDays(7).withOffsetSameInstant(ZoneOffset.UTC));
		entry.setDeleted(false);
		entry.setDisabled(false);

		return entry;
	}
	private static IdentityStateEntity getTestIdentityStateEntity(int i){
		IdentityStateEntity entry = new IdentityStateEntity();
		entry.setIdentityId(new IdentityId(UUID.randomUUID().toString()));
		entry.setTenantId(_tenantId);
		entry.setName("john.doe" +i);
		entry.setType(ReferenceType.IDENTITY.toString());
		entry.setAttributes(JsonUtil.toJson(new HashMap<String, String>()));
		entry.setAccounts(JsonUtil.toJson(new ArrayList<Account>()));
		entry.setAccess(JsonUtil.toJson(new ArrayList<HashMap<String, Object>>()));
		entry.setApps(JsonUtil.toJson(new ArrayList<App>()));
		entry.setLastEventTime(OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC));
		entry.setExpiration(OffsetDateTime.now().plusDays(7).withOffsetSameInstant(ZoneOffset.UTC));
		entry.setDeleted(false);
		entry.setDisabled(false);

		return entry;
	}
}

