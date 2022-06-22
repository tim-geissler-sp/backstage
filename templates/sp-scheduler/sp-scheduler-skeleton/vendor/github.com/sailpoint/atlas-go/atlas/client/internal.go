// Copyright (c) 2020. Sailpoint Technologies, Inc. All rights reserved.
package client

import (
	"fmt"
	"github.com/sailpoint/atlas-go/atlas/beacon"
	"net/http"
	"strings"
	"sync"

	"github.com/sailpoint/atlas-go/atlas"
	"github.com/sailpoint/atlas-go/atlas/config"
	"github.com/sailpoint/atlas-go/atlas/crypto"
)

// Ensures that the DefaultServiceLocator implements the ServiceLocator interface
var _ ServiceLocator = (*DefaultServiceLocator)(nil)

// ServiceLocator is an interface for getting the URL for the specified service.
type ServiceLocator interface {
	GetURL(org atlas.Org, service string) string
}

// DefaultServiceLocator uses a simple pattern with string replacement for service location.
// Pattern is a string with $org and $service parameters that are replaced.
// Example Pattern: "https://$org.$service.services.sailpoint.com"
type DefaultServiceLocator struct {
	Pattern string
}

// BaseURLProvider is an interface for retrieving the base API url for a given org.
type BaseURLProvider interface {
	GetBaseURL(org atlas.Org) string
}

// DefaultBaseURLProvider is an implementation of BaseURLProvider that uses a printf-style
// pattern for building the base url:
// Example Pattern: "https://%s.api.cloud.sailpoint.com", where %s will be replace with
// the name of the org.
type DefaultBaseURLProvider struct {
	Pattern string
}

// InternalCredentialsProvider is an interface for types that provide credentials
// for service-to-service API calls.
type InternalCredentialsProvider interface {
	GetCredentials() (id string, secret string)
}

// DefaultInternalCredentialsProvider gets credentials for service-to-service API calls.
type DefaultInternalCredentialsProvider struct {
	id     string
	secret string
}

// InternalClientProvider is an interface for types that return an HTTP client
// for service-to-service API calls that automatically manages authentication.
type InternalClientProvider interface {
	GetInternalClient(tenantID atlas.TenantID, org atlas.Org) *http.Client
}

// DefaultInternalClientProvider is the default implementation of InternalClientProvider
// that caches clients per tenant.
type DefaultInternalClientProvider struct {
	stack               string
	serviceLocator      ServiceLocator
	credentialsProvider InternalCredentialsProvider
	baseURLProvider     BaseURLProvider

	mu    sync.RWMutex
	cache map[string]*http.Client
}

// NewBaseURLProvider constructs a new BaseURLProvider with configuration read from the specified source.
func NewBaseURLProvider(cfg config.Source) *DefaultBaseURLProvider {
	p := &DefaultBaseURLProvider{}
	p.Pattern = config.GetString(cfg, "ATLAS_BASE_URL_PATTERN", "https://%s.api.cloud.sailpoint.com")

	return p
}

// GetBaseURL retrieves the base URL for the specified org.
func (p *DefaultBaseURLProvider) GetBaseURL(org atlas.Org) string {
	return fmt.Sprintf(p.Pattern, string(org))
}

// BuildAPIURL constructs an API URL using by appending the specified path
// to the BaseURL generated by the provider.
func BuildAPIURL(provider BaseURLProvider, org atlas.Org, path string) string {
	baseURL := provider.GetBaseURL(org)
	if !strings.HasSuffix(baseURL, "/") {
		baseURL += "/"
	}

	return baseURL + path
}

// NewDefaultServiceLocator constructs a new ServiceLocator instance using a pattern read from
// a configuration source.
func NewDefaultServiceLocator(cfg config.Source) *DefaultServiceLocator {
	s := &DefaultServiceLocator{}
	s.Pattern = config.GetString(cfg, "SERVICE_LOCATION_PATTERN", "https://$org.$service.services.infra.identitysoon.com")

	return s
}

// NewServiceLocator constructs a new Decorated ServiceLocator depending on config
func NewServiceLocator(cfg config.Source, beaconRegistrar beacon.Registrar) ServiceLocator {
	defaultServiceLocator := NewDefaultServiceLocator(cfg)
	if beaconRegistrar != nil && config.GetBool(cfg, "BEACON_ENABLED", false) {
		return NewBeaconServiceLocator(defaultServiceLocator, beaconRegistrar)
	}

	return defaultServiceLocator
}

// GetURL returns the url for the specified service by performing string replacement
// of variables in the pattern.
func (s *DefaultServiceLocator) GetURL(org atlas.Org, service string) string {
	url := strings.ReplaceAll(s.Pattern, "$org", string(org))
	url = strings.ReplaceAll(url, "$service", service)

	return url
}

// NewInternalCredentialsProvider constructs a new provider using the current stack and JWT signing
// key to derive the id/secret.
func NewInternalCredentialsProvider(stack string, signingKey []byte) *DefaultInternalCredentialsProvider {
	p := &DefaultInternalCredentialsProvider{}
	p.id = "service_" + strings.ToLower(stack)
	p.secret = crypto.HashToHexString(signingKey, []byte(p.id))

	return p
}

// GetCredentials returns the pre-computed id/secret.
func (p *DefaultInternalCredentialsProvider) GetCredentials() (id string, secret string) {
	id = p.id
	secret = p.secret

	return
}

// NewInternalClientProvider constructs a new InternalClientProvider.
func NewInternalClientProvider(stack string, baseURLProvider BaseURLProvider, serviceLocator ServiceLocator, credentialsProvider InternalCredentialsProvider) *DefaultInternalClientProvider {
	p := &DefaultInternalClientProvider{}
	p.stack = stack
	p.serviceLocator = serviceLocator
	p.credentialsProvider = credentialsProvider
	p.baseURLProvider = baseURLProvider
	p.cache = make(map[string]*http.Client)

	return p
}

// GetInternalClient gets an HTTP client for the specified tenant that automatically manages authentication.
// This client assumes service-to-service API calls.
func (p *DefaultInternalClientProvider) GetInternalClient(tenantID atlas.TenantID, org atlas.Org) *http.Client {
	c := p.cacheRead(tenantID)
	if c != nil {
		return c
	}

	return p.cacheLoad(tenantID, org)
}

// cacheRead gets a client from the internal cache.
func (p *DefaultInternalClientProvider) cacheRead(tenantID atlas.TenantID) *http.Client {
	p.mu.RLock()
	defer p.mu.RUnlock()

	return p.cache[string(tenantID)]
}

// cacheLoad loads a client into the cache.
func (p *DefaultInternalClientProvider) cacheLoad(tenantID atlas.TenantID, org atlas.Org) *http.Client {
	p.mu.Lock()
	defer p.mu.Unlock()

	c := p.cache[string(tenantID)]
	if c != nil {
		return c
	}

	id, secret := p.credentialsProvider.GetCredentials()

	config := Config{
		Stack:        p.stack,
		ClientID:     id,
		ClientSecret: secret,
		TokenURL:     BuildAPIURL(p.baseURLProvider, org, "oauth/token"),
	}

	c = New(config)
	p.cache[string(tenantID)] = c

	return c

}