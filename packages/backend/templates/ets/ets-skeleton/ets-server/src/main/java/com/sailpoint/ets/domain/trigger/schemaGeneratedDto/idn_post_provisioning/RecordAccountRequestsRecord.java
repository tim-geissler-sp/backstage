/*
* Copyright (C) 2021 SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.ets.domain.trigger.schemaGeneratedDto.idn_post_provisioning;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonPropertyOrder({
    "source",
    "accountId",
    "accountOperation",
    "provisioningResult",
    "provisioningTarget",
    "ticketId",
    "attributeRequests"
})
@JsonIgnoreProperties(ignoreUnknown = true)
public class RecordAccountRequestsRecord {

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("source")
    private RecordSourceRef source;
    @JsonProperty("accountId")
    private String accountId;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("accountOperation")
    private String accountOperation;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("provisioningResult")
    private String provisioningResult;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("provisioningTarget")
    private String provisioningTarget;
    @JsonProperty("ticketId")
    private Object ticketId;
    @JsonProperty("attributeRequests")
    private Object attributeRequests;

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("source")
    public RecordSourceRef getSource() {
        return source;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("source")
    public void setSource(RecordSourceRef source) {
        this.source = source;
    }

    @JsonProperty("accountId")
    public String getAccountId() {
        return accountId;
    }

    @JsonProperty("accountId")
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("accountOperation")
    public String getAccountOperation() {
        return accountOperation;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("accountOperation")
    public void setAccountOperation(String accountOperation) {
        this.accountOperation = accountOperation;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("provisioningResult")
    public String getProvisioningResult() {
        return provisioningResult;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("provisioningResult")
    public void setProvisioningResult(String provisioningResult) {
        this.provisioningResult = provisioningResult;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("provisioningTarget")
    public String getProvisioningTarget() {
        return provisioningTarget;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("provisioningTarget")
    public void setProvisioningTarget(String provisioningTarget) {
        this.provisioningTarget = provisioningTarget;
    }

    @JsonProperty("ticketId")
    public Object getTicketId() {
        return ticketId;
    }

    @JsonProperty("ticketId")
    public void setTicketId(Object ticketId) {
        this.ticketId = ticketId;
    }

    @JsonProperty("attributeRequests")
    public Object getAttributeRequests() {
        return attributeRequests;
    }

    @JsonProperty("attributeRequests")
    public void setAttributeRequests(Object attributeRequests) {
        this.attributeRequests = attributeRequests;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(RecordAccountRequestsRecord.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("source");
        sb.append('=');
        sb.append(((this.source == null)?"<null>":this.source));
        sb.append(',');
        sb.append("accountId");
        sb.append('=');
        sb.append(((this.accountId == null)?"<null>":this.accountId));
        sb.append(',');
        sb.append("accountOperation");
        sb.append('=');
        sb.append(((this.accountOperation == null)?"<null>":this.accountOperation));
        sb.append(',');
        sb.append("provisioningResult");
        sb.append('=');
        sb.append(((this.provisioningResult == null)?"<null>":this.provisioningResult));
        sb.append(',');
        sb.append("provisioningTarget");
        sb.append('=');
        sb.append(((this.provisioningTarget == null)?"<null>":this.provisioningTarget));
        sb.append(',');
        sb.append("ticketId");
        sb.append('=');
        sb.append(((this.ticketId == null)?"<null>":this.ticketId));
        sb.append(',');
        sb.append("attributeRequests");
        sb.append('=');
        sb.append(((this.attributeRequests == null)?"<null>":this.attributeRequests));
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
        result = ((result* 31)+((this.accountId == null)? 0 :this.accountId.hashCode()));
        result = ((result* 31)+((this.provisioningResult == null)? 0 :this.provisioningResult.hashCode()));
        result = ((result* 31)+((this.provisioningTarget == null)? 0 :this.provisioningTarget.hashCode()));
        result = ((result* 31)+((this.accountOperation == null)? 0 :this.accountOperation.hashCode()));
        result = ((result* 31)+((this.source == null)? 0 :this.source.hashCode()));
        result = ((result* 31)+((this.attributeRequests == null)? 0 :this.attributeRequests.hashCode()));
        result = ((result* 31)+((this.ticketId == null)? 0 :this.ticketId.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof RecordAccountRequestsRecord) == false) {
            return false;
        }
        RecordAccountRequestsRecord rhs = ((RecordAccountRequestsRecord) other);
        return ((((((((this.accountId == rhs.accountId)||((this.accountId!= null)&&this.accountId.equals(rhs.accountId)))&&((this.provisioningResult == rhs.provisioningResult)||((this.provisioningResult!= null)&&this.provisioningResult.equals(rhs.provisioningResult))))&&((this.provisioningTarget == rhs.provisioningTarget)||((this.provisioningTarget!= null)&&this.provisioningTarget.equals(rhs.provisioningTarget))))&&((this.accountOperation == rhs.accountOperation)||((this.accountOperation!= null)&&this.accountOperation.equals(rhs.accountOperation))))&&((this.source == rhs.source)||((this.source!= null)&&this.source.equals(rhs.source))))&&((this.attributeRequests == rhs.attributeRequests)||((this.attributeRequests!= null)&&this.attributeRequests.equals(rhs.attributeRequests))))&&((this.ticketId == rhs.ticketId)||((this.ticketId!= null)&&this.ticketId.equals(rhs.ticketId))));
    }

}
