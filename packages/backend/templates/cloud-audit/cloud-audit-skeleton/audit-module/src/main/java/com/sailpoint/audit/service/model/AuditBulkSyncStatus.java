/*
 * Copyright (c) 2020. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.audit.service.model;

public class AuditBulkSyncStatus {
	private int _totalFiles = 0;
	private int _totalProcessed = 0;
	private int _totalFiltered = 0;
	private int _batchSynced = 0;
	private long _startTime = 0;

	public int getTotalFiles() {
		return _totalFiles;
	}

	public void incrementFiles() {
		_totalFiles++;
	}

	public int getTotalFiltered() {
		return _totalFiltered;
	}

	public void incrementTotalFiltered() {
		_totalFiltered++;
	}

	public int getTotalProcessed() {
		return _totalProcessed;
	}

	public void incrementTotalProcessed() {
		_totalProcessed++;
	}

	public int getBatchSynced() {
		return _batchSynced;
	}

	public void incrementBatchSynced() {
		_batchSynced++;
	}

	public void setBatchSynced(int batchSynced) {
		_batchSynced = batchSynced;
	}

	public long getStartTime() {
		return _startTime;
	}

	public void setStartTime(long startTime) {
		this._startTime = startTime;
	}

}
