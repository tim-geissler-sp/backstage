@prod
Feature: Test Phase: Interest Matched

  Background:
    * url baseUrl
    * def passwordTypeResponse = callonce read('classpath:com/sailpoint/authentication/oauth/oathkeeper/password_grant_type.feature')
    * def fromAddress = 'no-reply-e2e-test@identitysoon.com'

  Scenario: E2E to send kafka event MANUAL_TASKS known by interest matcher and email was rendered with context
    #clean up default brand first in case if any issues with previews test
    * header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    Given path 'hermes/context/debug/publish/event/branding/BRANDING_DELETED'
    And request '"default"'
    When method post
    Then status 200
    * def result = call read('classpath:com/sailpoint/notification/e2e/read-redis.js') {key: '#(response)', token: '#(passwordTypeResponse.response.access_token)'}
    * print result
    * match result == 'default'
    #verify branding create event
    * header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    Given path 'hermes/context/debug/publish/event/branding/BRANDING_CREATED'
    And request {"name":"brand", "productName":"Acme Solar Flare Company", "emailFromAddress":"#(fromAddress)"}
    When method post
    Then status 200
    * def result = call read('classpath:com/sailpoint/notification/e2e/read-redis.js') {key: '#(response)', token: '#(passwordTypeResponse.response.access_token)'}
    * print result
    * match result.attributes.brandingConfigs.brand.productName contains 'Acme Solar Flare Company'
    * header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    Given path 'hermes/interest-matcher/debug/publish/event/full/notification/MANUAL_TASKS_EVENT'
    And request {"task":"finish your homework", "recipient":{"id":"2c928090672c901401672c92d33c02ef","name":null,"phone":null,"email":"vasil.shlapkou@sailpoint.com"},"notificationKey":"cloud_manual_tasks","orgId":3,"org":"acme-solar","isTemplateEvaluated":false,"requestId":"e27b0172-94eb-4000-b9b3-6e603de96374"}
    When method post
    Then status 200
    * def result = call read('classpath:com/sailpoint/notification/e2e/read-redis.js') {key: '#(response)', token: '#(passwordTypeResponse.response.access_token)'}
    * print result
    * match result.html contains 'finish your homework'
    * match result.subject contains 'IdentityNow'
    #verify branding delete event
    * header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    Given path 'hermes/context/debug/publish/event/branding/BRANDING_DELETED'
    And request '"brand"'
    When method post
    Then status 200
    * def result = call read('classpath:com/sailpoint/notification/e2e/read-redis.js') {key: '#(response)', token: '#(passwordTypeResponse.response.access_token)'}
    * print result
    * match result == 'brand'

  Scenario: E2E to send kafka event MANUAL_TASKS known by interest matcher and email was rendered with context and default branding
    #clean up default brand first in case if any issues with previews test
    * header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    Given path 'hermes/context/debug/publish/event/branding/BRANDING_DELETED'
    And request '"default"'
    When method post
    Then status 200
    * def result = call read('classpath:com/sailpoint/notification/e2e/read-redis.js') {key: '#(response)', token: '#(passwordTypeResponse.response.access_token)'}
    * print result
    * match result == 'default'
    #verify default branding create event
    * header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    Given path 'hermes/context/debug/publish/event/branding/BRANDING_CREATED'
    And request {"name":"default", "productName":"Default Company Name", "emailFromAddress":"#(fromAddress)"}
    When method post
    Then status 200
    * def result = call read('classpath:com/sailpoint/notification/e2e/read-redis.js') {key: '#(response)', token: '#(passwordTypeResponse.response.access_token)'}
    * print result
    * match result.attributes.brandingConfigs.default.productName contains 'Default Company Name'
    * header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    Given path 'hermes/interest-matcher/debug/publish/event/full/notification/MANUAL_TASKS_EVENT'
    And request {"task":"test default brand", "recipient":{"id":"2c928090672c901401672c92d33c02ee","name":null,"phone":null,"email":"vasil.shlapkou@sailpoint.com"},"notificationKey":"cloud_manual_tasks","orgId":3,"org":"acme-solar","isTemplateEvaluated":false,"requestId":"f27b0172-94eb-4000-b9b3-6e603de96377"}
    When method post
    Then status 200
    * def result = call read('classpath:com/sailpoint/notification/e2e/read-redis.js') {key: '#(response)', token: '#(passwordTypeResponse.response.access_token)'}
    * print result
    * match result.html contains 'test default brand'
    * match result.subject contains 'Default Company Name'
    * match result.fromAddress contains '#(fromAddress)'
   #verify default delete event
    * header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    Given path 'hermes/context/debug/publish/event/branding/BRANDING_DELETED'
    And request '"default"'
    When method post
    Then status 200
    * def result = call read('classpath:com/sailpoint/notification/e2e/read-redis.js') {key: '#(response)', token: '#(passwordTypeResponse.response.access_token)'}
    * print result
    * match result == 'default'

  Scenario: E2E to send kafka event MANUAL_TASKS known by interest matcher and email was rendered with context and default branding without email
    #clean up default brand first in case if any issues with previews test
    * header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    Given path 'hermes/context/debug/publish/event/branding/BRANDING_DELETED'
    And request '"default"'
    When method post
    Then status 200
    * def result = call read('classpath:com/sailpoint/notification/e2e/read-redis.js') {key: '#(response)', token: '#(passwordTypeResponse.response.access_token)'}
    * print result
    * match result == 'default'
    #verify default branding create event
   * header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
   Given path 'hermes/context/debug/publish/event/branding/BRANDING_CREATED'
   And request {"name":"default", "productName":"Default Company Name"}
   When method post
   Then status 200
   * def result = call read('classpath:com/sailpoint/notification/e2e/read-redis.js') {key: '#(response)', token: '#(passwordTypeResponse.response.access_token)'}
   * print result
   * match result.attributes.brandingConfigs.default.productName contains 'Default Company Name'
   * header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
   Given path 'hermes/interest-matcher/debug/publish/event/full/notification/MANUAL_TASKS_EVENT'
   And request {"task":"test default brand", "recipient":{"id":"2c928090672c901401672c92d33c02ee","name":null,"phone":null,"email":"vasil.shlapkou@sailpoint.com"},"notificationKey":"cloud_manual_tasks","orgId":3,"org":"acme-solar","isTemplateEvaluated":false,"requestId":"f27b0172-94eb-4000-b9b3-6e603de96377"}
   When method post
   Then status 200
   * def result = call read('classpath:com/sailpoint/notification/e2e/read-redis.js') {key: '#(response)', token: '#(passwordTypeResponse.response.access_token)'}
   * print result
   * match result.html contains 'test default brand'
   * match result.subject contains 'Default Company Name'
   * match result.fromAddress contains 'no-reply@sailpoint.com'
   #verify default delete event
   * header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
   Given path 'hermes/context/debug/publish/event/branding/BRANDING_DELETED'
   And request '"default"'
   When method post
   Then status 200
   * def result = call read('classpath:com/sailpoint/notification/e2e/read-redis.js') {key: '#(response)', token: '#(passwordTypeResponse.response.access_token)'}
   * print result
   * match result == 'default'

  Scenario: Verify Notification Template Context read API
    # Given an 'debug_entry' entry
    Given path 'hermes/context/debug/publish/entry'
    And header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    And request {}
    When method GET
    Then status 200
    * def debugEntryValue = response

    # Then verify that 'debug_entry' is part of the attributes.
    Given path 'beta/notification-template-context/'
    And header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    When method GET
    Then status 200
    * match response.attributes.debug_entry == debugEntryValue
