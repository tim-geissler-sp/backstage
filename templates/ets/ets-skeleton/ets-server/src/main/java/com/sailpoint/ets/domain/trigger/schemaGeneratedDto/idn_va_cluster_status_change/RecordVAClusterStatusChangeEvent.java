/*
* Copyright (C) 2021 SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.ets.domain.trigger.schemaGeneratedDto.idn_va_cluster_status_change;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonPropertyOrder({
    "created",
    "type",
    "application",
    "healthCheckResult",
    "previousHealthCheckResult"
})
@JsonIgnoreProperties(ignoreUnknown = true)
public class RecordVAClusterStatusChangeEvent {

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("created")
    private String created;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("type")
    private String type;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("application")
    private RecordApplicationInfo application;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("healthCheckResult")
    private RecordHealthCheckResultInfo healthCheckResult;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("previousHealthCheckResult")
    private RecordPreviousHealthCheckResultInfo previousHealthCheckResult;

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("created")
    public String getCreated() {
        return created;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("created")
    public void setCreated(String created) {
        this.created = created;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("type")
    public String getType() {
        return type;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("type")
    public void setType(String type) {
        this.type = type;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("application")
    public RecordApplicationInfo getApplication() {
        return application;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("application")
    public void setApplication(RecordApplicationInfo application) {
        this.application = application;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("healthCheckResult")
    public RecordHealthCheckResultInfo getHealthCheckResult() {
        return healthCheckResult;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("healthCheckResult")
    public void setHealthCheckResult(RecordHealthCheckResultInfo healthCheckResult) {
        this.healthCheckResult = healthCheckResult;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("previousHealthCheckResult")
    public RecordPreviousHealthCheckResultInfo getPreviousHealthCheckResult() {
        return previousHealthCheckResult;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("previousHealthCheckResult")
    public void setPreviousHealthCheckResult(RecordPreviousHealthCheckResultInfo previousHealthCheckResult) {
        this.previousHealthCheckResult = previousHealthCheckResult;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(RecordVAClusterStatusChangeEvent.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("created");
        sb.append('=');
        sb.append(((this.created == null)?"<null>":this.created));
        sb.append(',');
        sb.append("type");
        sb.append('=');
        sb.append(((this.type == null)?"<null>":this.type));
        sb.append(',');
        sb.append("application");
        sb.append('=');
        sb.append(((this.application == null)?"<null>":this.application));
        sb.append(',');
        sb.append("healthCheckResult");
        sb.append('=');
        sb.append(((this.healthCheckResult == null)?"<null>":this.healthCheckResult));
        sb.append(',');
        sb.append("previousHealthCheckResult");
        sb.append('=');
        sb.append(((this.previousHealthCheckResult == null)?"<null>":this.previousHealthCheckResult));
        sb.append(',');
        if (sb.charAt((sb.length()- 1)) == ',') {
            sb.setCharAt((sb.length()- 1), ']');
        } else {
            sb.append(']');
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = ((result* 31)+((this.type == null)? 0 :this.type.hashCode()));
        result = ((result* 31)+((this.application == null)? 0 :this.application.hashCode()));
        result = ((result* 31)+((this.created == null)? 0 :this.created.hashCode()));
        result = ((result* 31)+((this.healthCheckResult == null)? 0 :this.healthCheckResult.hashCode()));
        result = ((result* 31)+((this.previousHealthCheckResult == null)? 0 :this.previousHealthCheckResult.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof RecordVAClusterStatusChangeEvent) == false) {
            return false;
        }
        RecordVAClusterStatusChangeEvent rhs = ((RecordVAClusterStatusChangeEvent) other);
        return ((((((this.type == rhs.type)||((this.type!= null)&&this.type.equals(rhs.type)))&&((this.application == rhs.application)||((this.application!= null)&&this.application.equals(rhs.application))))&&((this.created == rhs.created)||((this.created!= null)&&this.created.equals(rhs.created))))&&((this.healthCheckResult == rhs.healthCheckResult)||((this.healthCheckResult!= null)&&this.healthCheckResult.equals(rhs.healthCheckResult))))&&((this.previousHealthCheckResult == rhs.previousHealthCheckResult)||((this.previousHealthCheckResult!= null)&&this.previousHealthCheckResult.equals(rhs.previousHealthCheckResult))));
    }

}
