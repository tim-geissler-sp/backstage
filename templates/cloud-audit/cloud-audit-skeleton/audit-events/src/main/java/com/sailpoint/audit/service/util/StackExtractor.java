/*
 *
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 *
 */

package com.sailpoint.audit.service.util;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StackExtractor {

	private static Pattern stackFromApplication = Pattern.compile("\\A\\[(\\w+)\\]\\s{0,1}(.*)");
	private static Pattern sourceIdFromApplication = Pattern.compile("(.*)\\s+\\[(\\S+)\\]");

	public static Map<String, String> getStack(String application){
		Map<String, String> fields = new HashMap<>();
		if(application != null){
			Matcher matcher = stackFromApplication.matcher(application);
			if (matcher.find()) {
				fields.put("stack",matcher.group(1));
				application = matcher.group(2);
			}
			matcher = sourceIdFromApplication.matcher(application);
			if (matcher.find()) {
				application = matcher.group(1);
				fields.put("sourceId", matcher.group(2));
			}
		}
		fields.put("application", application);
		return fields;
	}
}
