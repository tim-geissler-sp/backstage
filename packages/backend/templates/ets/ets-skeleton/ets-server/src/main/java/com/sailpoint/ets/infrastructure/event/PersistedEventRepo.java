/*
 * Copyright (C) 2019 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.ets.infrastructure.event;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * PersistedEventRepo
 */
@Repository
public interface PersistedEventRepo extends CrudRepository<PersistedEvent, Long> {
}
