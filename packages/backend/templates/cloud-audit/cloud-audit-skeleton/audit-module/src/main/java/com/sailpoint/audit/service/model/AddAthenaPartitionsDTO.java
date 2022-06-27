/*
 * Copyright (c) 2021. SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.audit.service.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class AddAthenaPartitionsDTO {
    private String _partitionDate;

    public AddAthenaPartitionsDTO(String partitionDate){
        this._partitionDate = partitionDate;
    }

    public AddAthenaPartitionsDTO(){
        this._partitionDate = LocalDate.now().toString();
    }

    public String getPartitionDate() {
        return _partitionDate;
    }

    public void setPartitionDate(String partitionDate) {
        this._partitionDate = partitionDate;
    }
}
