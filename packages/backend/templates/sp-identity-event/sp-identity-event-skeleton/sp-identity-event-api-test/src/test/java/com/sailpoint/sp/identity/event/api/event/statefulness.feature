Feature: Statefulness

  Background:
    * url baseUrl
    * def passwordTypeResponse = callonce read('classpath:com/sailpoint/authentication/oauth/oathkeeper/password_grant_type.feature')

  # For a given Identity that doesn't yet exist validate the stateful nature of sp-identity-event
  # IDENTITY_DELETED of a non-existing ID -> IDENTITY_CHANGED of ID ->
  # Assert IdentiyCretedEvent —> Assert ! IdentityDeletedEvent   —>
  # IDENTITY_CHANGED —> IDENTITY_DELETED —>
  # Assert IdentityDeletedEvent —> Assert ! IdentityChangedEvent —>
  # IDENTITY_DELETED event —> Repeat IDENTITY_CHANGED event —> verify absence (non-deterministic, probabilistic)
  Scenario: Statefulness
    * def externalId = call read('classpath:com/sailpoint/sp/identity/event/utils/uuid.js')
    * print externalId

    # 1.a Publish IDENTITY_DELETED of a non-existing identity
    Given path '/sp-identity-event/debug/publish-event/IDENTITY_DELETED'
    * header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    And request read('IDENTITY_DELETED.json')
    When method POST
    Then status 204

    # 1.b Publish IDENTITY_CHANGED for the same id
    Given path '/sp-identity-event/debug/publish-event/IDENTITY_CHANGED'
    * header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    And request read('IDENTITY_CHANGED.json')
    When method POST
    Then status 204

    # Assert IdentityCreatedEvent emitted
    * def identityCreatedEvent = call read('classpath:com/sailpoint/sp/identity/event/utils/read-redis.js') {type: 'IdentityCreatedEvent', id:'#(externalId)', token: '#(passwordTypeResponse.response.access_token)'}
    Then match identityCreatedEvent == read('output/IdentityCreatedEvent.json')
    # Assert IdentityDeletedEvent absent (polling attempts set to 1)
    * def identityDeletedEvent = call read('classpath:com/sailpoint/sp/identity/event/utils/read-redis.js') {type: 'IdentityDeletedEvent', id:'#(externalId)', token: '#(passwordTypeResponse.response.access_token)', attempts: 1}
    Then match identityDeletedEvent == -1

    # 2.a Publish IDENTITY_CHANGED for the same id
    Given path '/sp-identity-event/debug/publish-event/IDENTITY_CHANGED'
    * header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    And request read('IDENTITY_CHANGED.json')
    When method POST
    Then status 204

    # 2.b Publish IDENTITY_DELETED for the same id
    Given path '/sp-identity-event/debug/publish-event/IDENTITY_DELETED'
    * header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    And request read('IDENTITY_DELETED.json')
    When method POST
    Then status 204

    # Assert IdentityDeletedEvent emitted.
    # It's contents, in this particular scenario, should be the same as IdentityCreatedEvent as there have been no other changes.
    * def identityDeletedEvent = call read('classpath:com/sailpoint/sp/identity/event/utils/read-redis.js') {type: 'IdentityDeletedEvent', id:'#(externalId)', token: '#(passwordTypeResponse.response.access_token)'}
    Then match identityDeletedEvent == read('output/IdentityCreatedEvent.json')

    #Assert IdentityChangedEvent wasn't emitted.
    * def identityCreatedEvent = call read('classpath:com/sailpoint/sp/identity/event/utils/read-redis.js') {type: 'IdentityCreatedEvent', id:'#(externalId)', token: '#(passwordTypeResponse.response.access_token)', attempts: 1}
    Then match identityCreatedEvent == -1

    # 3.a Publish IDENTITY_CHANGED for the same id (identity is now deleted)
    Given path '/sp-identity-event/debug/publish-event/IDENTITY_CHANGED'
    * header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    And request read('IDENTITY_CHANGED.json')
    When method POST
    Then status 204

    # 3.b Publish IDENTITY_DELETED for the same id
    Given path '/sp-identity-event/debug/publish-event/IDENTITY_DELETED'
    * header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    And request read('IDENTITY_DELETED.json')
    When method POST
    Then status 204

    * def identityCreatedEvent = call read('classpath:com/sailpoint/sp/identity/event/utils/read-redis.js') {type: 'IdentityCreatedEvent', id:'#(externalId)', token: '#(passwordTypeResponse.response.access_token)', attempts: 10}
    Then match identityCreatedEvent == -1
    * def identityDeletedEvent = call read('classpath:com/sailpoint/sp/identity/event/utils/read-redis.js') {type: 'IdentityDeletedEvent', id:'#(externalId)', token: '#(passwordTypeResponse.response.access_token)', attempts: 1}
    Then match identityDeletedEvent == -1
