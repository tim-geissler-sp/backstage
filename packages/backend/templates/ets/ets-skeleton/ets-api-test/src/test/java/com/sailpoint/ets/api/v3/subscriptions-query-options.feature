Feature: ETS Subscriptions API Query Options
Background:
    * url baseUrl
    * def passwordTypeResponse = callonce read('classpath:com/sailpoint/authentication/oauth/oathkeeper/password_grant_type.feature')
    * configure retry = { count: 3, interval: 4000 }
    # clean subscriptions if still exist after failed tests.
    * callonce read('classpath:com/sailpoint/ets/utils/clean-subscriptions.feature')

  Scenario: List Subscriptions filter query
    # Create test:fire-and-forget subscription
    Given path '/beta/trigger-subscriptions'
    And retry until responseStatus == 201
    And header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    And request
    """
    {
      "triggerId": "test:fire-and-forget",
      "type": "HTTP",
      "httpConfig": {
        "httpAuthenticationType": "NO_AUTH",
        "basicAuthConfig": {},
        "bearerTokenAuthConfig": {},
        "httpDispatchMode": "SYNC",
        "url": "test"
      }
    }
    """
    When method POST
    Then status 201

    # Create test:request-response subscription to match against
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
    * def triggerId = response.triggerId
    * def type = response.type

    # Get subscription by id
    Given path '/beta/trigger-subscriptions'
    And param filters = 'id eq "' + subscriptionId + '"'
    And retry until responseStatus == 200
    And header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    When method GET
    Then status 200
    And match response == '#[1]'
    And match response[0].id == subscriptionId

    # Get subscription by triggerId
    Given path '/beta/trigger-subscriptions'
    And param filters = 'triggerId eq "' + triggerId + '"'
    And retry until responseStatus == 200
    And header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    When method GET
    Then status 200
    And match response == '#[1]'
    And match response[0].triggerId == triggerId

    # Get subscription by type
    Given path '/beta/trigger-subscriptions'
    And param filters = 'type eq "' + type + '"'
    And retry until responseStatus == 200
    And header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    When method GET
    Then status 200
    And match response == '#[1]'
    And match response[0].type == type

    # Get subscription by type using not operator
    Given path '/beta/trigger-subscriptions'
    And param filters = 'not type eq "SCRIPT"'
    And retry until responseStatus == 200
    And header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    When method GET
    Then status 200
    And match response == '#[1]'
    And match response[0].type != "SCRIPT"


    # Delete subscriptions
    * call read('classpath:com/sailpoint/ets/utils/clean-subscriptions.feature')

  Scenario: List Subscriptions sort query
    # Create test:fire-and-forget subscription
    Given path '/beta/trigger-subscriptions'
    And retry until responseStatus == 201
    And header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    And request
    """
    {
      "triggerId": "test:fire-and-forget",
      "type": "HTTP",
      "httpConfig": {
        "httpAuthenticationType": "NO_AUTH",
        "basicAuthConfig": {},
        "bearerTokenAuthConfig": {},
        "httpDispatchMode": "SYNC",
        "url": "test"
      }
    }
    """
    When method POST
    Then status 201
    * def triggerId1 = response.triggerId

    # Create test:request-response subscription
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
    * def triggerId2 = response.triggerId

    # Get subscriptions sorted by triggerId in desc order
    Given path '/beta/trigger-subscriptions'
    And param sorters = '-triggerId'
    And retry until responseStatus == 200
    And header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    When method GET
    Then status 200
    And match response == '#[2]'
    And match response[0].triggerId == triggerId2
    And match response[1].triggerId == triggerId1

    # Delete subscriptions
    * call read('classpath:com/sailpoint/ets/utils/clean-subscriptions.feature')