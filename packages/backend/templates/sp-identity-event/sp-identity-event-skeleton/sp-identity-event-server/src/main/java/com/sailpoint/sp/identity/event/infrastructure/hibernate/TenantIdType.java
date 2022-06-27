/*
 * Copyright (C) 2020 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.sp.identity.event.infrastructure.hibernate;

import com.sailpoint.sp.identity.event.domain.TenantId;
import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.descriptor.sql.VarcharTypeDescriptor;

/**
 * TenantIdType
 */
public class TenantIdType extends AbstractSingleColumnStandardBasicType<TenantId> {

	public static final com.sailpoint.sp.identity.event.infrastructure.hibernate.TenantIdType INSTANCE = new com.sailpoint.sp.identity.event.infrastructure.hibernate.TenantIdType();

	public TenantIdType() {
		super(VarcharTypeDescriptor.INSTANCE, TenantIdJavaDescriptor.INSTANCE);
	}

	@Override
	public String getName() {
		return "TenantId";
	}
}
