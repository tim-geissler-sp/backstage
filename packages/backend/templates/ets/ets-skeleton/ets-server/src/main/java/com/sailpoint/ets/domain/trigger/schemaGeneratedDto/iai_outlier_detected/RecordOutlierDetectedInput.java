/*
* Copyright (C) 2021 SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.ets.domain.trigger.schemaGeneratedDto.iai_outlier_detected;

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
    "identity",
    "outlierType"
})
@JsonIgnoreProperties(ignoreUnknown = true)
public class RecordOutlierDetectedInput {

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("identity")
    private RecordOutlierIdentityRef identity;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("outlierType")
    private RecordOutlierDetectedInput.OutlierType outlierType;

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("identity")
    public RecordOutlierIdentityRef getIdentity() {
        return identity;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("identity")
    public void setIdentity(RecordOutlierIdentityRef identity) {
        this.identity = identity;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("outlierType")
    public RecordOutlierDetectedInput.OutlierType getOutlierType() {
        return outlierType;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("outlierType")
    public void setOutlierType(RecordOutlierDetectedInput.OutlierType outlierType) {
        this.outlierType = outlierType;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(RecordOutlierDetectedInput.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("identity");
        sb.append('=');
        sb.append(((this.identity == null)?"<null>":this.identity));
        sb.append(',');
        sb.append("outlierType");
        sb.append('=');
        sb.append(((this.outlierType == null)?"<null>":this.outlierType));
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
        result = ((result* 31)+((this.identity == null)? 0 :this.identity.hashCode()));
        result = ((result* 31)+((this.outlierType == null)? 0 :this.outlierType.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof RecordOutlierDetectedInput) == false) {
            return false;
        }
        RecordOutlierDetectedInput rhs = ((RecordOutlierDetectedInput) other);
        return (((this.identity == rhs.identity)||((this.identity!= null)&&this.identity.equals(rhs.identity)))&&((this.outlierType == rhs.outlierType)||((this.outlierType!= null)&&this.outlierType.equals(rhs.outlierType))));
    }

@JsonIgnoreProperties(ignoreUnknown = true)
    public enum OutlierType {

        LOW_SIMILARITY("LOW_SIMILARITY"),
        STRUCTURAL("STRUCTURAL");
        private final String value;
        private final static Map<String, RecordOutlierDetectedInput.OutlierType> CONSTANTS = new HashMap<String, RecordOutlierDetectedInput.OutlierType>();

        static {
            for (RecordOutlierDetectedInput.OutlierType c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        OutlierType(String value) {
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
        public static RecordOutlierDetectedInput.OutlierType fromValue(String value) {
            RecordOutlierDetectedInput.OutlierType constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
