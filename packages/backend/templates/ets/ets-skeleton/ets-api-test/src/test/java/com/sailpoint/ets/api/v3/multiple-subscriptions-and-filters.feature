Feature: Multiple Subscriptions to Fire-and-Forget Trigger Invocations Test

  Background:
    * url baseUrl
    * def clientCredentialsResponse = callonce read('classpath:com/sailpoint/authentication/oauth/oathkeeper/client_credentials_grant_type.feature')
    * def passwordTypeResponse = callonce read('classpath:com/sailpoint/authentication/oauth/oathkeeper/password_grant_type.feature')
    * def triggerId = 'test:fire-and-forget'
    * def debugURL = baseUrl + 'ets-debug/invocations/success'
    * def SubscriptionDto = {"triggerId": '#(triggerId)', "type":"HTTP", "httpConfig": {"url":'#(debugURL)'}}
    * configure retry = { count: 3, interval: 3000 }
    # clean subscriptions if still exist after failed tests.
    * callonce read('classpath:com/sailpoint/ets/utils/clean-subscriptions.feature')

  Scenario: Subscribe multiple times to test:fire-and-forget trigger and verify both test invocation and real invocation

    # Initially verify that there is no pre-existing subscription to test:fire-and-forget trigger
    Given path '/beta/trigger-subscriptions'
    And retry until responseStatus == 200
    And header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    When method GET
    Then status 200
    And match response[*].triggerId !contains triggerId

    # First subscription to test:fire-and-forget trigger
    Given path '/beta/trigger-subscriptions'
    And retry until responseStatus == 201
    And header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    And set SubscriptionDto.filter = '$.approved'
    And request SubscriptionDto
    When method POST
    Then status 201
    And match response.id == '#string'
    And match response.triggerId == triggerId
    And match response.type == 'HTTP'
    And match response.httpConfig.url == debugURL
    And match response.filter == '$.approved'
    * def subscriptionIdOne = response.id

    # Second subscription to test:fire-and-forget trigger
    Given path '/beta/trigger-subscriptions'
    And retry until responseStatus == 201
    And header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    And set SubscriptionDto.filter = '$.nonexistent'
    And request SubscriptionDto
    When method POST
    Then status 201
    And match response.id == '#string'
    And match response.triggerId == triggerId
    And match response.type == 'HTTP'
    And match response.httpConfig.url == debugURL
    And match response.filter == '$.nonexistent'
    * def subscriptionIdTwo = response.id

    # Third subscription to test:fire-and-forget trigger
    Given path '/beta/trigger-subscriptions'
    And retry until responseStatus == 201
    And header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    And remove SubscriptionDto.filter
    And request SubscriptionDto
    When method POST
    Then status 201
    And match response.id == '#string'
    And match response.triggerId == triggerId
    And match response.type == 'HTTP'
    And match response.httpConfig.url == debugURL
    And match response.filter == '#notpresent'
    * def subscriptionIdThree = response.id

    # Fourth subscription to test:fire-and-forget trigger
    Given path '/beta/trigger-subscriptions'
    And retry until responseStatus == 201
    And header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    And set SubscriptionDto.filter = '$.approved'
    And request SubscriptionDto
    When method POST
    Then status 201
    And match response.id == '#string'
    And match response.triggerId == triggerId
    And match response.type == 'HTTP'
    And match response.httpConfig.url == debugURL
    And match response.filter == '$.approved'
    * def subscriptionIdFour = response.id

    # Disable the fourth subscription
    Given path '/beta/trigger-subscriptions/' + subscriptionIdFour
    And retry until responseStatus == 200
    * header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    And request {"triggerId": "test:fire-and-forget", "type":"HTTP", "responseDeadline":"PT1H", "httpConfig": {"url":"notExistedUrlToSimulateInvocationFailure","httpDispatchMode":"SYNC"}, "enabled": false}
    When method PUT
    Then status 200
    And match response.id == '#string'
    And match response.enabled == false

    # Test-Invoke test:fire-and-forget trigger
    Given path '/beta/trigger-invocations/test'
    And retry until responseStatus == 200
    And header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    And request {contentJson: {}, "triggerId": '#(triggerId)'}
    When method POST
    Then status 200
    And match response == '#[3]'
    And match each response == { id: '#string', secret: '#string', triggerId: '#(triggerId)', contentJson: {} }

    # Invoke test:fire-and-forget trigger
    Given path '/ets/trigger-invocations/start'
    And retry until responseStatus == 200
    And header Authorization = 'Bearer ' + clientCredentialsResponse.response.access_token
    And request {"triggerId": '#(triggerId)', "input":{"identityId": "201327fda1c44704ac01181e963d463c", "approved": true}, "contentJson": {}}
    When method POST
    Then status 200
    And match response == '#[2]'
    And match each response == { id: '#string', secret: '#string', triggerId: '#(triggerId)', contentJson: {} }
    And match response[0].id != response[1].id
    * def invocationIdOne = response[0].id
    * def invocationIdTwo = response[1].id

    # Sleep
    * call read('classpath:com/sailpoint/ets/api/sleep.js') {interval: 5000}

    # Read from redis to verify two invocations (i.e. first subscription with matching filter, and third with no filter)
    Given def result = call read('classpath:com/sailpoint/ets/api/read-redis.js') {key: '#(invocationIdOne)', token: '#(clientCredentialsResponse.response.access_token)'}
    Then match result.identityId == '201327fda1c44704ac01181e963d463c'

    Given def result = call read('classpath:com/sailpoint/ets/api/read-redis.js') {key: '#(invocationIdTwo)', token: '#(clientCredentialsResponse.response.access_token)'}
    Then match result.identityId == '201327fda1c44704ac01181e963d463c'

    # Clean up. Remove the subscriptions created for this test
    * call read('classpath:com/sailpoint/ets/utils/delete-subscription.feature') {id: '#(subscriptionIdOne)'}
    * call read('classpath:com/sailpoint/ets/utils/delete-subscription.feature') {id: '#(subscriptionIdTwo)'}
    * call read('classpath:com/sailpoint/ets/utils/delete-subscription.feature') {id: '#(subscriptionIdThree)'}
    * call read('classpath:com/sailpoint/ets/utils/delete-subscription.feature') {id: '#(subscriptionIdFour)'}


