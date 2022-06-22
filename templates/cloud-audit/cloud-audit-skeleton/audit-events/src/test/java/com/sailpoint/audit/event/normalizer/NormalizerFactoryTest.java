/*
 * Copyright (C) 2019 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.audit.event.normalizer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class NormalizerFactoryTest {

	private NormalizerFactory _normalizerFactory = new NormalizerFactory();

	@Test
	public void get() {

		assertEquals(JsonPathNormalizer.class, _normalizerFactory.get(null).getClass());
		assertEquals(JsonPathNormalizer.class, _normalizerFactory.get("JsonPathNormalizer").getClass());
	}

	@Test(expected = IllegalArgumentException.class)
	public void getError() {

		_normalizerFactory.get("NotFoundNormalizer");
	}
}
