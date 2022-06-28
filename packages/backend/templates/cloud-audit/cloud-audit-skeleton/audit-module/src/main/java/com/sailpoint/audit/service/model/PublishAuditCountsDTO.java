/*
 * Copyright (c) 2021. SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.audit.service.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class PublishAuditCountsDTO {
    private String _publishCountsDate;

    public PublishAuditCountsDTO(String publishCountsDate){
        this._publishCountsDate = publishCountsDate;
    }

    public String getPublishCountsDate() {
        return _publishCountsDate;
    }

    public void setPublishCountsDate(String publishCountsDate) {
        this._publishCountsDate = publishCountsDate;
    }
}
