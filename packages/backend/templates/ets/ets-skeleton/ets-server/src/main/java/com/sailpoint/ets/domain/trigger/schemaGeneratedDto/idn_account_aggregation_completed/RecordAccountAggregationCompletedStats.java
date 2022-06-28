/*
* Copyright (C) 2021 SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.ets.domain.trigger.schemaGeneratedDto.idn_account_aggregation_completed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonPropertyOrder({
    "scanned",
    "unchanged",
    "changed",
    "added",
    "removed"
})
@JsonIgnoreProperties(ignoreUnknown = true)
public class RecordAccountAggregationCompletedStats {

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("scanned")
    private Double scanned;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("unchanged")
    private Double unchanged;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("changed")
    private Double changed;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("added")
    private Double added;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("removed")
    private Double removed;

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("scanned")
    public Double getScanned() {
        return scanned;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("scanned")
    public void setScanned(Double scanned) {
        this.scanned = scanned;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("unchanged")
    public Double getUnchanged() {
        return unchanged;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("unchanged")
    public void setUnchanged(Double unchanged) {
        this.unchanged = unchanged;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("changed")
    public Double getChanged() {
        return changed;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("changed")
    public void setChanged(Double changed) {
        this.changed = changed;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("added")
    public Double getAdded() {
        return added;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("added")
    public void setAdded(Double added) {
        this.added = added;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("removed")
    public Double getRemoved() {
        return removed;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("removed")
    public void setRemoved(Double removed) {
        this.removed = removed;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(RecordAccountAggregationCompletedStats.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("scanned");
        sb.append('=');
        sb.append(((this.scanned == null)?"<null>":this.scanned));
        sb.append(',');
        sb.append("unchanged");
        sb.append('=');
        sb.append(((this.unchanged == null)?"<null>":this.unchanged));
        sb.append(',');
        sb.append("changed");
        sb.append('=');
        sb.append(((this.changed == null)?"<null>":this.changed));
        sb.append(',');
        sb.append("added");
        sb.append('=');
        sb.append(((this.added == null)?"<null>":this.added));
        sb.append(',');
        sb.append("removed");
        sb.append('=');
        sb.append(((this.removed == null)?"<null>":this.removed));
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
        result = ((result* 31)+((this.unchanged == null)? 0 :this.unchanged.hashCode()));
        result = ((result* 31)+((this.removed == null)? 0 :this.removed.hashCode()));
        result = ((result* 31)+((this.added == null)? 0 :this.added.hashCode()));
        result = ((result* 31)+((this.scanned == null)? 0 :this.scanned.hashCode()));
        result = ((result* 31)+((this.changed == null)? 0 :this.changed.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof RecordAccountAggregationCompletedStats) == false) {
            return false;
        }
        RecordAccountAggregationCompletedStats rhs = ((RecordAccountAggregationCompletedStats) other);
        return ((((((this.unchanged == rhs.unchanged)||((this.unchanged!= null)&&this.unchanged.equals(rhs.unchanged)))&&((this.removed == rhs.removed)||((this.removed!= null)&&this.removed.equals(rhs.removed))))&&((this.added == rhs.added)||((this.added!= null)&&this.added.equals(rhs.added))))&&((this.scanned == rhs.scanned)||((this.scanned!= null)&&this.scanned.equals(rhs.scanned))))&&((this.changed == rhs.changed)||((this.changed!= null)&&this.changed.equals(rhs.changed))));
    }

}
