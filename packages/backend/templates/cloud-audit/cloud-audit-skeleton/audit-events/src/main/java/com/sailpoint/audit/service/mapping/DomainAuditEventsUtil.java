/*
 * Copyright (C) 2021 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.audit.service.mapping;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sailpoint.audit.event.model.EventCatalog;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Singleton
public class DomainAuditEventsUtil {

    private Set<String> _actionSet;

    private HashMap<String, String> _actionToType;

    @Inject
    public DomainAuditEventsUtil(EventCatalog eventCatalog) {
        _actionSet = eventCatalog.stream()
                .map(entry -> entry.getEvent().getAction())
                .collect(Collectors.toSet());

        _actionToType = new HashMap<>();
        eventCatalog.stream()
                .forEach(entry -> _actionToType.put(entry.getEvent().getAction(), entry.getEvent().getType()));
    }

    public boolean isDomainAuditEvent(String action) {
        return _actionSet.contains(action);
    }

    public Set<String> getDomainEventActions() {
        return new HashSet<>(_actionSet);
    }

    public String getDomainType(String domainAction) {
        return _actionToType.get(domainAction);
    }
}
