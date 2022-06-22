Feature: ETS subscription endpoints
  Background:
    * url baseUrl
    * def passwordTypeResponse = callonce read('classpath:com/sailpoint/authentication/oauth/oathkeeper/password_grant_type.feature')
    # clean subscriptions if still exist after failed tests.
    * callonce read('classpath:com/sailpoint/ets/utils/clean-subscriptions.feature')
    * header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token

  Scenario: Get subscription
    Given path '/beta/trigger-subscriptions'
    When method GET
    Then status 200

  Scenario: Create subscriptions without type and config
    Given path '/beta/trigger-subscriptions'
    And request {"triggerId": "foo"}
    When method POST
    Then status 400

  Scenario: Create subscriptions without config
    Given path '/beta/trigger-subscriptions'
    And request {"triggerId": "foo", "type":"HTTP"}
    When method POST
    Then status 400

  Scenario: Create subscriptions without detailed config
    Given path '/beta/trigger-subscriptions'
    And request {"triggerId": "foo", "type":"HTTP", "httpConfig" : {}}
    When method POST
    Then status 400

  Scenario: Create subscriptions with invalid config
    Given path '/beta/trigger-subscriptions'
    And request {"triggerId": "foo", "type":"HTTP", "httpConfig": {"noUrl":"bar"}}
    When method POST
    Then status 400

  Scenario: Create subscriptions with invalid type
    Given path '/beta/trigger-subscriptions'
    And request {"triggerId": "foo", "type":"NotExisted", "httpConfig": {"url":"bar"}}
    When method POST
    Then status 400

  Scenario: Delete a subscription that doesn't exist
    Given path '/beta/trigger-subscriptions/0612a993-a2f8-4365-9dcc-4b5d620a64f0'
    When method DELETE
    Then status 404

  Scenario: Get subscription with multiple filters
    Given path '/beta/trigger-subscriptions'
    And param filters = 'type eq "HTTP" and id eq "does not exist"'
    When method GET
    Then status 200
    And match response.size() == 0

  Scenario: Creating a workflow subscription without workflowId should return 400
    Given path '/beta/trigger-subscriptions'
    And request
    """
    {
      "triggerId":"test:fire-and-forget",
      "type":"WORKFLOW",
      "workflowConfig": {}
    }
    """
    When method POST
    Then status 400
