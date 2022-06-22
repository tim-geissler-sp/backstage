Feature: Test Phase: Template Rendering

  Background:
    * url baseUrl
    * def creds = callonce read('classpath:com/sailpoint/util/get-oauth-internal-client.js') {name: 'hermes'}
    * def clientCredentialsTypeResponse = call read('classpath:com/sailpoint/authentication/oauth/oathkeeper/client_credentials_grant_type.feature') {apiUser: '#(creds.clientId)', apiKey: '#(creds.secret)'}
    * header Authorization = 'Bearer ' + clientCredentialsTypeResponse.response.access_token

  Scenario: Validate Template rendering event
    Given path 'hermes/notification/template/debug/publish/event/notification/NOTIFICATION_PREFERENCES_MATCHED'
    And request {"domainEvent": { "headers": { "pod": "dev", "org": "acme-solar" }, "id": "680ee45292df47fa858d0925023d3868", "timestamp": "2018-11-13T10:21:00.668-06:00", "type": "ACCESS_APPROVAL_REQUESTED", "content" : { "approvers": [{ "id": "314cf125-f892-4b16-bcbb-bfe4afb01f85", "name": "james.smith" }, { "id": "70e7cde5-3473-46ea-94ea-90bc8c605a6c", "name": "jane.doe" }], "requester_id": "46ec3058-eb0a-41b2-8df8-1c3641e4d771", "requester_name": "boss.man", "accessItems": [{ "type": "ROLE", "name": "Engineering Administrator" }] } }, "recipient": { "id": "70e7cde5-3473-46ea-94ea-90bc8c605a6c", "name": "james.smith", "email": "james.smith@sailpoint.com", "phone": "512-888-8888" }, "medium": "email", "notificationKey": "notificationKey"}
    When method post
    Then status 200
    * def result = callonce read('classpath:com/sailpoint/notification/e2e/read-redis.js') {key: '#(response)', token: '#(clientCredentialsTypeResponse.response.access_token)'}
    * print result
    * match result contains 'Welcome james.smith'

  Scenario: Validate Template rendering event with email override
    #Todo Test currently publishes the EMAIL_REDIRECTION_ENABLED event to the notification topic. When available in the future make a CC API call instead, which will publish the required event.
    Given path 'hermes/context/debug/publish/event/notification/EMAIL_REDIRECTION_ENABLED'
    And request 'test@forward.com'
    When method post
    Then status 200
    * def result = call read('classpath:com/sailpoint/notification/e2e/read-redis.js') {key: '#(response)', token: '#(clientCredentialsTypeResponse.response.access_token)'}
    * match result contains 'test@forward.com'
    * header Authorization = 'Bearer ' + clientCredentialsTypeResponse.response.access_token
    Given path 'hermes/notification/template/debug/publish/event/notification/NOTIFICATION_PREFERENCES_MATCHED'
    And request {"domainEvent": { "headers": { "pod": "dev", "org": "acme-solar" }, "id": "680ee45292df47fa858d0925023d3868", "timestamp": "2018-11-13T10:21:00.668-06:00", "type": "ACCESS_APPROVAL_REQUESTED", "content" : { "approvers": [{ "id": "314cf125-f892-4b16-bcbb-bfe4afb01f85", "name": "james.smith" }, { "id": "70e7cde5-3473-46ea-94ea-90bc8c605a6c", "name": "jane.doe" }], "requester_id": "46ec3058-eb0a-41b2-8df8-1c3641e4d771", "requester_name": "boss.man", "accessItems": [{ "type": "ROLE", "name": "Engineering Administrator" }] } }, "recipient": { "id": "70e7cde5-3473-46ea-94ea-90bc8c605a6c", "name": "james.smith", "email": "james.smith@sailpoint.com", "phone": "512-888-8888" }, "medium": "email", "notificationKey": "notificationKey"}
    When method post
    Then status 200
    * def result2 = call read('classpath:com/sailpoint/notification/e2e/read-redis.js') {key: '#(response)', token: '#(clientCredentialsTypeResponse.response.access_token)'}
    * match result2 contains '[Original recipient: james.smith@sailpoint.com]'
    * header Authorization = 'Bearer ' + clientCredentialsTypeResponse.response.access_token
    #See above comment
    Given path 'hermes/context/debug/publish/event/notification/EMAIL_REDIRECTION_DISABLED'
    And request ''
    When method post
    Then status 200
    * def result = call read('classpath:com/sailpoint/notification/e2e/read-redis.js') {key: '#(response)', token: '#(clientCredentialsTypeResponse.response.access_token)'}
    * match result !contains { 'emailOverride': '#notnull' }
