/*
 * Copyright (c) 2022. SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.audit.service.model;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@JsonIgnoreProperties
public class AuditReportRequest {
    private Integer days = 7;
    private String auditType;
    private String userId;
    private String userName;
    private String appId;
    private String application;
    private String searchText;

    public Integer getDays() {
        return days;
    }

    public void setDays(Integer days) {
        this.days = days;
    }

    public String getAuditType() {
        return auditType;
    }

    public void setAuditType(String auditType) {
        this.auditType = auditType;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getApplication() {
        return application;
    }

    public void setApplication(String application) {
        this.application = application;
    }

    public String getSearchText() {
        return searchText;
    }

    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }


    @Override
    public String toString() {
        return "AuditReportRequest{" +
                "days=" + days +
                ", auditType='" + auditType + '\'' +
                ", userId=" + userId +
                ", userName='" + userName + '\'' +
                ", appId='" + appId + '\'' +
                ", application='" + application + '\'' +
                ", searchText='" + searchText + '\'' +
                '}';
    }
}
