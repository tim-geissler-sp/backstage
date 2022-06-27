Feature: Test Template Versions REST API

  Background:
    * url baseUrl
    * def creds = callonce read('classpath:com/sailpoint/util/get-oauth-internal-client.js') {name: 'hermes'}
    * def clientCredentialsTypeResponse = call read('classpath:com/sailpoint/authentication/oauth/oathkeeper/client_credentials_grant_type.feature') {apiUser: '#(creds.clientId)', apiKey: '#(creds.secret)'}

  Scenario: Verify version API not enabled.
    Given path '/beta/notification-template-versions'
    And header Content-Type = 'application/json'
    * header Authorization = 'Bearer ' + clientCredentialsTypeResponse.response.access_token
    And request {}
    When method GET
    Then status 404
