Feature: ETS EVENTBRIDGE subscription

  Background:
    * url baseUrl
    * def passwordTypeResponse = callonce read('classpath:com/sailpoint/authentication/oauth/oathkeeper/password_grant_type.feature')
    * configure retry = { count: 3, interval: 3000 }
    # clean subscriptions if still exist after failed tests.
    * callonce read('classpath:com/sailpoint/ets/utils/clean-subscriptions.feature')
    * def eventBridgeSubTestAccount = "831458296683"
    * def eventBridgeSubTestRegion = "us-east-1"

  @devOnly
  Scenario: Try to create EVENTBRIDGE subscription without eventBridgeConfig
    Given path '/beta/trigger-subscriptions'
    And header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    And request
    """
    {
      "triggerId": "test:request-response",
      "name": "name",
      "description": "description",
      "type": "EVENTBRIDGE",
      "filter": "$.*"
    }
    """
    When method POST
    Then status 400
    And match response.messages[0].text == 'Required field \"eventBridgeConfig\" was missing or empty.'

  @devOnly
  Scenario: Create update and delete EVENTBRIDGE subscription

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
      "type": "EVENTBRIDGE",
      "eventBridgeConfig": {
        "awsAccount": "#(eventBridgeSubTestAccount)",
        "awsRegion": "#(eventBridgeSubTestRegion)"
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
    And match response.type == 'EVENTBRIDGE'
    And match response.eventBridgeConfig.awsAccount == '#(eventBridgeSubTestAccount)'
    And match response.eventBridgeConfig.awsRegion == '#(eventBridgeSubTestRegion)'
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

 # Update subscription
    Given path '/beta/trigger-subscriptions/' + subscriptionId
    And retry until responseStatus == 200
    And header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    And request
    """
    {
      "triggerId": "test:request-response",
      "name": "new name",
      "description": "new description",
      "type": "EVENTBRIDGE",
      "eventBridgeConfig": {
        "awsAccount": "#(eventBridgeSubTestAccount)",
        "awsRegion": "#(eventBridgeSubTestRegion)"
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

    # Clean up
    * call read('classpath:com/sailpoint/ets/utils/delete-subscription.feature') {id: '#(subscriptionId)'}