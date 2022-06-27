/*
 * Copyright (C) 2019 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.ets.infrastructure.hibernate;

import com.sailpoint.ets.domain.trigger.TriggerId;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractTypeDescriptor;
import org.hibernate.type.descriptor.java.ImmutableMutabilityPlan;

/**
 * TriggerIdJavaDescriptor
 */
public class TriggerIdJavaDescriptor extends AbstractTypeDescriptor<TriggerId> {

	public static final TriggerIdJavaDescriptor INSTANCE = new TriggerIdJavaDescriptor();

	private TriggerIdJavaDescriptor() {
		super(TriggerId.class, ImmutableMutabilityPlan.INSTANCE);
	}

	@Override
	public TriggerId fromString(String string) {
		return new TriggerId(string);
	}

	@Override
	public <X> X unwrap(TriggerId value, Class<X> type, WrapperOptions options) {
		if (value == null) {
			return null;
		}

		if (String.class.isAssignableFrom(type)) {
			return (X)value.toString();
		}

		throw unknownUnwrap(type);
	}

	@Override
	public <X> TriggerId wrap(X value, WrapperOptions options) {
		if (value == null) {
			return null;
		}

		if (String.class.isInstance(value)) {
			return new TriggerId((String)value);
		}

		throw unknownWrap(value.getClass());
	}
}
