Feature: ETS SCRIPT subscription
  Background:
    * url baseUrl
    * def passwordTypeResponse = callonce read('classpath:com/sailpoint/authentication/oauth/oathkeeper/password_grant_type.feature')
    # clean subscriptions if still exist after failed tests.
    * callonce read('classpath:com/sailpoint/ets/utils/clean-subscriptions.feature')
    * header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token

  Scenario: Create update and delete SCRIPT subscription

    # Subscribe to trigger
    Given path '/beta/trigger-subscriptions'
    * header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    And request {"triggerId": "test:request-response", "type":"SCRIPT", "scriptConfig": {"language":"JAVASCRIPT", "source":"hello"}}
    When method POST
    Then status 201
    And match response.id == '#string'
    And match response.enabled == true
    And match response.scriptConfig.source == 'hello'
    And match response.scriptConfig.responseMode == 'SYNC'
    * def subscriptionId = response.id

    # Update subscription
    Given path '/beta/trigger-subscriptions/' + subscriptionId
    * header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    And request {"triggerId": "test:request-response", "type":"SCRIPT", "scriptConfig": {"language":"JAVASCRIPT", "source":"bye", "responseMode": "ASYNC"}}
    When method PUT
    Then status 200
    And match response.id == '#string'
    And match response.scriptConfig.source == 'bye'
    And match response.scriptConfig.responseMode == 'ASYNC'

    # Clean up
    * call read('classpath:com/sailpoint/ets/utils/delete-subscription.feature') {id: '#(subscriptionId)'}
