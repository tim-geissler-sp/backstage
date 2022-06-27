Feature: Trigger Schedule

  Background:
    * url baseUrl
    * def creds = callonce read('classpath:com/sailpoint/util/get-oauth-internal-client.js') {name: 'sp-scheduler'}
    * def clientCredentialsTypeResponse = call read('classpath:com/sailpoint/authentication/oauth/oathkeeper/client_credentials_grant_type.feature') {apiUser: '#(creds.clientId)', apiKey: '#(creds.secret)'}
    * header Authorization = 'Bearer ' + clientCredentialsTypeResponse.response.access_token
    * configure retry = { count: 10, interval: 2000 }

  Scenario: Trigger a schedule now, no cronstring
    * def now =
    """
    function() {
      var Instant = Java.type('java.time.Instant');
      return Instant.now().toString();
    }
    """

    * def waitUntilNoResult =
    """
    function(actionId) {
      karate.log('input ' + actionId)
      var attempts = 10
      while(attempts--> 0) {
        var result = karate.call('../util/find-util.feature', {'id': actionId});
        karate.log('result ' + result.found);
        if (result.found.length == 0) {
          return result.found.length;
        }
        java.lang.Thread.sleep(2000);
      }
      return result.found.length
    }
    """

    * table actions
      | event                                                   | deadline                | meta                        |
      | { topic : 'internal_test', type : 'scheduler-test' }    | now()                   | { key : 'trigger test'}     |

    * def result = call read('../util/create-without-cron-util.feature') actions
    * def status = $result[0].responseStatus
    * match status == 200

    # Take the action Id and poll 'sp-scheduler/scheduled-actions' until it goes away
    * def actionId = $result[0].response.id
    * def pollResult = call waitUntilNoResult actionId
    * match pollResult == 0

  Scenario: Trigger a schedule now, with cronstring
    * def now =
    """
    function() {
      var Instant = Java.type('java.time.Instant');
      return Instant.now().toString();
    }
    """

    # not ideal sleeping for 10 seconds as the result could still be
    # undeterministic(deleting/saving in the db), however the chance of DB being
    # slow is extremely being low
    * def waitForAResult =
    """
    function(actionId) {
      karate.log('input ' + actionId)
      java.lang.Thread.sleep(10000);
      var result = karate.call('../util/find-util.feature', {'id': actionId});
      karate.log('result ' + result.found);
      return result.found.length
    }
    """

    * table actions
      | event                                                   | deadline                | meta                        | cronString    |
      | { topic : 'internal_test', type : 'scheduler-test' }    | now()                   | { key : 'trigger test'}     | '* * */5 * *' |

    * def result = call read('../util/create-with-cron-util.feature') actions
    * def status = $result[0].responseStatus
    * match status == 200

    # Take the action Id and poll 'sp-scheduler/scheduled-actions'.
    # We should see that the schedule persists since there is a cronstring
    * def actionId = $result[0].response.id
    * def pollResult = call waitForAResult actionId
    * match pollResult == 1
