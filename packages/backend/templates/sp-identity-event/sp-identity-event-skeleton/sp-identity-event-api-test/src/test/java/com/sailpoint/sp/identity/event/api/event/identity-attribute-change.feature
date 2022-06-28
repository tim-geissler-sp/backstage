Feature: Changing Identity Attributes
  Background:
    * url baseUrl
    * def passwordTypeResponse = callonce read('classpath:com/sailpoint/authentication/oauth/oathkeeper/password_grant_type.feature')
    * def externalId = call read('classpath:com/sailpoint/sp/identity/event/utils/uuid.js')
    * def IDENTITY_CHANGED = read('IDENTITY_CHANGED.json')
    * def identityDeleteJson = read('IDENTITY_DELETED.json')


  Scenario:  Verify several minor string change (<name>) produces the expected IdentityAttributesChangedEvent event

    Given path '/sp-identity-event/debug/publish-event/IDENTITY_CHANGED'
    * header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    And request IDENTITY_CHANGED
    When method POST
    Then status 204

    # Publish IDENTITY_CHANGED multiple user attribute changes and an identity change, and verify that a correct IdentityCreatedEvent was made.
    * set IDENTITY_CHANGED.users[0].name = "jeff.sitara"
    * set IDENTITY_CHANGED.users[0].attributes.lastname = "Leroy  "
    * set IDENTITY_CHANGED.users[0].attributes.firstname = "Jeff"
    * set IDENTITY_CHANGED.users[0].attributes.displayName = "sitara leroy"
    * set IDENTITY_CHANGED.users[0].attributes.protected = "true"
    * set IDENTITY_CHANGED.users[0].attributes.isManager = ""

    Given path '/sp-identity-event/debug/publish-event/IDENTITY_CHANGED'
    * header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    And request IDENTITY_CHANGED
    When method POST
    Then status 204
    # We expect to see changes for each attribute changed.  We don't expect to see the change in users[0].name to show up
    * def identityAttributesChangedEvent = call read('classpath:com/sailpoint/sp/identity/event/utils/read-redis.js') {type: 'IdentityAttributesChangedEvent', id:'#(externalId)', token: '#(passwordTypeResponse.response.access_token)', attempts: 30 }
    Then match identityAttributesChangedEvent ==
    """
    {
      "identity": {
        "id" : '#(externalId)',
        "name": "leroy.sitara",
        "type": "IDENTITY"
      },
      "changes" : [
        {
          "attribute":"firstname",
          "oldValue":"Sitara",
          "newValue":"Jeff"
        },
        {
          "attribute":"displayName",
          "oldValue":"Sitara Leroy",
          "newValue":"sitara leroy"
        },
        {
          "attribute":"lastname",
          "oldValue":"Leroy",
          "newValue":"Leroy  "
        },
        {
          "attribute":"protected",
          "oldValue": null,
          "newValue": "true"
        }
      ]
    }
    """

    # Publish IDENTITY_DELETED for the same identity
    Given path '/sp-identity-event/debug/publish-event/IDENTITY_DELETED'
    * header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    And request identityDeleteJson
    When method POST
    Then status 204
