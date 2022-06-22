/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.notification.sender.email;

import com.sailpoint.notification.api.event.dto.NotificationRendered;
import com.sailpoint.notification.sender.common.exception.InvalidNotificationException;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

public class Validator {

	public void notificationRenderedValidator(NotificationRendered notificationRendered) throws InvalidNotificationException {
		try {
			requireNonNull(notificationRendered);
			requireNonNull(notificationRendered.getRecipient());
			requireNonNull(notificationRendered.getRecipient().getEmail(), "Recipient address cannot be null");
			requireNonNull(notificationRendered.getFrom(), "From address cannot be null");
			requireNonNull(notificationRendered.getBody());
			requireNonNull(notificationRendered.getSubject());

			if (mailAddressInvalid(notificationRendered.getFrom())) {
				throw new IllegalArgumentException("From address " + notificationRendered.getFrom() + " is invalid");
			}

			if (mailAddressInvalid(notificationRendered.getRecipient().getEmail())) {
				throw new IllegalArgumentException("Recipient address " + notificationRendered.getRecipient().getEmail() + " is invalid");
			}

			if( mailAddressHasUnicode(notificationRendered.getFrom())) {
				throw new IllegalArgumentException("From address " + notificationRendered.getFrom() + " has unicode in it");
			}

			if (mailAddressHasUnicode(notificationRendered.getRecipient().getEmail())) {
				throw new IllegalArgumentException("Recipient address " + notificationRendered.getRecipient().getEmail() + " has unicode in it");
			}

			//If we still have a replyTo after the negative filters, throw an exception
			Optional.ofNullable(notificationRendered.getReplyTo())
					.filter(replyTo -> !replyTo.trim().equals(""))
					.filter(this::mailAddressInvalid)
					.ifPresent(replyTo -> {
						throw new IllegalArgumentException("Reply-To address " + replyTo + " is invalid");
					});
		} catch (Exception e) {
			throw new InvalidNotificationException(e);
		}
	}

	/**
	 * The address specs in IETF RFC # 822 are generous, so even a@a is a valid email.
	 * So we will do a very basic contains check here, which is an O(n).
	 * @param emailAddress
	 * @return
	 */
	private boolean mailAddressInvalid(String emailAddress) {
		return (emailAddress != null && !emailAddress.contains("@") );
	}

	/**
	 * AWS SES only supports ASCII in the local part of email addresses; so fail validation if we
	 * encounter accents or other unicode only characters
	 * @param emailAddress
	 * @return
	 */
	private boolean mailAddressHasUnicode(String emailAddress)
	{
		String local = emailAddress.split("@")[0];
		boolean hasUnicodeInLocal = local.chars().filter(c -> c > 127).findAny().isPresent();
		return hasUnicodeInLocal;
	}
}
