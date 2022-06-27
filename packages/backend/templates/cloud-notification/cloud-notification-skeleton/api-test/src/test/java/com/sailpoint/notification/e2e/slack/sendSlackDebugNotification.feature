@dev
@ignore
Feature: Slack notification rendered test

  Background:
    * url baseUrl
    * def creds = callonce read('classpath:com/sailpoint/util/get-oauth-internal-client.js') {name: 'hermes'}
    * def clientCredentialsTypeResponse = call read('classpath:com/sailpoint/authentication/oauth/oathkeeper/client_credentials_grant_type.feature') {apiUser: '#(creds.clientId)', apiKey: '#(creds.secret)'}
    * def SlackDTOCorrect = { recipient :{id:"2c91808d757b9e8401758ef268b67554",name:null,phone:null,email:null},text:"Hello world from E2E",blocks:"[{'type': 'section', 'text': {'type': 'plain_text', 'text': 'Hello world from Hermes'}}]",attachments:"[{'pretext': 'pre-hello', 'text': 'text-world'}]",notificationKey:null,domainEvent:null}
    * def SlackDTOInvalid = { recipient :{id:"12345",name:null,phone:null,email:null},text:"Hello world",blocks:"[{'type': 'section', 'text': {'type': 'plain_text', 'text': 'Hello world'}}]",attachments:"[{'pretext': 'pre-hello', 'text': 'text-world'}]",notificationKey:null,domainEvent:null}
    * header Authorization = 'Bearer ' + clientCredentialsTypeResponse.response.access_token

  Scenario: E2E to send test message and verify it was submitted to Slack
    Given path 'hermes/sender/debug/sendslack'
    And request SlackDTOCorrect
    When method post
    Then status 200
    * def result = call read('classpath:com/sailpoint/notification/e2e/read-redis.js') {key: '#(response)', token: '#(clientCredentialsTypeResponse.response.access_token)'}
    * print result
    * match result contains { 'text': 'Hello world from E2E' }

  Scenario: E2E to send test message with an invalid recipient id and verify the exception
    Given path 'hermes/sender/debug/sendslack'
    And request SlackDTOInvalid
    When method post
    Then status 200
    * def result = call read('classpath:com/sailpoint/notification/e2e/read-redis.js') {key: '#(response)', token: '#(clientCredentialsTypeResponse.response.access_token)'}
    * print result
    * match result contains { "error":"recipient_not_found" }
