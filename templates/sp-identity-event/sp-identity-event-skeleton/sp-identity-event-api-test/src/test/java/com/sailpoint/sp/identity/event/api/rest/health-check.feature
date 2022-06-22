Feature: ETS health check
  Background:
    * url baseUrl
    * def passwordTypeResponse = callonce read('classpath:com/sailpoint/authentication/oauth/oathkeeper/password_grant_type.feature')
    * header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token

  Scenario: Health check
    Given path 'sp-identity-event/health/system'
    When method GET
    Then status 200
    And match response contains { status: 'UP' }
