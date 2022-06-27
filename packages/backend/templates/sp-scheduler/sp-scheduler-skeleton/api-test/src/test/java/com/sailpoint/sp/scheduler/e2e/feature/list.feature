@prod
Feature: List Schedules

  Background:
    * url baseUrl
    * def passwordTypeResponse = callonce read('classpath:com/sailpoint/authentication/oauth/oathkeeper/password_grant_type.feature')
    * header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token

  Scenario: List by Meta
    * table actions
      | event                                       | deadline                    | meta                             |
      | { topic : 'internal_test', type : 'dummy' } | '2100-03-27T20:27:17+00:00' | { key : 'meta test1'}            |
      | { topic : 'internal_test', type : 'dummy' } | '2100-03-27T20:27:17+00:00' | { key : 'meta test2'}            |
      | { topic : 'internal_test', type : 'dummy' } | '2100-03-27T20:27:17+00:00' | { key : 'meta test2'}            |

    * def result = call read('../util/create-without-cron-util.feature') actions
    * def status = $result[*].responseStatus
    * match each status == 200
    Given path 'sp-scheduler/scheduled-actions'
    * param filters = 'meta eq "{"key":"meta test2"}"'
    When method GET
    Then status 200
    And match response == '#[2]'
    And match each response == { id:#uuid, created:#notnull, cronString: #notnull, deadline:#notnull, event:#notnull, meta:#notnull }

    # Test non-existed meta
    * header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    Given path 'sp-scheduler/scheduled-actions'
    * param filters = 'meta eq "{"key":"non-existed"}"'
    When method GET
    Then status 200
    And match response == '#[0]'

    # Test not supported filter property
    * header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    Given path 'sp-scheduler/scheduled-actions'
    * param filters = 'notSupported eq "{"key":"meta test2"}"'
    When method GET
    Then status 400

  Scenario: List schedules with limit and offset
    * table actions
      | event                                         | deadline                    | meta                              |
      | { topic : 'internal_test', type : 'dummy1' }  | '2100-03-27T20:27:17+00:00' | { key : 'limit and offset test'}  |
      | { topic : 'internal_test', type : 'dummy2' }  | '2100-03-27T20:27:17+00:00' | { key : 'limit and offset test'}  |
      | { topic : 'internal_test', type : 'dummy3' }  | '2100-03-27T20:27:17+00:00' | { key : 'limit and offset test'}  |

    * def result = call read('../util/create-without-cron-util.feature') actions
    * def status = $result[*].responseStatus
    * match each status == 200
    Given path 'sp-scheduler/scheduled-actions'
    * param offset = 1
    * param limit = 1
    When method GET
    Then status 200
    And match response == '#[1]'
    And match response[0] == { id:#uuid, created:#notnull, cronString: #notnull, deadline:#notnull, event:#notnull, meta:#notnull, cronTimezone:#ignore }

    # Test incorrect offset
    * header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    Given path 'sp-scheduler/scheduled-actions'
    * param offset = -1
    When method GET
    Then status 400

    # Test incorrect limit
    * header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    Given path 'sp-scheduler/scheduled-actions'
    * param limit = 0
    When method GET
    Then status 400

