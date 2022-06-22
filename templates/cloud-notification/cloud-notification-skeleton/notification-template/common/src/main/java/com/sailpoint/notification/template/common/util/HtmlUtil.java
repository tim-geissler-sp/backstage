/*
 * Copyright (C) 2020 SailPoint Technologies, Inc.
 */
package com.sailpoint.notification.template.common.util;

import com.sailpoint.atlas.util.StringUtil;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;


/**
 * Utilities to help with HTML sanitize.
 */
public class HtmlUtil {
	// default HTML sanitizer policy
	private static final PolicyFactory _sanitizer;

	static {
		String[] allowElements = {"body", "img", "html", "a", "table", "tr", "td", "th", "br"};
		_sanitizer = new HtmlPolicyBuilder().allowCommonInlineFormattingElements()
				.allowCommonBlockElements()
				.allowElements(allowElements)
				.allowAttributes("id").onElements(allowElements)
				.allowAttributes("src", "alt", "height", "width").onElements("img")
				.allowAttributes("size", "color", "face", "width", "style").onElements("font")
				.allowAttributes("href").onElements("a")
				.allowUrlProtocols("https", "http", "mailto")
				.allowStyling().toFactory();
	}

	/**
	 * Sanitize HTML of elements and properties that could be used for XSS.
	 *
	 * @param html The HTML to sanitize.
	 * @return The sanitized HTML.
	 */
	public static synchronized String sanitize(String html) {
		if (StringUtil.isNullOrEmpty(html)) {
			return html; // handle null or empty string
		}
		return _sanitizer.sanitize(html);
	}

	/**
	 * Sanitize HTML of elements and properties that could be used for XSS.
	 * Allow @ sign and ' sign.
	 *
	 * @param html The HTML to sanitize.
	 * @return The sanitized HTML.
	 */
	public static synchronized String sanitizeWithPunctuation(String html) {
		if (StringUtil.isNullOrEmpty(html)) {
			return html; // handle null or empty string
		}
		return  _sanitizer.sanitize(html).replace("&#64;", "@").replace("&#39;", "'");
	}
}
