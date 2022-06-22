// Code generated by MockGen. DO NOT EDIT.
// Source: github.com/sailpoint/atlas-go/atlas/event (interfaces: Publisher)

// Package mocks is a generated GoMock package.
package mocks

import (
	context "context"
	reflect "reflect"

	gomock "github.com/golang/mock/gomock"
	event "github.com/sailpoint/atlas-go/atlas/event"
)

// MockPublisher is a mock of Publisher interface.
type MockPublisher struct {
	ctrl     *gomock.Controller
	recorder *MockPublisherMockRecorder
}

// MockPublisherMockRecorder is the mock recorder for MockPublisher.
type MockPublisherMockRecorder struct {
	mock *MockPublisher
}

// NewMockPublisher creates a new mock instance.
func NewMockPublisher(ctrl *gomock.Controller) *MockPublisher {
	mock := &MockPublisher{ctrl: ctrl}
	mock.recorder = &MockPublisherMockRecorder{mock}
	return mock
}

// EXPECT returns an object that allows the caller to indicate expected use.
func (m *MockPublisher) EXPECT() *MockPublisherMockRecorder {
	return m.recorder
}

// BulkPublish mocks base method.
func (m *MockPublisher) BulkPublish(arg0 context.Context, arg1 []event.EventAndTopic) ([]*event.FailedEventAndTopic, error) {
	m.ctrl.T.Helper()
	ret := m.ctrl.Call(m, "BulkPublish", arg0, arg1)
	ret0, _ := ret[0].([]*event.FailedEventAndTopic)
	ret1, _ := ret[1].(error)
	return ret0, ret1
}

// BulkPublish indicates an expected call of BulkPublish.
func (mr *MockPublisherMockRecorder) BulkPublish(arg0, arg1 interface{}) *gomock.Call {
	mr.mock.ctrl.T.Helper()
	return mr.mock.ctrl.RecordCallWithMethodType(mr.mock, "BulkPublish", reflect.TypeOf((*MockPublisher)(nil).BulkPublish), arg0, arg1)
}

// Publish mocks base method.
func (m *MockPublisher) Publish(arg0 context.Context, arg1 event.TopicDescriptor, arg2 *event.Event) error {
	m.ctrl.T.Helper()
	ret := m.ctrl.Call(m, "Publish", arg0, arg1, arg2)
	ret0, _ := ret[0].(error)
	return ret0
}

// Publish indicates an expected call of Publish.
func (mr *MockPublisherMockRecorder) Publish(arg0, arg1, arg2 interface{}) *gomock.Call {
	mr.mock.ctrl.T.Helper()
	return mr.mock.ctrl.RecordCallWithMethodType(mr.mock, "Publish", reflect.TypeOf((*MockPublisher)(nil).Publish), arg0, arg1, arg2)
}

// PublishToTopic mocks base method.
func (m *MockPublisher) PublishToTopic(arg0 context.Context, arg1 event.Topic, arg2 *event.Event) error {
	m.ctrl.T.Helper()
	ret := m.ctrl.Call(m, "PublishToTopic", arg0, arg1, arg2)
	ret0, _ := ret[0].(error)
	return ret0
}

// PublishToTopic indicates an expected call of PublishToTopic.
func (mr *MockPublisherMockRecorder) PublishToTopic(arg0, arg1, arg2 interface{}) *gomock.Call {
	mr.mock.ctrl.T.Helper()
	return mr.mock.ctrl.RecordCallWithMethodType(mr.mock, "PublishToTopic", reflect.TypeOf((*MockPublisher)(nil).PublishToTopic), arg0, arg1, arg2)
}
