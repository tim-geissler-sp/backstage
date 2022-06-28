/*
 * Copyright (C) 2021 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.audit.event.model;

public class NoSuchTemplateException extends IllegalStateException {

	public NoSuchTemplateException(String template) {

		super(String.format("Invalid Super Template: [%s]", template));
	}
}
