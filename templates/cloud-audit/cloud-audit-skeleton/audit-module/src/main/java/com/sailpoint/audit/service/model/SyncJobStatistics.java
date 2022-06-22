/*
 * Copyright (c) 2021. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.audit.service.model;

import com.sailpoint.atlas.OrgData;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Record to keep track of the progress of a synchronization job.  This is accessed by both the job handler and by
 * the worker threads so they can communicate about various peg counters as the synchronization job is processed.
 *
 * When a sync job starts there is some set of AuditEvent IDs that get added to a queue for processing. These are
 * counted in the `submittedToQueue` counter.  These go to a thread pool where are worker thread adopts a request
 * to retrieve a single AuditEvent from a backing store (i.e CIS when syncing to S3). When a worker thread adopts
 * an ID to sync hte `adoptedByWorkerThread` counter is incremented. When the job is done these two peg counters
 * should match; the total number submitted should equal the number adopted by worker threads.
 *
 * Three things can happen inside a worker thread after it adopts an ID to sync.  It can
 * A) not be found in the source store (i.e CIS doesn't have it).  This increments the `missingFromSource` counter.
 * B) be found in the source store and successfully get created in the target, incrementing `newlyCreatedInTarget`.
 * C) be found in the source store and already exist in the target, incrementing the `alreadyExistedInTarget`.
 *
 * The sum of the counters from conditions A, B, C should match the submission counter and adoption counters when
 * the job is completed.  Any records that fall under condition "A" are exceptional (i.e.WARN/ERROR) problems.
 *
 */
public class SyncJobStatistics {

	// Records submitted to the queue for processing.
	AtomicLong submittedToQueue = new AtomicLong(0);

	// Records picked up by a worker thread to examine.
	AtomicLong adoptedByWorkerThread = new AtomicLong(0);

	// Records missing from the source of the sync data (i.e. missing from CIS).
	AtomicLong missingFromSource = new AtomicLong(0);

	// Records successfully created in the target system.  Normal, successful sync count.
	AtomicLong newlyCreatedInTarget = new AtomicLong(0);

	// Records that already existed and match in the target.  No work was necessary.
	AtomicLong alreadyExistedInTarget = new AtomicLong(0);

	// Records that resulted in some unexpected exception.
	AtomicLong exceptionCounter = new AtomicLong(0);

	// Records submitted to the queue for processing.
	public AtomicLong getSubmittedToQueue() {
		return submittedToQueue;
	}

	public void setSubmittedToQueue(AtomicLong submittedToQueue) {
		this.submittedToQueue = submittedToQueue;
	}

	// Records picked up by a worker thread to examine.
	public AtomicLong getAdoptedByWorkerThread() {
		return adoptedByWorkerThread;
	}

	public void setAdoptedByWorkerThread(AtomicLong adoptedByWorkerThread) {
		this.adoptedByWorkerThread = adoptedByWorkerThread;
	}

	// Records missing from the source of the sync data (i.e. missing from CIS).
	public AtomicLong getMissingFromSource() {
		return missingFromSource;
	}

	public void setMissingFromSource(AtomicLong missingFromSource) {
		this.missingFromSource = missingFromSource;
	}

	// Records successfully created in the target system.  Normal, successful sync count.
	public AtomicLong getNewlyCreatedInTarget() {
		return newlyCreatedInTarget;
	}

	public void setNewlyCreatedInTarget(AtomicLong newlyCreatedInTarget) {
		this.newlyCreatedInTarget = newlyCreatedInTarget;
	}

	// Records that already existed and match in the target.  No work was necessary.
	public AtomicLong getAlreadyExistedInTarget() {
		return alreadyExistedInTarget;
	}

	public void setAlreadyExistedInTarget(AtomicLong alreadyExistedInTarget) {
		this.alreadyExistedInTarget = alreadyExistedInTarget;
	}

	// Records that resulted in some unexpected exception.
	public AtomicLong getExceptionCounter() {
		return exceptionCounter;
	}

	public void setExceptionCounter(AtomicLong exceptionCounter) {
		this.exceptionCounter = exceptionCounter;
	}

	/**
	 * Sums the 'missingFromSource', 'newlyCreatedInTarget' and 'alreadyExistedInTarget' peg counters to
	 * count the output and work completed by a worker thread pool for a given sync job.
	 * @return - the sum of the counters.
	 */
	public long getSumResultsCount() {
		return missingFromSource.get() + newlyCreatedInTarget.get() + alreadyExistedInTarget.get();
	}

	/**
	 * Sum another SyncJobStatistics contents into this record.
	 * @param other
	 * @return - this SyncJobStatiscs, summed with the values from the other SyncJobStatiscs.
	 */
	public SyncJobStatistics mergeResults(SyncJobStatistics other) {
		if (null == other) return this;
		submittedToQueue.addAndGet(other.submittedToQueue.get());
		adoptedByWorkerThread.addAndGet(other.adoptedByWorkerThread.get());
		missingFromSource.addAndGet(other.missingFromSource.get());
		newlyCreatedInTarget.addAndGet(other.newlyCreatedInTarget.get());
		alreadyExistedInTarget.addAndGet(other.alreadyExistedInTarget.get());
		exceptionCounter.addAndGet(other.exceptionCounter.get());
		return this;
	}

	/**
	 * Was this SyncJob a complete success.  A completely successful SyncJob has no errors, no not-found records,
	 * and the adoption matched the queued number and the sum of newly created and already existed equals the number
	 * of records submitted.
	 *
	 * @return - a true/false value for if the SyncJob was a complete success.
	 */
	public boolean isCompleteSuccess() {
		if (0 != exceptionCounter.get()) return false;
		if (0 != missingFromSource.get()) return false;
		if (submittedToQueue.get() != adoptedByWorkerThread.get()) return false;
		if (submittedToQueue.get() != ( newlyCreatedInTarget.get() + alreadyExistedInTarget.get() )) return false;
		return true;
	}

}
