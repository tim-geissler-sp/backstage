@ignore
Feature: Reusable scenario to delete a schedule

  Background:
    * url baseUrl
    * def passwordTypeResponse = callonce read('classpath:com/sailpoint/authentication/oauth/oathkeeper/password_grant_type.feature')
    * header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token

  Scenario: Delete a schedule
    Given path 'sp-scheduler/scheduled-actions', karate.get('id')
    When method DELETE
