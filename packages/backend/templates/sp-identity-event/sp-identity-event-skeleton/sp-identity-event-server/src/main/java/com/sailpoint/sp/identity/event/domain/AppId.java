package com.sailpoint.sp.identity.event.domain;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class AppId {
	@NonNull String _id;
	@NonNull String _name;

	@Override
	public String toString() {
		return _id;
	}
}
