@ignore
Feature: utility for deleting all existing subscriptions

  Background:
    * url baseUrl
    * def passwordTypeResponse = callonce read('classpath:com/sailpoint/authentication/oauth/oathkeeper/password_grant_type.feature')
    * configure retry = { count: 3, interval: 3000 }

  Scenario: Delete all existing subscriptions then verify no subscriptions
    # Get existing subscriptions
    Given path '/beta/trigger-subscriptions'
    And retry until responseStatus == 200
    And header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    When method GET
    Then status 200
    * def subscriptions = response

    # Deletes all subscriptions in the `subscriptions` array. Ref: https://github.com/intuit/karate#data-driven-features
    * call read('classpath:com/sailpoint/sp/identity/event/utils/ets/delete-subscription.feature') subscriptions

    # Verify no subscription exists
    Given path '/beta/trigger-subscriptions'
    And retry until responseStatus == 200
    And header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    When method GET
    Then status 200
    And match response == []
