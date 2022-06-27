Feature: ETS Test Invoke With Auth
  Background:
    * url baseUrl
    * def clientCredentialsResponse = callonce read('classpath:com/sailpoint/authentication/oauth/oathkeeper/client_credentials_grant_type.feature')
    * def passwordTypeResponse = callonce read('classpath:com/sailpoint/authentication/oauth/oathkeeper/password_grant_type.feature')
    * def fireForgetTriggerId = 'test:fire-and-forget'
    * def debugURL = baseUrl + 'ets-debug/invocations/success'
    * def SubscriptionBearerDto = {"triggerId": '#(fireForgetTriggerId)', "type":"HTTP", "responseDeadline":"PT15M", "httpConfig": {"url":'#(debugURL)',  "httpAuthenticationType": "BEARER_TOKEN","bearerTokenAuthConfig": {"bearerToken":"aladdin"}}}
    * def SubscriptionBasicDto = {"triggerId": '#(fireForgetTriggerId)', "type":"HTTP", "responseDeadline":"PT15M", "httpConfig": {"url":'#(debugURL)', "httpAuthenticationType": "BASIC_AUTH","basicAuthConfig": {"userName":"aladdin", "password":"opensesame"}}}
    # clean subscriptions if still exist after failed tests.
    * callonce read('classpath:com/sailpoint/ets/utils/clean-subscriptions.feature')
    * header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token

  Scenario: Invoke Trigger and validate it was executed with correct Auth.

    # Pre check to make sure there is not subscription for this trigger to begin with
    Given path '/beta/trigger-subscriptions'
    * header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    When method GET
    Then status 200
    And match response[*].triggerId !contains fireForgetTriggerId

    # Subscribe to trigger defined by triggerId with BearerDto
    Given path '/beta/trigger-subscriptions'
    * header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    And request SubscriptionBearerDto
    When method POST
    Then status 201
    And match response.id == '#string'
    And match response.triggerId == fireForgetTriggerId
    And match response.type == 'HTTP'
    And match response.responseDeadline == 'PT15M'
    * def subscriptionId = response.id

    # Invoke the trigger as API role
    Given path '/ets/trigger-invocations/start'
    * header Authorization = 'Bearer ' + clientCredentialsResponse.response.access_token
    And request {"triggerId": '#(fireForgetTriggerId)', "input":{"identityId": "201327fda1c44704ac01181e963d463f", "approved": true}, "contentJson": {}}
    When method POST
    Then status 200
    And match response == [ { id: '#string', secret: '#string', triggerId: '#(fireForgetTriggerId)', contentJson: {} } ]
    * def invocationId = response[0].id

    # Sleep
    * def result = call read('classpath:com/sailpoint/ets/api/sleep.js') {interval: 5000}

    # Read from redis verify invocation completed.
    * def result = call read('classpath:com/sailpoint/ets/api/read-redis.js') {key: '#(invocationId)', token: '#(clientCredentialsResponse.response.access_token)'}
    * print result
    * match result.authorization == 'Bearer aladdin'

    # Clean up. Remove the subscription created for this test
    Given path '/beta/trigger-subscriptions/' + subscriptionId
    * header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    When method DELETE
    Then status 204

   # Subscribe to trigger defined by triggerId with BasicDto
    Given path '/beta/trigger-subscriptions'
    * header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    And request SubscriptionBasicDto
    When method POST
    Then status 201
    And match response.id == '#string'
    And match response.triggerId == fireForgetTriggerId
    And match response.type == 'HTTP'
    And match response.responseDeadline == 'PT15M'
    * def subscriptionBasicId = response.id

    # Invoke the trigger as API role
    Given path '/ets/trigger-invocations/start'
    * header Authorization = 'Bearer ' + clientCredentialsResponse.response.access_token
    And request {"triggerId": '#(fireForgetTriggerId)', "input":{"identityId": "201327fda1c44704ac01181e963d463a", "approved": true}, "contentJson": {}}
    When method POST
    Then status 200
    And match response == [ { id: '#string', secret: '#string', triggerId: '#(fireForgetTriggerId)', contentJson: {} } ]
    * def invocationBasicId = response[0].id

    # Sleep
    * def result = call read('classpath:com/sailpoint/ets/api/sleep.js') {interval: 5000}

    # Read from redis verify invocation completed.
    * def result = call read('classpath:com/sailpoint/ets/api/read-redis.js') {key: '#(invocationBasicId)', token: '#(clientCredentialsResponse.response.access_token)'}
    * print result
    * match result.authorization == 'Basic YWxhZGRpbjpvcGVuc2VzYW1l'

    # Clean up. Remove the subscription created for this test
    * call read('classpath:com/sailpoint/ets/utils/delete-subscription.feature') {id: '#(subscriptionBasicId)'}