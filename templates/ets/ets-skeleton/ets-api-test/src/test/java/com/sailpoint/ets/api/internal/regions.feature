Feature: ETS region endpoint
  Background:
    * url baseUrl
    * def passwordTypeResponse = callonce read('classpath:com/sailpoint/authentication/oauth/oathkeeper/password_grant_type.feature')
    * header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token

  Scenario: Region endpoint
    Given path 'ets/system/regions'
    When method GET
    Then status 200
    And assert response.size() > 0
    And match response[0] == '#string'

  Scenario: Region endpoint without GovCloud
    Given path 'ets/system/regions'
    * param excludeGovCloud = true
    When method GET
    Then status 200
    * print response
    And assert response.size() > 0
    And match response[0] == '#string'
    And match response !contains ['us-gov-west-1', 'us-gov-east-1']

  Scenario: Region endpoint WITH GovCloud
    Given path 'ets/system/regions'
    * param excludeGovCloud = false
    When method GET
    Then status 200
    And assert response.size() > 0
    And match response[0] == '#string'
    And match response contains ['us-gov-west-1', 'us-gov-east-1']

