package com.sailpoint.notification.api.event.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class TeamsNotificationResponse {
	private boolean _ok;
	private String _warning;
	private String _error;
}
