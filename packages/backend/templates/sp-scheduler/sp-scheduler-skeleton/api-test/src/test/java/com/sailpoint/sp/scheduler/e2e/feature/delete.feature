@prod
Feature: Delete Schedule

  Background:
    * url baseUrl
    * def passwordTypeResponse = callonce read('classpath:com/sailpoint/authentication/oauth/oathkeeper/password_grant_type.feature')

  Scenario: Delete schedule by ID - 204 response
    * table actions
      | event                                       | deadline                    | meta                      |
      | { topic : 'internal_test', type : 'dummy' } | '2100-03-27T20:27:17+00:00' | { key : 'delete test'}    |

    * def result = call read('../util/create-without-cron-util.feature') actions
    * def status = $result[*].responseStatus
    * match each status == 200

    * header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    Given path 'sp-scheduler/scheduled-actions/', $result[0].response.id
    When method DELETE
    Then status 204

    #To verify successful delete, repeat and check for 404
    * header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    Given path 'sp-scheduler/scheduled-actions/', $result[0].response.id
    When method DELETE
    Then status 404

  Scenario: Delete schedule by ID - Not found
    * header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    Given path 'sp-scheduler/scheduled-actions/00000000-0000-0000-0000-000000000000'
    When method DELETE
    Then status 404

  Scenario: Delete schedule by ID - Bad request
    * header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    Given path 'sp-scheduler/scheduled-actions/1234'
    When method DELETE
    Then status 400
