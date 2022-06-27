/*
 * Copyright (c) 2020. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.template.common.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit test for HtmlUtil
 */
public class HtmlUtilTest {

	/**
	 * Test the 'onerror' property is removed on sanitization.
	 */
	@Test
	public void testOnerror() {
		final String HTML = "<img src=x onerror=alert(1)>";

		String sanitized = HtmlUtil.sanitize(HTML);

		assertTrue(sanitized.contains("img"));
		assertTrue(sanitized.contains("src"));
		assertFalse(sanitized.contains("onerror"));
	}

	/**
	 * Test the 'onload' property is removed on sanitization.
	 */
	@Test
	public void testOnload() {
		final String HTML1 = "<body onload=alert(1)></body>";
		final String HTML2 = "<img src=x onload=alert(1)>";

		String sanitized = HtmlUtil.sanitize(HTML1);
		assertTrue(sanitized.contains("body"));
		assertFalse(sanitized.contains("onload"));

		sanitized = HtmlUtil.sanitize(HTML2);
		assertTrue(sanitized.contains("img"));
		assertTrue(sanitized.contains("src"));
		assertFalse(sanitized.contains("onload"));
	}

	/**
	 * Test the 'onmouseover' property is removed on sanitization.
	 */
	@Test
	public void testOnmouseover() {
		final String HTML = "<div onmouseover=alert(1)></div>";

		String sanitized = HtmlUtil.sanitize(HTML);
		assertTrue(sanitized.contains("div"));
		assertFalse(sanitized.contains("onmouseover"));
	}

	/**
	 * Test the 'script' element is removed on sanitization.
	 */
	@Test
	public void testScript() {
		final String HTML = "<div><script>alert(1)></script></div>";

		String sanitized = HtmlUtil.sanitize(HTML);

		assertTrue(sanitized.contains("div"));
		assertFalse(sanitized.contains("script"));
		assertFalse(sanitized.contains("alert"));
		final String HTML1 = "The user's email is jon.white@acme.com";
		String sanitized1 = HtmlUtil.sanitizeWithPunctuation(HTML1);
		assertEquals("The user's email is jon.white@acme.com", sanitized1);
	}

	@Test
	public void testEmails() {
		final String HTML_EMAIL = "The user's email is jon.white@acmesolar.com";

		String sanitized = HtmlUtil.sanitize(HTML_EMAIL);
		assertEquals("The user&#39;s email is jon.white&#64;acmesolar.com", sanitized);

		String sanitizedEmail = HtmlUtil.sanitizeWithPunctuation(HTML_EMAIL);
		assertEquals("The user's email is jon.white@acmesolar.com",  sanitizedEmail);
	}
}
