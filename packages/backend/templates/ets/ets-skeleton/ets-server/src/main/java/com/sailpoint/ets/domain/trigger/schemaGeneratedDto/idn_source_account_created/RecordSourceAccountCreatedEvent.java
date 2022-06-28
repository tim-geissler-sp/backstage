/*
* Copyright (C) 2021 SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.ets.domain.trigger.schemaGeneratedDto.idn_source_account_created;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonPropertyOrder({
    "uuid",
    "id",
    "nativeIdentifier",
    "sourceId",
    "sourceName",
    "identityId",
    "identityName",
    "attributes"
})
@JsonIgnoreProperties(ignoreUnknown = true)
public class RecordSourceAccountCreatedEvent {

    @JsonProperty("uuid")
    private String uuid;
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
    @JsonProperty("nativeIdentifier")
    private String nativeIdentifier;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("sourceId")
    private String sourceId;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("sourceName")
    private String sourceName;
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
    @JsonProperty("identityName")
    private String identityName;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("attributes")
    private Object attributes = null;

    @JsonProperty("uuid")
    public String getUuid() {
        return uuid;
    }

    @JsonProperty("uuid")
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

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
    @JsonProperty("nativeIdentifier")
    public String getNativeIdentifier() {
        return nativeIdentifier;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("nativeIdentifier")
    public void setNativeIdentifier(String nativeIdentifier) {
        this.nativeIdentifier = nativeIdentifier;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("sourceId")
    public String getSourceId() {
        return sourceId;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("sourceId")
    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("sourceName")
    public String getSourceName() {
        return sourceName;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("sourceName")
    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

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

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("identityName")
    public String getIdentityName() {
        return identityName;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("identityName")
    public void setIdentityName(String identityName) {
        this.identityName = identityName;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("attributes")
    public Object getAttributes() {
        return attributes;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("attributes")
    public void setAttributes(Object attributes) {
        this.attributes = attributes;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(RecordSourceAccountCreatedEvent.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("uuid");
        sb.append('=');
        sb.append(((this.uuid == null)?"<null>":this.uuid));
        sb.append(',');
        sb.append("id");
        sb.append('=');
        sb.append(((this.id == null)?"<null>":this.id));
        sb.append(',');
        sb.append("nativeIdentifier");
        sb.append('=');
        sb.append(((this.nativeIdentifier == null)?"<null>":this.nativeIdentifier));
        sb.append(',');
        sb.append("sourceId");
        sb.append('=');
        sb.append(((this.sourceId == null)?"<null>":this.sourceId));
        sb.append(',');
        sb.append("sourceName");
        sb.append('=');
        sb.append(((this.sourceName == null)?"<null>":this.sourceName));
        sb.append(',');
        sb.append("identityId");
        sb.append('=');
        sb.append(((this.identityId == null)?"<null>":this.identityId));
        sb.append(',');
        sb.append("identityName");
        sb.append('=');
        sb.append(((this.identityName == null)?"<null>":this.identityName));
        sb.append(',');
        sb.append("attributes");
        sb.append('=');
        sb.append(((this.attributes == null)?"<null>":this.attributes));
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
        result = ((result* 31)+((this.sourceId == null)? 0 :this.sourceId.hashCode()));
        result = ((result* 31)+((this.identityName == null)? 0 :this.identityName.hashCode()));
        result = ((result* 31)+((this.nativeIdentifier == null)? 0 :this.nativeIdentifier.hashCode()));
        result = ((result* 31)+((this.identityId == null)? 0 :this.identityId.hashCode()));
        result = ((result* 31)+((this.attributes == null)? 0 :this.attributes.hashCode()));
        result = ((result* 31)+((this.id == null)? 0 :this.id.hashCode()));
        result = ((result* 31)+((this.sourceName == null)? 0 :this.sourceName.hashCode()));
        result = ((result* 31)+((this.uuid == null)? 0 :this.uuid.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof RecordSourceAccountCreatedEvent) == false) {
            return false;
        }
        RecordSourceAccountCreatedEvent rhs = ((RecordSourceAccountCreatedEvent) other);
        return (((((((((this.sourceId == rhs.sourceId)||((this.sourceId!= null)&&this.sourceId.equals(rhs.sourceId)))&&((this.identityName == rhs.identityName)||((this.identityName!= null)&&this.identityName.equals(rhs.identityName))))&&((this.nativeIdentifier == rhs.nativeIdentifier)||((this.nativeIdentifier!= null)&&this.nativeIdentifier.equals(rhs.nativeIdentifier))))&&((this.identityId == rhs.identityId)||((this.identityId!= null)&&this.identityId.equals(rhs.identityId))))&&((this.attributes == rhs.attributes)||((this.attributes!= null)&&this.attributes.equals(rhs.attributes))))&&((this.id == rhs.id)||((this.id!= null)&&this.id.equals(rhs.id))))&&((this.sourceName == rhs.sourceName)||((this.sourceName!= null)&&this.sourceName.equals(rhs.sourceName))))&&((this.uuid == rhs.uuid)||((this.uuid!= null)&&this.uuid.equals(rhs.uuid))));
    }

}
