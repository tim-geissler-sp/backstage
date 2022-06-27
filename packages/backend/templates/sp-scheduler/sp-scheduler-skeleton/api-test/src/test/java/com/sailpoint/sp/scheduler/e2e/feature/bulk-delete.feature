Feature: Bulk Delete Schedules

  Background:
    * url baseUrl
    * def creds = callonce read('classpath:com/sailpoint/util/get-oauth-internal-client.js') {name: 'sp-scheduler'}
    * def clientCredentialsTypeResponse = call read('classpath:com/sailpoint/authentication/oauth/oathkeeper/client_credentials_grant_type.feature') {apiUser: '#(creds.clientId)', apiKey: '#(creds.secret)'}
    * header Authorization = 'Bearer ' + clientCredentialsTypeResponse.response.access_token

  Scenario: Bulk delete schedules with and without meta
    * table actions
      | event                                       | deadline                    | meta                           |
      | { topic : 'internal_test', type : 'dummy' } | '2100-03-27T20:27:17+00:00' | { key : 'bulk delete test'}    |
      | { topic : 'internal_test', type : 'dummy' } | '2100-03-27T20:27:17+00:00' | { key : 'bulk delete test'}    |

    * def result = call read('../util/create-without-cron-util.feature') actions
    * def status = $result[*].responseStatus
    * match each status == 200

    Given path 'sp-scheduler/scheduled-actions/bulk-delete'
    And request { meta : { key : 'bulk delete test' } }
    When method POST
    Then status 200
    * match response.count == 2

    #To verify successful delete, repeat and check for 0 count
    * header Authorization = 'Bearer ' + clientCredentialsTypeResponse.response.access_token
    Given path 'sp-scheduler/scheduled-actions/bulk-delete'
    And request { meta : { key : 'bulk delete test' } }
    When method POST
    Then status 200
    * match response.count == 0

    # Test for error without meta
    * header Authorization = 'Bearer ' + clientCredentialsTypeResponse.response.access_token
    Given path 'sp-scheduler/scheduled-actions/bulk-delete'
    And request {}
    When method POST
    Then status 400
