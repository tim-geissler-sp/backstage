/*
 * Copyright (c) 2021. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.audit.util;

import com.sailpoint.atlas.search.util.JsonUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import sailpoint.object.AuditEvent;

import static java.util.Objects.requireNonNull;

/**
 * Compares two AuditEvents, or one AuditEvent and a JSON, to see if they are equal.
 *
 * When AuditEvents are persisted to JSON, the key sequence of the Attributes can read back differently.
 * For example these two JSON snippets:
 *
 *   ...,"attributes":{"attrib1Key":"attrib1Val","attrib2Key":"attrib2Val"},...
 *   ...,"attributes":{"attrib2Key":"attrib2Val","attrib1Key":"attrib1Val"},...
 *
 * Are different but came from the same AuditEvent that was round-tripped through a relational model. This
 * class works to mitigate these kinds of differences. If `attributes` ends up containing maps-of-maps then
 * we might need to walk this down an N-ary sequence for deep correction/comparison.  At this time we are not
 * seeing a need for that in testing.
 *
 * The AuditEvent class has a `description` field that can be set in the POJO. This field is not persisted
 * in the CIS MySQL or H2 databases, but _will_ be persisted in the S3-By-Time model of an AuditEvent.  This
 * _might_ cause differecnes in the comparison of AuditEvents for equality.
 *
 */
public class AuditEventComparator {

	public static Log log = LogFactory.getLog(AuditEventComparator.class);

	/**
	 * Compares two AuditEvents.  Returns null on no difference.
	 * @param lhs - left hand side AuditEvent to compare.
	 * @param rhs - right hand side AuditEvent to compare.
	 * @return - null on equality, or a String describing the difference on inequality.
	 */
	public String diffAuditEvents(AuditEvent lhs, AuditEvent rhs) {
		requireNonNull(lhs, "a valid AuditEvent reference for the lhs (left hand side) is required.");
		requireNonNull(rhs, "a valid AuditEvent reference for the rhs (right hand side) is required.");
		String lhsJson = JsonUtils.toJson(lhs);
		String rhsJson = JsonUtils.toJson(rhs);
		return diffAuditEvents(lhsJson, rhsJson);
	}

	/**
	 * Compares two AuditEvents.  Returns null on no difference.
	 * @param lhs - left hand side AuditEvent to compare.
	 * @param rhsJson - right hand side JSON model of an AuditEvent to compare.
	 * @return - null on equality, or a String describing the difference on inequality.
	 */
	public String diffAuditEvents(AuditEvent lhs, String rhsJson) {
		requireNonNull(lhs, "a valid AuditEvent reference for the lhs (left hand side) is required.");
		requireNonNull(rhsJson, "a valid String reference for the rhs (right hand side) is required.");
		String lhsJson = JsonUtils.toJson(lhs);
		return diffAuditEvents(lhsJson, rhsJson);
	}

	/**
	 * Compares two AuditEvent JSON Strings.  Returns null on no difference.
	 * @param lhsJson - left hand side JSON model of an AuditEvent to compare.
	 * @param rhsJson - right hand side JSON model of an AuditEvent to compare.
	 * @return - null on equality, or a String describing the difference on inequality.
	 */
	public String diffAuditEvents(String lhsJson, String rhsJson) {

		requireNonNull(lhsJson, "a valid String reference for the lhsJson (left hand side) is required.");
		requireNonNull(lhsJson, "a valid String reference for the rhsJson (right hand side) is required.");

		// Short circuit on simple first-pass JSON string equality from the two Audit Event models.
		if (lhsJson.equals(rhsJson)) return null;

		// The JSONs are different, but that does not mean necessarily that the two AuditEvents are different.
		// The sequence of the keys of the `attributes` map might be different.  Run both AuditEvents through
		// a serialization loop to standardize the key ordering and re-compare.
		AuditEvent lhsFromJson = JsonUtils.parse(AuditEvent.class, lhsJson);
		AuditEvent rhsFromJson = JsonUtils.parse(AuditEvent.class, rhsJson);

		String lhsNormalizedJson = JsonUtils.toJson(lhsFromJson);
		String rhsNormalizedJson = JsonUtils.toJson(rhsFromJson);

		// Short circuit if the normalized JSONs are equal.
		if (lhsNormalizedJson.equals(rhsNormalizedJson)) return null;

		// Build a message on where the JSONs differ.
		String fwdDiff = StringUtils.difference(lhsNormalizedJson, rhsNormalizedJson);
		String revDiff = StringUtils.difference(rhsNormalizedJson, lhsNormalizedJson);

		return String.format("AuditEvent JSONs differ at: [%s] != [%s]",
				fwdDiff.substring(0, 32),
				revDiff.substring(0, 32)
		);
	}

}
