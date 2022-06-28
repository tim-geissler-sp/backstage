Feature: Test Phase: Template Rendering for Teams Medium

  Background:
    * url baseUrl
    * def creds = callonce read('classpath:com/sailpoint/util/get-oauth-internal-client.js') {name: 'hermes'}
    * def clientCredentialsTypeResponse = call read('classpath:com/sailpoint/authentication/oauth/oathkeeper/client_credentials_grant_type.feature') {apiUser: '#(creds.clientId)', apiKey: '#(creds.secret)'}
    * header Authorization = 'Bearer ' + clientCredentialsTypeResponse.response.access_token
    * def TeamsEventDefault = read('classpath:com/sailpoint/notification/e2e/teams/teamsEventDefault.json')
    * def TeamsEvent = read('classpath:com/sailpoint/notification/e2e/teams/teamsEvent.json')

  Scenario: Validate Teams Default Template rendering event
    Given path 'hermes/notification/template/debug/publish/event/notification/NOTIFICATION_PREFERENCES_MATCHED'
    And request TeamsEventDefault
    When method post
    Then status 200
    * def result = call read('classpath:com/sailpoint/notification/e2e/read-redis.js') {key: '#(response)', token: '#(clientCredentialsTypeResponse.response.access_token)'}
    * print result
    * match result contains 'Test title Test text Test messageJson'

  Scenario: Validate Teams Template rendering event
    Given path 'hermes/notification/template/debug/publish/event/notification/NOTIFICATION_PREFERENCES_MATCHED'
    And request TeamsEvent
    When method post
    Then status 200
    * def result = call read('classpath:com/sailpoint/notification/e2e/read-redis.js') {key: '#(response)', token: '#(clientCredentialsTypeResponse.response.access_token)'}
    * print result
    * match result contains 'Request Submitted'
