Feature: Ordering

  Background:
    * url baseUrl
    * def passwordTypeResponse = callonce read('classpath:com/sailpoint/authentication/oauth/oathkeeper/password_grant_type.feature')

  Scenario: Out of Order Deletion
    * def externalId = call read('classpath:com/sailpoint/sp/identity/event/utils/uuid.js')
    * print externalId

    * def fiveMinutesEarlier =
    """
    function() {
      var Instant = Java.type('java.time.Instant');
      var ChronoUnit = Java.type('java.time.temporal.ChronoUnit');
      return Instant.now()
           .minus(5, ChronoUnit.MINUTES)
           .toString();
    }
    """

    # Publish IDENTITY_CHANGED for a non-existing identity
    Given path '/sp-identity-event/debug/publish-event/IDENTITY_CHANGED'
    * header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    And request read('IDENTITY_CHANGED.json')
    When method POST
    Then status 204

    # Assert IdentityCreatedEvent emitted
    * def identityCreatedEvent = call read('classpath:com/sailpoint/sp/identity/event/utils/read-redis.js') {type: 'IdentityCreatedEvent', id:'#(externalId)', token: '#(passwordTypeResponse.response.access_token)'}
    Then match identityCreatedEvent == read('output/IdentityCreatedEvent.json')

    # Publish IDENTITY_DELETED for the same identity with a past date
    Given path '/sp-identity-event/debug/publish-event/IDENTITY_DELETED'
    * header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    And param created = fiveMinutesEarlier()
    And request read('IDENTITY_DELETED.json')
    When method POST
    Then status 204

    #Assert no IdentityDeletedEvent emitted
    * def identityDeletedEvent = call read('classpath:com/sailpoint/sp/identity/event/utils/read-redis.js') {type: 'IdentityDeletedEvent', id:'#(externalId)', token: '#(passwordTypeResponse.response.access_token)', attempts: 10}
    Then match identityDeletedEvent == -1

    # Publish IDENTITY_DELETED for the same identity (this time it is current)
    Given path '/sp-identity-event/debug/publish-event/IDENTITY_DELETED'
    * header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    And request read('IDENTITY_DELETED.json')
    When method POST
    Then status 204

    #Assert IdentityDeletedEvent emitted
    * def identityDeletedEvent = call read('classpath:com/sailpoint/sp/identity/event/utils/read-redis.js') {type: 'IdentityDeletedEvent', id:'#(externalId)', token: '#(passwordTypeResponse.response.access_token)', attempts: 10}
    Then match identityDeletedEvent == read('output/IdentityCreatedEvent.json')
