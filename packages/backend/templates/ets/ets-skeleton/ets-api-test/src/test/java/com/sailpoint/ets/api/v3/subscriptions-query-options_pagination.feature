Feature: ETS Subscriptions API Query Options -- Pagination

  Background:
    * url baseUrl
    * def passwordTypeResponse = callonce read('classpath:com/sailpoint/authentication/oauth/oathkeeper/password_grant_type.feature')
    * configure retry = { count: 10, interval: 8000 }
    # clean subscriptions if still exist after failed tests.
    * callonce read('classpath:com/sailpoint/ets/utils/clean-subscriptions.feature')


  Scenario: List Subscriptions pagination
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

    # Get first page containing one subscription
    Given path '/beta/trigger-subscriptions'
    And param offset = '0'
    And param limit = '1'
    And retry until responseStatus == 200 && response.length == 1
    And header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    When method GET
    Then status 200
    And match response == '#[1]'
    * def idFromPage1 = response[0].id

    # Get second page containing one subscription
    Given path '/beta/trigger-subscriptions'
    And param offset = '1'
    And param limit = '1'
    And retry until responseStatus == 200 && response.length == 1
    And header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    When method GET
    Then status 200
    And match response == '#[1]'
    * def idFromPage2 = response[0].id

    # Verify distinct subscription is returned in each page
    * match idFromPage1 != idFromPage2

    # Delete subscriptions
    * call read('classpath:com/sailpoint/ets/utils/clean-subscriptions.feature')

  Scenario: List Subscriptions pagination with filters and count header
    # Create first test:fire-and-forget subscription
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

    # Create second test:fire-and-forget subscription
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
        "url": "test2"
      }
    }
    """
    When method POST
    Then status 201

    # Create test:request-response subscription which should be filtered out
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

    # Get a page containing one subscription and verify X-Total-Count header
    Given path '/beta/trigger-subscriptions'
    And param count = 'true'
    And param offset = '0'
    And param limit = '1'
    And param filters = 'triggerId eq "test:fire-and-forget"'
    And retry until responseStatus == 200 && response.length == 1
    And header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    When method GET
    Then status 200
    And match response == '#[1]'
    And match header X-Total-Count == '2'

    # Delete subscriptions
    * call read('classpath:com/sailpoint/ets/utils/clean-subscriptions.feature')
