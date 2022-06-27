@ignore
Feature: Test Notifications Service: Test all steps.

  Background:
    * url baseUrl
    * def creds = callonce read('classpath:com/sailpoint/util/get-oauth-internal-client.js') {name: 'hermes'}
    * def clientCredentialsTypeResponse = call read('classpath:com/sailpoint/authentication/oauth/oathkeeper/client_credentials_grant_type.feature') {apiUser: '#(creds.clientId)', apiKey: '#(creds.secret)'}
    * def approvalsGeneratedEvent = read('classpath:com/sailpoint/notification/e2e/notification/approvalsGeneratedEvent.json')

  Scenario: E2E to sent kafka event APPROVALS_GENERATED known by interest matcher and verify all phase with default
    Given path 'hermes/interest-matcher/debug/publish/event/full/access_request/APPROVALS_GENERATED'
    And header Authorization = 'Bearer ' + clientCredentialsTypeResponse.response.access_token
    And request approvalsGeneratedEvent
    When method post
    Then status 200
    # Sleep
    * def result = call read('classpath:com/sailpoint/notification/e2e/sleep.js') {interval: 10000}
    * def result = call read('classpath:com/sailpoint/notification/e2e/read-redis.js') {key: '#(response)', token: '#(clientCredentialsTypeResponse.response.access_token)'}

    * def keyForSlackRequest = response + "-request"
    * def requestToSlack = call read('classpath:com/sailpoint/notification/e2e/read-redis.js') {key: '#(keyForSlackRequest)', token: '#(clientCredentialsTypeResponse.response.access_token)'}
    # If this test fails, then checkif HERMES_SLACK_NOTIFICATION_ENABLED is turned on.
    # printing requestToSlack object, if needed for debugging
    * print "Request to Slack ", requestToSlack
    * match requestToSlack contains { "attachments":""}
    * match requestToSlack.recipient contains { "id":"2c91808d757b9e8401758ef268b67554" }
    * match requestToSlack contains { "text":"Request Submitted" }

    # result from slack service, if needed for future enhancements of test or local debuggging
    * print "Result from Posting Domain Event= ", result
