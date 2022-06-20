@ignore
Feature: Reusable scenario to create a schedule

  Background:
    * url baseUrl
    * def passwordTypeResponse = callonce read('classpath:com/sailpoint/authentication/oauth/oathkeeper/password_grant_type.feature')
    * header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token

  @ignore
  Scenario: Create a schedule
    Given path 'sp-scheduler/scheduled-actions'
    And request { event : '#(event)', deadline : '#(deadline)', meta : '#(meta)' }
    When method POST
