/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.notification.api.event.dto;

import com.sailpoint.notification.api.event.RecipientBuilder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Class define Recipient attributes.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Recipient {

	private String _id;

	private String _name;

	private String _phone;

	private String _email;

	public Recipient(RecipientBuilder recipientBuilder) {
		_id = recipientBuilder.getId();
		_email = recipientBuilder.getEmail();
		_name = recipientBuilder.getName();
		_phone = recipientBuilder.getPhone();
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

	public RecipientBuilder derive() {
		return new RecipientBuilder()
				.withEmail(_email)
				.withId(_id)
				.withName(_name)
				.withPhone(_phone);
	}

	@Override
	public String toString() {
		return "Recipient {" +
				" id='" + _id + '\'' +
				", name='" + _name + '\'' +
				", phone='" + _phone + '\'' +
				", email='" + _email + '\'' +
				" }";
	}
}
