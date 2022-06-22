Feature: Changing Account Attributes
  Background:
    * url baseUrl
    * def passwordTypeResponse = callonce read('classpath:com/sailpoint/authentication/oauth/oathkeeper/password_grant_type.feature')
    * def externalId = call read('classpath:com/sailpoint/sp/identity/event/utils/uuid.js')
    * def IDENTITY_CHANGED = read('IDENTITY_CHANGED.json')
    * def IDENTITY_DELETED = read('IDENTITY_DELETED.json')

  Scenario:  Verify that changing the account attributes produces the expected IdentityAccountAttributesChangedEvent event

    # UUID for an account is nullable and our sample doesn't have it. In identity-lifecycle, we are testing
    # the null variation. In this test, we will set a value for UUID.
    * set IDENTITY_CHANGED.users[0].accounts[0].uuid = "{111}"

    Given path '/sp-identity-event/debug/publish-event/IDENTITY_CHANGED'
    * header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    And request IDENTITY_CHANGED
    When method POST
    Then status 204

    # Simulate account attribute changes and verify the output IdentityAccountAttributesChangedEvent.
    * set IDENTITY_CHANGED.users[0].accounts[0].name = "jeff.sitara"
    * set IDENTITY_CHANGED.users[0].accounts[0].accountAttributes.name = "jeff.sitara"
    * set IDENTITY_CHANGED.users[0].accounts[0].locked = true
    * set IDENTITY_CHANGED.users[0].accounts[0].accountAttributes.country = "GB"

    Given path '/sp-identity-event/debug/publish-event/IDENTITY_CHANGED'
    * header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    And request IDENTITY_CHANGED
    When method POST
    Then status 204
    # We expect to see changes for each attribute changed.  We don't expect to see the change in users[0].name to show up
    * def identityAccountAttributesChangedEvent = call read('classpath:com/sailpoint/sp/identity/event/utils/read-redis.js') {type: 'IdentityAccountAttributesChangedEvent', id:'#(externalId + "-2c9180845e865b19015e9ff4d2ef5108")', token: '#(passwordTypeResponse.response.access_token)', attempts: 30 }
    * def expectedChangedEvent = read('output/IdentityAccountAttributesChangedEvent0.json')
    Then match identityAccountAttributesChangedEvent.identity == expectedChangedEvent.identity
    And match identityAccountAttributesChangedEvent.account == expectedChangedEvent.account
    And match identityAccountAttributesChangedEvent.source == expectedChangedEvent.source
    And match identityAccountAttributesChangedEvent.changes contains only expectedChangedEvent.changes

    # Publish IDENTITY_DELETED for the same identity
    Given path '/sp-identity-event/debug/publish-event/IDENTITY_DELETED'
    * header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    And request IDENTITY_DELETED
    When method POST
    Then status 204
