Feature: Send Test Notification

  Background:
    * url baseUrl
    * def passwordTypeResponse = callonce read('classpath:com/sailpoint/authentication/oauth/oathkeeper/password_grant_type.feature')
    * configure retry = { count: 3, interval: 3000 }

  Scenario: Send test notification to V3 endpoint and verify 204 response
    Given path 'beta/send-test-notification'
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
    Then status 204

  Scenario: Send test notification to V3 endpoint with incorrect key and verify 400 response
    Given path 'beta/send-test-notification'
    * header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    And request
    """
      {
        "key": "missing_template",
        "medium": "EMAIL"
      }
    """
    When method post
    Then status 400

  Scenario: Send test notification to V3 endpoint with null key and verify 400 response
    Given path 'beta/send-test-notification'
    * header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    And request
    """
      {
        "medium": "EMAIL"
      }
    """
    When method post
    Then status 400

  Scenario: Send test notification to V3 endpoint with null medium and verify 400 response
    Given path 'beta/send-test-notification'
    * header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    And request
    """
      {
        "key": "cloud_manual_tasks",
        "medium": "GMAIL"
      }
    """
    When method post
    Then status 400
