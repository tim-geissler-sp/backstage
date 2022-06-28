Feature: Additional tests for notification-preferences V3 API
#The happy path -- i.e. toggling preference is covered in workItems.feature

  Background:
    * url baseUrl
    * def creds = callonce read('classpath:com/sailpoint/util/get-oauth-internal-client.js') {name: 'hermes'}
    * def clientCredentialsTypeResponse = call read('classpath:com/sailpoint/authentication/oauth/oathkeeper/client_credentials_grant_type.feature') {apiUser: '#(creds.clientId)', apiKey: '#(creds.secret)'}
    * header Authorization = 'Bearer ' + clientCredentialsTypeResponse.response.access_token

  Scenario: Test GET 404
    Given path 'hermes/v3/notification-preferences/incorrect_path_param'
    When method get
    Then status 404

  Scenario: Test PUT 404
    Given path 'hermes/v3/notification-preferences/incorrect_path_param'
    And request {key:"cloud_manual_work_item_summary", "mediums":["EMAIL"]}
    When method put
    Then status 404

  Scenario: Test key mismatch in payload
    Given path 'hermes/v3/notification-preferences/cloud_manual_work_item_summary'
    And request {key:"some_other_value", "mediums":["EMAIL"]}
    When method put
    Then status 400