Feature: ETS Test Fire And Forget Invoke
  Background:
    * url baseUrl
    * def clientCredentialsResponse = callonce read('classpath:com/sailpoint/authentication/oauth/oathkeeper/client_credentials_grant_type.feature')
    * def passwordTypeResponse = callonce read('classpath:com/sailpoint/authentication/oauth/oathkeeper/password_grant_type.feature')
    * def triggerId = 'test:fire-and-forget'
    * def debugURL = baseUrl + 'ets-debug/invocations/success'
    * def SubscriptionDto = {"triggerId": '#(triggerId)', "type":"HTTP", "responseDeadline":"PT15M", "httpConfig": {"url":'#(debugURL)'}}
    # clean subscriptions if still exist after failed tests.
    * callonce read('classpath:com/sailpoint/ets/utils/clean-subscriptions.feature')
    * header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token

  Scenario: Invoke Fire And Forget trigger and validate it was executed.

    # Pre check to make sure there is not subscription for this trigger to begin with
    Given path '/beta/trigger-subscriptions'
    * header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    When method GET
    Then status 200
    And match response[*].triggerId !contains triggerId

    # Subscribe to trigger defined by triggerId in background
    Given path '/beta/trigger-subscriptions'
    * header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    And request SubscriptionDto
    When method POST
    Then status 201
    And match response.id == '#string'
    And match response.triggerId == triggerId
    And match response.type == 'HTTP'
    And match response.responseDeadline == 'PT15M'
    * def subscriptionId = response.id

    # Invoke the trigger as API role
    Given path '/ets/trigger-invocations/start'
    * header Authorization = 'Bearer ' + clientCredentialsResponse.response.access_token
    And request {"triggerId": '#(triggerId)', "input":{"identityId": "201327fda1c44704ac01181e963d463c", "approved": true}, "contentJson": {}}
    When method POST
    Then status 200
    And match response == [ { id: '#string', secret: '#string', triggerId: '#(triggerId)', contentJson: {} } ]
    * def invocationId = response[0].id
    * def secret = response[0].secret

    # Sleep
    * def result = call read('classpath:com/sailpoint/ets/api/sleep.js') {interval: 5000}

    # Read from redis verify invocation completed.
    * def result = call read('classpath:com/sailpoint/ets/api/read-redis.js') {key: '#(invocationId)', token: '#(clientCredentialsResponse.response.access_token)'}
    * print result
    * match result.identityId == '201327fda1c44704ac01181e963d463c'

    # Clean up. Remove the subscription created for this test
    * call read('classpath:com/sailpoint/ets/utils/delete-subscription.feature') {id: '#(subscriptionId)'}