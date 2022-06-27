/*
 * Copyright (C) 2019 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.ets.infrastructure.hibernate;

import com.sailpoint.ets.domain.Secret;
import com.sailpoint.ets.infrastructure.util.HashService;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractTypeDescriptor;
import org.hibernate.type.descriptor.java.ImmutableMutabilityPlan;

/**
 * SecretJavaDescriptor
 */
public class SecretJavaDescriptor extends AbstractTypeDescriptor<Secret> {

	private HashService _hashService;

	public static final SecretJavaDescriptor INSTANCE = new SecretJavaDescriptor();

	public static void initHashService(HashService hashService) {
		INSTANCE.setHashService(hashService);
	}

	private SecretJavaDescriptor() {
		super(Secret.class, ImmutableMutabilityPlan.INSTANCE);
	}

	@Override
	public Secret fromString(String string) {
		return new Secret(string);
	}

	@Override
	public <X> X unwrap(Secret value, Class<X> type, WrapperOptions options) {
		if (value == null) {
			return null;
		}

		if (String.class.isAssignableFrom(type)) {
			return (X)_hashService.encode(value.toString());
		}

		throw unknownUnwrap(type);
	}

	@Override
	public <X> Secret wrap(X value, WrapperOptions options) {
		if (value == null) {
			return null;
		}

		if (String.class.isInstance(value)) {
			return new Secret((String)value);
		}

		throw unknownWrap(value.getClass());
	}

	private void setHashService(HashService hashService) {
		_hashService = hashService;
	}
}
