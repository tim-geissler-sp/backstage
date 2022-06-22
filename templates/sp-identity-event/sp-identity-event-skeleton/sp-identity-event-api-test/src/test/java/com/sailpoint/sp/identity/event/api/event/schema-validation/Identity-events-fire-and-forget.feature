Feature: sp-identity-event to ETS events (Fire and Forget): idn:identity-created, idn:identity-attributes-changed, idn:account-correlated
  Background:
    * url baseUrl
    * def clientCredentialsResponse = callonce read('classpath:com/sailpoint/authentication/oauth/oathkeeper/client_credentials_grant_type.feature')
    * def passwordTypeResponse = callonce read('classpath:com/sailpoint/authentication/oauth/oathkeeper/password_grant_type.feature')

    * def externalId = call read('classpath:com/sailpoint/sp/identity/event/utils/uuid.js')
    * def identity_created_triggerId = 'idn:identity-created'
    * def identity_attributes_changed_triggerId = 'idn:identity-attributes-changed'
    * def identity_account_correlated_triggerId = 'idn:account-correlated'

    * def now = function(){ return java.lang.System.currentTimeMillis() }
    * def identityCreatedRedisKey = 'Identity-event-KarateTest_created' + now()
    * def identityAttrChangedRedisKey = 'Identity-event-KarateTest_AttrChanged' + now()
    * def accountCorrelatedRedisKey = 'Identity-event-KarateTest_correlated' + now()

    * def identityCreatedDebugURL = baseUrl + 'ets-debug/invocations/success?redisKey=' + identityCreatedRedisKey
    * def identityAttrChangedDebugURL = baseUrl + 'ets-debug/invocations/success?redisKey=' + identityAttrChangedRedisKey
    * def accountCorrelatedDebugURL = baseUrl + 'ets-debug/invocations/success?redisKey=' + accountCorrelatedRedisKey

    # clean subscription after each scenario
    * configure afterScenario =
    """
      function() {
        if (subscriptionId != null) {
          karate.call('classpath:com/sailpoint/sp/identity/event/utils/ets/delete-subscription.feature', {id:subscriptionId});
        }
      }
    """
    * def subscriptionId = null

    * def identityCreatedRequest = read('classpath:com/sailpoint/sp/identity/event/api/event/output/IdentityCreatedEvent.json')
    * def identityAttributeChangedRequest = read('classpath:com/sailpoint/sp/identity/event/api/event/output/IdentityAttributesChangedEvent.json')
    * def identityAccountCorrelatedRequest = read('classpath:com/sailpoint/sp/identity/event/api/event/output/IdentityAccountCorrelatedEvent2.json')

  Scenario: Invoke IdentityCreated Fire and Forget trigger and validate it was executed.
    * def createIdentityDto = {"triggerId": '#(identity_created_triggerId)', "type":"HTTP", "responseDeadline":"PT15M", "httpConfig": {"url":'#(identityCreatedDebugURL)',"httpDispatchMode":"SYNC"}}
    #Subscribe to trigger
    Given path '/beta/trigger-subscriptions'
    And header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    And request createIdentityDto

    When method POST
    Then status 201
    * def subscriptionId = response.id

    Given path '/ets/trigger-invocations/start'
    And header Authorization = 'Bearer ' + clientCredentialsResponse.response.access_token
    And request {"triggerId": '#(identity_created_triggerId)', "input":'#(identityCreatedRequest)', "contentJson":{}}
    When method POST
    Then status 200
    * def invocationId = response[0].id

    # Sleep
    * def result = call read('classpath:com/sailpoint/sp/identity/event/utils/sleep.js') {interval: 5000}

    # Read from redis verify invocation completed.
    * def result = call read('classpath:com/sailpoint/sp/identity/event/utils/ets/read-redis.js') { key: '#(identityCreatedRedisKey)', token: '#(clientCredentialsResponse.response.access_token)'}
    * assert result.identity.name != null
    * match result.identity.id == externalId
    * assert result.identity.type != null

  Scenario: Invoke identityAttributeChanged Fire and Forget trigger and validate it was executed.
    * def identityAttributeChangedDto = {"triggerId": '#(identity_attributes_changed_triggerId)', "type":"HTTP", "responseDeadline":"PT15M", "httpConfig": {"url":'#(identityAttrChangedDebugURL)',"httpDispatchMode":"SYNC"}}
    #Subscribe to trigger
    Given path '/beta/trigger-subscriptions'
    And header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    And request identityAttributeChangedDto

    When method POST
    Then status 201
    * def subscriptionId = response.id

    Given path '/ets/trigger-invocations/start'
    And header Authorization = 'Bearer ' + clientCredentialsResponse.response.access_token
    And request {"triggerId": '#(identity_attributes_changed_triggerId)', "input":'#(identityAttributeChangedRequest)', "contentJson":{}}
    When method POST
    Then status 200
    * def invocationId = response[0].id

    # Sleep
    * def result = call read('classpath:com/sailpoint/sp/identity/event/utils/sleep.js') {interval: 5000}

    # Read from redis verify invocation completed.
    * def result = call read('classpath:com/sailpoint/sp/identity/event/utils/ets/read-redis.js') { key: '#(identityAttrChangedRedisKey)', token: '#(clientCredentialsResponse.response.access_token)'}
    * assert result.changes[0].attribute != null
    * assert result.changes[0].newValue != null
    * assert result.changes[0].oldValue != null

  Scenario: Invoke identityAccountCorrelated Fire and Forget trigger and validate it was executed.
    * def identityAccountCorrelatedDto = {"triggerId": '#(identity_account_correlated_triggerId)', "type":"HTTP", "responseDeadline":"PT15M", "httpConfig": {"url":'#(accountCorrelatedDebugURL)',"httpDispatchMode":"SYNC"}}
    #Subscribe to trigger
    Given path '/beta/trigger-subscriptions'
    And header Authorization = 'Bearer ' + passwordTypeResponse.response.access_token
    And request identityAccountCorrelatedDto

    When method POST
    Then status 201
    * def subscriptionId = response.id

    Given path '/ets/trigger-invocations/start'
    And header Authorization = 'Bearer ' + clientCredentialsResponse.response.access_token
    And request {"triggerId": '#(identity_account_correlated_triggerId)', "input":'#(identityAccountCorrelatedRequest)', "contentJson":{}}
    When method POST
    Then status 200
    * def invocationId = response[0].id

    # Sleep
    * def result = call read('classpath:com/sailpoint/sp/identity/event/utils/sleep.js') {interval: 5000}

    # Read from redis verify invocation completed.
    * def result = call read('classpath:com/sailpoint/sp/identity/event/utils/ets/read-redis.js') { key: '#(accountCorrelatedRedisKey)', token: '#(clientCredentialsResponse.response.access_token)'}
    * assert result.account.id != null
    * assert result.account.name != null
    * assert result.account.nativeIdentity != null
    * assert result.account.type != null

