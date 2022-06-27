Feature: ETS Subscriptions Export Import API

  Background:
    * url baseUrl
    * def passwordTypeResponse = callonce read('classpath:com/sailpoint/authentication/oauth/oathkeeper/password_grant_type.feature')
    * configure retry = { count: 1, interval: 3000 }
    # clean subscriptions if still exist after failed tests.
    * callonce read('classpath:com/sailpoint/ets/utils/clean-subscriptions.feature')

  Scenario: Export and Import Subscriptions
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
    * def subscriptionId = response.id
    * def triggerId = response.triggerId
    * def type = response.type

    # Export subscriptions
    Given path '/beta/trigger-subscriptions/export'
    And param count = 'true'
    And retry until responseStatus == 200 && response.length == 2
    And header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    When method GET
    Then status 200
    And match response == '#[2]'
    And match response[0].self == '#notnull'
    And match response[0].object == '#notnull'
    And match header X-Total-Count == '2'
    * def exportedData = response

    # Delete subscriptions
    * call read('classpath:com/sailpoint/ets/utils/clean-subscriptions.feature')

    Given path '/beta/trigger-subscriptions/import'
    And request exportedData
    And header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    When method POST
    Then status 200
    And match response.importedObjects == '#[2]'

     # Delete subscriptions
    * call read('classpath:com/sailpoint/ets/utils/clean-subscriptions.feature')
