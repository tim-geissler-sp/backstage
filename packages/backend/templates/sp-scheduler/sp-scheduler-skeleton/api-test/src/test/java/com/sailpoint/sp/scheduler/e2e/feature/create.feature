@prod
Feature: Create Schedule

  Background:
    * url baseUrl
    * def passwordTypeResponse = callonce read('classpath:com/sailpoint/authentication/oauth/oathkeeper/password_grant_type.feature')
    * header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token

  Scenario: Create schedules without cron - 200
    * table actions
      | event                                       | deadline                    | meta                      |
      | { topic : 'internal_test', type : 'dummy' } | '2100-03-27T20:27:17+00:00' | { key : 'val'}            |

    * def result = call read('../util/create-without-cron-util.feature') actions
    * def status = $result[*].responseStatus
    * match each status == 200
    * match $result[*].response contains { event : {headers: #notnull, content: #null, topic : 'internal_test', type : 'dummy' }, cronString: '', deadline : '2100-03-27T20:27:17Z', meta : { key : 'val' }, id : #uuid, created: #notnull}

  Scenario: Create schedules with both cron and deadline - 200
    * table actions
      | event                                       | deadline                    | meta                      | cronString    |
      | { topic : 'internal_test', type : 'dummy' } | '2100-03-27T20:27:17+00:00' | { key : 'val'}            | '* * */5 * *' |

    * def result = call read('../util/create-with-cron-util.feature') actions
    * def status = $result[*].responseStatus
    * match each status == 200
    * match $result[*].response contains { event : {headers: #notnull, content: #null, topic : 'internal_test', type : 'dummy' }, cronString: '* * */5 * *', deadline : '2100-03-27T20:27:17Z', meta : { key : 'val' }, id : #uuid, created: #notnull}

  Scenario: Create schedules with cron only - 200
    * table actions
      | event                                       | deadline                    | meta                      | cronString    |
      | { topic : 'internal_test', type : 'dummy' } | ''                          | { key : 'val'}            | '* * */5 * *' |
      | { topic : 'internal_test', type : 'dummy' } | (null)                      | { key : 'val'}            | '* * */5 * *' |

    * def result = call read('../util/create-with-cron-util.feature') actions
    * def status = $result[*].responseStatus
    * match each status == 200
    * match $result[*].response contains { event : {headers: #notnull, content: #null, topic : 'internal_test', type : 'dummy' }, cronString: '* * */5 * *', deadline : #notnull, meta : { key : 'val' }, id : #uuid, created: #notnull}

  Scenario: Create schedules - Bad requests
    * table actions
      | event                                       | deadline                    | meta                      |
      | { topic : 'internal_test', type : 'dummy' } | '2100-03-27T20:27:17+00:00' |                           |
      | { topic : 'internal_test', type : 'dummy' } | 'Day after tomorrow'        | { key : 'val'}            |
      | { topic : 'non-existant', type : 'dummy' }  | '2100-03-27T20:27:17+00:00' | { key : 'val'}            |

    * def result = call read('../util/create-without-cron-util.feature') actions
    * def status = $result[*].responseStatus
    * match each status == 400
