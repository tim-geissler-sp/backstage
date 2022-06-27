Feature: ETS triggers endpoints
  Background:
    * url baseUrl
    * def passwordTypeResponse = callonce read('classpath:com/sailpoint/authentication/oauth/oathkeeper/password_grant_type.feature')
    * header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token

  Scenario: Get triggers
    Given path '/beta/triggers'
    When method GET
    Then status 200
    And match responseHeaders !contains { 'X-Total-Count': '#notnull' }
    And assert response.size() >= 1
    And match response[0] contains { 'id': '#notnull', 'inputSchema': '#notnull'}

  Scenario: Get triggers with count header
    Given path '/beta/triggers'
    And param count = 'true'
    When method GET
    Then status 200
    And match responseHeaders contains { 'X-Total-Count': '#notnull' }
    And match response.size() == responseHeaders['X-Total-Count'][0] * 1

  Scenario: Get triggers with filter
    # Filter for a trigger that exists
    Given path '/beta/triggers'
    And param filters = 'id eq "test:request-response"'
    * header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    When method GET
    Then status 200
    And match response.size() == 1
    And match response[0] contains { 'id': 'test:request-response'}
    # Filter for a trigger that does not exist
    Given path '/beta/triggers'
    And param filters = 'id eq "does not exist"'
    * header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    When method GET
    Then status 200
    And match response.size() == 0

  Scenario: Get triggers with invalid filter
    Given path '/beta/triggers'
    And param filters = 'input eq "something"'
    When method GET
    Then status 400

  Scenario: Get triggers with sorter
    # Sort ascending by id
    Given path '/beta/triggers'
    And param sorters = 'id'
    * header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    When method GET
    Then status 200
    And assert response.size() >= 2
    And assert response[0].id < response[1].id
    # Sort descending by id
    Given path '/beta/triggers'
    And param sorters = '-id'
    * header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    When method GET
    Then status 200
    And assert response.size() >= 2
    And assert response[0].id > response[1].id

  Scenario: Get triggers with limit and offset
    # Limit and offset with 1 result
    Given path '/beta/triggers'
    And param limit = '1'
    And param offset = '1'
    * header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    When method GET
    Then status 200
    And match response.size() == 1
    # Very large offset
    Given path '/beta/triggers'
    And param limit = '1'
    And param offset = '100000'
    * header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    When method GET
    Then status 200
    And match response.size() == 0

  Scenario: Get triggers filters, sorters, count, limit and offset
    Given path '/beta/triggers'
    And param count = 'true'
    And param filters = 'id eq "test:request-response"'
    And param sorters = 'id'
    And param limit = '1'
    And param offset = '0'
    When method GET
    Then status 200
    And match response.size() == 1
    And assert responseHeaders['X-Total-Count'][0] * 1 >= 1
    And match response[0] contains { 'id': 'test:request-response'}

  Scenario: Get triggers with count header when limit is less than total count
    Given path '/beta/triggers'
    And param count = 'true'
    And param limit = '1'
    When method GET
    Then status 200
    * print 'total count in xtotalcount = ' + responseHeaders['X-Total-Count'][0] * 1
    * print 'response.size = ' + response.size()
    And match responseHeaders contains { 'X-Total-Count': '#notnull' }
    And match response.size()*1 != responseHeaders['X-Total-Count'][0] * 1

