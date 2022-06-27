package com.sailpoint.notification.api.event.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class SlackNotificationResponse {
	private boolean _ok;
	private String _warning;
	private String _error;
	private String _channel;
	private String _ts;
}
