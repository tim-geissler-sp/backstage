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
    "name",
    "created",
    "modified",
    "campaignRef",
    "completed",
    "decisionsMade",
    "decisionsTotal",
    "due",
    "signed",
    "reviewer",
    "campaignOwner",
    "reassignment",
    "hasErrors",
    "errorMessage",
    "phase",
    "entitiesCompleted",
    "entitiesTotal"
})
@JsonIgnoreProperties(ignoreUnknown = true)
public class RecordCertification {

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
    @JsonProperty("created")
    private String created;
    @JsonProperty("modified")
    private Object modified = null;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("campaignRef")
    private RecordCampaignRef campaignRef;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("completed")
    private Boolean completed;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("decisionsMade")
    private Double decisionsMade;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("decisionsTotal")
    private Double decisionsTotal;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("due")
    private String due;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("signed")
    private String signed;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("reviewer")
    private RecordReviewer reviewer;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("campaignOwner")
    private RecordCampaignOwner campaignOwner;
    @JsonProperty("reassignment")
    private Object reassignment = null;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("hasErrors")
    private Boolean hasErrors;
    @JsonProperty("errorMessage")
    private Object errorMessage = null;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("phase")
    private RecordCertification.EnumCertificationPhase phase;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("entitiesCompleted")
    private Double entitiesCompleted;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("entitiesTotal")
    private Double entitiesTotal;

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
    @JsonProperty("created")
    public String getCreated() {
        return created;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("created")
    public void setCreated(String created) {
        this.created = created;
    }

    @JsonProperty("modified")
    public Object getModified() {
        return modified;
    }

    @JsonProperty("modified")
    public void setModified(Object modified) {
        this.modified = modified;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("campaignRef")
    public RecordCampaignRef getCampaignRef() {
        return campaignRef;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("campaignRef")
    public void setCampaignRef(RecordCampaignRef campaignRef) {
        this.campaignRef = campaignRef;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("completed")
    public Boolean getCompleted() {
        return completed;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("completed")
    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("decisionsMade")
    public Double getDecisionsMade() {
        return decisionsMade;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("decisionsMade")
    public void setDecisionsMade(Double decisionsMade) {
        this.decisionsMade = decisionsMade;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("decisionsTotal")
    public Double getDecisionsTotal() {
        return decisionsTotal;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("decisionsTotal")
    public void setDecisionsTotal(Double decisionsTotal) {
        this.decisionsTotal = decisionsTotal;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("due")
    public String getDue() {
        return due;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("due")
    public void setDue(String due) {
        this.due = due;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("signed")
    public String getSigned() {
        return signed;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("signed")
    public void setSigned(String signed) {
        this.signed = signed;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("reviewer")
    public RecordReviewer getReviewer() {
        return reviewer;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("reviewer")
    public void setReviewer(RecordReviewer reviewer) {
        this.reviewer = reviewer;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("campaignOwner")
    public RecordCampaignOwner getCampaignOwner() {
        return campaignOwner;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("campaignOwner")
    public void setCampaignOwner(RecordCampaignOwner campaignOwner) {
        this.campaignOwner = campaignOwner;
    }

    @JsonProperty("reassignment")
    public Object getReassignment() {
        return reassignment;
    }

    @JsonProperty("reassignment")
    public void setReassignment(Object reassignment) {
        this.reassignment = reassignment;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("hasErrors")
    public Boolean getHasErrors() {
        return hasErrors;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("hasErrors")
    public void setHasErrors(Boolean hasErrors) {
        this.hasErrors = hasErrors;
    }

    @JsonProperty("errorMessage")
    public Object getErrorMessage() {
        return errorMessage;
    }

    @JsonProperty("errorMessage")
    public void setErrorMessage(Object errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("phase")
    public RecordCertification.EnumCertificationPhase getPhase() {
        return phase;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("phase")
    public void setPhase(RecordCertification.EnumCertificationPhase phase) {
        this.phase = phase;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("entitiesCompleted")
    public Double getEntitiesCompleted() {
        return entitiesCompleted;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("entitiesCompleted")
    public void setEntitiesCompleted(Double entitiesCompleted) {
        this.entitiesCompleted = entitiesCompleted;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("entitiesTotal")
    public Double getEntitiesTotal() {
        return entitiesTotal;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("entitiesTotal")
    public void setEntitiesTotal(Double entitiesTotal) {
        this.entitiesTotal = entitiesTotal;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(RecordCertification.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("id");
        sb.append('=');
        sb.append(((this.id == null)?"<null>":this.id));
        sb.append(',');
        sb.append("name");
        sb.append('=');
        sb.append(((this.name == null)?"<null>":this.name));
        sb.append(',');
        sb.append("created");
        sb.append('=');
        sb.append(((this.created == null)?"<null>":this.created));
        sb.append(',');
        sb.append("modified");
        sb.append('=');
        sb.append(((this.modified == null)?"<null>":this.modified));
        sb.append(',');
        sb.append("campaignRef");
        sb.append('=');
        sb.append(((this.campaignRef == null)?"<null>":this.campaignRef));
        sb.append(',');
        sb.append("completed");
        sb.append('=');
        sb.append(((this.completed == null)?"<null>":this.completed));
        sb.append(',');
        sb.append("decisionsMade");
        sb.append('=');
        sb.append(((this.decisionsMade == null)?"<null>":this.decisionsMade));
        sb.append(',');
        sb.append("decisionsTotal");
        sb.append('=');
        sb.append(((this.decisionsTotal == null)?"<null>":this.decisionsTotal));
        sb.append(',');
        sb.append("due");
        sb.append('=');
        sb.append(((this.due == null)?"<null>":this.due));
        sb.append(',');
        sb.append("signed");
        sb.append('=');
        sb.append(((this.signed == null)?"<null>":this.signed));
        sb.append(',');
        sb.append("reviewer");
        sb.append('=');
        sb.append(((this.reviewer == null)?"<null>":this.reviewer));
        sb.append(',');
        sb.append("campaignOwner");
        sb.append('=');
        sb.append(((this.campaignOwner == null)?"<null>":this.campaignOwner));
        sb.append(',');
        sb.append("reassignment");
        sb.append('=');
        sb.append(((this.reassignment == null)?"<null>":this.reassignment));
        sb.append(',');
        sb.append("hasErrors");
        sb.append('=');
        sb.append(((this.hasErrors == null)?"<null>":this.hasErrors));
        sb.append(',');
        sb.append("errorMessage");
        sb.append('=');
        sb.append(((this.errorMessage == null)?"<null>":this.errorMessage));
        sb.append(',');
        sb.append("phase");
        sb.append('=');
        sb.append(((this.phase == null)?"<null>":this.phase));
        sb.append(',');
        sb.append("entitiesCompleted");
        sb.append('=');
        sb.append(((this.entitiesCompleted == null)?"<null>":this.entitiesCompleted));
        sb.append(',');
        sb.append("entitiesTotal");
        sb.append('=');
        sb.append(((this.entitiesTotal == null)?"<null>":this.entitiesTotal));
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
        result = ((result* 31)+((this.phase == null)? 0 :this.phase.hashCode()));
        result = ((result* 31)+((this.reassignment == null)? 0 :this.reassignment.hashCode()));
        result = ((result* 31)+((this.hasErrors == null)? 0 :this.hasErrors.hashCode()));
        result = ((result* 31)+((this.created == null)? 0 :this.created.hashCode()));
        result = ((result* 31)+((this.decisionsTotal == null)? 0 :this.decisionsTotal.hashCode()));
        result = ((result* 31)+((this.errorMessage == null)? 0 :this.errorMessage.hashCode()));
        result = ((result* 31)+((this.entitiesTotal == null)? 0 :this.entitiesTotal.hashCode()));
        result = ((result* 31)+((this.signed == null)? 0 :this.signed.hashCode()));
        result = ((result* 31)+((this.completed == null)? 0 :this.completed.hashCode()));
        result = ((result* 31)+((this.reviewer == null)? 0 :this.reviewer.hashCode()));
        result = ((result* 31)+((this.campaignOwner == null)? 0 :this.campaignOwner.hashCode()));
        result = ((result* 31)+((this.campaignRef == null)? 0 :this.campaignRef.hashCode()));
        result = ((result* 31)+((this.due == null)? 0 :this.due.hashCode()));
        result = ((result* 31)+((this.entitiesCompleted == null)? 0 :this.entitiesCompleted.hashCode()));
        result = ((result* 31)+((this.name == null)? 0 :this.name.hashCode()));
        result = ((result* 31)+((this.modified == null)? 0 :this.modified.hashCode()));
        result = ((result* 31)+((this.id == null)? 0 :this.id.hashCode()));
        result = ((result* 31)+((this.decisionsMade == null)? 0 :this.decisionsMade.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof RecordCertification) == false) {
            return false;
        }
        RecordCertification rhs = ((RecordCertification) other);
        return (((((((((((((((((((this.phase == rhs.phase)||((this.phase!= null)&&this.phase.equals(rhs.phase)))&&((this.reassignment == rhs.reassignment)||((this.reassignment!= null)&&this.reassignment.equals(rhs.reassignment))))&&((this.hasErrors == rhs.hasErrors)||((this.hasErrors!= null)&&this.hasErrors.equals(rhs.hasErrors))))&&((this.created == rhs.created)||((this.created!= null)&&this.created.equals(rhs.created))))&&((this.decisionsTotal == rhs.decisionsTotal)||((this.decisionsTotal!= null)&&this.decisionsTotal.equals(rhs.decisionsTotal))))&&((this.errorMessage == rhs.errorMessage)||((this.errorMessage!= null)&&this.errorMessage.equals(rhs.errorMessage))))&&((this.entitiesTotal == rhs.entitiesTotal)||((this.entitiesTotal!= null)&&this.entitiesTotal.equals(rhs.entitiesTotal))))&&((this.signed == rhs.signed)||((this.signed!= null)&&this.signed.equals(rhs.signed))))&&((this.completed == rhs.completed)||((this.completed!= null)&&this.completed.equals(rhs.completed))))&&((this.reviewer == rhs.reviewer)||((this.reviewer!= null)&&this.reviewer.equals(rhs.reviewer))))&&((this.campaignOwner == rhs.campaignOwner)||((this.campaignOwner!= null)&&this.campaignOwner.equals(rhs.campaignOwner))))&&((this.campaignRef == rhs.campaignRef)||((this.campaignRef!= null)&&this.campaignRef.equals(rhs.campaignRef))))&&((this.due == rhs.due)||((this.due!= null)&&this.due.equals(rhs.due))))&&((this.entitiesCompleted == rhs.entitiesCompleted)||((this.entitiesCompleted!= null)&&this.entitiesCompleted.equals(rhs.entitiesCompleted))))&&((this.name == rhs.name)||((this.name!= null)&&this.name.equals(rhs.name))))&&((this.modified == rhs.modified)||((this.modified!= null)&&this.modified.equals(rhs.modified))))&&((this.id == rhs.id)||((this.id!= null)&&this.id.equals(rhs.id))))&&((this.decisionsMade == rhs.decisionsMade)||((this.decisionsMade!= null)&&this.decisionsMade.equals(rhs.decisionsMade))));
    }

@JsonIgnoreProperties(ignoreUnknown = true)
    public enum EnumCertificationPhase {

        SIGNED("SIGNED");
        private final String value;
        private final static Map<String, RecordCertification.EnumCertificationPhase> CONSTANTS = new HashMap<String, RecordCertification.EnumCertificationPhase>();

        static {
            for (RecordCertification.EnumCertificationPhase c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        EnumCertificationPhase(String value) {
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
        public static RecordCertification.EnumCertificationPhase fromValue(String value) {
            RecordCertification.EnumCertificationPhase constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
