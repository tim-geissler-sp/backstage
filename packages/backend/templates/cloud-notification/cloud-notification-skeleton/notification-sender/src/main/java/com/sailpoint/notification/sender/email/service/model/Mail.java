/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.notification.sender.email.service.model;

import com.google.common.base.Strings;

import java.util.Map;

public class Mail {

	private final String _fromAddress;

	private final String _replyToAddress;

	private final String _toAddress;

	private final String _subject;

	private final String _html;

	private final Map<String, String> _tags;

	private final String _configurationSet;

	Mail(MailBuilder builder) {
		_fromAddress = builder._fromAddress;
		_replyToAddress = builder._replyToAddress;
		_toAddress = builder._toAddress;
		_subject = builder._subject;
		_html = builder._html;
		_tags = builder._tags;
		_configurationSet = builder._configurationSet;
	}

	public String getFromAddress() {
		return _fromAddress;
	}

	public String getReplyToAddress() {
		return _replyToAddress;
	}

	/**
	 * Treats empty replyToAddress as null
	 * @return normalized replyToAddress
	 */
	public String getNormalizedReplyToAddress() {
		return Strings.emptyToNull(_replyToAddress);
	}

	public String getToAddress() {
		return _toAddress;
	}

	public String getSubject() {
		return _subject;
	}

	public String getHtml() {
		return _html;
	}

	public Map<String, String> getTags() {
		return _tags;
	}

	public String getConfigurationSet() {
		return _configurationSet;
	}

	public boolean hasConfigurationSet() {
		return _configurationSet != null;
	}

	public MailBuilder derive() {
		return new MailBuilder()
				.withFromAddress(_fromAddress)
				.withReplyToAddress(_replyToAddress)
				.withToAddress(_toAddress)
				.withSubject(_subject)
				.withHtml(_html)
				.withConfigurationSet(_configurationSet);
	}

	public static class MailBuilder {

		private String _fromAddress;

		private String _replyToAddress;

		private String _toAddress;

		private String _subject;

		private String _html;

		private Map<String, String> _tags;

		private String _configurationSet;

		public MailBuilder withFromAddress(String fromAddress) {
			_fromAddress = fromAddress;
			return this;
		}

		public MailBuilder withReplyToAddress(String replyToAddress) {
			_replyToAddress = replyToAddress;
			return this;
		}

		public MailBuilder withToAddress(String toAddress) {
			_toAddress = toAddress;
			return this;
		}

		public MailBuilder withSubject(String subject) {
			_subject = subject;
			return this;
		}

		public MailBuilder withHtml(String html) {
			_html = html;
			return this;
		}

		public MailBuilder withTags(Map<String, String> tags) {
			_tags = tags;
			return this;
		}

		public MailBuilder withConfigurationSet(String configurationSet) {
			_configurationSet = configurationSet;
			return this;
		}

		public Mail build() {
			return new Mail(this);
		}
	}
}
