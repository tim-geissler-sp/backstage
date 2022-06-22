/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.audit.service.model;

import java.util.List;

public class SearchableEvent {

	private String actionVerb;
	private String status;
	private List<String> domainObjects;

	public SearchableEvent(List<String> domainObjects, String actionVerb, String status) {
		this.domainObjects = domainObjects;
		this.actionVerb = actionVerb;
		this.status = status;
	}

	public String getActionVerb() {
		return actionVerb;
	}

	public String getStatus() {
		return status;
	}

	public List<String> getDomainObjects() {
		return domainObjects;
	}
}