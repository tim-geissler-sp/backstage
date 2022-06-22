/*
* Copyright (C) 2021 SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.ets.domain.trigger.schemaGeneratedDto.idn_identity_attributes_changed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonPropertyOrder({
    "attribute",
    "oldValue",
    "newValue"
})
@JsonIgnoreProperties(ignoreUnknown = true)
public class RecordAttributeChange {

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("attribute")
    private String attribute;
    @JsonProperty("oldValue")
    private Object oldValue = null;
    @JsonProperty("newValue")
    private Object newValue = null;

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("attribute")
    public String getAttribute() {
        return attribute;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("attribute")
    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }

    @JsonProperty("oldValue")
    public Object getOldValue() {
        return oldValue;
    }

    @JsonProperty("oldValue")
    public void setOldValue(Object oldValue) {
        this.oldValue = oldValue;
    }

    @JsonProperty("newValue")
    public Object getNewValue() {
        return newValue;
    }

    @JsonProperty("newValue")
    public void setNewValue(Object newValue) {
        this.newValue = newValue;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(RecordAttributeChange.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("attribute");
        sb.append('=');
        sb.append(((this.attribute == null)?"<null>":this.attribute));
        sb.append(',');
        sb.append("oldValue");
        sb.append('=');
        sb.append(((this.oldValue == null)?"<null>":this.oldValue));
        sb.append(',');
        sb.append("newValue");
        sb.append('=');
        sb.append(((this.newValue == null)?"<null>":this.newValue));
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
        result = ((result* 31)+((this.newValue == null)? 0 :this.newValue.hashCode()));
        result = ((result* 31)+((this.attribute == null)? 0 :this.attribute.hashCode()));
        result = ((result* 31)+((this.oldValue == null)? 0 :this.oldValue.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof RecordAttributeChange) == false) {
            return false;
        }
        RecordAttributeChange rhs = ((RecordAttributeChange) other);
        return ((((this.newValue == rhs.newValue)||((this.newValue!= null)&&this.newValue.equals(rhs.newValue)))&&((this.attribute == rhs.attribute)||((this.attribute!= null)&&this.attribute.equals(rhs.attribute))))&&((this.oldValue == rhs.oldValue)||((this.oldValue!= null)&&this.oldValue.equals(rhs.oldValue))));
    }

}
