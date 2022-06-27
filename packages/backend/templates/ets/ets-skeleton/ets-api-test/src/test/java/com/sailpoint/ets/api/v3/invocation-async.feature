Feature: ETS test ASYNC Invoke
  Background:
    * url baseUrl
    * def clientCredentialsResponse = callonce read('classpath:com/sailpoint/authentication/oauth/oathkeeper/client_credentials_grant_type.feature')
    * def passwordTypeResponse = callonce read('classpath:com/sailpoint/authentication/oauth/oathkeeper/password_grant_type.feature')
    * configure retry = { count: 3, interval: 3000 }
    * def triggerId = 'test:request-response'
    * def SubscriptionDto = {"triggerId": '#(triggerId)', "type":"HTTP", "responseDeadline":"PT15M", "httpConfig": {"url":"https://www.sailpoint.com/","httpDispatchMode":"ASYNC"}}
    # clean subscriptions if still exist after failed tests.
    * callonce read('classpath:com/sailpoint/ets/utils/clean-subscriptions.feature')
    * header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token

  Scenario: Invoke trigger in ASYNC mode

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
    And match response.httpConfig == {"url":"https://www.sailpoint.com/","httpAuthenticationType":"NO_AUTH","basicAuthConfig":null,"bearerTokenAuthConfig":null,"httpDispatchMode":"ASYNC"}
    * def subscriptionId = response.id

    # Invoke the trigger as API role
    Given path '/ets/trigger-invocations/start'
    * header Authorization = 'Bearer ' + clientCredentialsResponse.response.access_token
    And request {"triggerId": '#(triggerId)', "input":{"identityId": "201327fda1c44704ac01181e963d463c"}, "contentJson": {}}
    When method POST
    Then status 200
    And match response == [ { id: '#string', secret: '#string', triggerId: '#(triggerId)', contentJson: {} } ]
    * def invocationId = response[0].id
    * def secret = response[0].secret

    # Get active trigger invocations
    Given path '/ets/trigger-invocations'
    And param filters = 'id eq "' + invocationId + '"'
    * header Authorization = 'Bearer ' + clientCredentialsResponse.response.access_token
    When method GET
    Then status 200
    And match responseHeaders !contains { 'X-Total-Count': '#notnull' }
    And assert response.size() == 1
    And match response[0] contains { 'id': '#notnull', 'created': '#notnull', 'triggerId': '#notnull', 'deadline': '#notnull' }

    # Complete the invocation.
    Given path '/beta/trigger-invocations/' + invocationId + '/complete'
    And header Authorization = 'Bearer ' + clientCredentialsResponse.response.access_token
    And retry until responseStatus == 204
    And request {"secret": '#(secret)', "error": "Karate test mock error"}
    When method POST
    Then assert responseStatus == 204

    # Clean up. Remove the subscription created for this test
    * call read('classpath:com/sailpoint/ets/utils/delete-subscription.feature') {id: '#(subscriptionId)'}
