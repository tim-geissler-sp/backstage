/*
 * Copyright (C) 2019 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.ets.infrastructure.hibernate;

import com.sailpoint.ets.domain.trigger.TriggerId;
import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.descriptor.sql.VarcharTypeDescriptor;

/**
 * TriggerIdType
 */
public class TriggerIdType extends AbstractSingleColumnStandardBasicType<TriggerId> {

	public static final TriggerIdType INSTANCE = new TriggerIdType();

	public TriggerIdType() {
		super(VarcharTypeDescriptor.INSTANCE, TriggerIdJavaDescriptor.INSTANCE);
	}

	@Override
	public String getName() {
		return "TriggerId";
	}
}
