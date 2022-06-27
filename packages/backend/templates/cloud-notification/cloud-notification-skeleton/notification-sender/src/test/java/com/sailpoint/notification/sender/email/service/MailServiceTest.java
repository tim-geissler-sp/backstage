/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.notification.sender.email.service;

import com.amazonaws.SdkClientException;
import com.sailpoint.atlas.AtlasConfig;
import com.sailpoint.atlas.OrgData;
import com.sailpoint.atlas.RequestContext;
import com.sailpoint.atlas.event.idn.IdnTopic;
import com.sailpoint.atlas.security.AdministratorSecurityContext;
import com.sailpoint.iris.client.Event;
import com.sailpoint.iris.client.GlobalTopic;
import com.sailpoint.iris.server.EventHandlerContext;
import com.sailpoint.notification.api.event.RecipientBuilder;
import com.sailpoint.notification.api.event.dto.NotificationRendered;
import com.sailpoint.notification.sender.common.exception.InvalidNotificationException;
import com.sailpoint.notification.sender.email.MailClient;
import com.sailpoint.notification.sender.email.Validator;
import com.sailpoint.notification.sender.common.lifecycle.NotificationMetricsUtil;
import com.sailpoint.notification.sender.email.service.model.Mail;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test class for testing MailService
 */
public class MailServiceTest {

	@Mock
	MailClient _mailClient;

	@Mock
	AtlasConfig _atlasConfig;

	@Mock
	EventHandlerContext _context;

	@Mock
	Event _event;

	private MailService _mailService;

	private NotificationRendered notificationRendered;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);

		_mailService = new MailService();
		_mailService._mailClient = _mailClient;
		_mailService._validator = new Validator();
		_mailService._metricsUtil = new NotificationMetricsUtil(_atlasConfig);

		notificationRendered = NotificationRendered.builder()
				.recipient(new RecipientBuilder()
						.withEmail("to@to")
						.build())
				.from("from@from")
				.subject("test")
				.body("<body>hello<br />html</body>")
				.replyTo("reply@to")
				.notificationKey("notificationKey")
				.build();

		when(_context.getEvent()).thenReturn(_event);
		when(_event.getType())
				.thenReturn("NOTIFICATION");
		when(_context.getTopic())
				.thenReturn(new GlobalTopic(IdnTopic.NOTIFICATION.getName()));

		when(_event.getHeader(anyString())).thenReturn(Optional.empty());

		givenRequestContext();
	}

	@Test
	public void testSendMailWithStopSubject() throws Exception {
		notificationRendered.setSubject("no_send");
		_mailService.sendMail(notificationRendered);
		verify(_mailClient, never()).sendMail(any(Mail.class));

		notificationRendered.setSubject("stop");
		_mailService.sendMail(notificationRendered);
		verify(_mailClient, never()).sendMail(any(Mail.class));
	}

	@Test
	public void testSendMail() throws Exception {
		_mailService.sendMail(notificationRendered);

		verify(_mailClient, times(1)).sendMail(any(Mail.class));
	}

	@Test (expected = InvalidNotificationException.class)
	public void testSendMailInvalid() throws Exception {
		notificationRendered = notificationRendered.derive()
				.from(null)
				.build();
		_mailService.sendMail(notificationRendered);
		verify(_mailClient, never()).sendMail(any(Mail.class));
	}

	@Test (expected = InvalidNotificationException.class)
	public void testSendMailInvalidFromAddress() throws Exception {
		notificationRendered = notificationRendered.derive()
				.from("invalidfromaddress")
				.build();
		_mailService.sendMail(notificationRendered);
		verify(_mailClient, never()).sendMail(any(Mail.class));
	}

	@Test (expected = RuntimeException.class)
	public void testSendMailClientException() throws Exception {
		doThrow(SdkClientException.class).when(_mailClient).sendMail(any());
		_mailService.sendMail(notificationRendered);
	}

	private void givenRequestContext() {
		OrgData orgData = new OrgData();
		orgData.setPod("dev");
		orgData.setOrg("acme-solar");

		RequestContext requestContext = new RequestContext();
		requestContext.setSecurityContext(new AdministratorSecurityContext());
		requestContext.setOrgData(orgData);
		RequestContext.set(requestContext);
	}
}
