/*
* Copyright (C) 2021 SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.ets.domain.trigger.schemaGeneratedDto.idn_access_request_post_approval;

import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonPropertyOrder({
    "accessRequestId",
    "requestedFor",
    "requestedItemsStatus",
    "requestedBy"
})
@JsonIgnoreProperties(ignoreUnknown = true)
public class RecordAccessRequestPreApprovalInput {

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("accessRequestId")
    private String accessRequestId;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("requestedFor")
    private RecordRequestForIdentityRef requestedFor;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("requestedItemsStatus")
    private List<RecordRequestedItemsStatusRef> requestedItemsStatus = new ArrayList<RecordRequestedItemsStatusRef>();
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("requestedBy")
    private RecordRequestByIdentityRef requestedBy;

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("accessRequestId")
    public String getAccessRequestId() {
        return accessRequestId;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("accessRequestId")
    public void setAccessRequestId(String accessRequestId) {
        this.accessRequestId = accessRequestId;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("requestedFor")
    public RecordRequestForIdentityRef getRequestedFor() {
        return requestedFor;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("requestedFor")
    public void setRequestedFor(RecordRequestForIdentityRef requestedFor) {
        this.requestedFor = requestedFor;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("requestedItemsStatus")
    public List<RecordRequestedItemsStatusRef> getRequestedItemsStatus() {
        return requestedItemsStatus;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("requestedItemsStatus")
    public void setRequestedItemsStatus(List<RecordRequestedItemsStatusRef> requestedItemsStatus) {
        this.requestedItemsStatus = requestedItemsStatus;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("requestedBy")
    public RecordRequestByIdentityRef getRequestedBy() {
        return requestedBy;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("requestedBy")
    public void setRequestedBy(RecordRequestByIdentityRef requestedBy) {
        this.requestedBy = requestedBy;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(RecordAccessRequestPreApprovalInput.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("accessRequestId");
        sb.append('=');
        sb.append(((this.accessRequestId == null)?"<null>":this.accessRequestId));
        sb.append(',');
        sb.append("requestedFor");
        sb.append('=');
        sb.append(((this.requestedFor == null)?"<null>":this.requestedFor));
        sb.append(',');
        sb.append("requestedItemsStatus");
        sb.append('=');
        sb.append(((this.requestedItemsStatus == null)?"<null>":this.requestedItemsStatus));
        sb.append(',');
        sb.append("requestedBy");
        sb.append('=');
        sb.append(((this.requestedBy == null)?"<null>":this.requestedBy));
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
        result = ((result* 31)+((this.requestedFor == null)? 0 :this.requestedFor.hashCode()));
        result = ((result* 31)+((this.requestedBy == null)? 0 :this.requestedBy.hashCode()));
        result = ((result* 31)+((this.accessRequestId == null)? 0 :this.accessRequestId.hashCode()));
        result = ((result* 31)+((this.requestedItemsStatus == null)? 0 :this.requestedItemsStatus.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof RecordAccessRequestPreApprovalInput) == false) {
            return false;
        }
        RecordAccessRequestPreApprovalInput rhs = ((RecordAccessRequestPreApprovalInput) other);
        return (((((this.requestedFor == rhs.requestedFor)||((this.requestedFor!= null)&&this.requestedFor.equals(rhs.requestedFor)))&&((this.requestedBy == rhs.requestedBy)||((this.requestedBy!= null)&&this.requestedBy.equals(rhs.requestedBy))))&&((this.accessRequestId == rhs.accessRequestId)||((this.accessRequestId!= null)&&this.accessRequestId.equals(rhs.accessRequestId))))&&((this.requestedItemsStatus == rhs.requestedItemsStatus)||((this.requestedItemsStatus!= null)&&this.requestedItemsStatus.equals(rhs.requestedItemsStatus))));
    }

}
