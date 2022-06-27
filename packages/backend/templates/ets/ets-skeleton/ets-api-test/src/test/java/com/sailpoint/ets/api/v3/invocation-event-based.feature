Feature: ETS Test Event Based Fire And Forget Invocation
  Background:
    * url baseUrl
    * def passwordTypeResponse = callonce read('classpath:com/sailpoint/authentication/oauth/oathkeeper/password_grant_type.feature')
    * def triggerId = 'test:fire-and-forget'
    * def now = function(){ return java.lang.System.currentTimeMillis() }
    * def redisKey = 'EtsKarateTest-' + now()
    * def debugURL = baseUrl + 'ets-debug/invocations/success?redisKey=' + redisKey
    * def SubscriptionDto = {"triggerId": '#(triggerId)', "type":"HTTP", "httpConfig": {"url":'#(debugURL)'}}
    # clean subscriptions if still exist after failed tests.
    * callonce read('classpath:com/sailpoint/ets/utils/clean-subscriptions.feature')
    * header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token

  Scenario: Publish Event for Fire And Forget trigger and validate it was executed.

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
    And match response.responseDeadline == 'PT1H'
    * def subscriptionId = response.id

    # Invoke the trigger by sending a fake event
    Given path '/ets-debug/publish-event'
    * header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    And request {"identityId": "201327fda1c44704ac01181e963d463c", "approved": false, "someExtraField": "garbage"}
    When method POST
    Then status 200

    # Sleep
    * def result = call read('classpath:com/sailpoint/ets/api/sleep.js') {interval: 5000}

    # Read from redis verify invocation completed. Verify that extra field in an invocation is removed.
    * def result = call read('classpath:com/sailpoint/ets/api/read-redis.js') {key: '#(redisKey)', token: '#(passwordTypeResponse.response.access_token)'}
    * print result
    * match result.identityId == '201327fda1c44704ac01181e963d463c'
    * match result.approved == false
    * match result.someExtraField == '#notpresent'

    # Clean up. Remove the subscription created for this test
    * call read('classpath:com/sailpoint/ets/utils/delete-subscription.feature') {id: '#(subscriptionId)'}