/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.sp.identity.event.domain;

import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import lombok.NonNull;
import lombok.Value;

import java.io.IOException;
import java.io.Serializable;

/**
 * TenantId value type.
 */
@Value
@JsonAdapter(TenantId.Adapter.class)
public class TenantId implements Serializable {
	@NonNull String _id;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return _id;
	}

	/**
	 * GSON adapter to serialize as a simple string
	 */
	public static class Adapter extends TypeAdapter<TenantId> {
		/**
		 * {@inheritDoc}
		 */
		@Override
		public void write(JsonWriter out, TenantId value) throws IOException {
			out.value(value.getId());
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public TenantId read(JsonReader in) throws IOException {
			final String id = in.nextString();
			return new TenantId(id);
		}
	}
}
