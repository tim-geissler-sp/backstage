Feature: ETS test SYNC Invoke
  Background:
    * url baseUrl
    * def clientCredentialsResponse = callonce read('classpath:com/sailpoint/authentication/oauth/oathkeeper/client_credentials_grant_type.feature')
    * def passwordTypeResponse = callonce read('classpath:com/sailpoint/authentication/oauth/oathkeeper/password_grant_type.feature')
    * configure retry = { count: 3, interval: 3000 }
    * def invocation_triggerId = 'test:request-response'
    * def SubscriptionDto = {"triggerId": '#(invocation_triggerId)', "type":"HTTP", "responseDeadline":"PT1H", "httpConfig": {"url":"notExistedUrlToSimulateInvocationFailure","httpDispatchMode":"SYNC"}}
    # clean subscriptions if still exist after failed tests.
    * callonce read('classpath:com/sailpoint/ets/utils/clean-subscriptions.feature')
    * header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token

  Scenario: Invoke trigger and simulate failure(Switch to empty org if this scenario failed)

    # Pre check to make sure there is not subscription for this trigger to begin with
    Given path '/beta/trigger-subscriptions'
    * header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    When method GET
    Then status 200
    And match response[*].triggerId !contains invocation_triggerId

    # Subscribe to trigger defined by triggerId in background
    Given path '/beta/trigger-subscriptions'
    * header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    And request SubscriptionDto
    When method POST
    Then status 201
    And match response.id == '#string'
    And match response.triggerId == invocation_triggerId
    And match response.type == 'HTTP'
    And match response.responseDeadline == 'PT1H'
    And match response.httpConfig == {"url":"notExistedUrlToSimulateInvocationFailure","httpAuthenticationType":"NO_AUTH","basicAuthConfig":null,"bearerTokenAuthConfig":null,"httpDispatchMode":"SYNC"}
    * def subscriptionId = response.id

    # Invoke the trigger as API role
    Given path '/ets/trigger-invocations/start'
    * header Authorization = 'Bearer ' + clientCredentialsResponse.response.access_token
    And request {"triggerId": '#(invocation_triggerId)', "input":{"identityId": "201327fda1c44704ac01181e963d463c"}, "contentJson": {}}
    When method POST
    Then status 200
    And match response == [ { id: '#string', secret: '#string', triggerId: '#(invocation_triggerId)', contentJson: {} } ]
    * def invocationId = response[0].id
    * def secret = response[0].secret

    # Wait for lambda to fail the invocation
    * call read('classpath:com/sailpoint/ets/api/sleep.js') {interval:10000}

    # Verify invocation completed with error
    Given path '/beta/trigger-invocations/' + invocationId + '/complete'
    And header Authorization = 'Bearer ' + clientCredentialsResponse.response.access_token
    And retry until responseStatus == 400
    And request {"secret": '#(secret)', "error": "Karate test mock error"}
    When method POST
    Then assert responseStatus == 400 && response.detailCode == '400.1.404 Referenced object not found'

    # Clean up. Remove the subscription created for this test
    * call read('classpath:com/sailpoint/ets/utils/delete-subscription.feature') {id: '#(subscriptionId)'}