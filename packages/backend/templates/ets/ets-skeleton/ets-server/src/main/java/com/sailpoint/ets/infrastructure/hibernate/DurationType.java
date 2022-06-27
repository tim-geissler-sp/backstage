/*
 * Copyright (C) 2020 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.ets.infrastructure.hibernate;

import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.descriptor.sql.VarcharTypeDescriptor;

import java.time.Duration;

/**
 * DurationType
 */
public class DurationType extends AbstractSingleColumnStandardBasicType<Duration> {

	public static final DurationType INSTANCE = new DurationType();

	public DurationType() {
		super(VarcharTypeDescriptor.INSTANCE, DurationJavaDescriptor.INSTANCE);
	}

	@Override
	public String getName() {
		return "Duration";
	}
}
