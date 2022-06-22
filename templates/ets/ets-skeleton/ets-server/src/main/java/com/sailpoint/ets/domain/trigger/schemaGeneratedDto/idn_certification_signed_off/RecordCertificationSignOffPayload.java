/*
* Copyright (C) 2021 SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.ets.domain.trigger.schemaGeneratedDto.idn_certification_signed_off;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonPropertyOrder({
    "certification"
})
@JsonIgnoreProperties(ignoreUnknown = true)
public class RecordCertificationSignOffPayload {

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("certification")
    private RecordCertification certification;

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("certification")
    public RecordCertification getCertification() {
        return certification;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("certification")
    public void setCertification(RecordCertification certification) {
        this.certification = certification;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(RecordCertificationSignOffPayload.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("certification");
        sb.append('=');
        sb.append(((this.certification == null)?"<null>":this.certification));
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
        result = ((result* 31)+((this.certification == null)? 0 :this.certification.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof RecordCertificationSignOffPayload) == false) {
            return false;
        }
        RecordCertificationSignOffPayload rhs = ((RecordCertificationSignOffPayload) other);
        return ((this.certification == rhs.certification)||((this.certification!= null)&&this.certification.equals(rhs.certification)));
    }

}
