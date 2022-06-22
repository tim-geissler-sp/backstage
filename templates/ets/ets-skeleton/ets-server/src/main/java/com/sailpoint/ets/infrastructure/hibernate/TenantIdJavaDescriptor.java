/*
 * Copyright (C) 2019 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.ets.infrastructure.hibernate;

import com.sailpoint.ets.domain.TenantId;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractTypeDescriptor;
import org.hibernate.type.descriptor.java.ImmutableMutabilityPlan;

/**
 * TenantIdJavaDescriptor
 */
public class TenantIdJavaDescriptor extends AbstractTypeDescriptor<TenantId> {

	public static final TenantIdJavaDescriptor INSTANCE = new TenantIdJavaDescriptor();

	private TenantIdJavaDescriptor() {
		super(TenantId.class, ImmutableMutabilityPlan.INSTANCE);
	}

	@Override
	public TenantId fromString(String string) {
		return new TenantId(string);
	}

	@Override
	public <X> X unwrap(TenantId value, Class<X> type, WrapperOptions options) {
		if (value == null) {
			return null;
		}

		if (String.class.isAssignableFrom(type)) {
			return (X)value.toString();
		}

		throw unknownUnwrap(type);
	}

	@Override
	public <X> TenantId wrap(X value, WrapperOptions options) {
		if (value == null) {
			return null;
		}

		if (String.class.isInstance(value)) {
			return new TenantId((String)value);
		}

		throw unknownWrap(value.getClass());
	}
}
