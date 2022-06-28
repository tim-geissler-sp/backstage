Feature: Teams notification rendered test

  Background:
    * url baseUrl
    * def creds = callonce read('classpath:com/sailpoint/util/get-oauth-internal-client.js') {name: 'hermes'}
    * def clientCredentialsTypeResponse = call read('classpath:com/sailpoint/authentication/oauth/oathkeeper/client_credentials_grant_type.feature') {apiUser: '#(creds.clientId)', apiKey: '#(creds.secret)'}
    * header Authorization = 'Bearer ' + clientCredentialsTypeResponse.response.access_token

  Scenario: Verify hermes can call teams' get tenant endpoint
    Given url 'https://teams-integration.cloud.sailpoint.com/v3/api/tenants'
    When method GET
    Then status 200
    * match response ==
    """
    {
      "tenants": #array
    }
    """
