/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.notification.userpreferences.dto;

import com.sailpoint.notification.api.event.dto.Recipient;

import java.util.Optional;

public class UserPreferences {

	final private Recipient _recipient;

	final private Optional<String> _brand;

	UserPreferences(UserPreferencesBuilder builder) {
		_recipient = builder._recipient;
		_brand = builder._brand;
	}

	public Recipient getRecipient() {
		return _recipient;
	}

	public Optional<String> getBrand() {
		if (_brand != null) {
			return _brand;
		} else {
			return Optional.empty();
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof UserPreferences) {
			return this.getRecipient().getId().equals(((UserPreferences) obj).getRecipient().getId());
		}
		return false;
	}

	public static class UserPreferencesBuilder {
		private Recipient _recipient;
		private Optional<String> _brand;

		public UserPreferencesBuilder withRecipient(Recipient recipient) {
			_recipient = recipient;
			return this;
		}

		public UserPreferencesBuilder withBrand(Optional<String> brand) {
			_brand = brand;
			return this;
		}

		public UserPreferences build() {
			return new UserPreferences(this);
		}
	}

	public UserPreferencesBuilder derive() {
		return new UserPreferencesBuilder()
				.withRecipient(_recipient)
				.withBrand(_brand);
	}
}
