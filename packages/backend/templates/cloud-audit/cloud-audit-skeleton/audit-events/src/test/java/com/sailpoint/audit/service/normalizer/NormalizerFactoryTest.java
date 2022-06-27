/*
 * Copyright (c) 2019. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.audit.service.normalizer;

import com.sailpoint.audit.service.mapping.DomainAuditEventsUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import sailpoint.object.AuditEvent;

import java.util.Date;

import static com.sailpoint.audit.AuditActions.USER_RESET;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class NormalizerFactoryTest {
	@Mock
    DomainAuditEventsUtil _domainAuditEventsUtil;

	AuditEvent _auditEvent;

	NormalizerFactory _sut;

	@Before
	public void setUp() throws Exception {
		_sut = new NormalizerFactory();
		_sut._domainAuditEventsUtil = _domainAuditEventsUtil;

		_auditEvent = new AuditEvent("Actor", USER_RESET.toString());//whitelisted audit event
		_auditEvent.setCreated(new Date());
		_auditEvent.setApplication("[mantis] My App [source-1234]");
		_auditEvent.setString4("I have information");

		when(_domainAuditEventsUtil.isDomainAuditEvent(anyString())).thenReturn(false);
	}

	@Test
	public void testBaseFactory() {
		_auditEvent.setTarget("SomeDomainObject");
		Normalizer normalizer = _sut.getNormalizer(_auditEvent);
		Assert.assertEquals("BaseNormalizer", normalizer.getClass().getSimpleName());
	}

	@Test
	public void testCrudFactory() {
		_auditEvent.setAction("create");
		_auditEvent.setTarget("Cloud Role:value");
		Normalizer normalizer = _sut.getNormalizer(_auditEvent);
		Assert.assertEquals("CRUDNormalizer", normalizer.getClass().getSimpleName());

		_auditEvent.setAction("update");
		normalizer = _sut.getNormalizer(_auditEvent);
		Assert.assertEquals("CRUDNormalizer", normalizer.getClass().getSimpleName());

		_auditEvent.setAction("delete");
		normalizer = _sut.getNormalizer(_auditEvent);
		Assert.assertEquals("CRUDNormalizer", normalizer.getClass().getSimpleName());

		_auditEvent.setTarget("Unidentified:value");
		normalizer = _sut.getNormalizer(_auditEvent);
		Assert.assertEquals("NonWhitelistedNormalizer", normalizer.getClass().getSimpleName());

	}

	@Test
	public void testNonwhitelistedNormalizer() {
		_auditEvent.setAction(null);
		Normalizer normalizer = _sut.getNormalizer(_auditEvent);
		Assert.assertEquals("NonWhitelistedNormalizer", normalizer.getClass().getSimpleName());

		_auditEvent.setAction("non_whitelisted");
		normalizer = _sut.getNormalizer(_auditEvent);
		Assert.assertEquals("NonWhitelistedNormalizer", normalizer.getClass().getSimpleName());
	}

}