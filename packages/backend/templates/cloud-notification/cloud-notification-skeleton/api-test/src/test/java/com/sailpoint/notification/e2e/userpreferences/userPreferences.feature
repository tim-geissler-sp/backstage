Feature: Test Phase: User Preferences

  Background:
    * url baseUrl
    * def creds = callonce read('classpath:com/sailpoint/util/get-oauth-internal-client.js') {name: 'hermes'}
    * def clientCredentialsTypeResponse = call read('classpath:com/sailpoint/authentication/oauth/oathkeeper/client_credentials_grant_type.feature') {apiUser: '#(creds.clientId)', apiKey: '#(creds.secret)'}
    * header Authorization = 'Bearer ' + clientCredentialsTypeResponse.response.access_token
    * def InterestEvent = read('classpath:com/sailpoint/notification/e2e/userpreferences/interestEvent.json')
    * configure retry = { count: 3, interval: 3000 }

  Scenario: E2E to sent kafka IdentityCreatedEvent event known by interest matcher and verify it is saved in userPreference Repo
    Given path 'hermes/user/preferences/debug/publish/event/identity_event/IdentityCreatedEvent'
    And request
    """
      {
        "identity": {
            "id":"19983d18-78c1-4c10-85b3-dce050d7cd37",
            "name":"JaneDoe",
            "type":"IDENTITY"
          },
        "attributes": {
            "firstname":"Jane",
            "last name":"Doe",
            "email":"jane.doe@sailpoint.com",
            "displayName":"New Test User",
            "phone":"5125125125",
            "brand":"default"
            }
       }
    """
    When method post
    Then status 200

    # Wait for repo userPreference creation triggered by IdentityCreatedEvent
    * def result = call read('classpath:com/sailpoint/notification/e2e/sleep.js') {interval: 5000}

    #retrieve new userPreference from userPreference Repo
    * header Authorization = 'Bearer ' + clientCredentialsTypeResponse.response.access_token
    Given path 'hermes/user/preferences/debug/' + podName + '__' + orgName
    And request {}
    When method get
    Then status 200
    And match $ contains {"recipient":{"phone":"5125125125","name":"New Test User","id":"19983d18-78c1-4c10-85b3-dce050d7cd37","email":"jane.doe@sailpoint.com"},"brand":{"value":"default"}}

    #Sent kafka event known by interest matcher and verify it was matched
    * header Authorization = 'Bearer ' + clientCredentialsTypeResponse.response.access_token
    Given path 'hermes/user/preferences/debug/publish/event/notification/NOTIFICATION_INTEREST_MATCHED'
    And request InterestEvent
    When method post
    Then status 200
    * def slackEventKey = response + ":SLACK"
    * print "Verify slack event posted -- key: ", slackEventKey
    * def result = call read('classpath:com/sailpoint/notification/e2e/read-redis.js') {key: '#(slackEventKey)', token: '#(clientCredentialsTypeResponse.response.access_token)'}
    * match result contains 'jane.doe@sailpoint.com'
    * def emailEventKey = response + ":EMAIL"
    * print "Verify email event event posted -- key: ", emailEventKey
    * def result = call read('classpath:com/sailpoint/notification/e2e/read-redis.js') {key: '#(emailEventKey)', token: '#(clientCredentialsTypeResponse.response.access_token)'}
    * match result contains 'jane.doe@sailpoint.com'
    * def teamsEventKey = response + ":TEAMS"
    * print "Verify teams event event posted -- key: ", teamsEventKey
    * def result = call read('classpath:com/sailpoint/notification/e2e/read-redis.js') {key: '#(teamsEventKey)', token: '#(clientCredentialsTypeResponse.response.access_token)'}
    * match result contains 'jane.doe@sailpoint.com'

  Scenario: E2E to sent kafka IdentityAttributesChangedEvent event known by interest matcher and verify it is saved in userPreference Repo
    Given path 'hermes/user/preferences/debug/publish/event/identity_event/IdentityAttributesChangedEvent'
    And request
    """
      {
        "identity": {
          "id": "19983d18-78c1-4c10-85b3-dce050d7cd37",
          "name": "JaneDoe",
          "type": "IDENTITY"
        },
        "changes": [
          {
            "attribute": "email",
            "oldValue": "jane.doe@sailpoint.com",
            "newValue": "jane.smith@sailpoint.com"
          }
        ]
      }
      """
    When method post
    Then status 200

    # Wait for userPreference update triggered by IdentityAttributesChangedEvent
    * def result = call read('classpath:com/sailpoint/notification/e2e/sleep.js') {interval: 5000}

    #retrieve userPreference with updated attributes from userPreference Repo
    And header Authorization = 'Bearer ' + clientCredentialsTypeResponse.response.access_token
    Given path 'hermes/user/preferences/debug/' + podName + '__' + orgName
    And request {}
    When method get
    Then status 200
    And match $ contains {"recipient":{"phone":"5125125125","name":"New Test User","id":"19983d18-78c1-4c10-85b3-dce050d7cd37","email":"jane.smith@sailpoint.com"},"brand":{"value":"default"}}

  Scenario: E2E to sent kafka IdentityDeletedEvent event known by interest matcher and verify it is removed from userPreference Repo
    Given path 'hermes/user/preferences/debug/publish/event/identity_event/IdentityDeletedEvent'
    And request {"identity":{"id":"19983d18-78c1-4c10-85b3-dce050d7cd37","name":"New Test User","type":"IDENTITY"},"attributes":{"firstname":"Jane","last name":"Doe","email":"jane.smith@sailpoint.com","displayName":"New Test User","phone":"5125125125","brand":"default"}}
    When method post
    Then status 200

    # Wait for repo deletion triggered by IdentityDeletedEvent
    * def result = call read('classpath:com/sailpoint/notification/e2e/sleep.js') {interval: 5000}

    #Verify target userPreference is deleted from userPreference Repo
    And header Authorization = 'Bearer ' + clientCredentialsTypeResponse.response.access_token
    Given path 'hermes/user/preferences/debug/' + podName + '__' + orgName
    And request {}
    When method get
    Then status 200
    And match $ !contains {"recipient":{"phone":"5125125125","name":"New Test User","id":"19983d18-78c1-4c10-85b3-dce050d7cd37","email":"jane.smith@sailpoint.com"},"brand":{"value":"default"}}
