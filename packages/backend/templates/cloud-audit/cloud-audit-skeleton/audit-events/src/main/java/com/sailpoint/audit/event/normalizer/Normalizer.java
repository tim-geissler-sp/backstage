/*
 * Copyright (C) 2019 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.audit.event.normalizer;

import com.sailpoint.atlas.search.model.event.Event;
import com.sailpoint.audit.event.EventContext;
import com.sailpoint.audit.event.model.EventDescriptor;

public interface Normalizer {

	Event normalize(EventContext context, EventDescriptor eventDescriptor);
}
