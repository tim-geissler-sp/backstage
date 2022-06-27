/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.notification.api.event;

/**
 * Class define events types published to iris bus.
 */
public class EventType {

	public static final String NOTIFICATION_RENDERED = "NOTIFICATION_RENDERED";
	public static final String EXTENDED_NOTIFICATION_EVENT = "EXTENDED_NOTIFICATION_EVENT";
	public static final String NOTIFICATION_INTEREST_MATCHED = "NOTIFICATION_INTEREST_MATCHED";
	public static final String NOTIFICATION_USER_PREFERENCES_MATCHED = "NOTIFICATION_PREFERENCES_MATCHED";

	/**
	 * Identities events.
	 *
	 * IDENTITY_DELETED - indicated identity was deleted.
	 * IDENTITY_ATTRIBUTE_CHANGED - indicated identity had one or more attributes changed in value
	 * IDENTITY_CREATED - new identity has been created
	 */
	public static final String IDENTITY_DELETED = "IdentityDeletedEvent";
	public static final String IDENTITY_ATTRIBUTE_CHANGED = "IdentityAttributesChangedEvent";
	public static final String IDENTITY_CREATED = "IdentityCreatedEvent";

	/**
	 * Org Lifecycle events.
	 */
	public static final String ORG_DELETED = "ORG_DELETED";
	public static final String ORG_CREATED = "ORG_CREATED";
	public static final String ORG_UPGRADED = "ORG_UPGRADED";

	/**
	 * Branding events.
	 */
	public static final String BRANDING_CREATED = "BRANDING_CREATED";
	public static final String BRANDING_UPDATED = "BRANDING_UPDATED";
	public static final String BRANDING_DELETED = "BRANDING_DELETED";

	/**
	 * Email redirection events.
	 */
	public static final String EMAIL_REDIRECTION_ENABLED = "EMAIL_REDIRECTION_ENABLED";
	public static final String EMAIL_REDIRECTION_DISABLED = "EMAIL_REDIRECTION_DISABLED";
}
