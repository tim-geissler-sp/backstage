Feature: Test Phase: User Preferences

  Background:
    * url baseUrl
    * def creds = callonce read('classpath:com/sailpoint/util/get-oauth-internal-client.js') {name: 'hermes'}
    * def clientCredentialsTypeResponse = call read('classpath:com/sailpoint/authentication/oauth/oathkeeper/client_credentials_grant_type.feature') {apiUser: '#(creds.clientId)', apiKey: '#(creds.secret)'}

  Scenario: Verify UserPreference create and delete via OLS events
    # Create
    Given path 'hermes/user/preferences/debug/create'
    And header Authorization = 'Bearer ' + clientCredentialsTypeResponse.response.access_token
    And request {}
    When method POST
    Then status 200
    * print response

    # List
    Given path 'hermes/user/preferences/debug/' + podName + '__deleted-org'
    And header Authorization = 'Bearer ' + clientCredentialsTypeResponse.response.access_token
    And request {}
    When method GET
    Then status 200
    * match response != '#[0]'

    # Delete
    Given path 'hermes/org-lifecycle/debug/publish/event/ORG_LIFECYCLE/ORG_DELETED'
    And header Authorization = 'Bearer ' + clientCredentialsTypeResponse.response.access_token
    And request {}
    When method POST
    Then status 200

    # Sleep
    * def result = call read('classpath:com/sailpoint/notification/e2e/sleep.js') {interval: 10000}

     # List
    Given path 'hermes/user/preferences/debug/' + podName + '__deleted-org'
    And header Authorization = 'Bearer ' + clientCredentialsTypeResponse.response.access_token
    And request {}
    When method GET
    Then status 200
    * match response == '#[0]'
