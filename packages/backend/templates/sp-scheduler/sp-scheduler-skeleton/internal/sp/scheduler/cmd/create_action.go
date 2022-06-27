// Copyright (c) 2020. Sailpoint Technologies, Inc. All rights reserved.
package cmd

import (
	"context"
	"fmt"
	"time"

	"github.com/gorhill/cronexpr"

	"github.com/sailpoint/atlas-go/atlas/log"
	"github.com/sailpoint/sp-scheduler/internal/sp/scheduler"
	"github.com/sailpoint/sp-scheduler/internal/sp/scheduler/util"
)

// CreateAction is a command that schedules a new action to be fired at a certain deadline.
type CreateAction struct {
	tenantID         scheduler.TenantID
	deadline         time.Time
	cronString       string
	event            scheduler.Event
	meta             map[string]interface{}
	timezoneLocation string
	timezoneOffset   string
}

// NewCreateAction constructs a new command, returning an error if validation fails.
func NewCreateAction(tenantID string, deadline time.Time, cronString string, event scheduler.Event, meta map[string]interface{}, timezoneLocation string, timezoneOffset string) (*CreateAction, error) {

	cmd := &CreateAction{}
	if tenantID == "" {
		return nil, fmt.Errorf("tenantID is required: %w", scheduler.ErrInvalidInput)
	}
	cmd.tenantID = scheduler.TenantID(tenantID)

	if event.Topic == "" {
		return nil, fmt.Errorf("eventTopic is required: %w", scheduler.ErrInvalidInput)
	}

	if event.Type == "" {
		return nil, fmt.Errorf("eventType is required: %w", scheduler.ErrInvalidInput)
	}

	_, err := event.ValidateTopic()
	if err != nil {
		return nil, err
	}
	cmd.event = event

	// if both cron and deadline are non-existent, we don't know when to schedule, return 4xx
	if cronString == "" && deadline.IsZero() {
		return nil, fmt.Errorf("must provide either cronstring or deadline: %w", scheduler.ErrInvalidInput)
	}

	// save either timezone Location or the offset, but not both
	if err := setupTimezoneInfo(cmd, timezoneLocation, timezoneOffset); err != nil {
		return nil, err
	}

	nextDeadline := deadline.UTC()

	// if the cron string is present, check if valid cron string,
	// and if the deadline is not present, set the next deadline per cron
	if cronString != "" {
		expr, err := cronexpr.Parse(cronString)
		if err != nil {
			return nil, fmt.Errorf("malformed cronstring: %w", scheduler.ErrInvalidInput)
		}
		if nextDeadline.IsZero() {
			nextDeadline, err = util.GetNextCronValueUsingTimezone(time.Now().UTC(), cmd.timezoneLocation, cmd.timezoneOffset, expr)
			if err != nil {
				return nil, fmt.Errorf("could not parse associated timezone info: %w", scheduler.ErrInvalidInput)
			}
		}
	}

	cmd.deadline = nextDeadline
	cmd.cronString = cronString
	cmd.meta = meta

	return cmd, nil
}

// Setups timezone by giving priority to the timezone location.  If location information isn't present, then
// use offset.  If both are present, then return an error, since we only allow one or the other
func setupTimezoneInfo(cmd *CreateAction, timezoneLocation string, timezoneOffset string) error {
	if timezoneLocation != "" && timezoneOffset != "" {
		return fmt.Errorf("either timezone location or timezone offset can be provided, but not both")
	}

	if timezoneLocation != "" {
		var loc time.Location
		loc = *time.UTC

		if loadedLocation, err := time.LoadLocation(timezoneLocation); err == nil {
			loc = *loadedLocation
		} else {
			return fmt.Errorf("cannot use '%s' as a timezone location -- see the IANA Time Zone database for valid values: err %v", timezoneLocation, err)
		}

		cmd.timezoneLocation = loc.String()
		cmd.timezoneOffset = ""

	} else if timezoneOffset != "" {
		if util.TimezoneOffsetRegEx.MatchString(timezoneOffset) {

			cmd.timezoneOffset = timezoneOffset
			cmd.timezoneLocation = ""
		} else {
			return fmt.Errorf("timezone offset must start with a '+' or '-' and be of the form dd:dd")
		}
	}
	return nil
}

// Handle creates the new action
func (cmd *CreateAction) Handle(ctx context.Context, repo scheduler.ActionRepo) (*scheduler.Action, error) {
	a := scheduler.NewAction(cmd.tenantID, cmd.deadline, cmd.timezoneLocation, cmd.timezoneOffset, cmd.cronString, cmd.event, cmd.meta)

	if err := repo.Save(ctx, a); err != nil {
		return nil, err
	}

	log.Infof(ctx, "scheduled action to publish event of type '%s' to topic '%s' on %s", a.Event.Type, a.Event.Topic, a.Deadline)

	return a, nil
}
