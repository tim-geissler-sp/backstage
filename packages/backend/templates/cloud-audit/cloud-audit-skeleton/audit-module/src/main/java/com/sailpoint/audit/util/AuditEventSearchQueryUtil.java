/*
 * Copyright (c) 2022. SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.audit.util;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sailpoint.mantis.core.service.CrudService;
import org.apache.commons.lang3.StringUtils;
import sailpoint.object.Identity;

import java.util.HashSet;
import java.util.Set;

@Singleton
public class AuditEventSearchQueryUtil {
    private static final String LAST_SEVEN_DAYS = " AND created:[now-7d/d TO now] ";
    private static final String BASE_QUERY = " actor.name:\"%s\" OR target.name:\"%s\" ";
    public static final String UID = "uid";

    @Inject
    private CrudService _crudService;

    public Set<String> getAllUserNames(String identityId) {
        Set<String> usernames = new HashSet<>();
        usernames.add(identityId);

        //Eventually it is best to go to Search instead of CIS, although CIS identities is not going away
        _crudService.findByName(Identity.class, identityId).ifPresent(identity -> {
            if (identity.getAttribute(UID) != null) {
                usernames.add((String) identity.getAttribute(UID));
            }
        });

        return usernames;
    }

    public String buildSearchQuery(String identityId, String searchText) {
        StringBuilder query = new StringBuilder();
        query.append("( ");

        Set<String> userNames = getAllUserNames(identityId);
        for (String userName : userNames) {
            query.append(String.format(BASE_QUERY, userName, userName));
            query.append(" OR ");
        }
        query.delete(query.lastIndexOf(" OR "), query.length());
        query.append(" ) ");

        query.append(LAST_SEVEN_DAYS);
        if (StringUtils.isNotEmpty(searchText)) {
            query.append(" AND ");
            query.append("*").append(searchText).append("*");
        }
        return query.toString();
    }
}
