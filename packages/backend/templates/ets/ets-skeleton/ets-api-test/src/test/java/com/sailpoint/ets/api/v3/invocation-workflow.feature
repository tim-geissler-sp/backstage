Feature: ETS Trigger Invocations with Workflow Subscriptions

  Background:
    * url baseUrl
    * def clientCredentialsResponse = callonce read('classpath:com/sailpoint/authentication/oauth/oathkeeper/client_credentials_grant_type.feature')
    * def passwordTypeResponse = callonce read('classpath:com/sailpoint/authentication/oauth/oathkeeper/password_grant_type.feature')
    * configure retry = { count: 1, interval: 3000 }
    # clean subscriptions if still exist after failed tests.
    * callonce read('classpath:com/sailpoint/ets/utils/clean-subscriptions.feature')

    @ignore
  Scenario: Invoke test:fire-and-forget trigger using workflow subscription
    # Create workflow
    Given path '/sp-workflow-config/workflows'
    And header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    And request
    """
    {
      "name":"empty-wf",
      "definition":{}
    }
    """
    When method POST
    Then status 200
    * def workflowId = response.id

    # Create workflow subscription
    Given path '/beta/trigger-subscriptions'
    And retry until responseStatus == 201
    And header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    And request
    """
    {
      "triggerId": "test:fire-and-forget",
      "type": "WORKFLOW",
      "workflowConfig": {
        "workflowId": '#(workflowId)'
      }
    }
    """
    When method POST
    Then status 201
    * def subscriptionId = response.id

    # Invoke the trigger as API role
    Given path '/ets/trigger-invocations/start'
    And retry until responseStatus == 200
    And header Authorization = 'Bearer ' + clientCredentialsResponse.response.access_token
    And request {"triggerId":"test:fire-and-forget","input":{"approved":true,"identityId":"invocation-workflow.feature"},"contentJson":{}}
    When method POST
    Then status 200
    * def invocationId = response[0].id

    # Sleep
    * call read('classpath:com/sailpoint/ets/api/sleep.js') {interval:3000}

    # Check invocation status and match trigger input and null output
    Given path '/beta/trigger-invocations/status'
    And param filters = 'id eq "' + invocationId + '"'
    And retry until responseStatus == 200 && response.length == 1
    And header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    When method GET
    Then status 200
    And match response == '#[1]'
    And match response[0].startInvocationInput.input == {"approved":true,"identityId":"invocation-workflow.feature"}
    And match response[0].completeInvocationInput.output == '#null'
    And match response[0].completed == '#notnull'

    # Delete subscription
    * call read('classpath:com/sailpoint/ets/utils/delete-subscription.feature') {id: '#(subscriptionId)'}

    # Delete workflow
    Given path '/sp-workflow-config/workflows/' + workflowId
    And header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    When method DELETE
    Then status 204
