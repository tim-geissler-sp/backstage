/*
* Copyright (C) 2021 SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.ets.domain.trigger.schemaGeneratedDto.idn_source_deleted;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonPropertyOrder({
    "id",
    "name",
    "type",
    "deleted",
    "connector",
    "actor"
})
@JsonIgnoreProperties(ignoreUnknown = true)
public class RecordSourceDeletedEvent {

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
    @JsonProperty("type")
    private Object type;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("deleted")
    private String deleted;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("connector")
    private Object connector;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("actor")
    private RecordActor actor;

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
    @JsonProperty("type")
    public Object getType() {
        return type;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("type")
    public void setType(Object type) {
        this.type = type;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("deleted")
    public String getDeleted() {
        return deleted;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("deleted")
    public void setDeleted(String deleted) {
        this.deleted = deleted;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("connector")
    public Object getConnector() {
        return connector;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("connector")
    public void setConnector(Object connector) {
        this.connector = connector;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("actor")
    public RecordActor getActor() {
        return actor;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("actor")
    public void setActor(RecordActor actor) {
        this.actor = actor;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(RecordSourceDeletedEvent.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("id");
        sb.append('=');
        sb.append(((this.id == null)?"<null>":this.id));
        sb.append(',');
        sb.append("name");
        sb.append('=');
        sb.append(((this.name == null)?"<null>":this.name));
        sb.append(',');
        sb.append("type");
        sb.append('=');
        sb.append(((this.type == null)?"<null>":this.type));
        sb.append(',');
        sb.append("deleted");
        sb.append('=');
        sb.append(((this.deleted == null)?"<null>":this.deleted));
        sb.append(',');
        sb.append("connector");
        sb.append('=');
        sb.append(((this.connector == null)?"<null>":this.connector));
        sb.append(',');
        sb.append("actor");
        sb.append('=');
        sb.append(((this.actor == null)?"<null>":this.actor));
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
        result = ((result* 31)+((this.actor == null)? 0 :this.actor.hashCode()));
        result = ((result* 31)+((this.deleted == null)? 0 :this.deleted.hashCode()));
        result = ((result* 31)+((this.connector == null)? 0 :this.connector.hashCode()));
        result = ((result* 31)+((this.name == null)? 0 :this.name.hashCode()));
        result = ((result* 31)+((this.id == null)? 0 :this.id.hashCode()));
        result = ((result* 31)+((this.type == null)? 0 :this.type.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof RecordSourceDeletedEvent) == false) {
            return false;
        }
        RecordSourceDeletedEvent rhs = ((RecordSourceDeletedEvent) other);
        return (((((((this.actor == rhs.actor)||((this.actor!= null)&&this.actor.equals(rhs.actor)))&&((this.deleted == rhs.deleted)||((this.deleted!= null)&&this.deleted.equals(rhs.deleted))))&&((this.connector == rhs.connector)||((this.connector!= null)&&this.connector.equals(rhs.connector))))&&((this.name == rhs.name)||((this.name!= null)&&this.name.equals(rhs.name))))&&((this.id == rhs.id)||((this.id!= null)&&this.id.equals(rhs.id))))&&((this.type == rhs.type)||((this.type!= null)&&this.type.equals(rhs.type))));
    }

}
