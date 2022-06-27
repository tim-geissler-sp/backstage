package com.sailpoint.ets.infrastructure.util;

import com.sailpoint.ets.domain.trigger.Trigger;
import com.sailpoint.ets.domain.trigger.TriggerId;
import com.sailpoint.ets.service.TriggerService;
import org.modelmapper.Converter;
import org.modelmapper.spi.MappingContext;

import java.util.Optional;

/**
 * Utility class for converting TriggerId to TriggerName(String) using triggerRepo.
 */
public class TriggerIdToTriggerNameConverter implements Converter<TriggerId, String> {

	TriggerService _triggerService;

	public TriggerIdToTriggerNameConverter(TriggerService triggerService) {
		this._triggerService = triggerService;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String convert(MappingContext<TriggerId, String> context) {
		Optional<Trigger> trigger =  _triggerService.findByTriggerId(context.getSource());
		if(trigger.isPresent() && trigger.get().getName() != null){
				return trigger.get().getName().toString();
		}
		return null;
	}
}
