/*
 * Copyright (c) 2021. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.audit.service.model;

public class FirehoseTrackingRecord {
    int initialFirehoseId;
    int currentFirehoseId;

    public int getInitialFirehoseId() {
        return initialFirehoseId;
    }

    public void setInitialFirehoseId(int initialFirehoseId) {
        this.initialFirehoseId = initialFirehoseId;
    }

    public int getCurrentFirehoseId() {
        return currentFirehoseId;
    }

    public void setCurrentFirehoseId(int currentFirehoseId) {
        this.currentFirehoseId = currentFirehoseId;
    }
}
