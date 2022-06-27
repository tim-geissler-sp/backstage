Feature: Create Schedule

  Background:
    * url baseUrl
    * def creds = callonce read('classpath:com/sailpoint/util/get-oauth-internal-client.js') {name: 'sp-scheduler'}
    * def clientCredentialsTypeResponse = call read('classpath:com/sailpoint/authentication/oauth/oathkeeper/client_credentials_grant_type.feature') {apiUser: '#(creds.clientId)', apiKey: '#(creds.secret)'}
    * header Authorization = 'Bearer ' + clientCredentialsTypeResponse.response.access_token

  Scenario: Create schedules with cron and bad timezone offset information - 400
    Given path 'sp-scheduler/scheduled-actions'
    * header Authorization = 'Bearer ' + clientCredentialsTypeResponse.response.access_token
    And request {"cronTimezone":{"offset":"5"},"cronString":"* 18 * * *","event":{"topic":"internal_test","type":"SCHEDULER_INTERNAL_TEST","content":{"accessRequestId":"1234","requester":{"type":"IDENTITY","id":"12352352345","name":"john.doe"}}},"meta":{}}
    When method POST
    Then status 400
    And match response contains { messages:[{ locale :#notnull, localeOrigin :#notnull, text :"timezone offset must start with a '+' or '-' and be of the form dd:dd"}]}

  Scenario: Create schedules with cron and timezone location information - 400
    Given path 'sp-scheduler/scheduled-actions'
    * header Authorization = 'Bearer ' + clientCredentialsTypeResponse.response.access_token
    And request {"cronTimezone":{"location":"CST"},"cronString":"* 18 * * *","event":{"topic":"internal_test","type":"SCHEDULER_INTERNAL_TEST","content":{"accessRequestId":"1234","requester":{"type":"IDENTITY","id":"12352352345","name":"john.doe"}}},"meta":{}}
    When method POST
    Then status 400
    And match response contains { messages :[{ locale :#notnull, localeOrigin :#notnull, text :"cannot use 'CST' as a timezone location -- see the IANA Time Zone database for valid values: err unknown time zone CST"}]}

  Scenario: Create schedules with cron and timezone location and offset - 400
    Given path 'sp-scheduler/scheduled-actions'
    * header Authorization = 'Bearer ' + clientCredentialsTypeResponse.response.access_token
    And request {"cronTimezone":{"location":"EST","offset":"+10:00"},"cronString":"* 18 * * *","event":{"topic":"internal_test","type":"SCHEDULER_INTERNAL_TEST","content":{"accessRequestId":"1234","requester":{"type":"IDENTITY","id":"12352352345","name":"john.doe"}}},"meta":{}}
    When method POST
    Then status 400
    And match response contains {messages:[{locale :#notnull, localeOrigin :#notnull, text:"either timezone location or timezone offset can be provided, but not both"}]}

  Scenario: Create schedules with cron and timezone location information - 200
    Given path 'sp-scheduler/scheduled-actions'
    * header Authorization = 'Bearer ' + clientCredentialsTypeResponse.response.access_token
    And request {"cronTimezone":{"location":"America/Santiago"},"cronString":"* 18 * * *","event":{"topic":"internal_test","type":"SCHEDULER_INTERNAL_TEST","content":{"accessRequestId":"1234","requester":{"type":"IDENTITY","id":"12352352345","name":"john.doe"}}},"meta":{}}
    When method POST
    Then status 200
