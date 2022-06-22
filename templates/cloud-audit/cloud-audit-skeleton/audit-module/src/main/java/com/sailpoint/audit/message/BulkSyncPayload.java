/*
 *
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 *
 */

package com.sailpoint.audit.message;

import java.util.HashMap;
import java.util.Map;

public class BulkSyncPayload {
	private Map<String,Object> _arguments;
	private boolean _countOnly = false;
	private boolean _reset = false;
	private boolean _override = false;

	public BulkSyncPayload() {
		_arguments = new HashMap<>();
	}

	public Map<String,Object> getArguments() {
		return _arguments;
	}

	public void setArguments(Map<String, Object> _arguments) {
		this._arguments = _arguments;
	}

	public boolean isReset() {
		return _reset;
	}

	public void setReset(boolean reset) {
		_reset = reset;
	}

	public boolean isCountOnly() {
		return _countOnly;
	}

	public void setCountOnly(boolean countOnly) {
		_countOnly = countOnly;
	}

	public void setOverride(boolean override) {
		_override = override;
	}

	public boolean isOverride() {
		return _override;
	}
}
