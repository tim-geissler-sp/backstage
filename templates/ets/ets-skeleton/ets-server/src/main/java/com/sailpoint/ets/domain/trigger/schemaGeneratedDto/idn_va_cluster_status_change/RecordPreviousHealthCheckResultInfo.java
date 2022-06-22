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
    "message",
    "resultType",
    "status"
})
@JsonIgnoreProperties(ignoreUnknown = true)
public class RecordPreviousHealthCheckResultInfo {

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("message")
    private String message;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("resultType")
    private String resultType;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("status")
    private String status;

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("message")
    public String getMessage() {
        return message;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("message")
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("resultType")
    public String getResultType() {
        return resultType;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("resultType")
    public void setResultType(String resultType) {
        this.resultType = resultType;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("status")
    public String getStatus() {
        return status;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("status")
    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(RecordPreviousHealthCheckResultInfo.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("message");
        sb.append('=');
        sb.append(((this.message == null)?"<null>":this.message));
        sb.append(',');
        sb.append("resultType");
        sb.append('=');
        sb.append(((this.resultType == null)?"<null>":this.resultType));
        sb.append(',');
        sb.append("status");
        sb.append('=');
        sb.append(((this.status == null)?"<null>":this.status));
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
        result = ((result* 31)+((this.message == null)? 0 :this.message.hashCode()));
        result = ((result* 31)+((this.resultType == null)? 0 :this.resultType.hashCode()));
        result = ((result* 31)+((this.status == null)? 0 :this.status.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof RecordPreviousHealthCheckResultInfo) == false) {
            return false;
        }
        RecordPreviousHealthCheckResultInfo rhs = ((RecordPreviousHealthCheckResultInfo) other);
        return ((((this.message == rhs.message)||((this.message!= null)&&this.message.equals(rhs.message)))&&((this.resultType == rhs.resultType)||((this.resultType!= null)&&this.resultType.equals(rhs.resultType))))&&((this.status == rhs.status)||((this.status!= null)&&this.status.equals(rhs.status))));
    }

}
