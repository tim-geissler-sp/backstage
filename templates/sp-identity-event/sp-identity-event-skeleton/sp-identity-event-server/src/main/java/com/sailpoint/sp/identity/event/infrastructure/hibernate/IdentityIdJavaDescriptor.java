/*
 * Copyright (C) 2020 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.sp.identity.event.infrastructure.hibernate;

import com.sailpoint.sp.identity.event.domain.IdentityId;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractTypeDescriptor;
import org.hibernate.type.descriptor.java.ImmutableMutabilityPlan;

/**
 * IdentityIdJavaDescriptor
 */
public class IdentityIdJavaDescriptor extends AbstractTypeDescriptor<IdentityId> {

	public static final IdentityIdJavaDescriptor INSTANCE = new IdentityIdJavaDescriptor();

	private IdentityIdJavaDescriptor() {
		super(IdentityId.class, ImmutableMutabilityPlan.INSTANCE);
	}

	@Override
	public IdentityId fromString(String string) {
		return new IdentityId(string);
	}

	@Override
	public <X> X unwrap(IdentityId value, Class<X> type, WrapperOptions options) {
		if (value == null) {
			return null;
		}

		if (String.class.isAssignableFrom(type)) {
			return (X)value.toString();
		}

		throw unknownUnwrap(type);
	}

	@Override
	public <X> IdentityId wrap(X value, WrapperOptions options) {
		if (value == null) {
			return null;
		}

		if (String.class.isInstance(value)) {
			return new IdentityId((String)value);
		}

		throw unknownWrap(value.getClass());
	}
}
