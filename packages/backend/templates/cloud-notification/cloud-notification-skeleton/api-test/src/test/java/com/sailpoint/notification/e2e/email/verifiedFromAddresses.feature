Feature: Verified From Addresses API Test

  Background:
    * url baseUrl
    * def resourceUrl = 'beta/verified-from-addresses'
    * def creds = callonce read('classpath:com/sailpoint/util/get-oauth-internal-client.js') {name: 'hermes'}
    * def clientCredentialsTypeResponse = call read('classpath:com/sailpoint/authentication/oauth/oathkeeper/client_credentials_grant_type.feature') {apiUser: '#(creds.clientId)', apiKey: '#(creds.secret)'}
    * configure retry = { count: 3, interval: 3000 }
    * call read('classpath:com/sailpoint/notification/util/uuid.feature')

  Scenario: E2E test around verified-from-addresses API

    * def emailMock = 'hermes-e2e-test-' + uuid() + '@example.com'

    # Create
    Given path resourceUrl
    And retry until responseStatus == 201
    And header Authorization = 'Bearer ' + clientCredentialsTypeResponse.response.access_token
    And request { email : '#(emailMock)' }
    When method post
    Then status 201
    And match response == { id: '#uuid', email : '#(emailMock)', verificationStatus: 'PENDING' }
    * def emailMockId = response.id
    * def createResponse = response

    # Get
    Given path resourceUrl
    And retry until responseStatus == 200
    And header Authorization = 'Bearer ' + clientCredentialsTypeResponse.response.access_token
    When method get
    Then status 200
    And match response contains createResponse
    And match response == '#[_ <= 10]'

    # Delete
    Given path resourceUrl + '/' + emailMockId
    And retry until responseStatus == 204
    And header Authorization = 'Bearer ' + clientCredentialsTypeResponse.response.access_token
    When method delete
    Then status 204
    And match response == ''

    # Create it again
    Given path resourceUrl
    And retry until responseStatus == 201
    And header Authorization = 'Bearer ' + clientCredentialsTypeResponse.response.access_token
    And request { email : '#(emailMock)' }
    When method post
    Then status 201

    # Emit ORG_DELETED event
    Given path 'hermes/org-lifecycle/debug/publish/event/ORG_LIFECYCLE/ORG_DELETED'
    And header Authorization = 'Bearer ' + clientCredentialsTypeResponse.response.access_token
    And request {}
    When method POST
    Then status 200

    # Verify deletion
    Given path resourceUrl
    And param filters = 'email eq  "' + emailMock + '"'
    And retry until response == '[]'
    And header Authorization = 'Bearer ' + clientCredentialsTypeResponse.response.access_token
    When method get
    Then status 200
    And match response == '#[0]'