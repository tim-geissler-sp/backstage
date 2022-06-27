Feature: ETS HTTP subscription

  Background:
    * url baseUrl
    * def passwordTypeResponse = callonce read('classpath:com/sailpoint/authentication/oauth/oathkeeper/password_grant_type.feature')
    * configure retry = { count: 3, interval: 3000 }
    # clean subscriptions if still exist after failed tests.
    * callonce read('classpath:com/sailpoint/ets/utils/clean-subscriptions.feature')

  Scenario: Create update and delete HTTP subscription

    # Subscribe to trigger
    Given path '/beta/trigger-subscriptions'
    And retry until responseStatus == 201
    And header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    And request
    """
    {
      "triggerId": "test:request-response",
      "name": "name",
      "description": "description",
      "type": "HTTP",
      "httpConfig": {
        "url": "notExistedUrlToSimulateInvocationFailure",
        "httpDispatchMode": "SYNC"
      },
      "filter": "$.*"
    }
    """
    When method POST
    Then status 201
    And match response.id == '#string'
    And match response.enabled == true
    And match response.name == 'name'
    And match response.description == 'description'
    And match response.filter == '$.*'
    And match response.responseDeadline == 'PT1H'
    And match response.triggerName == 'Request-Response Test'
    * def subscriptionId = response.id

    # Get subscription
    Given path '/beta/trigger-subscriptions'
    And param filters = 'id eq "' + subscriptionId + '"'
    And retry until responseStatus == 200
    And header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    When method GET
    Then status 200
    And match response == '#[1]'
    And match response[0].name == 'name'
    And match response[0].description == 'description'
    And match response[0].filter == '$.*'
    And match response[0].triggerName == 'Request-Response Test'

    # Get subscription by id
    Given path '/beta/trigger-subscriptions/' + subscriptionId
    And retry until responseStatus == 200
    And header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    When method GET
    Then status 200
    And match response.id == subscriptionId
    And match response.name == 'name'
    And match response.description == 'description'
    And match response.triggerName == 'Request-Response Test'

    # Update subscription with a PUT
    Given path '/beta/trigger-subscriptions/' + subscriptionId
    And retry until responseStatus == 200
    And header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    And request
    """
    {
      "triggerId": "test:request-response",
      "name": "new name",
      "description": "new description",
      "type": "HTTP",
      "httpConfig": {
        "url": "notExistedUrlToSimulateInvocationFailure",
        "httpDispatchMode": "SYNC"
      },
      "filter": "$",
      "enabled": false
    }
    """
    When method PUT
    Then status 200
    And match response.id == subscriptionId
    And match response.name == 'new name'
    And match response.description == 'new description'
    And match response.filter == '$'
    And match response.enabled == false

    # Update subscription with a PATCH
    Given path '/beta/trigger-subscriptions/' + subscriptionId
    And retry until responseStatus == 200
    # application/json-patch+json necessary for PATCH
    And header Content-Type = 'application/json-patch+json'
    And header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    And request
    """
    [
      {
        "op": "replace",
        "path": "/name",
        "value": "replaced name"
      },
      {
        "op": "replace",
        "path": "/filter",
        "value": "replacedFilter"
      },
      {
        "op": "replace",
        "path": "/description",
        "value": "replaced description"
      },
      {
        "op": "replace",
        "path": "/enabled",
        "value": true
      }
    ]
    """
    When method PATCH

    Then status 200
    And match response.id == subscriptionId
    And match response.name == 'replaced name'
    And match response.description == 'replaced description'
    And match response.filter == 'replacedFilter'
    And match response.enabled == true

    # Clean up
    * call read('classpath:com/sailpoint/ets/utils/delete-subscription.feature') {id: '#(subscriptionId)'}