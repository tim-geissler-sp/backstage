/*
* Copyright (C) 2021 SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.ets.domain.trigger.schemaGeneratedDto.test_request_response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonPropertyOrder({
    "approved"
})
@JsonIgnoreProperties(ignoreUnknown = true)
public class RecordAccessRequestedOutput {

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("approved")
    private Boolean approved;

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("approved")
    public Boolean getApproved() {
        return approved;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("approved")
    public void setApproved(Boolean approved) {
        this.approved = approved;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(RecordAccessRequestedOutput.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("approved");
        sb.append('=');
        sb.append(((this.approved == null)?"<null>":this.approved));
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
        result = ((result* 31)+((this.approved == null)? 0 :this.approved.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof RecordAccessRequestedOutput) == false) {
            return false;
        }
        RecordAccessRequestedOutput rhs = ((RecordAccessRequestedOutput) other);
        return ((this.approved == rhs.approved)||((this.approved!= null)&&this.approved.equals(rhs.approved)));
    }

}
