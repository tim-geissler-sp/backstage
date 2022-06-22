@ignore
Feature: Teams notification rendered test

  Background:
    * url baseUrl
    * def creds = callonce read('classpath:com/sailpoint/util/get-oauth-internal-client.js') {name: 'hermes'}
    * def clientCredentialsTypeResponse = call read('classpath:com/sailpoint/authentication/oauth/oathkeeper/client_credentials_grant_type.feature') {apiUser: '#(creds.clientId)', apiKey: '#(creds.secret)'}
    * def TeamsDTOCorrect = { recipient :{id:"2c91808d757b9e8401758ef268b67554",name:null,phone:null,email:null},text:"Hello world from E2E Text",messageJSON:"{\r\n    \"$schema\": \"http:\/\/adaptivecards.io\/schemas\/adaptive-card.json\",\r\n    \"type\": \"AdaptiveCard\",\r\n    \"version\": \"1.2\",\r\n    \"body\": [\r\n      {\r\n        \"type\": \"TextBlock\",\r\n        \"size\": \"default\",\r\n        \"isSubtle\": true,\r\n        \"text\": \"Hello world from Hermes!\",\r\n        \"wrap\": true,\r\n        \"maxLines\": 0\r\n      }\r\n    ]\r\n}",title:"Hello world from E2E Title",notificationKey:null,domainEvent:null}
    * def TeamsDTOInvalid = { recipient :{id:"12345",name:null,phone:null,email:null},text:"Hello world",messageJSON:"{\r\n    \"$schema\": \"http:\/\/adaptivecards.io\/schemas\/adaptive-card.json\",\r\n    \"type\": \"AdaptiveCard\",\r\n    \"version\": \"1.2\",\r\n    \"body\": [\r\n      {\r\n        \"type\": \"TextBlock\",\r\n        \"size\": \"default\",\r\n        \"isSubtle\": true,\r\n        \"text\": \"Hello world!\",\r\n        \"wrap\": true,\r\n        \"maxLines\": 0\r\n      }\r\n    ]\r\n}",title:"Hello world from E2E Title",notificationKey:null,domainEvent:null}
    * header Authorization = 'Bearer ' + clientCredentialsTypeResponse.response.access_token

  Scenario: E2E to send test message and verify it was submitted to Teams
    Given path 'hermes/sender/debug/sendteams'
    And request TeamsDTOCorrect
    When method post
    Then status 200
    * def result = call read('classpath:com/sailpoint/notification/e2e/read-redis.js') {key: '#(response)', token: '#(clientCredentialsTypeResponse.response.access_token)'}
    * print result
    * match result contains { 'text': 'Hello world from E2E Text' }

  Scenario: E2E to send test message with an invalid recipient id and verify the exception
    Given path 'hermes/sender/debug/sendteams'
    And request TeamsDTOInvalid
    When method post
    Then status 200
    * def result = call read('classpath:com/sailpoint/notification/e2e/read-redis.js') {key: '#(response)', token: '#(clientCredentialsTypeResponse.response.access_token)'}
    * print result
    * match result contains { "error":"recipient_not_found" }
