/*
 * Copyright (C) 2020 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.ets.infrastructure.util;

import static com.google.common.base.CaseFormat.LOWER_CAMEL;
import static com.google.common.base.CaseFormat.UPPER_UNDERSCORE;

/**
 * Util class for convert to camel case.
 */
public class CamelCaseUtil {
	/**
	 * Convert an enum to camel case string
	 * @param type an enum
	 * @return camel cased string
	 */
	public static String toCamelCase(Enum<?> type) {
		if (type == null) {
			return null;
		}
		return toCamelCase(type.name());
	}

	/**
	 * Convert a string to camel case
	 * @param string the input string
	 * @return camel cased string
	 */
	public static String toCamelCase(String string) {
		if (string == null) {
			return null;
		}
		return UPPER_UNDERSCORE.to(LOWER_CAMEL, string);
	}
}
