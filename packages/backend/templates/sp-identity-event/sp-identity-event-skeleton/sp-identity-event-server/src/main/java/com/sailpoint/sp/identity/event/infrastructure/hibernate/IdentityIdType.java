/*
 * Copyright (C) 2020 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.sp.identity.event.infrastructure.hibernate;

import com.sailpoint.sp.identity.event.domain.IdentityId;
import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.descriptor.sql.VarcharTypeDescriptor;

/**
 * IdentityIdType
 */
public class IdentityIdType extends AbstractSingleColumnStandardBasicType<IdentityId> {

	public static final IdentityIdType INSTANCE = new IdentityIdType();

	public IdentityIdType() {
		super(VarcharTypeDescriptor.INSTANCE, IdentityIdJavaDescriptor.INSTANCE);
	}

	@Override
	public String getName() {
		return "IdentityId";
	}
}
