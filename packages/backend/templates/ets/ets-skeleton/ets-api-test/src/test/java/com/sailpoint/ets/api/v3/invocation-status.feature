
Feature: ETS Test Invocation status
  Background:
    * url baseUrl
    * def passwordTypeResponse = callonce read('classpath:com/sailpoint/authentication/oauth/oathkeeper/password_grant_type.feature')
    * def invocation_status_triggerId = 'test:fire-and-forget'
    * def debugURL = baseUrl + 'ets-debug/invocations/success'
    * def SubscriptionDto = {"type":"HTTP","httpConfig":{"url":'#(debugURL)'},"triggerId":'#(invocation_status_triggerId)', "name":"karate test sub name"}
    * configure retry = { count: 10, interval: 10000 }
    # clean subscriptions if still exist after failed tests.
    * callonce read('classpath:com/sailpoint/ets/utils/clean-subscriptions.feature')

  Scenario: Check invocation status with fire and forget trigger with multiple test invocations

    # Pre check to make sure there is not subscription for this trigger to begin with
    Given path '/beta/trigger-subscriptions'
    And retry until responseStatus == 200
    And header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    When method GET
    Then status 200
    And match response[*].triggerId !contains invocation_status_triggerId

    # First subscription to trigger
    * print 'create first subscription to trigger'
    Given path '/beta/trigger-subscriptions'
    And retry until responseStatus == 201
    And header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    And request SubscriptionDto
    When method POST
    Then status 201
    And match response.id == '#string'
    And match response.triggerId == invocation_status_triggerId
    And match response.type == 'HTTP'
    And match response.responseDeadline == 'PT1H'
    And match response.enabled == true
    * def subscriptionIdOne = response.id

    # Second subscription to trigger
    * print 'create second subscription to trigger'
    Given path '/beta/trigger-subscriptions'
    And retry until responseStatus == 201
    And header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    And request SubscriptionDto
    When method POST
    Then status 201
    And match response.id == '#string'
    And match response.triggerId == invocation_status_triggerId
    And match response.type == 'HTTP'
    And match response.responseDeadline == 'PT1H'
    And match response.enabled == true
    * def subscriptionIdTwo = response.id

    # Test invoking the trigger without input (it should use example input)
    * print 'Test invoking the trigger without input'
    Given path '/beta/trigger-invocations/test'
    And retry until responseStatus == 200
    And header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    And request {contentJson: {}, "triggerId": '#(invocation_status_triggerId)'}
    When method POST
    Then status 200
    And match response == '#[2]'
    And match each response == { id: '#string', secret: '#string', triggerId: '#(invocation_status_triggerId)', contentJson: {} }
    * def invocationIdOne = response[0].id
    * def invocationIdTwo = response[1].id

    # Test invoking the trigger with input that doesn't conform to schema and verify 400
    * print 'Test invoking the trigger with input that doesn't conform to schema and verify 400'
    Given path '/beta/trigger-invocations/test'
    And retry until responseStatus == 400
    And header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    And request {contentJson: {}, "triggerId": '#(invocation_status_triggerId)', input : { approved : false}, subscriptionIds : ['#(subscriptionIdOne)'] }
    When method POST
    Then status 400

    # Test invoking the trigger with input and subscription id one
    * print 'Test invoking the trigger with input and subscription id one'
    Given path '/beta/trigger-invocations/test'
    And retry until responseStatus == 200
    And header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    And request {contentJson: {}, "triggerId": '#(invocation_status_triggerId)', input : { approved : false, identityId : 'InvocationStatusKarateTest' }, subscriptionIds : ['#(subscriptionIdOne)'] }
    When method POST
    Then status 200
    And match response == '#[1]'
    And match each response == { id: '#string', secret: '#string', triggerId: '#(invocation_status_triggerId)', contentJson: {} }
    * def invocationIdThree = response[0].id

    # Sleep
    * def result = call read('classpath:com/sailpoint/ets/api/sleep.js') {interval: 5000}

    # Check invocation status -- filter for invocationIdOne
    * print 'check invocation status for: ', invocationIdOne
    Given path '/beta/trigger-invocations/status/' + invocationIdOne
    And retry until responseStatus == 200
    And header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    When method GET
    Then status 200
    * match $response.id == invocationIdOne
    # Invocations one and two had example input where approved = true
    * def approved = karate.jsonPath(response, "$[?(@.id == '" + invocationIdOne + "')]..input.approved")
    * match approved == [ true ]
    * match $response.subscriptionName == "karate test sub name"

    # Check invocation status -- filter for invocationIdTwo
    * print 'check invocation status for: ',  invocationIdTwo
    Given path '/beta/trigger-invocations/status/' + invocationIdTwo
    And retry until responseStatus == 200
    And header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    When method GET
    Then status 200
    * match $response.id == invocationIdTwo
    # Invocations one and two had example input where approved = true
    * def approved = karate.jsonPath(response, "$[?(@.id == '" + invocationIdTwo + "')]..input.approved")
    * match approved == [ true ]
    * match $response.subscriptionName == "karate test sub name"

    # Check invocation status -- filter for invocationIdThree
    * print 'check invocation status for: ', invocationIdThree
    Given path '/beta/trigger-invocations/status/' + invocationIdThree
    And retry until responseStatus == 200
    And header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    When method GET
    Then status 200
    * match $response.id == invocationIdThree
    # Invocation three has input where approved = false
    * def approved = karate.jsonPath(response, "$[?(@.id == '" + invocationIdThree + "')]..input.approved")
    * match approved == [ false ]
    * match $response.subscriptionName == "karate test sub name"

    # Get a page of invocation status by triggerId
    Given path '/beta/trigger-invocations/status'
    And param count = 'true'
    And param filters = 'triggerId eq "' + invocation_status_triggerId + '"'
    And param sorters = 'triggerId'
    And param offset = '0'
    And param limit = '3'
    And retry until responseStatus == 200
    And header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    When method GET
    Then status 200
    And match response == '#[3]'
    And match header X-Total-Count == '#present'

    # Clean up.
    # Remove the first subscription
    Given path '/beta/trigger-subscriptions/' + subscriptionIdOne
    And retry until responseStatus == 204
    And header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    When method DELETE
    Then status 204

    # Remove the second subscription
    Given path '/beta/trigger-subscriptions/' + subscriptionIdTwo
    And retry until responseStatus == 204
    And header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    When method DELETE
    Then status 204

  Scenario: Test invoke test:request-response trigger as INLINE subscription and verify completed invocation status

    # Subscribe to test:request-response trigger as INLINE subscription
    Given path '/beta/trigger-subscriptions'
    And retry until responseStatus == 201
    And header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    And request {"triggerId":"test:request-response","type":"INLINE","name":"karate test sub 2","inlineConfig":{"output":{"approved":false}}}
    When method POST
    Then status 201
    And match response.id == '#string'
    And match response.triggerId == 'test:request-response'
    And match response.type == 'INLINE'
    And match response.inlineConfig == {"output":{"approved":false},"error":null}
    * def inlineSubscriptionId = response.id

    # Test invoke the trigger
    * print 'test invoking test:request trigger'
    Given path '/beta/trigger-invocations/test'
    And retry until responseStatus == 200
    And header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    And request {"triggerId":"test:request-response","input":{"identityId":"InlineSubscriptionInvocationStatusTest"},"contentJson":{},"subscriptionIds":['#(inlineSubscriptionId)']}
    When method POST
    Then status 200
    And match response == '#[1]'
    And match response[0] == { id: '#string', secret: '#string', triggerId: 'test:request-response', contentJson: {} }
    * def inlineInvocationId = response[0].id

    # Sleep
    * call read('classpath:com/sailpoint/ets/api/sleep.js') {interval: 5000}

    # Check invocation status
    * print 'get invocation status with param triggerId eq "test:request-response" '
    Given path '/beta/trigger-invocations/status/' + inlineInvocationId
    And retry until responseStatus == 200
    And header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    When method GET
    Then status 200
    And match response contains
    """
    {
      "subscriptionId": '#(inlineSubscriptionId)',
      "subscriptionName": "karate test sub 2",
      "triggerId": "test:request-response",
      "startInvocationInput": {
        "input": {
          "identityId": "InlineSubscriptionInvocationStatusTest"
        },
        "triggerId": "test:request-response",
        "contentJson": {}
      },
      "completeInvocationInput": {
        "output": {
          "approved": false
        },
        "localizedError": null,
      },
      "completed": '#string',
      "created": '#string',
      "id": '#(inlineInvocationId)',
      "type": "TEST"
    }
    """

    # Clean up: delete INLINE subscription
    Given path '/beta/trigger-subscriptions/' + inlineSubscriptionId
    And retry until responseStatus == 204
    And header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    When method DELETE
    Then status 204

  Scenario: Test invoke and complete test:request-response trigger using script subscription SYNC mode
    # Create script subscription
    Given path '/beta/trigger-subscriptions'
    And retry until responseStatus == 201
    And header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    And request
    """
    {
      "triggerId": "test:request-response",
      "type": "SCRIPT",
      "scriptConfig": {
        "language": "JAVASCRIPT",
        "responseMode": "SYNC",
        "source": "async(event,context)=>{const triggerOutput={approved:!1};return new EventCallResult(Status.SUCCESS,triggerOutput,null)}"
      }
    }
    """
    When method POST
    Then status 201
    * def subscriptionId = response.id

    # Test invoke the trigger
    * print 'Test invocating test:request-response trigger'
    Given path '/beta/trigger-invocations/test'
    And retry until responseStatus == 200
    And header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    And request {"triggerId":"test:request-response","input":{"identityId":"invocation-status.feature"},"contentJson":{}}
    When method POST
    Then status 200
    * def invocationId = response[0].id

    # Sleep
    * call read('classpath:com/sailpoint/ets/api/sleep.js') {interval: 5000}

    # Check invocation status and match trigger input and output
    * print 'Check invocation status.  There should be exactly one response with output set to approved false and input's IdentityId set to invocation-status.feature'
    Given path '/beta/trigger-invocations/status/' + invocationId
    And retry until responseStatus == 200
    And header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    When method GET
    Then status 200
    And match response.startInvocationInput.input == {"identityId":"invocation-status.feature"}
    And match response.completeInvocationInput.output == {"approved": false}

    # Delete subscription
    * call read('classpath:com/sailpoint/ets/utils/delete-subscription.feature') {id: '#(subscriptionId)'}
