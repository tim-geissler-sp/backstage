@ignore
Feature: Reusable scenario to find schedule by id

  Background:
    * url baseUrl
    * def passwordTypeResponse = callonce read('classpath:com/sailpoint/authentication/oauth/oathkeeper/password_grant_type.feature')
    * header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token

  Scenario: Find schedule by id
    Given path 'sp-scheduler/scheduled-actions'
    When method GET
    * def found = karate.jsonPath(response, "$..[?(@.id =='" + karate.get('id') + "')]")
