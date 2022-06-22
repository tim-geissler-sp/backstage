@devOnly
Feature: ETS triggers/all endpoint
  Background:
    * url baseUrl
    * def creds = callonce read('classpath:com/sailpoint/util/get-oauth-internal-client.js') {name: 'hermes'}
    * def clientCredentialsTypeResponse = call read('classpath:com/sailpoint/authentication/oauth/oathkeeper/client_credentials_grant_type.feature') {apiUser: '#(creds.clientId)', apiKey: '#(creds.secret)'}
    * header Authorization = 'Bearer ' + clientCredentialsTypeResponse.response.access_token

  Scenario: Get all triggers
    Given path '/ets/triggers/all'
    When method GET
    Then status 200
    And match responseHeaders !contains { 'X-Total-Count': '#notnull' }
    And assert response.size() >= 1
    And match response[0] ==
    """
    {
      "id": "#string",
      "name": "#string",
      "type": "#string",
      "description": "#string",
      "inputSchema": "#string",
      "outputSchema": "#present",
      "exampleInput": "#object",
      "exampleOutput": "#present",
      "featureStoreKey": "#string"
    }
    """
