package com.sailpoint.ets.domain.trigger.schemaGeneratedDto.requestResponseTrigger;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
	"approved",
	"identityId"
})
@JsonIgnoreProperties(ignoreUnknown = true)
public class RecordAccessRequestedOutput {

	@JsonProperty("approved")
	private String approved;
	/**
	 * (Required)
	 */
	@JsonProperty("identityId")
	private String identityId;

	@JsonProperty("approved")
	public String getApproved() {
		return approved;
	}

	@JsonProperty("approved")
	public void setApproved(String approved) {
		this.approved = approved;
	}

	/**
	 * (Required)
	 */
	@JsonProperty("identityId")
	public String getIdentityId() {
		return identityId;
	}

	/**
	 * (Required)
	 */
	@JsonProperty("identityId")
	public void setIdentityId(String identityId) {
		this.identityId = identityId;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(RecordAccessRequestedOutput.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
		sb.append("approved");
		sb.append('=');
		sb.append(((this.approved == null) ? "<null>" : this.approved));
		sb.append(',');
		sb.append("identityId");
		sb.append('=');
		sb.append(((this.identityId == null) ? "<null>" : this.identityId));
		sb.append(',');
		if (sb.charAt((sb.length() - 1)) == ',') {
			sb.setCharAt((sb.length() - 1), ']');
		} else {
			sb.append(']');
		}
		return sb.toString();
	}

	@Override
	public int hashCode() {
		int result = 1;
		result = ((result * 31) + ((this.approved == null) ? 0 : this.approved.hashCode()));
		result = ((result * 31) + ((this.identityId == null) ? 0 : this.identityId.hashCode()));
		return result;
	}

	@Override
	public boolean equals(Object other) {
		if (other == this) {
			return true;
		}
		if ((other instanceof RecordAccessRequestedOutput) == false) {
			return false;
		}
		RecordAccessRequestedOutput rhs = ((RecordAccessRequestedOutput) other);
		return (((this.approved == rhs.approved) || ((this.approved != null) && this.approved.equals(rhs.approved))) && ((this.identityId == rhs.identityId) || ((this.identityId != null) && this.identityId.equals(rhs.identityId))));
	}

}
