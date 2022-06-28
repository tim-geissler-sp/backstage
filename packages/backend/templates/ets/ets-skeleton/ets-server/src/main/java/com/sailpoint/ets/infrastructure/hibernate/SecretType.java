/*
 * Copyright (C) 2019 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.ets.infrastructure.hibernate;


import com.sailpoint.ets.domain.Secret;
import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.descriptor.sql.VarcharTypeDescriptor;

/**
 * SecretType
 */
public class SecretType extends AbstractSingleColumnStandardBasicType<Secret> {

	public static final SecretType INSTANCE = new SecretType();

	public SecretType() {
		super(VarcharTypeDescriptor.INSTANCE, SecretJavaDescriptor.INSTANCE);
	}

	@Override
	public String getName() {
		return "Secret";
	}
}
