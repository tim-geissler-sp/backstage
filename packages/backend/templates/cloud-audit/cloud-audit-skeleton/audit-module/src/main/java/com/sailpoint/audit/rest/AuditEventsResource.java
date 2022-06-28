/*
 * Copyright (C) 2017-2019 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.audit.rest;

import com.google.inject.Inject;
import com.sailpoint.atlas.idn.RestClientProvider;
import com.sailpoint.atlas.idn.ServiceNames;
import com.sailpoint.atlas.search.model.search.Query;
import com.sailpoint.atlas.search.model.search.QueryResultFilter;
import com.sailpoint.atlas.search.model.search.Search;
import com.sailpoint.atlas.security.RequireGroup;
import com.sailpoint.atlas.service.FeatureFlagService;
import com.sailpoint.audit.service.AuditEventReportingService;
import com.sailpoint.audit.service.AuditReportService;
import com.sailpoint.audit.service.model.AuditDetails;
import com.sailpoint.audit.service.model.AuditReportRequest;
import com.sailpoint.audit.service.model.AuditReportResponse;
import com.sailpoint.audit.util.AuditEventSearchQueryUtil;
import com.sailpoint.mantis.core.service.CrudService;
import com.sailpoint.mantis.platform.rest.BaseListFilter;
import com.sailpoint.mantis.platform.rest.ListResult;
import com.sailpoint.mantisclient.BaseRestClient;
import com.sailpoint.mantisclient.Params;
import com.sailpoint.metrics.annotation.Timed;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;
import sailpoint.object.AuditEvent;
import sailpoint.object.Filter;
import sailpoint.object.QueryOptions;
import sailpoint.tools.GeneralException;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import java.util.stream.Collectors;

import static com.sailpoint.atlas.idn.SecurityGroups.CERT_ADMIN;
import static com.sailpoint.atlas.idn.SecurityGroups.DASHBOARD;
import static com.sailpoint.atlas.idn.SecurityGroups.HELPDESK;
import static com.sailpoint.atlas.idn.SecurityGroups.ORG_ADMIN;
import static com.sailpoint.atlas.idn.SecurityGroups.REPORT_ADMIN;
import static com.sailpoint.atlas.idn.SecurityGroups.ROLE_ADMIN;
import static com.sailpoint.atlas.idn.SecurityGroups.ROLE_SUBADMIN;
import static com.sailpoint.atlas.idn.SecurityGroups.SOURCE_ADMIN;
import static com.sailpoint.atlas.idn.SecurityGroups.SOURCE_SUBADMIN;

/**
 * Created by mark.boyle on 4/3/17.
 */
@Path("auditEvents")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuditEventsResource {

	private static final Log log = LogFactory.getLog(AuditEventsResource.class);

	private static final String SEARCH_ENDPOINT = "/search/v3/search";

	private static final String ITEMS = "items";
	private static final String TOTAL = "total";

	@Inject
	private AuditReportService _auditService;

	@Inject
	private CrudService _crudService;

	@Inject
	private FeatureFlagService _featureFlagService;

	@Inject
	private RestClientProvider _restClientProvider;

	@Inject
	private AuditEventReportingService _auditEventReportingService;

	@Inject
	private AuditEventSearchQueryUtil _auditEventSearchQueryUtil;

	/**
	 * List audit records.  Passed parameters are cumulative; if multiple are specified then returned events
	 * match all of them.  Returned events are ordered with the most recent events first.
	 *
	 * @param type If specified, find all events of the given type.
	 * @param action If specified, find all events with the given action.
	 * @param application If specified, find all events by the given application ("ends with" match).
	 * @param days If specified, only return results that occurred in the given number of days before.
	 * @param since If specified, only return results that occurred since the given time (in ISO format,
	 *                 eg "2010-01-01T12:00:00Z")
	 * @param user If specified, find all events initiated by, or applied to, the given user.
	 * @param limit Return only this number of records maximum.
	 * @param filtersJSON If specified, find all events that match the filters.
	 *
	 * @return audit records
	 * @throws GeneralException
	 */
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@RequireGroup({CERT_ADMIN, DASHBOARD, HELPDESK, ORG_ADMIN, REPORT_ADMIN, ROLE_ADMIN, ROLE_SUBADMIN, SOURCE_ADMIN, SOURCE_SUBADMIN})
	@Timed
	public ListResult list(
			@QueryParam("type") String type,
			@QueryParam("action") String action,
			@QueryParam("application") String application,
			@QueryParam("days") Integer days,
			@QueryParam("since") String since,
			@QueryParam("user") String user,
			@QueryParam("limit") int limit,
			@QueryParam("start") int start,
			@QueryParam("filters") String filtersJSON) throws GeneralException {

		QueryOptions queryOptions = new QueryOptions();
		queryOptions.setOrderBy("created");
		queryOptions.setOrderAscending(false);

		if (start > 0) {
			queryOptions.setFirstRow(start);
		}

		if (limit > 0) {
			queryOptions.setResultLimit(limit);
		}

		if (type != null) {
			Filter f = _auditService.buildFilterByType(type);
			if (f != null) {
				queryOptions.add(f);
			}
		}

		if (days != null) {
			queryOptions.add(_auditService.buildFilterByDays(days));
		}

		if (since != null) {
			queryOptions.add(_auditService.buildFilterSinceDate(since));
		}

		if (action != null) {
			queryOptions.add(_auditService.buildFilterByAction(action));
		}

		if (application != null) {
			queryOptions.add(_auditService.buildFilterByApplication(application));
		}

		if (user != null) {
			queryOptions.add(_auditService.buildFilterByUser(user));
		}

		if (filtersJSON != null) {
			BaseListFilter filters = BaseListFilter.getFilterFromJson(filtersJSON);
			BaseListFilter.prepareQueryOptions(queryOptions, filters, null);
		}

		//Filter out some noisy or irrelevant events here, such as the 'emailFailure' event that has been caused
		// by a CIS misconfiguration and the 'import' event triggered at CIS startup.
		queryOptions.add(Filter.and(
				Filter.not(Filter.eq("action", "emailFailure")),
				Filter.not(Filter.eq("action", "import"))
		));

		long auditCount = _auditService.getAuditEventsCount(queryOptions);
		List<AuditDetails> result = _crudService.getContext().getObjects(AuditEvent.class, queryOptions).stream()
				.map(AuditDetails::new)
				.collect(Collectors.toList());

		return new ListResult(result, (int) auditCount);
	}

	/**
	 * Submits RDE (search export) report, which is used in downloading audit activity of the userId
	 *
	 * @param auditReportRequest - accepts a body with userId and searchText and few other fields
	 * @return - AuditReportResponse which has id (report_id) field, which can used to download the result.
	 */
	@POST
	@Path("/identity-events-reports")
	@Produces(MediaType.APPLICATION_JSON)
	@RequireGroup({CERT_ADMIN, DASHBOARD, HELPDESK, ORG_ADMIN, REPORT_ADMIN, ROLE_ADMIN, ROLE_SUBADMIN, SOURCE_ADMIN, SOURCE_SUBADMIN})
	public Response getAuditEventReports(AuditReportRequest auditReportRequest) {
		Response.ResponseBuilder res;
		if (auditReportRequest == null || auditReportRequest.getUserId() == null) {
			res = Response.status(HttpStatus.SC_BAD_REQUEST)
					.entity("userId query param must not be empty");
			return res.build();
		}
		AuditReportResponse auditReportResponse = _auditEventReportingService.generateReport(auditReportRequest);
		res = Response.ok(auditReportResponse);
		return res.build();
	}

	/**
	 * Get audit events for an identity
	 * userId is mandatory. "*" is not a permitted value for userId
	 * This is a temporary API created until the helpdesk permission level is given to Events index in Search
	 * @param identityId
	 * @return
	 */
	@GET
	@Path("/identity-events")
	@Produces(MediaType.APPLICATION_JSON)
	@RequireGroup({CERT_ADMIN, DASHBOARD, HELPDESK, ORG_ADMIN, REPORT_ADMIN, ROLE_ADMIN, ROLE_SUBADMIN, SOURCE_ADMIN, SOURCE_SUBADMIN})
	public Response getAuditEventsForIdentity(@QueryParam("userId") String identityId,
						  @QueryParam("searchString") String searchText) {
		Response.ResponseBuilder res;
		if (null == identityId || identityId.length() <= 0 || "*".equals(identityId.trim())) {
			res = Response.status(HttpStatus.SC_BAD_REQUEST)
					.entity("userId query param must not be empty, and value should not be *");
			return res.build();
		}

		String query = _auditEventSearchQueryUtil.buildSearchQuery(identityId, searchText);

		Search search = Search.builder()
				.withIndices("events")
				.withQuery(Query.of(query)
						//not adding actor.name and target.name since they are already part of the
						//base query with userId
						.withFields("name", "attributes.sourceName", "ipAddress", "attributes.errors"))
				.withQueryResultFilter(QueryResultFilter.includes
						("id","name","attributes.sourceName","actor.name","target.name","ipAddress"
								,"created", "attributes.errors"))
				.withSort("-created")
				.build();

		BaseRestClient sdsClient = _restClientProvider.getContextRestClient(ServiceNames.SDS);
		Params params = new Params();
		//Explicitly specifying limit for readability, although Search's default limit is also 250
		params.query("limit", 250);

		//This format is to keep it similar to the current end point
		Map<String, Object> finalResult = new LinkedHashMap<>();
		try {
			List<Map> auditList = sdsClient
					.postJson(List.class, SEARCH_ENDPOINT, search, params);

			if (auditList != null) {
				List<AuditDetails> result = auditList.stream()
						.filter(Objects::nonNull)
						.map(this::constructAuditDetails)
						.collect(Collectors.toList());

				finalResult.put(ITEMS, result);
				//Total is getting added in CC https://github.com/sailpoint/cloud/blob/master/cloudcommander/grails-app/services/com/cloudmasons/AuditService.groovy#L1307
				finalResult.put(TOTAL, result.size());
			} else {
				finalResult.put(ITEMS, Collections.emptyList());
				finalResult.put(TOTAL, 0);
			}
			res = Response.ok(finalResult);
			return res.build();
		} catch (Exception e) {
			res = Response.status(HttpStatus.SC_BAD_REQUEST);
			return res.build();
		}
	}

	private AuditDetails constructAuditDetails(Object o) {
		Map<String, Object> searchAuditEvent = (Map) o;
		AuditDetails auditDetails = new AuditDetails();
		Map<String, Object> attributes = (Map) searchAuditEvent.get("attributes");
		Map<String, Object> actor = (Map) searchAuditEvent.get("actor");
		Map<String, Object> target = (Map) searchAuditEvent.get("target");

		auditDetails.setId((String) searchAuditEvent.get("id"));
		auditDetails.setAction((String) searchAuditEvent.get("name"));

		if (attributes != null) {
			auditDetails.setApplication((String) attributes.get("sourceName"));
			auditDetails.setErrors((String) attributes.get("errors"));
		}

		if (actor != null) {
			auditDetails.setSource((String) actor.get("name"));
		}

		if (target != null) {
			auditDetails.setTarget((String) target.get("name"));
		}
		auditDetails.setIpaddr((String) searchAuditEvent.get("ipAddress"));

		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		df.setTimeZone(TimeZone.getTimeZone(ZoneId.of("UTC")));
		String createdDateString = (String) searchAuditEvent.get("created");
		try {
			if (StringUtils.isNotEmpty(createdDateString)) {
				auditDetails.setCreated(df.parse(createdDateString));
			}
		} catch (ParseException e) {

		}

		return auditDetails;
	}
}
