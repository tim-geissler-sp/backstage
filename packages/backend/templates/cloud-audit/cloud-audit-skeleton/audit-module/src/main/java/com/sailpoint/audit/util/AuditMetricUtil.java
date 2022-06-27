/*
 * Copyright (C) 2020 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.audit.util;

import com.sailpoint.iris.client.kafka.internal.NumberGauge;
import com.sailpoint.metrics.MetricsUtil;

import java.util.Map;

public class AuditMetricUtil {
	public final static String NON_WHITELISTED = "nonWhitelisted";

	public final static String REMAINING_COUNT_METRIC = "remaining.total";

	public final static String PROCESSED_COUNT_METRIC = "processed.total";

	public final static String UPLOADED_COUNT_METRIC = "uploaded.total";

	public final static String AUDIT_RECORD_COUNT = "records.total";

	public final static String UPLOAD_TIME_METRIC = "batch.seconds";

	private String _metricsBasePath;

	public AuditMetricUtil(String metricsBasePath) {
		_metricsBasePath = metricsBasePath;
	}

	public void writeGauge(String name, Number value, Map<String, String> tags) {
		String metricPath = MetricsUtil.getMetricsName(_metricsBasePath + "." + name, tags);
		if (!MetricsUtil.getRegistry().getGauges().containsKey(metricPath)) {
			MetricsUtil.getRegistry().register(metricPath, new NumberGauge(value));
		} else {
			NumberGauge gauge = (NumberGauge) MetricsUtil.getRegistry().getGauges().get(metricPath);
			gauge.setValue(value);
		}
	}
}
