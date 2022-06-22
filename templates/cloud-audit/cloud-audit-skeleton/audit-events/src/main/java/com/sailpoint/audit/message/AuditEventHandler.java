/*
 * Copyright (c) 2017. SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.audit.message;

import com.google.inject.Inject;
import com.sailpoint.atlas.messaging.server.MessageHandler;
import com.sailpoint.atlas.messaging.server.MessageHandlerContext;
import com.sailpoint.audit.service.AuditEventService;
import com.sailpoint.metrics.annotation.ExceptionMetered;
import com.sailpoint.metrics.annotation.Timed;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class AuditEventHandler implements MessageHandler {
	private static final Log log = LogFactory.getLog(AuditEventHandler.class);

	public enum MessageType {AUDIT_EVENT}

	public final static int MAX_RETRIES = 10;
	private final static int RETRY_DELAY_SECONDS = 120;

	@Inject
	AuditEventService _auditEventService;

	/**
	 * Handle the atomic message.
	 *
	 * @param context
	 * @throws Exception
	 */
	@ExceptionMetered
	@Timed
	@Override
	public void handleMessage(MessageHandlerContext context) throws Exception {
		try {
			context.setMaxRetries(MAX_RETRIES);
			context.setRetryDelaySeconds(RETRY_DELAY_SECONDS);

			AuditEventPayload payload = context.getMessageContent(AuditEventPayload.class);
			_auditEventService.processAuditMessage(payload, context);
		} catch (Exception e) {
			log.error("Failed to process AUDIT_EVENT", e);
			throw e;
		}
	}
}
