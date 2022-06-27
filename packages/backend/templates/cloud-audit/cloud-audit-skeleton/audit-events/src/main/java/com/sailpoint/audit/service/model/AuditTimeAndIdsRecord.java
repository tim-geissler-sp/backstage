/**
* Copyright (C) 2019 SailPoint Technologies, Inc.  All rights reserved.
*/
package com.sailpoint.audit.service.model;

import java.util.ArrayList;
import java.util.List;

public class AuditTimeAndIdsRecord {

	private long _timestamp;

	private int _totalProcessed;

	private int _totalUploaded;

	private List<String> _ids;

	private boolean _completed;

	public static AuditTimeAndIdsRecord of(long timestamp, String id) {

		AuditTimeAndIdsRecord auditTimeAndIdsRecord = new AuditTimeAndIdsRecord();

		auditTimeAndIdsRecord.setTimestamp(timestamp);
		auditTimeAndIdsRecord.addId(id);
		auditTimeAndIdsRecord.setCompleted(false);

		return auditTimeAndIdsRecord;
	}

	public long getTimestamp() {

		return _timestamp;
	}

	public void setTimestamp(long timestamp) {

		this._timestamp = timestamp;
	}

	public List<String> getIds() {

		return _ids;
	}

	public void addId(String id) {

		if (_ids == null) {

			_ids = new ArrayList<>();
		}

		_ids.add(id);
	}

	public void setIds(List<String> ids) {
		_ids = ids;
	}

	public int getTotalProcessed() {
		return _totalProcessed;
	}

	public void setTotalProcessed(int _totalProcessed) {
		this._totalProcessed = _totalProcessed;
	}

	public int getTotalUploaded() {
		return _totalUploaded;
	}

	public void setTotalUploaded(int _totalUploaded) {
		this._totalUploaded = _totalUploaded;
	}

	public boolean isCompleted() {
		return _completed;
	}

	public void setCompleted(boolean completed) {
		_completed = completed;
	}
}
