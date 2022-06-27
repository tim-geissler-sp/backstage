/*
 * Copyright (c) 2021. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.audit.service;

import java.time.Instant;

/**
 * The time range over which a CIS->S3 Sync Job applies.  Goes from `startTime` though `upToTime`.
 * For example: 2021-10-12T00:00:00.000Z -> 2021-10-12T01:59:59.999Z covers the first hour of Oct 12th.
 * The `expectedRecordCount` is the number of records counted to exist in that time range that should be
 * synchronized across.
 */

public class SyncJobTimeRange {

	Instant startTime;

	Instant upToTime;

	long expectedRecordCount;

	/**
	 * The Instant from which to start querying for Audit Events in CIS.
	 * @return the Instant time value.
	 */
	public Instant getStartTime() {
		return startTime;
	}

	/**
	 * Sets the Instant from which to start querying for Audit Events in CIS.
	 */
	public void setStartTime(Instant startTime) {
		this.startTime = startTime;
	}

	/**
	 * The Instant up though which to finishg querying for Audit Events in CIS.
	 * @return - the Instant time value.
	 */
	public Instant getUpToTime() {
		return upToTime;
	}

	/**
	 * Sets the Instant up though which to finishg querying for Audit Events in CIS.
	 */
	public void setUpToTime(Instant upToTime) {
		this.upToTime = upToTime;
	}

	/**
	 * @return - The number of records expected in the given time range.
	 */
	public long getExpectedRecordCount() {
		return expectedRecordCount;
	}

	/**
	 * Sets the number of records expected in the given time range.
	 */
	public void setExpectedRecordCount(long expectedRecordCount) {
		this.expectedRecordCount = expectedRecordCount;
	}

}
