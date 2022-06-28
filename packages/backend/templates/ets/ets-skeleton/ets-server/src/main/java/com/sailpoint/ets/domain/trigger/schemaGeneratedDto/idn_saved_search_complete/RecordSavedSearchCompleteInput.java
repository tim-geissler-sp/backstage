/*
* Copyright (C) 2021 SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.ets.domain.trigger.schemaGeneratedDto.idn_saved_search_complete;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonPropertyOrder({
    "fileName",
    "ownerEmail",
    "ownerName",
    "query",
    "searchName",
    "searchResults",
    "signedS3Url"
})
@JsonIgnoreProperties(ignoreUnknown = true)
public class RecordSavedSearchCompleteInput {

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("fileName")
    private String fileName;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("ownerEmail")
    private String ownerEmail;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("ownerName")
    private String ownerName;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("query")
    private String query;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("searchName")
    private String searchName;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("searchResults")
    private Object searchResults = null;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("signedS3Url")
    private String signedS3Url;

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("fileName")
    public String getFileName() {
        return fileName;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("fileName")
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("ownerEmail")
    public String getOwnerEmail() {
        return ownerEmail;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("ownerEmail")
    public void setOwnerEmail(String ownerEmail) {
        this.ownerEmail = ownerEmail;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("ownerName")
    public String getOwnerName() {
        return ownerName;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("ownerName")
    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("query")
    public String getQuery() {
        return query;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("query")
    public void setQuery(String query) {
        this.query = query;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("searchName")
    public String getSearchName() {
        return searchName;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("searchName")
    public void setSearchName(String searchName) {
        this.searchName = searchName;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("searchResults")
    public Object getSearchResults() {
        return searchResults;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("searchResults")
    public void setSearchResults(Object searchResults) {
        this.searchResults = searchResults;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("signedS3Url")
    public String getSignedS3Url() {
        return signedS3Url;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("signedS3Url")
    public void setSignedS3Url(String signedS3Url) {
        this.signedS3Url = signedS3Url;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(RecordSavedSearchCompleteInput.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("fileName");
        sb.append('=');
        sb.append(((this.fileName == null)?"<null>":this.fileName));
        sb.append(',');
        sb.append("ownerEmail");
        sb.append('=');
        sb.append(((this.ownerEmail == null)?"<null>":this.ownerEmail));
        sb.append(',');
        sb.append("ownerName");
        sb.append('=');
        sb.append(((this.ownerName == null)?"<null>":this.ownerName));
        sb.append(',');
        sb.append("query");
        sb.append('=');
        sb.append(((this.query == null)?"<null>":this.query));
        sb.append(',');
        sb.append("searchName");
        sb.append('=');
        sb.append(((this.searchName == null)?"<null>":this.searchName));
        sb.append(',');
        sb.append("searchResults");
        sb.append('=');
        sb.append(((this.searchResults == null)?"<null>":this.searchResults));
        sb.append(',');
        sb.append("signedS3Url");
        sb.append('=');
        sb.append(((this.signedS3Url == null)?"<null>":this.signedS3Url));
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
        result = ((result* 31)+((this.fileName == null)? 0 :this.fileName.hashCode()));
        result = ((result* 31)+((this.ownerName == null)? 0 :this.ownerName.hashCode()));
        result = ((result* 31)+((this.query == null)? 0 :this.query.hashCode()));
        result = ((result* 31)+((this.searchName == null)? 0 :this.searchName.hashCode()));
        result = ((result* 31)+((this.signedS3Url == null)? 0 :this.signedS3Url.hashCode()));
        result = ((result* 31)+((this.searchResults == null)? 0 :this.searchResults.hashCode()));
        result = ((result* 31)+((this.ownerEmail == null)? 0 :this.ownerEmail.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof RecordSavedSearchCompleteInput) == false) {
            return false;
        }
        RecordSavedSearchCompleteInput rhs = ((RecordSavedSearchCompleteInput) other);
        return ((((((((this.fileName == rhs.fileName)||((this.fileName!= null)&&this.fileName.equals(rhs.fileName)))&&((this.ownerName == rhs.ownerName)||((this.ownerName!= null)&&this.ownerName.equals(rhs.ownerName))))&&((this.query == rhs.query)||((this.query!= null)&&this.query.equals(rhs.query))))&&((this.searchName == rhs.searchName)||((this.searchName!= null)&&this.searchName.equals(rhs.searchName))))&&((this.signedS3Url == rhs.signedS3Url)||((this.signedS3Url!= null)&&this.signedS3Url.equals(rhs.signedS3Url))))&&((this.searchResults == rhs.searchResults)||((this.searchResults!= null)&&this.searchResults.equals(rhs.searchResults))))&&((this.ownerEmail == rhs.ownerEmail)||((this.ownerEmail!= null)&&this.ownerEmail.equals(rhs.ownerEmail))));
    }

}
