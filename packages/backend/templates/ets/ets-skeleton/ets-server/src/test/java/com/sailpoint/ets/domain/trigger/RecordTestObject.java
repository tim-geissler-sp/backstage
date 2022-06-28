package com.sailpoint.ets.domain.trigger;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
	"testArrayKey",
	"testNestedPropertyKey1",
	"testStringKey"
})
@JsonIgnoreProperties(ignoreUnknown = true)
public class RecordTestObject {

	/**
	 * (Required)
	 */
	@JsonProperty("testArrayKey")
	private List<Object> testArrayKey = new ArrayList<Object>();
	/**
	 * (Required)
	 */
	@JsonProperty("testNestedPropertyKey1")
	private RecordTestNestedPropertyKey1 testNestedPropertyKey1;
	/**
	 * (Required)
	 */
	@JsonProperty("testStringKey")
	private String testStringKey;

	/**
	 * (Required)
	 */
	@JsonProperty("testArrayKey")
	public List<Object> getTestArrayKey() {
		return testArrayKey;
	}

	/**
	 * (Required)
	 */
	@JsonProperty("testArrayKey")
	public void setTestArrayKey(List<Object> testArrayKey) {
		this.testArrayKey = testArrayKey;
	}

	/**
	 * (Required)
	 */
	@JsonProperty("testNestedPropertyKey1")
	public RecordTestNestedPropertyKey1 getTestNestedPropertyKey1() {
		return testNestedPropertyKey1;
	}

	/**
	 * (Required)
	 */
	@JsonProperty("testNestedPropertyKey1")
	public void setTestNestedPropertyKey1(RecordTestNestedPropertyKey1 testNestedPropertyKey1) {
		this.testNestedPropertyKey1 = testNestedPropertyKey1;
	}

	/**
	 * (Required)
	 */
	@JsonProperty("testStringKey")
	public String getTestStringKey() {
		return testStringKey;
	}

	/**
	 * (Required)
	 */
	@JsonProperty("testStringKey")
	public void setTestStringKey(String testStringKey) {
		this.testStringKey = testStringKey;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(RecordTestObject.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
		sb.append("testArrayKey");
		sb.append('=');
		sb.append(((this.testArrayKey == null) ? "<null>" : this.testArrayKey));
		sb.append(',');
		sb.append("testNestedPropertyKey1");
		sb.append('=');
		sb.append(((this.testNestedPropertyKey1 == null) ? "<null>" : this.testNestedPropertyKey1));
		sb.append(',');
		sb.append("testStringKey");
		sb.append('=');
		sb.append(((this.testStringKey == null) ? "<null>" : this.testStringKey));
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
		result = ((result * 31) + ((this.testNestedPropertyKey1 == null) ? 0 : this.testNestedPropertyKey1.hashCode()));
		result = ((result * 31) + ((this.testArrayKey == null) ? 0 : this.testArrayKey.hashCode()));
		result = ((result * 31) + ((this.testStringKey == null) ? 0 : this.testStringKey.hashCode()));
		return result;
	}

	@Override
	public boolean equals(Object other) {
		if (other == this) {
			return true;
		}
		if ((other instanceof RecordTestObject) == false) {
			return false;
		}
		RecordTestObject rhs = ((RecordTestObject) other);
		return ((((this.testNestedPropertyKey1 == rhs.testNestedPropertyKey1) || ((this.testNestedPropertyKey1 != null) && this.testNestedPropertyKey1.equals(rhs.testNestedPropertyKey1))) && ((this.testArrayKey == rhs.testArrayKey) || ((this.testArrayKey != null) && this.testArrayKey.equals(rhs.testArrayKey)))) && ((this.testStringKey == rhs.testStringKey) || ((this.testStringKey != null) && this.testStringKey.equals(rhs.testStringKey))));
	}

}
