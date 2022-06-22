/*
* Copyright (C) 2021 SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.ets.domain.trigger.schemaGeneratedDto.idn_campaign_activated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonPropertyOrder({
    "campaign"
})
@JsonIgnoreProperties(ignoreUnknown = true)
public class RecordCampaignActivatedPayload {

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("campaign")
    private RecordCampaign campaign;

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("campaign")
    public RecordCampaign getCampaign() {
        return campaign;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("campaign")
    public void setCampaign(RecordCampaign campaign) {
        this.campaign = campaign;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(RecordCampaignActivatedPayload.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("campaign");
        sb.append('=');
        sb.append(((this.campaign == null)?"<null>":this.campaign));
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
        result = ((result* 31)+((this.campaign == null)? 0 :this.campaign.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof RecordCampaignActivatedPayload) == false) {
            return false;
        }
        RecordCampaignActivatedPayload rhs = ((RecordCampaignActivatedPayload) other);
        return ((this.campaign == rhs.campaign)||((this.campaign!= null)&&this.campaign.equals(rhs.campaign)));
    }

}
