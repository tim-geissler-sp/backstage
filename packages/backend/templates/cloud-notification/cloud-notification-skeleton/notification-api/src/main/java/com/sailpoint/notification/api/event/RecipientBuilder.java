/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.notification.api.event;

import com.sailpoint.notification.api.event.dto.Recipient;

import java.util.UUID;

/**
 * Class define builder for recipient.
 */
public class RecipientBuilder {

	private String _id;

	private String _email;

	private String _name;

	private String _phone;

	public RecipientBuilder() {
		_id = UUID.randomUUID().toString();
	}

	public String getEmail() {
		return _email;
	}

	public String getId() {
		return _id;
	}

	public String getName() {
		return _name;
	}

	public String getPhone() {
		return _phone;
	}

	public RecipientBuilder withId(String id) {
		_id = id;
		return this;
	}

	public RecipientBuilder withName(String name) {
		_name = name;
		return this;
	}

	public RecipientBuilder withPhone(String phone) {
		_phone = phone;
		return this;
	}

	public RecipientBuilder withEmail(String email) {
		_email = email;
		return this;
	}

	public Recipient build() {
		return new Recipient(this);
	}
}
