/*
 * Copyright (c) 2017. SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.audit.service;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sailpoint.audit.service.mapping.AuditEventActionTypes.AuditType;
import com.sailpoint.audit.service.mapping.AuditEventActionTypes.AuditActionType;
import com.sailpoint.audit.service.mapping.AuditEventActionTypes.CISAuditReportType;
import com.sailpoint.audit.service.mapping.AuditEventTypeToAction;
import com.sailpoint.audit.service.model.AuditReportDetails;
import com.sailpoint.audit.service.model.ReportDTO;
import com.sailpoint.audit.service.util.StackExtractor;
import com.sailpoint.mantis.core.service.CachedConfigService;
import com.sailpoint.mantis.core.service.CrudService;
import com.sailpoint.metrics.annotation.Timed;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import sailpoint.object.Attributes;
import sailpoint.object.AuditEvent;
import sailpoint.object.Filter;
import sailpoint.object.Identity;
import sailpoint.object.QueryOptions;
import sailpoint.tools.GeneralException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

/**
 * Created by mark.boyle on 4/3/17.
 */
@Singleton
public class AuditReportService {

	private static Log log = LogFactory.getLog(AuditReportService.class);

	public static final String AUDIT_REPORT = "Audit Report";

	public final static String S3BUCKET = "orgDataS3Bucket";

	@Inject
	CachedConfigService _configService;

	/**
	 * List of the prefixes for the different types of reports we support.
	 */
	public enum ReportPrefix {
		AUDIT_TYPE_REPORT("audit-type-report-"),
		AUDIT_USER_REPORT("audit-user-report-");

		String value;

		ReportPrefix(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return value;
		}
	}

	@Inject
	RDEServiceClient _rdeService;

	@Inject
	CrudService _crudService;
	/**
	 * Get count of audit events for a given queryOption
	 * @param queryOptions QueryOption to describe the filters on audit events
	 *
	 * @return count
	 * @throws GeneralException
	 */
	public long getAuditEventsCount(QueryOptions queryOptions) throws GeneralException {
		if ( queryOptions == null ) {
			queryOptions = new QueryOptions();
		}

		long auditCount = _crudService.count(AuditEvent.class, queryOptions);
		return auditCount;
	}

	/**
	 * Lists reports, and generates initial reports if needed
	 */
	public List<AuditReportDetails> listTypeReports(int days) throws GeneralException {
		List<AuditReportDetails> results = new ArrayList<AuditReportDetails>();

		// For all audit events report only include the numDays argument
		Attributes<String, Object> daysArgs = new Attributes<String, Object>();
		daysArgs.put("numDays", days);
		results.add( getReportModel(ReportPrefix.AUDIT_TYPE_REPORT, "all", daysArgs));

		// For non-CIS types, use type arguments. [ AUTH, SSO ]
		for (AuditActionType actionType : AuditActionType.values()) {
			Attributes<String, Object> args = new Attributes<String, Object>(daysArgs);
			args.put("types", Arrays.asList(actionType.toString()));
			results.add(getReportModel(ReportPrefix.AUDIT_TYPE_REPORT, actionType, args));
		}

		// For CIS types use actions.
		for (CISAuditReportType actionType : CISAuditReportType.values()) {
			Attributes<String, Object> args = new Attributes<String, Object>(daysArgs);
			args.put("actions", AuditEventTypeToAction.CIS_AUDIT_ACTION_TYPE_MAP.get(actionType.toString()));
			results.add(getReportModel(ReportPrefix.AUDIT_TYPE_REPORT, actionType, args ));
		}

		return results;
	}

	/**
	 * Get a list of reports for the given user.
	 *
	 * @param days
	 * @param username
	 * @return
	 * @throws GeneralException
	 */
	public List<AuditReportDetails> getUserReport(int days, String username) throws GeneralException {
		// TODO: we may what to add a way to prune old user reports in the future
		List<AuditReportDetails> results = new ArrayList<AuditReportDetails>();

		Attributes<String, Object> reportArgs = new Attributes<String, Object>();
		List<String> users = new ArrayList<String>();
		users.add(username.toLowerCase());
		reportArgs.put("users", users);
		reportArgs.put("numDays", days);
		results.add(getReportModel(ReportPrefix.AUDIT_USER_REPORT, username, reportArgs));
		return results;
	}

	/**
	 * Get the details of a report for the given prefix, type and arguments.  If the required objects don't
	 * exist, they are created.
	 *
	 * @param reportPrefix
	 * @param reportType
	 * @param reportArgs
	 * @return
	 * @throws GeneralException
	 */
	private AuditReportDetails getReportModel(ReportPrefix reportPrefix, Object reportType,
											  Attributes<String, Object> reportArgs)
			throws GeneralException {
		String reportName = reportPrefix.toString() + reportType.toString().toLowerCase();
		ReportDTO reportDTO = _rdeService.reportResult(reportName, reportArgs.getInt("numDays",7 ));

		if ( ! reportName.equals(reportDTO.getReportName()) ){
			reportDTO.setReportName(reportName);
		}

		reportArgs.put("s3Bucket",  _configService.getString(S3BUCKET));

		if ( reportDTO.getId() == null ){
			reportDTO = _rdeService.runReport(reportName, reportArgs.getInt("numDays",7 ), reportArgs);
		}

		return new AuditReportDetails(reportDTO, reportArgs);
	}

	/**
	 * Build a filter that can be used to find AuditEvent instances for the given "type". A type is a collection of
	 * audit events with various actions, grouped into a functional area like 'SSO' or 'PASSWORD_MANAGEMENT'.
	 *
	 * @param type The type to build a filter for.
	 *
	 * @return A Filter instance that can be used when finding objects.
	 */
	public Filter buildFilterByType(String type ) {
		Filter f = null;

		//If you're unfamiliar with Java, and this confuses you, here's what's going on.  There's no way to
		// check if a given string is part of an enum without using valueOf() which unfortunately throws an
		// exception on failure.  So, we try to parse the string into both of the support enums and handle
		// the exception if necessary.
		try {
			f = buildFilterByType( AuditActionType.valueOf( type ) );
			return f;
		} catch ( IllegalArgumentException ignored ) {}

		if (AuditEventTypeToAction.CIS_AUDIT_ACTION_TYPE_MAP.get(type) != null) {
			f = Filter.in("action", AuditEventTypeToAction.CIS_AUDIT_ACTION_TYPE_MAP.get(type));
		}

		return f;
	}

	/**
	 * Build a filter that can be used to find AuditEvent instances for the given "type". A type is a collection of
	 * audit events with various actions, grouped into a functional area like 'SSO' or 'PASSWORD_MANAGEMENT'.
	 *
	 * @param type The type to build a filter for.
	 *
	 * @return A Filter instance that can be used when finding objects.
	 */
	public Filter buildFilterByTypeForSearch(String type ) {
		Filter f = null;

		if (AuditEventTypeToAction.ES_AUDIT_ACTION_TYPE_MAP.get(type) != null) {
			f = Filter.in("action", AuditEventTypeToAction.ES_AUDIT_ACTION_TYPE_MAP.get(type));
		}

		return f;
	}

	/**
	 * Build a Filter instance to scope AuditEvent objects based on the given type.
	 *
	 * @param type A type to filter by.
	 *
	 * @return A Filter configured to find AuditEvents matching this type.
	 */
	public Filter buildFilterByType( AuditActionType type ) {
		return Filter.eq( "instance", type.toString() );
	}

	/**
	 * Build a Filter instance to scope AuditEvent objects by the given action.
	 *
	 * @param action The action to search by.
	 * @return A Filter configured to find AuditEvents matching this action.
	 */
	public Filter buildFilterByAction( String action ) {
		return Filter.eq("action", action);
	}

	public Filter buildFilterByAction( List<String> actions ) {
		return Filter.in("action", actions);
	}

	/**
	 * Build a Filter instance to scope AuditEvent objects by the given
	 * application.  This looks for a "like" match anywhere in the application
	 * name.
	 *
	 * @param application The application to search by.
	 * @return A Filter configured to find AuditEvents matching this application.
	 */
	public Filter buildFilterByApplication( String application ) {
		application = StackExtractor.getStack(application).get("application");
		return Filter.like("application", application, Filter.MatchMode.ANYWHERE);
	}

	/**
	 * Build a Filter instance to scope AuditEvent objects by the given days
	 *
	 * @param days The number of days.
	 * @return A Filter configured to find AuditEvents matching this days constraint.
	 */
	public Filter buildFilterByDays( Integer days ) {
		final Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DAY_OF_MONTH, (-1) * days);

		return Filter.ge("created", cal.getTime());
	}

	/**
	 * Build a filter instance to scope AuditEvent objects that were created since the given date.
	 *
	 * @param time The ISO-formatted time that events must be created after.
	 * @return A Filter instance.
	 */
	public Filter buildFilterSinceDate(String time) {
		Calendar cal = javax.xml.bind.DatatypeConverter.parseDateTime(time);
		return Filter.ge("created", cal.getTime());
	}

	/**
	 * Build a Filter instance to scope AuditEvent objects by the given user. The user can be either source, or the
	 * target, of the audit event.
	 *
	 * A filter is constructed that searches both the primary identity name, as well as the uid field.  Some SSO
	 * events are audited under the uid.
	 *
	 * @param user The username, such as "thomas.edison".
	 * @return A Filter configured to find AuditEvents matching this user.
	 */
	public Filter buildFilterByUser( String user ) {
		List<String> usernames = new ArrayList<String>(2);
		usernames.add( user );

		_crudService.findByName( Identity.class, user ).ifPresent(identity -> {
			if( identity.getAttribute("uid") != null ) {
				usernames.add((String) identity.getAttribute("uid"));
			}
		});

		List<Filter> filters = new ArrayList<Filter>();
		for( String username : usernames ) {
			filters.add(Filter.eq("source", username));
			filters.add(Filter.eq("target", username));
		}

		return Filter.or(filters);
	}

}
