/*
* Copyright (C) 2021 SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.ets.domain.trigger.schemaGeneratedDto.idn_post_provisioning;

import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonPropertyOrder({
    "trackingNumber",
    "sources",
    "action",
    "errors",
    "warnings",
    "recipient",
    "requester",
    "accountRequests"
})
@JsonIgnoreProperties(ignoreUnknown = true)
public class RecordPostProvisioningInput {

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("trackingNumber")
    private String trackingNumber;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("sources")
    private String sources;
    @JsonProperty("action")
    private Object action;
    @JsonProperty("errors")
    private Object errors;
    @JsonProperty("warnings")
    private Object warnings;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("recipient")
    private RecordRequestForIdentityRef recipient;
    @JsonProperty("requester")
    private Object requester;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("accountRequests")
    private List<RecordAccountRequestsRecord> accountRequests = new ArrayList<RecordAccountRequestsRecord>();

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("trackingNumber")
    public String getTrackingNumber() {
        return trackingNumber;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("trackingNumber")
    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("sources")
    public String getSources() {
        return sources;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("sources")
    public void setSources(String sources) {
        this.sources = sources;
    }

    @JsonProperty("action")
    public Object getAction() {
        return action;
    }

    @JsonProperty("action")
    public void setAction(Object action) {
        this.action = action;
    }

    @JsonProperty("errors")
    public Object getErrors() {
        return errors;
    }

    @JsonProperty("errors")
    public void setErrors(Object errors) {
        this.errors = errors;
    }

    @JsonProperty("warnings")
    public Object getWarnings() {
        return warnings;
    }

    @JsonProperty("warnings")
    public void setWarnings(Object warnings) {
        this.warnings = warnings;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("recipient")
    public RecordRequestForIdentityRef getRecipient() {
        return recipient;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("recipient")
    public void setRecipient(RecordRequestForIdentityRef recipient) {
        this.recipient = recipient;
    }

    @JsonProperty("requester")
    public Object getRequester() {
        return requester;
    }

    @JsonProperty("requester")
    public void setRequester(Object requester) {
        this.requester = requester;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("accountRequests")
    public List<RecordAccountRequestsRecord> getAccountRequests() {
        return accountRequests;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("accountRequests")
    public void setAccountRequests(List<RecordAccountRequestsRecord> accountRequests) {
        this.accountRequests = accountRequests;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(RecordPostProvisioningInput.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("trackingNumber");
        sb.append('=');
        sb.append(((this.trackingNumber == null)?"<null>":this.trackingNumber));
        sb.append(',');
        sb.append("sources");
        sb.append('=');
        sb.append(((this.sources == null)?"<null>":this.sources));
        sb.append(',');
        sb.append("action");
        sb.append('=');
        sb.append(((this.action == null)?"<null>":this.action));
        sb.append(',');
        sb.append("errors");
        sb.append('=');
        sb.append(((this.errors == null)?"<null>":this.errors));
        sb.append(',');
        sb.append("warnings");
        sb.append('=');
        sb.append(((this.warnings == null)?"<null>":this.warnings));
        sb.append(',');
        sb.append("recipient");
        sb.append('=');
        sb.append(((this.recipient == null)?"<null>":this.recipient));
        sb.append(',');
        sb.append("requester");
        sb.append('=');
        sb.append(((this.requester == null)?"<null>":this.requester));
        sb.append(',');
        sb.append("accountRequests");
        sb.append('=');
        sb.append(((this.accountRequests == null)?"<null>":this.accountRequests));
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
        result = ((result* 31)+((this.requester == null)? 0 :this.requester.hashCode()));
        result = ((result* 31)+((this.sources == null)? 0 :this.sources.hashCode()));
        result = ((result* 31)+((this.warnings == null)? 0 :this.warnings.hashCode()));
        result = ((result* 31)+((this.recipient == null)? 0 :this.recipient.hashCode()));
        result = ((result* 31)+((this.action == null)? 0 :this.action.hashCode()));
        result = ((result* 31)+((this.accountRequests == null)? 0 :this.accountRequests.hashCode()));
        result = ((result* 31)+((this.trackingNumber == null)? 0 :this.trackingNumber.hashCode()));
        result = ((result* 31)+((this.errors == null)? 0 :this.errors.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof RecordPostProvisioningInput) == false) {
            return false;
        }
        RecordPostProvisioningInput rhs = ((RecordPostProvisioningInput) other);
        return (((((((((this.requester == rhs.requester)||((this.requester!= null)&&this.requester.equals(rhs.requester)))&&((this.sources == rhs.sources)||((this.sources!= null)&&this.sources.equals(rhs.sources))))&&((this.warnings == rhs.warnings)||((this.warnings!= null)&&this.warnings.equals(rhs.warnings))))&&((this.recipient == rhs.recipient)||((this.recipient!= null)&&this.recipient.equals(rhs.recipient))))&&((this.action == rhs.action)||((this.action!= null)&&this.action.equals(rhs.action))))&&((this.accountRequests == rhs.accountRequests)||((this.accountRequests!= null)&&this.accountRequests.equals(rhs.accountRequests))))&&((this.trackingNumber == rhs.trackingNumber)||((this.trackingNumber!= null)&&this.trackingNumber.equals(rhs.trackingNumber))))&&((this.errors == rhs.errors)||((this.errors!= null)&&this.errors.equals(rhs.errors))));
    }

}
