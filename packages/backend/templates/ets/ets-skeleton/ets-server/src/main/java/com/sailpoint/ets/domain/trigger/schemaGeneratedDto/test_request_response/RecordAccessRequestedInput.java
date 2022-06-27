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
    "identityId"
})
@JsonIgnoreProperties(ignoreUnknown = true)
public class RecordAccessRequestedInput {

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("identityId")
    private String identityId;

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("identityId")
    public String getIdentityId() {
        return identityId;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("identityId")
    public void setIdentityId(String identityId) {
        this.identityId = identityId;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(RecordAccessRequestedInput.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("identityId");
        sb.append('=');
        sb.append(((this.identityId == null)?"<null>":this.identityId));
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
        result = ((result* 31)+((this.identityId == null)? 0 :this.identityId.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof RecordAccessRequestedInput) == false) {
            return false;
        }
        RecordAccessRequestedInput rhs = ((RecordAccessRequestedInput) other);
        return ((this.identityId == rhs.identityId)||((this.identityId!= null)&&this.identityId.equals(rhs.identityId)));
    }

}
