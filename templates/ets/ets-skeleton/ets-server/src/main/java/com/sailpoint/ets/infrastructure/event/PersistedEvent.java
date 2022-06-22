/*
 * Copyright (C) 2019 SailPoint Technologies, Inc.  All rights reserved.
 */
package com.sailpoint.ets.infrastructure.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * PersistedEvent
 */
@Entity
@Table(name="event")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersistedEvent {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name="topic")
	private String topic;

	@Column(name="event_json", columnDefinition="TEXT")
	private String eventJson;

}
