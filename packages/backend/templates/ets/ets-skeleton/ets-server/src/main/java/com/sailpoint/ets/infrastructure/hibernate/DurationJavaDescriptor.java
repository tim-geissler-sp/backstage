/*
 * Copyright (C) 2020 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.ets.infrastructure.hibernate;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractTypeDescriptor;
import org.hibernate.type.descriptor.java.ImmutableMutabilityPlan;

import java.time.Duration;

/**
 * DurationJavaDescriptor
 */
public class DurationJavaDescriptor extends AbstractTypeDescriptor<Duration> {

	public static final DurationJavaDescriptor INSTANCE = new DurationJavaDescriptor();

	private DurationJavaDescriptor() {
		super(Duration.class, ImmutableMutabilityPlan.INSTANCE);
	}

	@Override
	public Duration fromString(String string) {
		return Duration.parse(string);
	}

	@Override
	public <X> X unwrap(Duration value, Class<X> type, WrapperOptions options) {
		if (value == null) {
			return null;
		}

		if (String.class.isAssignableFrom(type)) {
			return (X)value.toString();
		}

		throw unknownUnwrap(type);
	}

	@Override
	public <X> Duration wrap(X value, WrapperOptions options) {
		if (value == null) {
			return null;
		}

		if (value instanceof String) {
			return Duration.parse((String)value);
		}

		throw unknownWrap(value.getClass());
	}
}
