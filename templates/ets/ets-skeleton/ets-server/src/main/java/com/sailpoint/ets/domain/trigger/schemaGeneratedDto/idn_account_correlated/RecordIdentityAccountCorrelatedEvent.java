/*
* Copyright (C) 2021 SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.ets.domain.trigger.schemaGeneratedDto.idn_account_correlated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonPropertyOrder({
    "identity",
    "source",
    "account",
    "attributes",
    "entitlementCount"
})
@JsonIgnoreProperties(ignoreUnknown = true)
public class RecordIdentityAccountCorrelatedEvent {

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
    @JsonProperty("source")
    private RecordSourceReference source;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("account")
    private RecordAccountReference account;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("attributes")
    private Object attributes = null;
    @JsonProperty("entitlementCount")
    private Double entitlementCount;

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
    @JsonProperty("source")
    public RecordSourceReference getSource() {
        return source;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("source")
    public void setSource(RecordSourceReference source) {
        this.source = source;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("account")
    public RecordAccountReference getAccount() {
        return account;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("account")
    public void setAccount(RecordAccountReference account) {
        this.account = account;
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

    @JsonProperty("entitlementCount")
    public Double getEntitlementCount() {
        return entitlementCount;
    }

    @JsonProperty("entitlementCount")
    public void setEntitlementCount(Double entitlementCount) {
        this.entitlementCount = entitlementCount;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(RecordIdentityAccountCorrelatedEvent.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("identity");
        sb.append('=');
        sb.append(((this.identity == null)?"<null>":this.identity));
        sb.append(',');
        sb.append("source");
        sb.append('=');
        sb.append(((this.source == null)?"<null>":this.source));
        sb.append(',');
        sb.append("account");
        sb.append('=');
        sb.append(((this.account == null)?"<null>":this.account));
        sb.append(',');
        sb.append("attributes");
        sb.append('=');
        sb.append(((this.attributes == null)?"<null>":this.attributes));
        sb.append(',');
        sb.append("entitlementCount");
        sb.append('=');
        sb.append(((this.entitlementCount == null)?"<null>":this.entitlementCount));
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
        result = ((result* 31)+((this.entitlementCount == null)? 0 :this.entitlementCount.hashCode()));
        result = ((result* 31)+((this.attributes == null)? 0 :this.attributes.hashCode()));
        result = ((result* 31)+((this.source == null)? 0 :this.source.hashCode()));
        result = ((result* 31)+((this.identity == null)? 0 :this.identity.hashCode()));
        result = ((result* 31)+((this.account == null)? 0 :this.account.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof RecordIdentityAccountCorrelatedEvent) == false) {
            return false;
        }
        RecordIdentityAccountCorrelatedEvent rhs = ((RecordIdentityAccountCorrelatedEvent) other);
        return ((((((this.entitlementCount == rhs.entitlementCount)||((this.entitlementCount!= null)&&this.entitlementCount.equals(rhs.entitlementCount)))&&((this.attributes == rhs.attributes)||((this.attributes!= null)&&this.attributes.equals(rhs.attributes))))&&((this.source == rhs.source)||((this.source!= null)&&this.source.equals(rhs.source))))&&((this.identity == rhs.identity)||((this.identity!= null)&&this.identity.equals(rhs.identity))))&&((this.account == rhs.account)||((this.account!= null)&&this.account.equals(rhs.account))));
    }

}
