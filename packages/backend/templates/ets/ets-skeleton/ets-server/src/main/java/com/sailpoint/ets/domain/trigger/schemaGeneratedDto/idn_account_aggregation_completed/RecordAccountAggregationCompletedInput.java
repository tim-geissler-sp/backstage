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
    "source",
    "status",
    "started",
    "completed",
    "errors",
    "warnings",
    "stats"
})
@JsonIgnoreProperties(ignoreUnknown = true)
public class RecordAccountAggregationCompletedInput {

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("source")
    private RecordSourceRef source;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("status")
    private String status;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("started")
    private String started;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("completed")
    private String completed;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("errors")
    private Object errors;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("warnings")
    private Object warnings;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("stats")
    private RecordAccountAggregationCompletedStats stats;

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("source")
    public RecordSourceRef getSource() {
        return source;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("source")
    public void setSource(RecordSourceRef source) {
        this.source = source;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("status")
    public String getStatus() {
        return status;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("status")
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("started")
    public String getStarted() {
        return started;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("started")
    public void setStarted(String started) {
        this.started = started;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("completed")
    public String getCompleted() {
        return completed;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("completed")
    public void setCompleted(String completed) {
        this.completed = completed;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("errors")
    public Object getErrors() {
        return errors;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("errors")
    public void setErrors(Object errors) {
        this.errors = errors;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("warnings")
    public Object getWarnings() {
        return warnings;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("warnings")
    public void setWarnings(Object warnings) {
        this.warnings = warnings;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("stats")
    public RecordAccountAggregationCompletedStats getStats() {
        return stats;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("stats")
    public void setStats(RecordAccountAggregationCompletedStats stats) {
        this.stats = stats;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(RecordAccountAggregationCompletedInput.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("source");
        sb.append('=');
        sb.append(((this.source == null)?"<null>":this.source));
        sb.append(',');
        sb.append("status");
        sb.append('=');
        sb.append(((this.status == null)?"<null>":this.status));
        sb.append(',');
        sb.append("started");
        sb.append('=');
        sb.append(((this.started == null)?"<null>":this.started));
        sb.append(',');
        sb.append("completed");
        sb.append('=');
        sb.append(((this.completed == null)?"<null>":this.completed));
        sb.append(',');
        sb.append("errors");
        sb.append('=');
        sb.append(((this.errors == null)?"<null>":this.errors));
        sb.append(',');
        sb.append("warnings");
        sb.append('=');
        sb.append(((this.warnings == null)?"<null>":this.warnings));
        sb.append(',');
        sb.append("stats");
        sb.append('=');
        sb.append(((this.stats == null)?"<null>":this.stats));
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
        result = ((result* 31)+((this.stats == null)? 0 :this.stats.hashCode()));
        result = ((result* 31)+((this.warnings == null)? 0 :this.warnings.hashCode()));
        result = ((result* 31)+((this.started == null)? 0 :this.started.hashCode()));
        result = ((result* 31)+((this.source == null)? 0 :this.source.hashCode()));
        result = ((result* 31)+((this.completed == null)? 0 :this.completed.hashCode()));
        result = ((result* 31)+((this.errors == null)? 0 :this.errors.hashCode()));
        result = ((result* 31)+((this.status == null)? 0 :this.status.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof RecordAccountAggregationCompletedInput) == false) {
            return false;
        }
        RecordAccountAggregationCompletedInput rhs = ((RecordAccountAggregationCompletedInput) other);
        return ((((((((this.stats == rhs.stats)||((this.stats!= null)&&this.stats.equals(rhs.stats)))&&((this.warnings == rhs.warnings)||((this.warnings!= null)&&this.warnings.equals(rhs.warnings))))&&((this.started == rhs.started)||((this.started!= null)&&this.started.equals(rhs.started))))&&((this.source == rhs.source)||((this.source!= null)&&this.source.equals(rhs.source))))&&((this.completed == rhs.completed)||((this.completed!= null)&&this.completed.equals(rhs.completed))))&&((this.errors == rhs.errors)||((this.errors!= null)&&this.errors.equals(rhs.errors))))&&((this.status == rhs.status)||((this.status!= null)&&this.status.equals(rhs.status))));
    }

}
