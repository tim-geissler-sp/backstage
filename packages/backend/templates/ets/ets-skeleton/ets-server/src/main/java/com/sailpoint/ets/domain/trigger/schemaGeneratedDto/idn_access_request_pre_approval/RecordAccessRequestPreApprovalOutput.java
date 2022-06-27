/*
* Copyright (C) 2021 SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.ets.domain.trigger.schemaGeneratedDto.idn_access_request_pre_approval;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonPropertyOrder({
    "approved",
    "comment",
    "approver"
})
@JsonIgnoreProperties(ignoreUnknown = true)
public class RecordAccessRequestPreApprovalOutput {

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
    @JsonProperty("comment")
    private Object comment;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("approver")
    private String approver;

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

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("comment")
    public Object getComment() {
        return comment;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("comment")
    public void setComment(Object comment) {
        this.comment = comment;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("approver")
    public String getApprover() {
        return approver;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("approver")
    public void setApprover(String approver) {
        this.approver = approver;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(RecordAccessRequestPreApprovalOutput.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("approved");
        sb.append('=');
        sb.append(((this.approved == null)?"<null>":this.approved));
        sb.append(',');
        sb.append("comment");
        sb.append('=');
        sb.append(((this.comment == null)?"<null>":this.comment));
        sb.append(',');
        sb.append("approver");
        sb.append('=');
        sb.append(((this.approver == null)?"<null>":this.approver));
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
        result = ((result* 31)+((this.approver == null)? 0 :this.approver.hashCode()));
        result = ((result* 31)+((this.approved == null)? 0 :this.approved.hashCode()));
        result = ((result* 31)+((this.comment == null)? 0 :this.comment.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof RecordAccessRequestPreApprovalOutput) == false) {
            return false;
        }
        RecordAccessRequestPreApprovalOutput rhs = ((RecordAccessRequestPreApprovalOutput) other);
        return ((((this.approver == rhs.approver)||((this.approver!= null)&&this.approver.equals(rhs.approver)))&&((this.approved == rhs.approved)||((this.approved!= null)&&this.approved.equals(rhs.approved))))&&((this.comment == rhs.comment)||((this.comment!= null)&&this.comment.equals(rhs.comment))));
    }

}
