package com.sailpoint.ets.domain.trigger;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
	"testNestedPropertyKey2"
})
@JsonIgnoreProperties(ignoreUnknown = true)
public class RecordTestNestedPropertyKey1 {

	/**
	 * (Required)
	 */
	@JsonProperty("testNestedPropertyKey2")
	private String testNestedPropertyKey2;

	/**
	 * (Required)
	 */
	@JsonProperty("testNestedPropertyKey2")
	public String getTestNestedPropertyKey2() {
		return testNestedPropertyKey2;
	}

	/**
	 * (Required)
	 */
	@JsonProperty("testNestedPropertyKey2")
	public void setTestNestedPropertyKey2(String testNestedPropertyKey2) {
		this.testNestedPropertyKey2 = testNestedPropertyKey2;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(RecordTestNestedPropertyKey1.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
		sb.append("testNestedPropertyKey2");
		sb.append('=');
		sb.append(((this.testNestedPropertyKey2 == null) ? "<null>" : this.testNestedPropertyKey2));
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
		result = ((result * 31) + ((this.testNestedPropertyKey2 == null) ? 0 : this.testNestedPropertyKey2.hashCode()));
		return result;
	}

	@Override
	public boolean equals(Object other) {
		if (other == this) {
			return true;
		}
		if ((other instanceof RecordTestNestedPropertyKey1) == false) {
			return false;
		}
		RecordTestNestedPropertyKey1 rhs = ((RecordTestNestedPropertyKey1) other);
		return ((this.testNestedPropertyKey2 == rhs.testNestedPropertyKey2) || ((this.testNestedPropertyKey2 != null) && this.testNestedPropertyKey2.equals(rhs.testNestedPropertyKey2)));
	}

}
