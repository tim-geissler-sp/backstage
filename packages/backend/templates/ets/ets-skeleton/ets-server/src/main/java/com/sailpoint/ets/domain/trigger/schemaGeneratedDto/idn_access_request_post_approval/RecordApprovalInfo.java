/*
* Copyright (C) 2021 SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.ets.domain.trigger.schemaGeneratedDto.idn_access_request_post_approval;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonPropertyOrder({
    "approvalComment",
    "approvalDecision",
    "approverName",
    "approver"
})
@JsonIgnoreProperties(ignoreUnknown = true)
public class RecordApprovalInfo {

    @JsonProperty("approvalComment")
    private Object approvalComment;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("approvalDecision")
    private String approvalDecision;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("approverName")
    private String approverName;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("approver")
    private RecordApproverIdentityRef approver;

    @JsonProperty("approvalComment")
    public Object getApprovalComment() {
        return approvalComment;
    }

    @JsonProperty("approvalComment")
    public void setApprovalComment(Object approvalComment) {
        this.approvalComment = approvalComment;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("approvalDecision")
    public String getApprovalDecision() {
        return approvalDecision;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("approvalDecision")
    public void setApprovalDecision(String approvalDecision) {
        this.approvalDecision = approvalDecision;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("approverName")
    public String getApproverName() {
        return approverName;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("approverName")
    public void setApproverName(String approverName) {
        this.approverName = approverName;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("approver")
    public RecordApproverIdentityRef getApprover() {
        return approver;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("approver")
    public void setApprover(RecordApproverIdentityRef approver) {
        this.approver = approver;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(RecordApprovalInfo.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("approvalComment");
        sb.append('=');
        sb.append(((this.approvalComment == null)?"<null>":this.approvalComment));
        sb.append(',');
        sb.append("approvalDecision");
        sb.append('=');
        sb.append(((this.approvalDecision == null)?"<null>":this.approvalDecision));
        sb.append(',');
        sb.append("approverName");
        sb.append('=');
        sb.append(((this.approverName == null)?"<null>":this.approverName));
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
        result = ((result* 31)+((this.approvalDecision == null)? 0 :this.approvalDecision.hashCode()));
        result = ((result* 31)+((this.approvalComment == null)? 0 :this.approvalComment.hashCode()));
        result = ((result* 31)+((this.approverName == null)? 0 :this.approverName.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof RecordApprovalInfo) == false) {
            return false;
        }
        RecordApprovalInfo rhs = ((RecordApprovalInfo) other);
        return (((((this.approver == rhs.approver)||((this.approver!= null)&&this.approver.equals(rhs.approver)))&&((this.approvalDecision == rhs.approvalDecision)||((this.approvalDecision!= null)&&this.approvalDecision.equals(rhs.approvalDecision))))&&((this.approvalComment == rhs.approvalComment)||((this.approvalComment!= null)&&this.approvalComment.equals(rhs.approvalComment))))&&((this.approverName == rhs.approverName)||((this.approverName!= null)&&this.approverName.equals(rhs.approverName))));
    }

}
