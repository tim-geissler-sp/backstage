@ignore
Feature: Send Test Notification As Large Event

  Background:
    * url baseUrl
    * def passwordTypeResponse = callonce read('classpath:com/sailpoint/authentication/oauth/oathkeeper/password_grant_type.feature')
    * configure retry = { count: 3, interval: 3000 }

  Scenario: Send debug test notification as large event and verify it was processed by hermes
    Given path 'hermes/sender/debug/send-large-test-notification'
    * header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    And request
    """
      {
        "key": "cloud_manual_tasks",
        "medium": "EMAIL",
        "context": { "task" : "foo" }
      }
    """
    When method post
    Then status 200
    * def result = call read('classpath:com/sailpoint/notification/e2e/read-redis.js') {key: '#(response)', token: '#(passwordTypeResponse.response.access_token)'}
    And match result contains {"html":"Test Large Event"}

