/*
 * Copyright (C) 2022 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.sp.identity.event.infrastructure;

import com.sailpoint.atlas.featureflag.FeatureFlagService;
import com.sailpoint.sp.identity.event.domain.IdentityId;
import com.sailpoint.sp.identity.event.domain.IdentityState;
import com.sailpoint.sp.identity.event.domain.IdentityStateRepository;
import com.sailpoint.sp.identity.event.domain.TenantId;
import com.sailpoint.utilities.CompressionUtil;
import com.sailpoint.utilities.JsonUtil;
import io.prometheus.client.Histogram;
import io.prometheus.client.SimpleTimer;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * IdentityStateRepository implementation that captures performance
 * metrics for a delegated implementation.
 */
public class InstrumentedIdentityStateRepository implements IdentityStateRepository {

	private static final Histogram _opHistogram = Histogram.build()
		.name("identity_state_operations")
		.help("instrumented operations for the IdentityStateRepository operations")
		.labelNames("impl", "op")
		.register();

	private static final Histogram _identityStateLengthHistogram = Histogram.build()
		.name("identity_state_encoded_length")
		.help("length of the encoded and compressed identity state")
		.buckets(32000, 400000)
		.register();

	private static final String SP_IDENTITY_EVENT_ENCODED_LENGTH = "SP_IDENTITY_EVENT_ENCODED_LENGTH";

	private final String _impl;
	private final IdentityStateRepository _delegate;
	private final FeatureFlagService _featureFlagService;

	public InstrumentedIdentityStateRepository(String impl,
											   IdentityStateRepository delegate,
											   FeatureFlagService featureFlagService) {
		_impl = requireNonNull(impl);
		_delegate = requireNonNull(delegate);
		_featureFlagService = requireNonNull(featureFlagService);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Optional<IdentityState> findById(TenantId tenantId, IdentityId identityId)  {
		io.prometheus.client.SimpleTimer timer = new SimpleTimer();
		try {
			return _delegate.findById(tenantId, identityId);
		} finally {
			_opHistogram.labels(_impl, "findById").observe(timer.elapsedSeconds());
		}

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void save(TenantId tenantId, IdentityState identityState) {
		io.prometheus.client.SimpleTimer timer = new SimpleTimer();
		try {
			_delegate.save(tenantId, identityState);

			if (_featureFlagService.getBoolean(SP_IDENTITY_EVENT_ENCODED_LENGTH, false)) {
				final byte[] encoded = encodeIdentityState(identityState);
				_identityStateLengthHistogram.observe(encoded.length);
			}
		} finally {
			_opHistogram.labels(_impl, "save").observe(timer.elapsedSeconds());
		}

	}

	/**
	 * Converts identity state to its compressed final state
	 * @param identityState
	 * @return Encoded and compressed identity state
	 */
	private static byte[] encodeIdentityState(IdentityState identityState) {
		final String stateJson = JsonUtil.toJson(identityState);
		final byte[] jsonBytes = stateJson.getBytes(StandardCharsets.UTF_8);

		return CompressionUtil.compress(jsonBytes);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void deleteAllByTenant(TenantId tenantId) {
		io.prometheus.client.SimpleTimer timer = new SimpleTimer();
		try {
			_delegate.deleteAllByTenant(tenantId);
		} finally {
			_opHistogram.labels(_impl, "deleteAllByTenant").observe(timer.elapsedSeconds());
		}
	}
}
