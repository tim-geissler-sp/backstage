@ignore @cleanup @prod
Feature: Reusable scenario to cleanup all schedules

  Background:
    * url baseUrl
    * def passwordTypeResponse = callonce read('classpath:com/sailpoint/authentication/oauth/oathkeeper/password_grant_type.feature')
    * header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token

  Scenario: Cleanup schedules
    Given path 'sp-scheduler/scheduled-actions'
    When method GET
    Then status 200
    * karate.log('cleanup will attempt to delete these schedules: ' + response)
    * def result = call read('delete-util.feature') response

