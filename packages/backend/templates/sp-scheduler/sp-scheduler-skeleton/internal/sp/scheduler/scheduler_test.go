// Copyright (c) 2020. Sailpoint Technologies, Inc. All rights reserved.
package scheduler

import (
	"testing"
)

func TestEventValidation(t *testing.T) {

	podHeaders := make(map[string]string)
	podHeaders["pod"] = "dev"

	// test happy path
	et := Event{
		Topic:   "identity",
		Type:    "IDENTITY_UPDATED",
		Content: make(map[string]interface{}),
		Headers: podHeaders,
	}

	topic, err := et.ValidateTopic()
	if err != nil {
		t.Fatalf("error getting topic: %v", err)
	}

	if topic.ID() != "identity__dev" {
		t.Fatalf("topic ID is incorrect: %s", topic.ID())
	}

	// test pod scope event without pod header
	et = Event{
		Topic:   "identity",
		Type:    "IDENTITY_UPDATED",
		Content: make(map[string]interface{}),
		Headers: make(map[string]string),
	}

	_, err = et.ValidateTopic()
	if err == nil {
		t.Fatalf("failed to detect pod scope event without pod header")
	}

	// test event with invalid topic name
	et = Event{
		Topic:   "doesNotExist",
		Type:    "IDENTITY_UPDATED",
		Content: make(map[string]interface{}),
		Headers: podHeaders,
	}

	_, err = et.ValidateTopic()
	if err == nil {
		t.Fatalf("failed to detect invalid topic names")
	}
}
