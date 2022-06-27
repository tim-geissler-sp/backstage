/*
 * Copyright (c) 2020. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.audit.service.model;

import java.util.HashSet;
import java.util.Set;

public class AuditUploadStatus {
	private int _batchProcessed = 0;
	private int _batchUploaded = 0;
	private int _sessionProcessed = 0;
	private int _sessionUploaded = 0;
	private int _sessionErrors = 0;
	private int _sessionSkipped = 0;
	private int _sessionLimit = 0;
	private int _totalRemaining = 0;
	private int _totalProcessed = 0;
	private int _totalUploaded = 0;
	private long _startTime = 0;
	private long _createdTime = 0;
	private long _lastCreatedTime = 0;
	private boolean _uploadToSearch = true;
	private boolean _onetimeSync = false;
	private Set<String> _lastIds = new HashSet<>();

	public int getBatchProcessed() {
		return _batchProcessed;
	}

	public void setBatchProcessed(int batchProcessed) {
		this._batchProcessed = batchProcessed;
	}

	public void incrementProcessed() {
		this._batchProcessed++;
		this._sessionProcessed++;
		this._totalProcessed++;
	}

	public int getBatchUploaded() {
		return _batchUploaded;
	}

	public void setBatchUploaded(int batchUploaded) {
		this._batchUploaded = batchUploaded;
	}

	public void incrementUploaded() {
		this._batchUploaded++;
		this._sessionUploaded++;
		this._totalUploaded++;
	}

	public int getSessionProcessed() {
		return _sessionProcessed;
	}

	public int getSessionUploaded() {
		return _sessionUploaded;
	}

	public int getTotalRemaining() {
		return _totalRemaining;
	}

	public void setTotalRemaining(int totalRemaining) {
		_totalRemaining = totalRemaining;
	}

	public int getTotalProcessed() {
		return _totalProcessed;
	}

	public void setTotalProcessed(int totalProcessed) {
		this._totalProcessed = totalProcessed;
	}

	public int getTotalUploaded() {
		return _totalUploaded;
	}

	public void setTotalUploaded(int totalUploaded) {
		this._totalUploaded = totalUploaded;
	}

	public int getSessionErrors() {
		return _sessionErrors;
	}

	public void incrementSessionErrors() {
		this._sessionErrors++;
	}

	public int getSessionSkipped() {
		return _sessionSkipped;
	}

	public void incrementSessionSkipped() {
		this._sessionSkipped++;
	}

	public int getSessionLimit() {
		return _sessionLimit;
	}

	public void setSessionLimit(int sessionLimit) {
		this._sessionLimit = sessionLimit;
	}

	public Set<String> getLastIds() {
		return _lastIds;
	}

	public void addId(String id) {
		_lastIds.add(id);
	}

	public void addIds(Set<String> ids) {
		_lastIds.addAll(ids);
	}

	public long getStartTime() {
		return _startTime;
	}

	public void setStartTime(long startTime) {
		this._startTime = startTime;
	}

	public long getCreatedTime() {
		return _createdTime;
	}

	public void setCreatedTime(long createdTime) {
		_createdTime = createdTime;
	}

	public long getLastCreatedTime() {
		return _lastCreatedTime;
	}

	public void setLastCreatedTime(long lastCreatedTime) {
		_lastCreatedTime = lastCreatedTime;
	}

	public boolean isUploadToSearch() {
		return _uploadToSearch;
	}

	public void setUploadToSearch(boolean uploadToSearch) {
		_uploadToSearch = uploadToSearch;
	}

	public boolean isOnetimeSync() {
		return _onetimeSync;
	}

	public void setOnetimeSync(boolean onetimeSync) {
		_onetimeSync = onetimeSync;
	}
}
