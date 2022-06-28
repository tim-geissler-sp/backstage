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
    "id",
    "name",
    "description",
    "type",
    "operation",
    "comment",
    "clientMetadata",
    "approvalInfo"
})
@JsonIgnoreProperties(ignoreUnknown = true)
public class RecordRequestedItemsStatusRef {

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("id")
    private String id;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("name")
    private String name;
    @JsonProperty("description")
    private Object description;
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
    @JsonProperty("operation")
    private String operation;
    @JsonProperty("comment")
    private Object comment;
    @JsonProperty("clientMetadata")
    private Object clientMetadata = null;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("approvalInfo")
    private List<RecordApprovalInfo> approvalInfo = new ArrayList<RecordApprovalInfo>();

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("id")
    public String getId() {
        return id;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("id")
    public void setId(String id) {
        this.id = id;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("name")
    public String getName() {
        return name;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("description")
    public Object getDescription() {
        return description;
    }

    @JsonProperty("description")
    public void setDescription(Object description) {
        this.description = description;
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
    @JsonProperty("operation")
    public String getOperation() {
        return operation;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("operation")
    public void setOperation(String operation) {
        this.operation = operation;
    }

    @JsonProperty("comment")
    public Object getComment() {
        return comment;
    }

    @JsonProperty("comment")
    public void setComment(Object comment) {
        this.comment = comment;
    }

    @JsonProperty("clientMetadata")
    public Object getClientMetadata() {
        return clientMetadata;
    }

    @JsonProperty("clientMetadata")
    public void setClientMetadata(Object clientMetadata) {
        this.clientMetadata = clientMetadata;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("approvalInfo")
    public List<RecordApprovalInfo> getApprovalInfo() {
        return approvalInfo;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("approvalInfo")
    public void setApprovalInfo(List<RecordApprovalInfo> approvalInfo) {
        this.approvalInfo = approvalInfo;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(RecordRequestedItemsStatusRef.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("id");
        sb.append('=');
        sb.append(((this.id == null)?"<null>":this.id));
        sb.append(',');
        sb.append("name");
        sb.append('=');
        sb.append(((this.name == null)?"<null>":this.name));
        sb.append(',');
        sb.append("description");
        sb.append('=');
        sb.append(((this.description == null)?"<null>":this.description));
        sb.append(',');
        sb.append("type");
        sb.append('=');
        sb.append(((this.type == null)?"<null>":this.type));
        sb.append(',');
        sb.append("operation");
        sb.append('=');
        sb.append(((this.operation == null)?"<null>":this.operation));
        sb.append(',');
        sb.append("comment");
        sb.append('=');
        sb.append(((this.comment == null)?"<null>":this.comment));
        sb.append(',');
        sb.append("clientMetadata");
        sb.append('=');
        sb.append(((this.clientMetadata == null)?"<null>":this.clientMetadata));
        sb.append(',');
        sb.append("approvalInfo");
        sb.append('=');
        sb.append(((this.approvalInfo == null)?"<null>":this.approvalInfo));
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
        result = ((result* 31)+((this.clientMetadata == null)? 0 :this.clientMetadata.hashCode()));
        result = ((result* 31)+((this.approvalInfo == null)? 0 :this.approvalInfo.hashCode()));
        result = ((result* 31)+((this.name == null)? 0 :this.name.hashCode()));
        result = ((result* 31)+((this.description == null)? 0 :this.description.hashCode()));
        result = ((result* 31)+((this.comment == null)? 0 :this.comment.hashCode()));
        result = ((result* 31)+((this.id == null)? 0 :this.id.hashCode()));
        result = ((result* 31)+((this.type == null)? 0 :this.type.hashCode()));
        result = ((result* 31)+((this.operation == null)? 0 :this.operation.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof RecordRequestedItemsStatusRef) == false) {
            return false;
        }
        RecordRequestedItemsStatusRef rhs = ((RecordRequestedItemsStatusRef) other);
        return (((((((((this.clientMetadata == rhs.clientMetadata)||((this.clientMetadata!= null)&&this.clientMetadata.equals(rhs.clientMetadata)))&&((this.approvalInfo == rhs.approvalInfo)||((this.approvalInfo!= null)&&this.approvalInfo.equals(rhs.approvalInfo))))&&((this.name == rhs.name)||((this.name!= null)&&this.name.equals(rhs.name))))&&((this.description == rhs.description)||((this.description!= null)&&this.description.equals(rhs.description))))&&((this.comment == rhs.comment)||((this.comment!= null)&&this.comment.equals(rhs.comment))))&&((this.id == rhs.id)||((this.id!= null)&&this.id.equals(rhs.id))))&&((this.type == rhs.type)||((this.type!= null)&&this.type.equals(rhs.type))))&&((this.operation == rhs.operation)||((this.operation!= null)&&this.operation.equals(rhs.operation))));
    }

}
