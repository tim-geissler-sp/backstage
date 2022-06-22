@ignore
Feature: ETS Trigger Invocations with Script Subscriptions (javascript invocation is deprecated and should be removed)

  Background:
    * url baseUrl
    * def clientCredentialsResponse = callonce read('classpath:com/sailpoint/authentication/oauth/oathkeeper/client_credentials_grant_type.feature')
    * def passwordTypeResponse = callonce read('classpath:com/sailpoint/authentication/oauth/oathkeeper/password_grant_type.feature')
    * configure retry = { count: 1, interval: 3000 }
    # clean subscriptions if still exist after failed tests.
    * callonce read('classpath:com/sailpoint/ets/utils/clean-subscriptions.feature')

  Scenario: Invoke test:request-response trigger and fail invocation due to javascript error
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
        "source": "async(event,context)=>{}"
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
    And request {"triggerId":"test:request-response","input":{"identityId":"invocation-script.feature"},"contentJson":{}}
    When method POST
    Then status 200
    * def invocationId = response[0].id

    # Sleep
    * call read('classpath:com/sailpoint/ets/api/sleep.js') {interval:3000}

    # Check invocation status and match trigger input and error
    Given path '/beta/trigger-invocations/status'
    And param filters = 'id eq "' + invocationId + '"'
    And retry until responseStatus == 200 && response.length == 1
    And header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    When method GET
    Then status 200
    And match response == '#[1]'
    And match response[0].startInvocationInput.input == {"identityId":"invocation-script.feature"}
    And match response[0].completeInvocationInput.output == '#null'
    And match response[0].completeInvocationInput.localizedError.text == '#string'

    # Delete subscription
    * call read('classpath:com/sailpoint/ets/utils/delete-subscription.feature') {id: '#(subscriptionId)'}

  Scenario: Invoke test:request-response trigger and fail invocation when customer code returns failed status
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
        "responseMode": "ASYNC",
        "source": "async(event,context)=>{return new EventCallResult(Status.FAILED,null,'customer error')}"
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
    And request {"triggerId":"test:request-response","input":{"identityId":"invocation-script.feature"},"contentJson":{}}
    When method POST
    Then status 200
    * def invocationId = response[0].id

    # Sleep
    * call read('classpath:com/sailpoint/ets/api/sleep.js') {interval:3000}

    # Check invocation status and match trigger input and error
    Given path '/beta/trigger-invocations/status'
    And param filters = 'id eq "' + invocationId + '"'
    And retry until responseStatus == 200 && response.length == 1
    And header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    When method GET
    Then status 200
    And match response == '#[1]'
    And match response[0].startInvocationInput.input == {"identityId":"invocation-script.feature"}
    And match response[0].completeInvocationInput.output == '#null'
    And match response[0].completeInvocationInput.localizedError.text == '#string'

    # Delete subscription
    * call read('classpath:com/sailpoint/ets/utils/delete-subscription.feature') {id: '#(subscriptionId)'}

  Scenario: Invoke and complete test:request-response trigger using script subscription SYNC mode
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

    # Invoke the trigger as API role
    Given path '/ets/trigger-invocations/start'
    And retry until responseStatus == 200
    And header Authorization = 'Bearer ' + clientCredentialsResponse.response.access_token
    And request {"triggerId":"test:request-response","input":{"identityId":"invocation-script.feature"},"contentJson":{}}
    When method POST
    Then status 200
    * def invocationId = response[0].id

    # Sleep
    * call read('classpath:com/sailpoint/ets/api/sleep.js') {interval:3000}

    # Check invocation status and match trigger input and output
    Given path '/beta/trigger-invocations/status'
    And param filters = 'id eq "' + invocationId + '"'
    And retry until responseStatus == 200 && response.length == 1
    And header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    When method GET
    Then status 200
    And match response == '#[1]'
    And match response[0].startInvocationInput.input == {"identityId":"invocation-script.feature"}
    And match response[0].completeInvocationInput.output == {"approved": false}
    And match response[0].completeInvocationInput.localizedError == '#null'

    # Delete subscription
    * call read('classpath:com/sailpoint/ets/utils/delete-subscription.feature') {id: '#(subscriptionId)'}

  Scenario: Invoke test:request-response trigger using script subscription ASYNC mode
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
        "responseMode": "ASYNC",
        "source": "async(event,context)=>{return new EventCallResult(Status.SUCCESS,null,null)}"
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
    And request {"triggerId":"test:request-response","input":{"identityId":"invocation-script.feature"},"contentJson":{}}
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
    And match response[0].startInvocationInput.input == {"identityId":"invocation-script.feature"}
    And match response[0].completeInvocationInput.output == '#null'
    And match response[0].completed == '#null'

    # Delete subscription
    * call read('classpath:com/sailpoint/ets/utils/delete-subscription.feature') {id: '#(subscriptionId)'}

  Scenario: Invoke test:request-response trigger using script subscription DYNAMIC mode and complete synchronously
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
        "responseMode": "DYNAMIC",
        "source": "async(event,context)=>{const triggerOutput={approved:!1};return new EventCallResult(Status.SUCCESS,triggerOutput,null)}"
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
    And request {"triggerId":"test:request-response","input":{"identityId":"invocation-script.feature"},"contentJson":{}}
    When method POST
    Then status 200
    * def invocationId = response[0].id

    # Sleep
    * call read('classpath:com/sailpoint/ets/api/sleep.js') {interval:3000}

    # Check invocation status and match trigger input and output
    Given path '/beta/trigger-invocations/status'
    And param filters = 'id eq "' + invocationId + '"'
    And retry until responseStatus == 200 && response.length == 1
    And header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    When method GET
    Then status 200
    And match response == '#[1]'
    And match response[0].startInvocationInput.input == {"identityId":"invocation-script.feature"}
    And match response[0].completeInvocationInput.output == {"approved": false}
    And match response[0].completeInvocationInput.localizedError == '#null'

    # Delete subscription
    * call read('classpath:com/sailpoint/ets/utils/delete-subscription.feature') {id: '#(subscriptionId)'}

  Scenario: Invoke test:request-response trigger using script subscription DYNAMIC mode asynchronously
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
        "responseMode": "DYNAMIC",
        "source": "async(event,context)=>{return new EventCallResult(Status.ACCEPTED,null,null)}"
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
    And request {"triggerId":"test:request-response","input":{"identityId":"invocation-script.feature"},"contentJson":{}}
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
    And match response[0].startInvocationInput.input == {"identityId":"invocation-script.feature"}
    And match response[0].completeInvocationInput.output == '#null'
    And match response[0].completed == '#null'

    # Delete subscription
    * call read('classpath:com/sailpoint/ets/utils/delete-subscription.feature') {id: '#(subscriptionId)'}

  Scenario: Invoke test:fire-and-forget trigger using script subscription
    # Create script subscription
    Given path '/beta/trigger-subscriptions'
    And retry until responseStatus == 201
    And header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    And request
    """
    {
      "triggerId": "test:fire-and-forget",
      "type": "SCRIPT",
      "scriptConfig": {
        "language": "JAVASCRIPT",
        "source": "async(event,context)=>{return null}"
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
    And request {"triggerId":"test:fire-and-forget","input":{"approved":true,"identityId":"invocation-script.feature"},"contentJson":{}}
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
    And match response[0].startInvocationInput.input == {"approved":true,"identityId":"invocation-script.feature"}
    And match response[0].completeInvocationInput.output == '#null'
    And match response[0].completed == '#notnull'

    # Delete subscription
    * call read('classpath:com/sailpoint/ets/utils/delete-subscription.feature') {id: '#(subscriptionId)'}
