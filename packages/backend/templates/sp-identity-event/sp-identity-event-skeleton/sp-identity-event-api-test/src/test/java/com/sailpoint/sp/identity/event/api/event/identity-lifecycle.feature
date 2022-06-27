Feature: Identity lifecycle
  Background:
    * url baseUrl
    * def passwordTypeResponse = callonce read('classpath:com/sailpoint/authentication/oauth/oathkeeper/password_grant_type.feature')

  Scenario: Publish events to identity topic and validate the output
    * def externalId = call read('classpath:com/sailpoint/sp/identity/event/utils/uuid.js')
    * print externalId

    # Simulate the creation of a new identity with two accounts
    * def IDENTITY_CHANGED = read('IDENTITY_CHANGED.json')
    * def IdentityCreatedEvent = read('output/IdentityCreatedEvent.json')

    Given path '/sp-identity-event/debug/publish-event/IDENTITY_CHANGED'
    * header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    And request IDENTITY_CHANGED
    When method POST
    Then status 204
    * print response

    # Validate the output -- an IdentityCreatedEvent event and two IdentityAccountCorrelatedEvent events.
    * def identityCreatedEvent = call read('classpath:com/sailpoint/sp/identity/event/utils/read-redis.js') {type: 'IdentityCreatedEvent', id:'#(externalId)', token: '#(passwordTypeResponse.response.access_token)'}
    Then match identityCreatedEvent == IdentityCreatedEvent
    * def identityAccountCorrelatedEvent = call read('classpath:com/sailpoint/sp/identity/event/utils/read-redis.js') {type: 'IdentityAccountCorrelatedEvent', id:'#(externalId + "-2c9180845e865b19015e9ff4d2ef5108")', token: '#(passwordTypeResponse.response.access_token)'}
    Then match identityAccountCorrelatedEvent == read('output/IdentityAccountCorrelatedEvent0.json')

    * def identityAccountCorrelatedEvent = call read('classpath:com/sailpoint/sp/identity/event/utils/read-redis.js') {type: 'IdentityAccountCorrelatedEvent', id:'#(externalId + "-ff80808171acbff70171acc45e9d001f")', token: '#(passwordTypeResponse.response.access_token)'}
    Then match identityAccountCorrelatedEvent == read('output/IdentityAccountCorrelatedEvent1.json')

    # Simulate an identity attribute change and removal of an account
    # Also mutate the IdentityCreatedEvent object for use in validation step
    * set IDENTITY_CHANGED.users[0].attributes.firstname = 'Sitaraa'
    * remove IDENTITY_CHANGED.users[0].accounts[1]
    * set IdentityCreatedEvent.attributes.firstname = 'Sitaraa'

    Given path '/sp-identity-event/debug/publish-event/IDENTITY_CHANGED'
    * header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    And request IDENTITY_CHANGED
    When method POST
    Then status 204
    * print response

    * def identityAttributesChangedEvent = call read('classpath:com/sailpoint/sp/identity/event/utils/read-redis.js') {type: 'IdentityAttributesChangedEvent', id:'#(externalId)', token: '#(passwordTypeResponse.response.access_token)'}
    Then match identityAttributesChangedEvent ==
      """
      {
        "identity": {
          "id": '#(externalId)',
          "name": "leroy.sitara",
          "type": "IDENTITY"
        },
        "changes": [
          {
            "attribute": "firstname",
            "oldValue": "Sitara",
            "newValue": "Sitaraa"
          }
        ]
      }
      """

    # Validate IdentityAccountUncorrelatedEvent emitted for account 1
    * def identityAccountUncorrelatedEvent = call read('classpath:com/sailpoint/sp/identity/event/utils/read-redis.js') {type: 'IdentityAccountUncorrelatedEvent', id:'#(externalId + "-ff80808171acbff70171acc45e9d001f")', token: '#(passwordTypeResponse.response.access_token)'}
    Then match identityAccountUncorrelatedEvent == read('output/IdentityAccountUncorrelated1.json')

    # Publish the same IDENTITY_CHANGED event and validate no IdentityAttributeChangedEvent is emitted.
    Given path '/sp-identity-event/debug/publish-event/IDENTITY_CHANGED'
    * header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    And request IDENTITY_CHANGED
    When method POST
    Then status 204

    * def identityAttributesChangedEvent = call read('classpath:com/sailpoint/sp/identity/event/utils/read-redis.js') {type: 'IdentityAttributesChangedEvent', id:'#(externalId)', token: '#(passwordTypeResponse.response.access_token)'}
    # Note that the read-redis.js method returns -1, when it can't find a value for that particular key
    Then match identityAttributesChangedEvent == -1

    # Test when happens if you change an attribute in non-existent identity
    * def fakeExternalId = call read('classpath:com/sailpoint/sp/identity/event/utils/uuid.js')
    * set IDENTITY_CHANGED.users[0].attributes.externalId = fakeExternalId
    * set IDENTITY_CHANGED.users[0].externalId = fakeExternalId
    * set IDENTITY_CHANGED.users[0].id = fakeExternalId
    Given path '/sp-identity-event/debug/publish-event/IDENTITY_CHANGED'
    * header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    And request IDENTITY_CHANGED
    When method POST
    Then status 204
    * def identityAttributesChangedEvent = call read('classpath:com/sailpoint/sp/identity/event/utils/read-redis.js') {type: 'IdentityAttributesChangedEvent', id:'#(externalId)', token: '#(passwordTypeResponse.response.access_token)'}
    * match identityAttributesChangedEvent == -1


    * set IDENTITY_CHANGED.users[0].attributes.externalId = externalId
    * set IDENTITY_CHANGED.users[0].externalId = externalId
    * set IDENTITY_CHANGED.users[0].id = externalId

    # Publish IDENTITY_DELETED for the same identity
    Given path '/sp-identity-event/debug/publish-event/IDENTITY_DELETED'
    * header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    And request read('IDENTITY_DELETED.json')
    When method POST
    Then status 204

    # Validate IdentityAccountUncorrelatedEvent emitted for account 0
    * def identityAccountUncorrelatedEvent = call read('classpath:com/sailpoint/sp/identity/event/utils/read-redis.js') {type: 'IdentityAccountUncorrelatedEvent', id:'#(externalId + "-2c9180845e865b19015e9ff4d2ef5108")', token: '#(passwordTypeResponse.response.access_token)'}
    Then match identityAccountUncorrelatedEvent == read('output/IdentityAccountUncorrelated0.json')

    # Validate IdentityDeletedEvent emitted
    * def identityDeletedEvent = call read('classpath:com/sailpoint/sp/identity/event/utils/read-redis.js') {type: 'IdentityDeletedEvent', id:'#(externalId)', token: '#(passwordTypeResponse.response.access_token)', attempts: 10}
    Then match identityDeletedEvent == IdentityCreatedEvent

   # Finally validate IdentityAccountUncorrelatedEvent not emitted for account 1 which was removed in earlier change
    * def identityAccountUncorrelatedEvent = call read('classpath:com/sailpoint/sp/identity/event/utils/read-redis.js') {type: 'IdentityAccountUncorrelatedEvent', id:'#(externalId + "-ff80808171acbff70171acc45e9d001f")', token: '#(passwordTypeResponse.response.access_token)', attempts: 1}
    Then match identityAccountUncorrelatedEvent == -1
