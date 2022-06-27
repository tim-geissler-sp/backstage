Feature: Test invalid event

  Background:
    * url baseUrl
    * def creds = callonce read('classpath:com/sailpoint/util/get-oauth-internal-client.js') {name: 'sp-scheduler'}
    * def clientCredentialsTypeResponse = call read('classpath:com/sailpoint/authentication/oauth/oathkeeper/client_credentials_grant_type.feature') {apiUser: '#(creds.clientId)', apiKey: '#(creds.secret)'}
    * def sleep15Seconds = function(){ java.lang.Thread.sleep(15000) }
    * def now = function(){ return java.lang.System.currentTimeMillis() }
    * def metaValue = now()
 
  Scenario: Test event with none existed topic
    # Create scheduled action with unique meta key and a past due deadline
    Given path 'sp-scheduler/scheduled-actions'
    * header Authorization = 'Bearer ' + clientCredentialsTypeResponse.response.access_token
    And request { event : { topic : 'internal_test', type : 'dummy' }, deadline : '2000-03-27T20:27:17+00:00', meta : { karateTest : '#(metaValue)' }}
    When method POST
    Then status 200

    # Sleep for 15 seconds. The scheduler should pick up the invalid event in 10 seconds after creation and delete it
    * sleep15Seconds()

    # Validate that the schedule is gone
    Given path 'sp-scheduler/scheduled-actions'
    * header Authorization = 'Bearer ' + clientCredentialsTypeResponse.response.access_token
    * param filters = 'meta eq "{"karateTest":' + metaValue + '}"'
    When method GET
    Then status 200
    * print response
    And assert response.length == 0
