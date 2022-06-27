/*
 * Copyright (C) 2022 SailPoint Technologies, Inc.  All rights reserved.
 */

package com.sailpoint.audit.verification;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Date;

/**
 * Class for use with JsonAdapter to serialize a Date into a ISO 8601 offset date time string
 */
public class IsoOffsetDateTimeAdapter extends TypeAdapter<Date> {
	@Override
	public void write(JsonWriter out, Date value) throws IOException {
		DateTimeFormatter df = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
		String isoDate = df.format(value.toInstant().atZone(ZoneId.of("UTC")));
		out.value(isoDate);
	}

	@Override
	public Date read(JsonReader in) throws IOException {
		DateTimeFormatter df = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
		TemporalAccessor temporal = df.parse(in.nextString());

		return Date.from(Instant.from(temporal));
	}
}
