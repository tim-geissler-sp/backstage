/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.interest.matcher.repository;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.sailpoint.notification.interest.matcher.repository.impl.json.InterestRepositoryJsonImpl;

/**
 * InterestRepositoryProvider
 */
@Singleton
public class InterestRepositoryProvider implements Provider<InterestRepository> {

	@Inject
	InterestRepositoryJsonImpl interestRepositoryJsonImpl;

	@Override
	public InterestRepository get() {
		return interestRepositoryJsonImpl;
	}
}
