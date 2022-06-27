/*
 * Copyright (c) 2020. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.sender.email.repository;

import com.google.common.collect.ImmutableSet;
import com.sailpoint.atlas.dynamodb.DynamoDBService;
import com.sailpoint.atlas.test.EnvironmentUtil;
import com.sailpoint.atlas.test.integration.dynamodb.DynamoDBServerRule;
import com.sailpoint.atlas.test.integration.dynamodb.EnableInMemoryDynamoDB;
import com.sailpoint.notification.sender.email.domain.TenantSenderEmail;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Unit tests for {@link DynamoDBTenantSenderEmailRepository} using embedded DynamoDB.
 *
 * @see TenantSenderEmail
 */
@EnableInMemoryDynamoDB
public class DynamoDBTenantSenderEmailRepositoryTest {

	private DynamoDBTenantSenderEmailRepository _repo;

	@Rule
	public DynamoDBServerRule _dynamoDBServerRule = new DynamoDBServerRule(EnvironmentUtil.findFreePort());

	@Before
	public void setup() {
		_dynamoDBServerRule.getDynamoDBService().createTable(TenantSenderEmail.class, DynamoDBService.PROJECTION_ALL);
		_repo = new DynamoDBTenantSenderEmailRepository(_dynamoDBServerRule.getAmazonDynamoDB());
	}

	@Test
	public void singleCRUD_ShouldPass() throws Exception {
		String senderEmailId = UUID.randomUUID().toString();

		TenantSenderEmail testEmail = TenantSenderEmail.builder()
				.tenant("dev#acme-solar")
				.id(senderEmailId)
				.email("sample@example.com")
				.verificationStatus("PENDING")
				.build();

		// Put item
		_repo.save(testEmail);

		// Get items for non-existent tenant
		List<TenantSenderEmail> emails = _repo.findAllByTenant("dev#acme-solar-none");
		// Assert items are 0 but no NPE
		assertEquals(0, emails.size());

		// Get items for tenant
		emails = _repo.findAllByTenant("dev#acme-solar");

		// Assert item exists
		assertEquals(1, emails.size());
		assertEquals(testEmail, emails.get(0));

		// Get item by tenant and non-existent id
		Optional email = _repo.findByTenantAndId("dev#acme-solar", UUID.randomUUID().toString());
		assertFalse(email.isPresent());

		// Get item by tenant and id
		email = _repo.findByTenantAndId("dev#acme-solar", senderEmailId);
		assertEquals(testEmail, email.get());

		// Get items by email
		emails = _repo.findAllByEmail("sample@example.com");
		assertEquals(1, emails.size());
		assertEquals(testEmail, emails.get(0));

		// Delete item
		_repo.delete("dev#acme-solar", senderEmailId);

		emails = _repo.findAllByTenant(senderEmailId);

		// Assert item deleted
		assertEquals(0, emails.size());
	}

	@Test
	public void batchCRUD_ShouldPass() throws Exception {
		TenantSenderEmail testEmail = TenantSenderEmail.builder()
				.id(UUID.randomUUID().toString())
				.tenant("dev#acme-solar")
				.email("sample@example.com")
				.verificationStatus("PENDING")
				.build();

		TenantSenderEmail testEmail2 = TenantSenderEmail.builder()
				.id(UUID.randomUUID().toString())
				.tenant("dev#acme-solar")
				.email("sample2@example.com")
				.verificationStatus("PENDING")
				.build();

		TenantSenderEmail testEmail3 = TenantSenderEmail.builder()
				.id(UUID.randomUUID().toString())
				.tenant("dev#acme-solar-staging")
				.email("sample@example.com")
				.verificationStatus("PENDING")
				.build();

		// Batch write 3 items
		_repo.batchSave(ImmutableSet.of(testEmail, testEmail2, testEmail3));

		// Find all by common email
		List<TenantSenderEmail> emails = _repo.findAllByEmail("sample@example.com");

		// Assert 2 items exists
		assertEquals(2, emails.size());

		// Find all by tenant acme-solar
		emails = _repo.findAllByTenant("dev#acme-solar");

		// Assert 2 items exists
		assertEquals(2, emails.size());

		// Delete 2 items
		_repo.delete(testEmail.getTenant(), testEmail.getId());
		_repo.delete(testEmail2.getTenant(), testEmail2.getId());
		_repo.delete(testEmail3.getTenant(), testEmail3.getId());

		// Find all by common email
		emails = _repo.findAllByEmail("sample@example.com");

		// Assert none exist
		assertEquals(0, emails.size());
	}

	@Test
	public void duplicateId() {
		String senderEmailId = UUID.randomUUID().toString();

		TenantSenderEmail conflictEmail1 = TenantSenderEmail.builder()
				.tenant("dev#acme-solar")
				.id(senderEmailId)
				.email("sample@example.com")
				.verificationStatus("PENDING")
				.build();

		TenantSenderEmail conflictEmail2 = TenantSenderEmail.builder()
				.tenant("dev#acme-solar")
				.id(senderEmailId)
				.email("sample2@example.com")
				.verificationStatus("PENDING")
				.build();

		TenantSenderEmail validEmail = TenantSenderEmail.builder()
				.tenant("dev#acme-solar")
				.id(UUID.randomUUID().toString())
				.email("sample2@example.com")
				.verificationStatus("SUCCESS")
				.build();

		_repo.batchSave(ImmutableSet.of(conflictEmail1, conflictEmail2, validEmail));
		List<TenantSenderEmail> emails = _repo.findAllByTenant("dev#acme-solar");

		//Assert batch write save failed because of conflict in id. Even validEmail is not saved as a result.
		assertEquals(0, emails.size());

		//Save conflicting item one after another
		_repo.save(conflictEmail1);
		emails = _repo.findAllByTenant("dev#acme-solar");
		assertEquals(1, emails.size());

		_repo.save(conflictEmail2);
		emails = _repo.findAllByTenant("dev#acme-solar");

		//Saving second email that has same id as first should have failed, and size should still be 1
		assertEquals(1, emails.size());
	}

	@Test
	public void statusChange() {
		TenantSenderEmail testEmail = TenantSenderEmail.builder()
				.tenant("dev#acme-solar")
				.id(UUID.randomUUID().toString())
				.email("sample@example.com")
				.verificationStatus("PENDING")
				.build();

		// Put item
		_repo.save(testEmail);

		_repo.save(testEmail
				.toBuilder()
				.verificationStatus("SUCCESS")
				.build());

		List<TenantSenderEmail> emails = _repo.findAllByTenant("dev#acme-solar");
		assertEquals("SUCCESS", emails.get(0).getVerificationStatus());
	}

	@Test(expected = NullPointerException.class)
	public void findAllByTenantNullCheck() {
		_repo.findAllByTenant(null);
	}

	@Test(expected = NullPointerException.class)
	public void findAllByTenantAndIdNullTenant() {
		_repo.findByTenantAndId(null, UUID.randomUUID().toString());
	}

	@Test(expected = NullPointerException.class)
	public void findAllByTenantAndIdNullId() {
		_repo.findByTenantAndId("dev#acme-solar", null);
	}

	@Test(expected = NullPointerException.class)
	public void findAllByEmailNullCheck() {
		_repo.findAllByEmail(null);
	}

	@Test(expected = NullPointerException.class)
	public void saveNullCheck() {
		_repo.save(null);
	}

	@Test(expected = NullPointerException.class)
	public void batchSaveNullCheck() {
		_repo.batchSave(null);
	}

	@Test(expected = NullPointerException.class)
	public void deleteNullTenant() {
		_repo.delete(null, UUID.randomUUID().toString());
	}

	@Test(expected = NullPointerException.class)
	public void deleteNullId() {
		_repo.delete("dev#acme-solar", null);
	}
}
