package com.sailpoint.sp.identity.event;

import com.sailpoint.atlas.featureflag.FeatureFlagService;
import com.sailpoint.sp.identity.event.domain.Identity;
import com.sailpoint.sp.identity.event.domain.IdentityId;
import com.sailpoint.sp.identity.event.domain.IdentityState;
import com.sailpoint.sp.identity.event.domain.IdentityStateRepository;
import com.sailpoint.sp.identity.event.domain.ReferenceType;
import com.sailpoint.sp.identity.event.domain.TenantId;
import com.sailpoint.sp.identity.event.infrastructure.DualWriterIdentityStateRepository;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DualWriterIdentityStateRepositoryTest {
	private final FeatureFlagService _featureFlagService = mock(FeatureFlagService.class);
	TenantId tenantId = new TenantId("tenantId");
	IdentityId identityId = new IdentityId("identityId");
	// Create reoccurring variables for reads
	IdentityState oldIdentityState = IdentityState.builder()
		.identity(buildDummyIdentity())
		.lastEventTime(OffsetDateTime.now())
		.build();
	Optional<IdentityState> optionalOldIdentityState = Optional.of(oldIdentityState);
	IdentityState newIdentityState = IdentityState.builder()
		.identity(buildDummyIdentity())
		.lastEventTime(OffsetDateTime.now())
		.build();
	Optional<IdentityState> optionalNewIdentityState = Optional.of(newIdentityState);
	Optional<IdentityState> optionalNewIdentityStateNonExistent = Optional.ofNullable(null);
	// Create reoccurring variables for saves
	IdentityState savedIdentity = IdentityState.builder()
		.identity(buildDummyIdentity())
		.lastEventTime(OffsetDateTime.now())
		.build();
	private IdentityStateRepository _oldIdentityStateRepository = mock(IdentityStateRepository.class);
	private IdentityStateRepository _newIdentityStateRepository = mock(IdentityStateRepository.class);
	private final DualWriterIdentityStateRepository _dualWriterIdentityStateRepository = new DualWriterIdentityStateRepository(_oldIdentityStateRepository, _newIdentityStateRepository, _featureFlagService);

	@Before
	public void setUp() {
		when(_oldIdentityStateRepository.findById(tenantId, identityId)).thenReturn(optionalOldIdentityState);
		when(_newIdentityStateRepository.findById(tenantId, identityId)).thenReturn(optionalNewIdentityState);
	}

	@After
	public void tearDown() {
		reset(_oldIdentityStateRepository);
		reset(_newIdentityStateRepository);
		reset(_featureFlagService);
	}

	// test retrieving from old repo when READ_FROM_OLD_REPO is true
	@Test
	public void testFindByIdOld() {
		when(_featureFlagService.getBoolean(DualWriterIdentityStateRepository.READ_FROM_OLD_REPO, false)).thenReturn(true);

		Optional<IdentityState> identityState = _dualWriterIdentityStateRepository.findById(tenantId, identityId);
		verify(_oldIdentityStateRepository, times(1)).findById(tenantId, identityId);
		verify(_newIdentityStateRepository, never()).findById(tenantId, identityId);

		assertSame(identityState, optionalOldIdentityState);
	}

	// test retrieving from new repo when READ_FROM_OLD_REPO is false
	@Test
	public void testFindByIdNew() {
		when(_featureFlagService.getBoolean(DualWriterIdentityStateRepository.READ_FROM_OLD_REPO, false)).thenReturn(false);

		Optional<IdentityState> identityState = _dualWriterIdentityStateRepository.findById(tenantId, identityId);
		verify(_newIdentityStateRepository, times(1)).findById(tenantId, identityId);
		verify(_oldIdentityStateRepository, never()).findById(tenantId, identityId);

		assertSame(identityState, optionalNewIdentityState);

	}

	// test retrieving from new repo when READ_FROM_OLD_REPO is false, where new repo doesn't have identity
	@Test
	public void testFindByIdNewNonExistent() {

		when(_featureFlagService.getBoolean(DualWriterIdentityStateRepository.READ_FROM_OLD_REPO, false)).thenReturn(false);
		when(_newIdentityStateRepository.findById(tenantId, identityId)).thenReturn(optionalNewIdentityStateNonExistent);

		Optional<IdentityState> identityState = _dualWriterIdentityStateRepository.findById(tenantId, identityId);
		verify(_newIdentityStateRepository, times(1)).findById(tenantId, identityId);
		verify(_oldIdentityStateRepository, times(1)).findById(tenantId, identityId);

		assertSame(identityState, optionalOldIdentityState);

	}

	// test saving to new and old repos
	@Test
	public void testSaveOld() {
		when(_featureFlagService.getBoolean(DualWriterIdentityStateRepository.WRITE_TO_OLD_REPO_FLAG, false)).thenReturn(true);
		_dualWriterIdentityStateRepository.save(tenantId, savedIdentity);
		verify(_oldIdentityStateRepository, times(1)).save(tenantId, savedIdentity);
		verify(_newIdentityStateRepository, never()).save(tenantId, savedIdentity);


	}

	@Test
	public void testSaveNew() {
		when(_featureFlagService.getBoolean(DualWriterIdentityStateRepository.WRITE_TO_NEW_REPO_FLAG, false)).thenReturn(true);
		_dualWriterIdentityStateRepository.save(tenantId, savedIdentity);

		verify(_newIdentityStateRepository, times(1)).save(tenantId, savedIdentity);
		verify(_oldIdentityStateRepository, never()).save(tenantId, savedIdentity);
	}

	@Test
	public void testSaveBoth() {

		when(_featureFlagService.getBoolean(DualWriterIdentityStateRepository.WRITE_TO_OLD_REPO_FLAG, false)).thenReturn(true);
		when(_featureFlagService.getBoolean(DualWriterIdentityStateRepository.WRITE_TO_NEW_REPO_FLAG, false)).thenReturn(true);
		_dualWriterIdentityStateRepository.save(tenantId, savedIdentity);

		verify(_newIdentityStateRepository, times(1)).save(tenantId, savedIdentity);
		verify(_oldIdentityStateRepository, times(1)).save(tenantId, savedIdentity);
	}

	// test saving to new and old repos
	@Test
	public void testDeleteOld() {
		when(_featureFlagService.getBoolean(DualWriterIdentityStateRepository.WRITE_TO_OLD_REPO_FLAG, false)).thenReturn(true);
		_dualWriterIdentityStateRepository.deleteAllByTenant(tenantId);
		verify(_oldIdentityStateRepository, times(1)).deleteAllByTenant(tenantId);
		verify(_newIdentityStateRepository, never()).deleteAllByTenant(tenantId);


	}

	@Test
	public void testDeleteNew() {
		when(_featureFlagService.getBoolean(DualWriterIdentityStateRepository.WRITE_TO_NEW_REPO_FLAG, false)).thenReturn(true);
		_dualWriterIdentityStateRepository.deleteAllByTenant(tenantId);

		verify(_newIdentityStateRepository, times(1)).deleteAllByTenant(tenantId);
		verify(_oldIdentityStateRepository, never()).deleteAllByTenant(tenantId);
	}

	@Test
	public void testDeleteBoth() {

		when(_featureFlagService.getBoolean(DualWriterIdentityStateRepository.WRITE_TO_OLD_REPO_FLAG, false)).thenReturn(true);
		when(_featureFlagService.getBoolean(DualWriterIdentityStateRepository.WRITE_TO_NEW_REPO_FLAG, false)).thenReturn(true);
		_dualWriterIdentityStateRepository.deleteAllByTenant(tenantId);

		verify(_newIdentityStateRepository, times(1)).deleteAllByTenant(tenantId);
		verify(_oldIdentityStateRepository, times(1)).deleteAllByTenant(tenantId);
	}

	public static Identity buildDummyIdentity() {
		IdentityId identityId = new IdentityId("id");

		Identity identity = Identity.builder()
			.id(identityId)
			.name("dummy")
			.type(ReferenceType.IDENTITY)
			.build();
		return identity;
	}

}

