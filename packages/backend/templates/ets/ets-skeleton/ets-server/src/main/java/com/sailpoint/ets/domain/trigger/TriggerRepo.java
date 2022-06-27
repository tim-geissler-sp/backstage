/*
 * Copyright (C) 2019 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.domain.trigger;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * TriggerRepo
 */
public interface TriggerRepo {

	Stream<Trigger> findAll();
	Optional<Trigger> findById(TriggerId id);
	Optional<TriggerId> findIdByEventSource(String topic, String type);

}
