package com.sailpoint.notification.api.event.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Class define Auto approval attributes.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class TeamsNotificationAutoApprovalData {
	private String _itemId;
	private String _itemType;
	private String _autoApprovalMessageJSON;
	private String _autoApprovalTitle;
}
