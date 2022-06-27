/*
 * Copyright (C) 2019 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.ets.infrastructure.hibernate;

import com.sailpoint.ets.domain.TenantId;
import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.descriptor.sql.VarcharTypeDescriptor;

/**
 * TenantIdType
 */
public class TenantIdType extends AbstractSingleColumnStandardBasicType<TenantId> {

	public static final TenantIdType INSTANCE = new TenantIdType();

	public TenantIdType() {
		super(VarcharTypeDescriptor.INSTANCE, TenantIdJavaDescriptor.INSTANCE);
	}

	@Override
	public String getName() {
		return "TenantId";
	}
}
