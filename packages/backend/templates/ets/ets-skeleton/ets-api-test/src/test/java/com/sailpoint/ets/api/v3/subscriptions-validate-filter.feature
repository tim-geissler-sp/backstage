Feature: ETS subscription validate filter endpoint
  Background:
    * url baseUrl
    * def passwordTypeResponse = callonce read('classpath:com/sailpoint/authentication/oauth/oathkeeper/password_grant_type.feature')

  Scenario: Validate filter happy path
    Given path '/beta/trigger-subscriptions/validate-filter'
    And header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    And request { input : { identityId : '201327fda1c44704ac01181e963d463c'}, filter : '[?($.identityId == "201327fda1c44704ac01181e963d463c")]' }
    When method POST
    Then status 200
    And match response.isValid == true

  Scenario: Validate filter - 401
    Given path '/beta/trigger-subscriptions/validate-filter'
    And request { input : { identityId : '201327fda1c44704ac01181e963d463c'}, filter : '[?($.identityId == "201327fda1c44704ac01181e963d463c")]' }
    When method POST
    Then status 401

  Scenario: Validate filter - false
    Given path '/beta/trigger-subscriptions/validate-filter'
    And header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    And request { input : { identityId : '201327fda1c44704ac01181e963d463c'}, filter : '[?($.identityId == "nonmatching")]' }
    When method POST
    Then status 200
    And match response.isValid == false

  Scenario: Validate filter - false on invalid key
    Given path '/beta/trigger-subscriptions/validate-filter'
    And header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    And request { input : { identityId : '201327fda1c44704ac01181e963d463c'}, filter : '$.changes' }
    When method POST
    Then status 200
    And match response.isValid == false

  Scenario: Validate filter - no filter provided
    Given path '/beta/trigger-subscriptions/validate-filter'
    And header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    And request { input : { identityId : '201327fda1c44704ac01181e963d463c'} }
    When method POST
    Then status 400
