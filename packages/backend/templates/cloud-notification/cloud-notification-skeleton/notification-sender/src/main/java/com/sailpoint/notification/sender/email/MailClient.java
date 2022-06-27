/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.sender.email;

import com.sailpoint.notification.sender.common.exception.InvalidNotificationException;
import com.sailpoint.notification.sender.email.dto.VerificationStatus;
import com.sailpoint.notification.sender.email.service.model.Mail;

import java.util.List;
import java.util.Map;

/**
 * MailClient interface
 */
public interface MailClient {

	/**
	 * Sends email
	 */
	void sendMail(Mail mail) throws InvalidNotificationException;

	/**
	 * Cloud based email providers require verification to use an email address in from or reply-to fields.
	 * This method gets the verification statuses for the given email addresses.
	 *
	 * @param mailAddresses The list of addresses
	 * @return Map of address and status
	 */
	Map<String, VerificationStatus> getVerificationStatus(List<String> mailAddresses);

	/**
	 * Cloud based email providers require verification to use an email address in from or reply-to fields.
	 * This method initiates verification of a given email address.
	 *
	 * @param mailAddress The address to initiate verification for
	 */
	void verifyAddress(String mailAddress);

	/**
	 * This method deletes a given verified email address.
	 *
	 * @param mailAddress The address to delete
	 */
	void deleteAddress(String mailAddress);
}
