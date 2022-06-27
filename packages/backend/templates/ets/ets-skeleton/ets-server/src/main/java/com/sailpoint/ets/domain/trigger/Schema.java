package com.sailpoint.ets.domain.trigger;

import java.util.Map;

public interface Schema {

	void processData(Map<String, Object> input);
	Object validate(Map<String, Object> input);
	void validateData(Map<String, Object> input);
}
