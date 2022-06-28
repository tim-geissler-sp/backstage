// Copyright (c) 2020. Sailpoint Technologies, Inc. All rights reserved.
package infra

import (
	"fmt"

	"github.com/sailpoint/atlas-go/atlas/web"
)

// SchedulerFilter is a struct for filter in sp-scheduler
type SchedulerFilter struct {
	Property  string
	Value     interface{}
	Operation web.LogicalOperation
	Filters   []SchedulerFilter
}

// SchedulerFilterBuilder is an implementation for atlas go filter builder
type SchedulerFilterBuilder struct{}

// And is not yet supported in sp-scheduler
func (b SchedulerFilterBuilder) And(filters []web.Filter) (web.Filter, error) {
	return nil, fmt.Errorf("operator AND is unsupported")
}

// Or is not yet supported in sp-scheduler
func (b SchedulerFilterBuilder) Or(filters []web.Filter) (web.Filter, error) {
	return nil, fmt.Errorf("operator OR is unsupported")
}

// Not is not yet supported in sp-scheduler
func (b SchedulerFilterBuilder) Not(filter web.Filter) (web.Filter, error) {
	return nil, fmt.Errorf("operator NOT is unsupported")
}

// NewFilter returns a new filter object from filter property, value and logical operation
func (b SchedulerFilterBuilder) NewFilter(op web.LogicalOperation, property string, valueObject interface{}) (web.Filter, error) {
	return SchedulerFilter{property, valueObject, op, nil}, nil
}

// NewFilterWithMatchMode is not yet supported in sp-scheduler
func (b SchedulerFilterBuilder) NewFilterWithMatchMode(op web.LogicalOperation, property string, valueObject interface{}, mode web.MatchMode) (web.Filter, error) {
	return nil, fmt.Errorf("match modes are not supported")
}

// IgnoreCase is not yet supported in sp-scheduler
func (b SchedulerFilterBuilder) IgnoreCase(filter web.Filter) (web.Filter, error) {
	return nil, fmt.Errorf("case sensitivity is not supported")
}

// NewFilterWithValueList is not yet supported in sp-scheduler
func (b SchedulerFilterBuilder) NewFilterWithValueList(op web.LogicalOperation, property string, valueList []interface{}) (web.Filter, error) {
	return nil, fmt.Errorf("operator IN is not supported")
}
