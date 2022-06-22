/*
 * Copyright (c) 2021. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.rest;

import com.google.inject.Inject;
import com.sailpoint.atlas.RequestContext;
import com.sailpoint.atlas.security.RequireRight;
import com.sailpoint.cloud.api.client.model.V3Resource;
import com.sailpoint.cloud.api.client.model.errors.ApiExceptionBuilder;
import com.sailpoint.notification.api.event.RecipientBuilder;
import com.sailpoint.notification.api.event.dto.Notification;
import com.sailpoint.notification.api.event.dto.NotificationRendered;
import com.sailpoint.notification.api.event.dto.Recipient;
import com.sailpoint.notification.context.service.GlobalContextService;
import com.sailpoint.notification.rest.dto.SendRequestDto;
import com.sailpoint.notification.rest.dto.TestSendRequestDto;
import com.sailpoint.notification.sender.common.exception.InvalidNotificationException;
import com.sailpoint.notification.sender.common.service.SenderService;
import com.sailpoint.notification.template.common.model.NotificationTemplate;
import com.sailpoint.notification.template.common.model.teams.TeamsTemplate;
import com.sailpoint.notification.template.common.model.slack.SlackTemplate;
import com.sailpoint.notification.template.common.repository.TemplateRepository;
import com.sailpoint.notification.template.service.NotificationTemplateService;
import com.sailpoint.notification.userpreferences.dto.UserPreferences;
import com.sailpoint.notification.userpreferences.repository.UserPreferencesRepository;
import com.sailpoint.utilities.StringUtil;
import lombok.extern.apachecommons.CommonsLog;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static com.sailpoint.notification.context.service.GlobalContextService.EMAIL_OVERRIDE;

/**
 * V3 resource for Notification
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@V3Resource
@CommonsLog
public class SendResource {

    @Inject
    TemplateRepository _templateRepository;

    @Inject
    NotificationTemplateService _notificationTemplateService;

    @Inject
    UserPreferencesRepository _userPreferencesRepository;

    @Inject
    GlobalContextService _globalContextService;

    @Inject
    SenderService _senderService;

    @POST
    @Path("send-notification")
    @RequireRight("sp:notification:send")
    public Response sendNotification(SendRequestDto sendRequestDto) {
        validateAndSendNotification(sendRequestDto);
        return Response.noContent().build();
    }

    @POST
    @Path("send-test-notification")
    @RequireRight("sp:notification-test:send")
    public Response sendTestNotification(TestSendRequestDto testSendRequestDto) {
        RequestContext rc = RequestContext.ensureGet();
        String recipientId = rc.getIdentity().orElseThrow(() -> new ApiExceptionBuilder().badRequest().build());

        SendRequestDto sendRequestDto = SendRequestDto.builder()
                .recipientId(recipientId)
                .key(testSendRequestDto.getKey())
                .medium(testSendRequestDto.getMedium())
                .locale(testSendRequestDto.getLocale())
                .context(testSendRequestDto.getContext())
                .build();

        validateAndSendNotification(sendRequestDto);
        return Response.noContent().build();
    }

    private void validateAndSendNotification(SendRequestDto sendRequestDto) {
        String tenant = RequestContext.ensureGet().getOrg();

        if (StringUtil.isNullOrEmpty(sendRequestDto.getRecipientId())) {
            new ApiExceptionBuilder()
                    .badRequest()
                    .required("recipientId")
                    .buildAndThrow();
        }
        UserPreferences userPreferences = _userPreferencesRepository.findByRecipientId(sendRequestDto.getRecipientId());
        if (userPreferences == null || userPreferences.getRecipient() == null) {
            new ApiExceptionBuilder()
                    .badRequest()
                    .params("recipientId")
                    .buildAndThrow();
        }

        if (sendRequestDto.getMedium() == null) {
            new ApiExceptionBuilder()
                    .badRequest()
                    .required("medium")
                    .buildAndThrow();
        }

        NotificationTemplate template = null;
        if (StringUtil.isNotNullOrEmpty(sendRequestDto.getKey())) {
            template = _templateRepository.findOneByTenantAndKeyAndMediumAndLocale(tenant,
                    sendRequestDto.getKey(),
                    sendRequestDto.getMedium().toString(),
                    sendRequestDto.getLocale() != null ? sendRequestDto.getLocale() : Locale.ENGLISH
            );
            if (template == null) {
                new ApiExceptionBuilder()
                        .badRequest()
                        .buildAndThrow();
            }
        } else {
            switch (sendRequestDto.getMedium()) {
                case EMAIL:
                    if (sendRequestDto.getEmailTemplate() == null) {
                        new ApiExceptionBuilder()
                                .badRequest()
                                .params("emailTemplate")
                                .buildAndThrow();
                    } else if (sendRequestDto.getEmailTemplate().getSubject() == null) {
                        new ApiExceptionBuilder()
                                .badRequest()
                                .params("emailTemplate.subject")
                                .buildAndThrow();
                    }
                    template = NotificationTemplate.newBuilder()
                            .medium(sendRequestDto.getMedium().toString())
                            .locale(sendRequestDto.getLocale() != null ? sendRequestDto.getLocale() : Locale.ENGLISH)
                            .subject(sendRequestDto.getEmailTemplate().getSubject())
                            .header(sendRequestDto.getEmailTemplate().getHeader())
                            .body(sendRequestDto.getEmailTemplate().getBody())
                            .footer(sendRequestDto.getEmailTemplate().getFooter())
                            .replyTo(sendRequestDto.getEmailTemplate().getReplyTo())
                            .from(sendRequestDto.getEmailTemplate().getFrom())
                            .build();
                    break;
                case SLACK:
                    if (sendRequestDto.getSlackTemplate() == null) {
                        new ApiExceptionBuilder()
                                .badRequest()
                                .params("slackTemplate")
                                .buildAndThrow();
                    }
                    template = NotificationTemplate.newBuilder()
                            .medium(sendRequestDto.getMedium().toString())
                            .locale(sendRequestDto.getLocale() != null ? sendRequestDto.getLocale() : Locale.ENGLISH)
                            .slackTemplate(
                                    SlackTemplate.builder()
                                            .text(sendRequestDto.getSlackTemplate().getText())
                                            .blocks(sendRequestDto.getSlackTemplate().getBlocks())
                                            .attachments(sendRequestDto.getSlackTemplate().getAttachments())
                                            .build()
                            )
                            .build();
                    break;
                case TEAMS:
                    if (sendRequestDto.getTeamsTemplate() == null) {
                        new ApiExceptionBuilder()
                                .badRequest()
                                .params("teamsTemplate")
                                .buildAndThrow();
                    }
                    template = NotificationTemplate.newBuilder()
                            .medium(sendRequestDto.getMedium().toString())
                            .locale(sendRequestDto.getLocale() != null ? sendRequestDto.getLocale() : Locale.ENGLISH)
                            .teamsTemplate(
                                    TeamsTemplate.builder()
                                            .title(sendRequestDto.getTeamsTemplate().getTitle())
                                            .text(sendRequestDto.getTeamsTemplate().getText())
                                            .messageJson(sendRequestDto.getTeamsTemplate().getMessageJson())
                                            .build()
                            )
                            .build();
                    break;
                default:
                    new ApiExceptionBuilder()
                            .badRequest()
                            .params("medium")
                            .buildAndThrow();
            }
        }

        Optional<String> brand = userPreferences.getBrand();
        Map<String, Object> globalContext = brand.isPresent() ? _globalContextService.getContext(tenant, brand.get()) :
                _globalContextService.getDefaultContext(tenant);

        Notification notification = _notificationTemplateService
                .renderTemplate(tenant, userPreferences.getRecipient(), template, globalContext, sendRequestDto.getContext());
        notification.setRecipient(userPreferences.getRecipient());

        // Handle Email Override
        String emailOverride = (String) globalContext.get(EMAIL_OVERRIDE);
        //Handle email override
        if (StringUtil.isNotNullOrEmpty(emailOverride) &&
                notification != null && notification instanceof NotificationRendered) {
            String originalRecipient = userPreferences.getRecipient().getEmail();
            Recipient newRecipient = new RecipientBuilder()
                    .withEmail(emailOverride)
                    .build();
            String newSubject =  String.format("[Original recipient: %s] %s", originalRecipient, ((NotificationRendered) notification).getSubject());
            notification = ((NotificationRendered) notification)
                    .derive()
                    .subject(newSubject)
                    .recipient(newRecipient)
                    .build();
            log.info("Redirecting email to " + emailOverride + ". Subject " + newSubject);
        }

        try {
            _senderService.sendNotification(notification);
        } catch (InvalidNotificationException ex) {
            log.error("Sending failed in http handler, exception: ", ex);
            new ApiExceptionBuilder()
                    .badRequest()
                    .buildAndThrow();
        }
    }
}
