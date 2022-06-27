/*
* Copyright (C) 2021 SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.ets.domain.trigger.schemaGeneratedDto.idn_campaign_generated;

import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonValue;

@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonPropertyOrder({
    "id",
    "name",
    "description",
    "created",
    "modified",
    "deadline",
    "type",
    "campaignOwner",
    "status"
})
@JsonIgnoreProperties(ignoreUnknown = true)
public class RecordCampaign {

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
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("description")
    private String description;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("created")
    private String created;
    @JsonProperty("modified")
    private Object modified = null;
    @JsonProperty("deadline")
    private Object deadline = null;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("type")
    private RecordCampaign.EnumType type;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("campaignOwner")
    private RecordCampaignOwner campaignOwner;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("status")
    private RecordCampaign.EnumGeneratedCampaignStatuses status;

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

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("description")
    public String getDescription() {
        return description;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("description")
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("created")
    public String getCreated() {
        return created;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("created")
    public void setCreated(String created) {
        this.created = created;
    }

    @JsonProperty("modified")
    public Object getModified() {
        return modified;
    }

    @JsonProperty("modified")
    public void setModified(Object modified) {
        this.modified = modified;
    }

    @JsonProperty("deadline")
    public Object getDeadline() {
        return deadline;
    }

    @JsonProperty("deadline")
    public void setDeadline(Object deadline) {
        this.deadline = deadline;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("type")
    public RecordCampaign.EnumType getType() {
        return type;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("type")
    public void setType(RecordCampaign.EnumType type) {
        this.type = type;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("campaignOwner")
    public RecordCampaignOwner getCampaignOwner() {
        return campaignOwner;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("campaignOwner")
    public void setCampaignOwner(RecordCampaignOwner campaignOwner) {
        this.campaignOwner = campaignOwner;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("status")
    public RecordCampaign.EnumGeneratedCampaignStatuses getStatus() {
        return status;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("status")
    public void setStatus(RecordCampaign.EnumGeneratedCampaignStatuses status) {
        this.status = status;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(RecordCampaign.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
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
        sb.append("created");
        sb.append('=');
        sb.append(((this.created == null)?"<null>":this.created));
        sb.append(',');
        sb.append("modified");
        sb.append('=');
        sb.append(((this.modified == null)?"<null>":this.modified));
        sb.append(',');
        sb.append("deadline");
        sb.append('=');
        sb.append(((this.deadline == null)?"<null>":this.deadline));
        sb.append(',');
        sb.append("type");
        sb.append('=');
        sb.append(((this.type == null)?"<null>":this.type));
        sb.append(',');
        sb.append("campaignOwner");
        sb.append('=');
        sb.append(((this.campaignOwner == null)?"<null>":this.campaignOwner));
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
        result = ((result* 31)+((this.created == null)? 0 :this.created.hashCode()));
        result = ((result* 31)+((this.name == null)? 0 :this.name.hashCode()));
        result = ((result* 31)+((this.description == null)? 0 :this.description.hashCode()));
        result = ((result* 31)+((this.modified == null)? 0 :this.modified.hashCode()));
        result = ((result* 31)+((this.id == null)? 0 :this.id.hashCode()));
        result = ((result* 31)+((this.deadline == null)? 0 :this.deadline.hashCode()));
        result = ((result* 31)+((this.type == null)? 0 :this.type.hashCode()));
        result = ((result* 31)+((this.campaignOwner == null)? 0 :this.campaignOwner.hashCode()));
        result = ((result* 31)+((this.status == null)? 0 :this.status.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof RecordCampaign) == false) {
            return false;
        }
        RecordCampaign rhs = ((RecordCampaign) other);
        return ((((((((((this.created == rhs.created)||((this.created!= null)&&this.created.equals(rhs.created)))&&((this.name == rhs.name)||((this.name!= null)&&this.name.equals(rhs.name))))&&((this.description == rhs.description)||((this.description!= null)&&this.description.equals(rhs.description))))&&((this.modified == rhs.modified)||((this.modified!= null)&&this.modified.equals(rhs.modified))))&&((this.id == rhs.id)||((this.id!= null)&&this.id.equals(rhs.id))))&&((this.deadline == rhs.deadline)||((this.deadline!= null)&&this.deadline.equals(rhs.deadline))))&&((this.type == rhs.type)||((this.type!= null)&&this.type.equals(rhs.type))))&&((this.campaignOwner == rhs.campaignOwner)||((this.campaignOwner!= null)&&this.campaignOwner.equals(rhs.campaignOwner))))&&((this.status == rhs.status)||((this.status!= null)&&this.status.equals(rhs.status))));
    }

@JsonIgnoreProperties(ignoreUnknown = true)
    public enum EnumGeneratedCampaignStatuses {

        STAGED("STAGED"),
        ACTIVATING("ACTIVATING"),
        ACTIVE("ACTIVE");
        private final String value;
        private final static Map<String, RecordCampaign.EnumGeneratedCampaignStatuses> CONSTANTS = new HashMap<String, RecordCampaign.EnumGeneratedCampaignStatuses>();

        static {
            for (RecordCampaign.EnumGeneratedCampaignStatuses c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        EnumGeneratedCampaignStatuses(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        @JsonValue
        public String value() {
            return this.value;
        }

        @JsonCreator
        public static RecordCampaign.EnumGeneratedCampaignStatuses fromValue(String value) {
            RecordCampaign.EnumGeneratedCampaignStatuses constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

@JsonIgnoreProperties(ignoreUnknown = true)
    public enum EnumType {

        MANAGER("MANAGER"),
        SOURCE_OWNER("SOURCE_OWNER"),
        SEARCH("SEARCH"),
        ROLE_COMPOSITION("ROLE_COMPOSITION");
        private final String value;
        private final static Map<String, RecordCampaign.EnumType> CONSTANTS = new HashMap<String, RecordCampaign.EnumType>();

        static {
            for (RecordCampaign.EnumType c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        EnumType(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        @JsonValue
        public String value() {
            return this.value;
        }

        @JsonCreator
        public static RecordCampaign.EnumType fromValue(String value) {
            RecordCampaign.EnumType constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
