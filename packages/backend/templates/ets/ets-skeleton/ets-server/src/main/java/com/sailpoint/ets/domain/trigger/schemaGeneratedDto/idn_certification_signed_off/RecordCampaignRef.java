/*
* Copyright (C) 2021 SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.ets.domain.trigger.schemaGeneratedDto.idn_certification_signed_off;

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
    "type",
    "name",
    "description",
    "campaignType"
})
@JsonIgnoreProperties(ignoreUnknown = true)
public class RecordCampaignRef {

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
    @JsonProperty("type")
    private RecordCampaignRef.EnumRefType type;
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
    @JsonProperty("campaignType")
    private RecordCampaignRef.EnumCampaignType campaignType;

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
    @JsonProperty("type")
    public RecordCampaignRef.EnumRefType getType() {
        return type;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("type")
    public void setType(RecordCampaignRef.EnumRefType type) {
        this.type = type;
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
    @JsonProperty("campaignType")
    public RecordCampaignRef.EnumCampaignType getCampaignType() {
        return campaignType;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("campaignType")
    public void setCampaignType(RecordCampaignRef.EnumCampaignType campaignType) {
        this.campaignType = campaignType;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(RecordCampaignRef.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("id");
        sb.append('=');
        sb.append(((this.id == null)?"<null>":this.id));
        sb.append(',');
        sb.append("type");
        sb.append('=');
        sb.append(((this.type == null)?"<null>":this.type));
        sb.append(',');
        sb.append("name");
        sb.append('=');
        sb.append(((this.name == null)?"<null>":this.name));
        sb.append(',');
        sb.append("description");
        sb.append('=');
        sb.append(((this.description == null)?"<null>":this.description));
        sb.append(',');
        sb.append("campaignType");
        sb.append('=');
        sb.append(((this.campaignType == null)?"<null>":this.campaignType));
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
        result = ((result* 31)+((this.name == null)? 0 :this.name.hashCode()));
        result = ((result* 31)+((this.description == null)? 0 :this.description.hashCode()));
        result = ((result* 31)+((this.id == null)? 0 :this.id.hashCode()));
        result = ((result* 31)+((this.campaignType == null)? 0 :this.campaignType.hashCode()));
        result = ((result* 31)+((this.type == null)? 0 :this.type.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof RecordCampaignRef) == false) {
            return false;
        }
        RecordCampaignRef rhs = ((RecordCampaignRef) other);
        return ((((((this.name == rhs.name)||((this.name!= null)&&this.name.equals(rhs.name)))&&((this.description == rhs.description)||((this.description!= null)&&this.description.equals(rhs.description))))&&((this.id == rhs.id)||((this.id!= null)&&this.id.equals(rhs.id))))&&((this.campaignType == rhs.campaignType)||((this.campaignType!= null)&&this.campaignType.equals(rhs.campaignType))))&&((this.type == rhs.type)||((this.type!= null)&&this.type.equals(rhs.type))));
    }

@JsonIgnoreProperties(ignoreUnknown = true)
    public enum EnumCampaignType {

        MANAGER("MANAGER"),
        SOURCE_OWNER("SOURCE_OWNER"),
        SEARCH("SEARCH"),
        ROLE_COMPOSITION("ROLE_COMPOSITION");
        private final String value;
        private final static Map<String, RecordCampaignRef.EnumCampaignType> CONSTANTS = new HashMap<String, RecordCampaignRef.EnumCampaignType>();

        static {
            for (RecordCampaignRef.EnumCampaignType c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        EnumCampaignType(String value) {
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
        public static RecordCampaignRef.EnumCampaignType fromValue(String value) {
            RecordCampaignRef.EnumCampaignType constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

@JsonIgnoreProperties(ignoreUnknown = true)
    public enum EnumRefType {

        CAMPAIGN("CAMPAIGN");
        private final String value;
        private final static Map<String, RecordCampaignRef.EnumRefType> CONSTANTS = new HashMap<String, RecordCampaignRef.EnumRefType>();

        static {
            for (RecordCampaignRef.EnumRefType c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        EnumRefType(String value) {
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
        public static RecordCampaignRef.EnumRefType fromValue(String value) {
            RecordCampaignRef.EnumRefType constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
