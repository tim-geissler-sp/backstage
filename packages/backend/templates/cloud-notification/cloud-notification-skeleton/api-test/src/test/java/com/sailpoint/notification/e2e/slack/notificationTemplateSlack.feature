Feature: Test Phase: Template Rendering for Slack Medium

  Background:
    * url baseUrl
    * def creds = callonce read('classpath:com/sailpoint/util/get-oauth-internal-client.js') {name: 'hermes'}
    * def clientCredentialsTypeResponse = call read('classpath:com/sailpoint/authentication/oauth/oathkeeper/client_credentials_grant_type.feature') {apiUser: '#(creds.clientId)', apiKey: '#(creds.secret)'}
    * header Authorization = 'Bearer ' + clientCredentialsTypeResponse.response.access_token
    * def SlackEventDefault = read('classpath:com/sailpoint/notification/e2e/slack/slackEventDefault.json')
    * def SlackEvent = read('classpath:com/sailpoint/notification/e2e/slack/slackEvent.json')

  Scenario: Validate Slack Default Template rendering event
    Given path 'hermes/notification/template/debug/publish/event/notification/NOTIFICATION_PREFERENCES_MATCHED'
    And request SlackEventDefault
    When method post
    Then status 200
    * def result = call read('classpath:com/sailpoint/notification/e2e/read-redis.js') {key: '#(response)', token: '#(clientCredentialsTypeResponse.response.access_token)'}
    * print result
    * match result contains 'Test text Test blocks Test attachments'

  Scenario: Validate Slack Template rendering event
    Given path 'hermes/notification/template/debug/publish/event/notification/NOTIFICATION_PREFERENCES_MATCHED'
    And request SlackEvent
    When method post
    Then status 200
    * def result = call read('classpath:com/sailpoint/notification/e2e/read-redis.js') {key: '#(response)', token: '#(clientCredentialsTypeResponse.response.access_token)'}
    * print result
    * match result contains 'Request Submitted'
