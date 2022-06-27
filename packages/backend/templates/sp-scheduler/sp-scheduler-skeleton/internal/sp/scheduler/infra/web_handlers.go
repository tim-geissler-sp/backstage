// Copyright (c) 2020. Sailpoint Technologies, Inc. All rights reserved.
package infra

import (
	"encoding/json"
	"fmt"
	"net/http"
	"time"

	mapset "github.com/deckarep/golang-set"
	"github.com/gorilla/mux"
	"github.com/sailpoint/atlas-go/atlas"
	"github.com/sailpoint/atlas-go/atlas/event"
	"github.com/sailpoint/atlas-go/atlas/trace"
	"github.com/sailpoint/atlas-go/atlas/web"
	"github.com/sailpoint/sp-scheduler/internal/sp/scheduler"
	"github.com/sailpoint/sp-scheduler/internal/sp/scheduler/cmd"
)

type timezone struct {
	Location string `json:"location,omitempty"`
	Offset   string `json:"offset,omitempty"`
}

// buildRoutes adds the application's HTTP routes to the server's router.
func (s *SchedulerService) buildRoutes() *mux.Router {
	r := web.NewRouter(web.DefaultAuthenticationConfig(s.TokenValidator))

	r.Handle("/scheduled-actions", s.requireRight("sp:scheduled-action:read",
		s.listActions())).Methods("GET")

	r.Handle("/scheduled-actions", s.requireRight("sp:scheduled-action:create",
		s.createAction())).Methods("POST")

	r.Handle("/scheduled-actions/{id}", s.requireRight("sp:scheduled-action:delete",
		s.deleteAction())).Methods("DELETE")

	r.Handle("/scheduled-actions/bulk-delete", s.requireRight("sp:scheduled-action:delete",
		s.bulkDeleteActions())).Methods("POST")

	return r
}

// listActions is a REST endpoint that allows the user to
// query for a list of actions by meta content.
func (s *SchedulerService) listActions() http.HandlerFunc {

	type output struct {
		Meta         map[string]interface{} `json:"meta"`
		Event        scheduler.Event        `json:"event"`
		Deadline     string                 `json:"deadline"`
		CronTimezone *timezone              `json:"cronTimezone,omitempty"`
		CronString   string                 `json:"cronString"`
		ID           string                 `json:"id"`
		Created      string                 `json:"created"`
	}

	return func(w http.ResponseWriter, r *http.Request) {
		ctx := r.Context()
		rc := atlas.GetRequestContext(ctx)
		queryOptions, err := web.GetQueryOptions(r, mapset.NewSet(), SchedulerFilterBuilder{}, mapset.NewSetWith("meta"))
		if err != nil {
			web.BadRequest(ctx, w, err)
			return
		}

		var meta map[string]interface{}
		if queryOptions.Filters != nil {
			if f, ok := queryOptions.Filters.(SchedulerFilter); ok && f.Operation == web.Eq {
				if unmarshalErr := json.Unmarshal([]byte(fmt.Sprintf("%v", f.Value)), &meta); err != nil {
					web.BadRequest(ctx, w, fmt.Errorf("failed to parse metadata: %w", unmarshalErr))
					return
				}
			} else {
				web.BadRequest(ctx, w, fmt.Errorf("failed to parse metadata"))
				return
			}
		}

		cmd, err := cmd.NewListActions(string(rc.TenantID), meta, queryOptions.Limit, queryOptions.Offset)
		if err != nil {
			web.BadRequest(ctx, w, err)
			return
		}

		actions, err := s.app.ListActions(ctx, *cmd)
		if err != nil {
			web.InternalServerError(ctx, w, err)
			return
		}

		var outputList = make([]output, 0)
		for _, action := range actions {
			out := output{
				Meta:       action.Meta,
				Event:      action.Event,
				Deadline:   action.Deadline.Format(time.RFC3339Nano),
				CronString: action.CronString,
				ID:         action.ID.String(),
				Created:    action.Created.Format(time.RFC3339Nano),
			}
			if action.TimezoneLocation != "" || action.TimezoneOffset != "" {
				out.CronTimezone = &timezone{
					Location: action.TimezoneLocation,
					Offset:   action.TimezoneOffset,
				}
			}
			outputList = append(outputList, out)
		}

		web.WriteJSON(ctx, w, outputList)
	}
}

// bulkDeleteActions is a REST endpoint that allows the user
// to delete all actions by meta content.
func (s *SchedulerService) bulkDeleteActions() http.HandlerFunc {
	type input struct {
		Meta map[string]interface{} `json:"meta"`
	}

	type output struct {
		Count int64 `json:"count"`
	}

	return func(w http.ResponseWriter, r *http.Request) {
		ctx := r.Context()
		decoder := json.NewDecoder(r.Body)

		var jsonInput input
		if err := decoder.Decode(&jsonInput); err != nil {
			web.BadRequest(ctx, w, err)
			return
		}

		rc := atlas.GetRequestContext(ctx)

		cmd, err := cmd.NewBulkDeleteActions(string(rc.TenantID), jsonInput.Meta)
		if err != nil {
			web.BadRequest(ctx, w, err)
			return
		}

		count, err := s.app.BulkDeleteActions(ctx, *cmd)
		if err != nil {
			web.InternalServerError(ctx, w, err)
			return
		}

		out := output{}
		out.Count = count

		web.WriteJSON(ctx, w, &out)
	}
}

// createAction is a REST endpoint that allows the user
// to create a new action.
func (s *SchedulerService) createAction() http.HandlerFunc {

	type input struct {
		Meta         map[string]interface{} `json:"meta"`
		Event        scheduler.Event        `json:"event"`
		Deadline     string                 `json:"deadline"`
		CronTimezone *timezone              `json:"cronTimezone,omitempty"`
		CronString   string                 `json:"cronString"`
	}

	type output struct {
		input

		ID      string `json:"id"`
		Created string `json:"created"`
	}

	return func(w http.ResponseWriter, r *http.Request) {
		ctx := r.Context()
		decoder := json.NewDecoder(r.Body)

		var jsonInput input
		if err := decoder.Decode(&jsonInput); err != nil {
			web.BadRequest(ctx, w, err)
			return
		}

		rc := atlas.GetRequestContext(ctx)

		if jsonInput.Event.Headers == nil {
			jsonInput.Event.Headers = make(map[string]string)
		}

		jsonInput.Event.Headers[event.HeaderKeyPod] = string(rc.Pod)
		jsonInput.Event.Headers[event.HeaderKeyOrg] = string(rc.Org)
		jsonInput.Event.Headers[event.HeaderKeyTenantID] = string(rc.TenantID)

		if tc := trace.GetTracingContext(ctx); tc != nil {
			jsonInput.Event.Headers[event.HeaderKeyRequestID] = string(tc.RequestID)
		}

		// create a zero time and wrap with atlas.Time to use the custom parser
		deadline := atlas.Time(time.Time{})
		if jsonInput.Deadline != "" {
			err := deadline.ParseTime(jsonInput.Deadline)
			if err != nil {
				web.BadRequest(ctx, w, err)
			}
		}

		var location, offset string
		if jsonInput.CronTimezone != nil {
			location = jsonInput.CronTimezone.Location
			offset = jsonInput.CronTimezone.Offset
		}

		cmd, err := cmd.NewCreateAction(string(rc.TenantID), time.Time(deadline), jsonInput.CronString, jsonInput.Event, jsonInput.Meta, location, offset)

		if err != nil {
			web.BadRequest(ctx, w, err)
			return
		}

		action, err := s.app.CreateAction(ctx, *cmd)
		if err != nil {
			web.InternalServerError(ctx, w, err)
			return
		}

		out := output{}
		out.ID = action.ID.String()
		out.Created = action.Created.Format(time.RFC3339Nano)
		out.Event = action.Event
		out.Deadline = action.Deadline.Format(time.RFC3339Nano)
		out.CronString = action.CronString
		out.Meta = action.Meta
		if action.TimezoneLocation != "" || action.TimezoneOffset != "" {
			out.CronTimezone = &timezone{
				Location: action.TimezoneLocation,
				Offset:   action.TimezoneOffset,
			}
		}

		web.WriteJSON(ctx, w, &out)
	}
}

// deleteAction is a REST endpoint that allows the user to delete
// an action by ID.
func (s *SchedulerService) deleteAction() http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		ctx := r.Context()
		rc := atlas.GetRequestContext(ctx)

		vars := mux.Vars(r)
		cmd, err := cmd.NewDeleteAction(string(rc.TenantID), vars["id"])
		if err != nil {
			web.BadRequest(ctx, w, err)
			return
		}

		deleted, err := s.app.DeleteAction(ctx, *cmd)
		if err != nil {
			web.InternalServerError(ctx, w, err)
			return
		}

		if !deleted {
			web.NotFound(ctx, w)
			return
		}

		web.NoContent(w)
	}
}

// requireRight is a middleware function that ensures that the current request
// has the specified right before calling the next handler in the chain.
// Request s that are missing the specified right will be terminated with
// a 402 Forbidden response.
func (s *SchedulerService) requireRight(right string, next http.Handler) http.Handler {
	m := web.RequireRights(s.AccessSummarizer, right)
	return m.Middleware(next)
}
