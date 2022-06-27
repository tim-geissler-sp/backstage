/*
 * Copyright (C) 2021 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.audit.event;

import com.google.inject.Inject;
import com.sailpoint.atlas.search.model.event.Event;
import com.sailpoint.atlas.service.FeatureFlagService;
import com.sailpoint.audit.service.AuditEventService;
import com.sailpoint.audit.service.FeatureFlags;
import com.sailpoint.audit.service.FirehoseService;
import com.sailpoint.audit.service.util.AuditUtil;
import com.sailpoint.iris.server.EventHandler;
import com.sailpoint.iris.server.EventHandlerContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class AuditEventS3Handler implements EventHandler {

    private static Log _log = LogFactory.getLog(AuditEventS3Handler.class);

    /*
    Timestamp when the first audit event in parquet format was persisted in S3 via firehose
    This is also the timestamp until we bulk sync audit events
     */
    public static final String AUDITEVENT_PARQUET_TIMESTAMP = "auditevent_parquet_timestamp";

    @Inject
    FirehoseService _firehoseService;

    @Inject
    FeatureFlagService _featureFlagService;

    @Inject
    AuditEventService _auditEventService;

    @Override
    public void handleEvent(EventHandlerContext eventHandlerContext) throws Exception {
        if (_featureFlagService.getBoolean(FeatureFlags.WRITE_AUDIT_DATA_IN_PARQUET, false)) {
            Event event = eventHandlerContext.getEvent().getContent(Event.class);

			AuditUtil.checkEventSizeAndFieldLimits(event);

            _auditEventService.addCheckpoint(
                    event.getCreated().getTime(),
                    event.getId(),
                    AUDITEVENT_PARQUET_TIMESTAMP);

            _firehoseService.sendToFirehose(event);
        }
    }
}
