/*
 * Copyright (c) 2021. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.sender.common.service;

import com.sailpoint.notification.api.event.dto.Notification;
import com.sailpoint.notification.api.event.dto.NotificationRendered;
import com.sailpoint.notification.api.event.dto.SlackNotificationRendered;
import com.sailpoint.notification.api.event.dto.TeamsNotificationRendered;
import com.sailpoint.notification.sender.common.exception.InvalidNotificationException;
import com.sailpoint.notification.sender.email.service.MailService;
import com.sailpoint.notification.sender.slack.service.SlackService;
import com.sailpoint.notification.sender.teams.service.TeamsService;
import lombok.RequiredArgsConstructor;

import javax.inject.Inject;

/**
 * SenderService
 */
@RequiredArgsConstructor(onConstructor_={@Inject})
public class SenderService {

    private final MailService _mailService;
    private final SlackService _slackService;
    private final TeamsService _teamsService;

    /**
     * Sends a notification
     * @param notification The notification to send
     */
    public void sendNotification(Notification notification) throws InvalidNotificationException {
        if (notification == null) {
            throw new IllegalStateException();
        }

        if (notification instanceof NotificationRendered) {
            _mailService.sendMail((NotificationRendered)notification);
        } else if (notification instanceof SlackNotificationRendered) {
            _slackService.sendSlackNotifications((SlackNotificationRendered)notification, null);
        } else if (notification instanceof TeamsNotificationRendered) {
            _teamsService.sendTeamsNotifications((TeamsNotificationRendered)notification, null);
        }
    }
}
