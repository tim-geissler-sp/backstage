package com.sailpoint.ets.domain.trigger;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EventSource {
	private final String topic;
	private final String eventType;
}
