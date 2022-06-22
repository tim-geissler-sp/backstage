Feature: Test Notifications Service: Test all steps.

  Background:
    * url baseUrl
    * def creds = callonce read('classpath:com/sailpoint/util/get-oauth-internal-client.js') {name: 'hermes'}
    * def clientCredentialsTypeResponse = call read('classpath:com/sailpoint/authentication/oauth/oathkeeper/client_credentials_grant_type.feature') {apiUser: '#(creds.clientId)', apiKey: '#(creds.secret)'}
    * def TemplateBulkDeleteDto = [{ "key": "cloud_manual_tasks" }]
    * header Authorization = 'Bearer ' + clientCredentialsTypeResponse.response.access_token

  Scenario: E2E send to kafka event MANUAL_WORK_ITEM_SUMMARY known by interest matcher and verify all phase with cloud_manual_work_item_summary template

    #clean up default brand first in case if any issues with previews test
    Given path 'hermes/context/debug/publish/event/branding/BRANDING_DELETED'
    And request '"default"'
    When method post
    Then status 200
    * def result = call read('classpath:com/sailpoint/notification/e2e/read-redis.js') {key: '#(response)', token: '#(clientCredentialsTypeResponse.response.access_token)'}
    * print result
    * match result == 'default'

    #Tenant opt in
    * header Authorization = 'Bearer ' + clientCredentialsTypeResponse.response.access_token
    Given path 'hermes/v3/notification-preferences/cloud_manual_work_item_summary'
    And request {key:"cloud_manual_work_item_summary", "mediums":["EMAIL"]}
    When method put
    Then status 200

    * header Authorization = 'Bearer ' + clientCredentialsTypeResponse.response.access_token
    Given path 'hermes/user/preferences/debug/publish/event/identity_event/IdentityCreatedEvent'
    And request {"identity":{"id":"be49f789-ff7c-4411-84f1-32b2741247bf","name":"New Test User","type":"IDENTITY"},"attributes":{"firstname":"vasil","last name":"shlapkou","email":"vasil.shlapkou@sailpoint.com","displayName":"New Test User","phone":"5125125125","brand":"default"}}
    When method post
    Then status 200
    * def result = call read('classpath:com/sailpoint/notification/e2e/read-redis.js') {key: '#(response)', token: '#(clientCredentialsTypeResponse.response.access_token)'}
    * print result
    * match result contains 'be49f789-ff7c-4411-84f1-32b2741247bf'

    #Send debug MANUAL_WORK_ITEM_SUMMARY event and verify result email
    * header Authorization = 'Bearer ' + clientCredentialsTypeResponse.response.access_token
    Given path 'hermes/interest-matcher/debug/publish/event/full/manual_work_item/MANUAL_WORK_ITEM_SUMMARY'
    And request {"ownerId":"be49f789-ff7c-4411-84f1-32b2741247bf","numberOfPendingTasks":2}
    When method post
    Then status 200
    * def result = call read('classpath:com/sailpoint/notification/e2e/read-redis.js') {key: '#(response)', token: '#(clientCredentialsTypeResponse.response.access_token)'}
    * print result
    * match result.fromAddress contains "no-reply@sailpoint.com"
    * match result.fromAddress contains "no-reply@sailpoint.com"
    * match result.toAddress contains "vasil.shlapkou@sailpoint.com"
    * match result.subject contains "You have 2 tasks to complete in IdentityNow."
    * match result.html contains "New Test User"

    #Tenant opt back out
    * header Authorization = 'Bearer ' + clientCredentialsTypeResponse.response.access_token
    Given path 'hermes/v3/notification-preferences/cloud_manual_work_item_summary'
    And request {key:"cloud_manual_work_item_summary", "mediums":[]}
    When method put
    Then status 200

    #Verify opt out
    * header Authorization = 'Bearer ' + clientCredentialsTypeResponse.response.access_token
    Given path 'hermes/v3/notification-preferences/cloud_manual_work_item_summary'
    When method get
    Then status 200
    * def mediums = response.mediums
    * print mediums
    * match mediums == []

    #Resend debug MANUAL_WORK_ITEM_SUMMARY event and verify email doesn't go out
    * header Authorization = 'Bearer ' + clientCredentialsTypeResponse.response.access_token
    Given path 'hermes/interest-matcher/debug/publish/event/full/manual_work_item/MANUAL_WORK_ITEM_SUMMARY'
    And request {"ownerId":"be49f789-ff7c-4411-84f1-32b2741247bf","numberOfPendingTasks":2}
    When method post
    Then status 200
    * def result = call read('classpath:com/sailpoint/notification/e2e/read-redis.js') {key: '#(response)', token: '#(clientCredentialsTypeResponse.response.access_token)'}
    * match result contains -1

    #Delete new test user
    * header Authorization = 'Bearer ' + clientCredentialsTypeResponse.response.access_token
    Given path 'hermes/user/preferences/debug/publish/event/identity_event/IdentityDeletedEvent'
    And request {"identity":{"id":"be49f789-ff7c-4411-84f1-32b2741247bf","name":"New Test User","type":"IDENTITY"},"attributes":{"firstname":"Jane","last name":"Doe","email":"jane.doe@sailpoint.com","displayName":"New Test User","phone":"5125125125","brand":"default"}}
    When method post
    Then status 200
    * def result = call read('classpath:com/sailpoint/notification/e2e/read-redis.js') {key: '#(response)', token: '#(clientCredentialsTypeResponse.response.access_token)'}
    * print result
    * match result contains 'be49f789-ff7c-4411-84f1-32b2741247bf'
