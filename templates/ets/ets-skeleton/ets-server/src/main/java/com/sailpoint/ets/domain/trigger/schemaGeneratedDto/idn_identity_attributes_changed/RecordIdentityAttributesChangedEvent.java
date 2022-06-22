/*
* Copyright (C) 2021 SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.ets.domain.trigger.schemaGeneratedDto.idn_identity_attributes_changed;

import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonPropertyOrder({
    "identity",
    "changes"
})
@JsonIgnoreProperties(ignoreUnknown = true)
public class RecordIdentityAttributesChangedEvent {

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("identity")
    private RecordIdentityReference identity;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("changes")
    private List<RecordAttributeChange> changes = new ArrayList<RecordAttributeChange>();

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("identity")
    public RecordIdentityReference getIdentity() {
        return identity;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("identity")
    public void setIdentity(RecordIdentityReference identity) {
        this.identity = identity;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("changes")
    public List<RecordAttributeChange> getChanges() {
        return changes;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("changes")
    public void setChanges(List<RecordAttributeChange> changes) {
        this.changes = changes;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(RecordIdentityAttributesChangedEvent.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("identity");
        sb.append('=');
        sb.append(((this.identity == null)?"<null>":this.identity));
        sb.append(',');
        sb.append("changes");
        sb.append('=');
        sb.append(((this.changes == null)?"<null>":this.changes));
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
        result = ((result* 31)+((this.changes == null)? 0 :this.changes.hashCode()));
        result = ((result* 31)+((this.identity == null)? 0 :this.identity.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof RecordIdentityAttributesChangedEvent) == false) {
            return false;
        }
        RecordIdentityAttributesChangedEvent rhs = ((RecordIdentityAttributesChangedEvent) other);
        return (((this.changes == rhs.changes)||((this.changes!= null)&&this.changes.equals(rhs.changes)))&&((this.identity == rhs.identity)||((this.identity!= null)&&this.identity.equals(rhs.identity))));
    }

}
