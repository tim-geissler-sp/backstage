@ignore
Feature: ETS utility for deleting a subscription given id

  Background:
    * url baseUrl
    * def passwordTypeResponse = callonce read('classpath:com/sailpoint/authentication/oauth/oathkeeper/password_grant_type.feature')
    * configure retry = { count: 3, interval: 3000 }

  Scenario: Delete subscription
    # id should be passed in by the calling feature
    Given path '/beta/trigger-subscriptions/' + id
    And retry until responseStatus == 204
    And header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    When method DELETE
    Then status 204
