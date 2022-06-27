Feature: Test Template Default REST API

  Background:
    * url baseUrl
    * def creds = callonce read('classpath:com/sailpoint/util/get-oauth-internal-client.js') {name: 'hermes'}
    * def clientCredentialsTypeResponse = call read('classpath:com/sailpoint/authentication/oauth/oathkeeper/client_credentials_grant_type.feature') {apiUser: '#(creds.clientId)', apiKey: '#(creds.secret)'}
    * header Authorization = 'Bearer ' + clientCredentialsTypeResponse.response.access_token

  Scenario: GET all default templates
    Given path '/beta/notification-template-defaults'
	And param sorters = 'key'
    And param count = 'true'
    When method get
    Then status 200
    * print result
    * def default_template = $response[*].key
    * print default_template
    * match default_template contains 'default_template'

  Scenario: GET default templates by email and locale
    Given path '/beta/notification-template-defaults'
    And param filters = 'key eq "default_template" and medium eq "email" and locale eq "en"'
    When method get
    Then status 200
    * print result
    * def default_template = response[0].key
    * print default_template
    * match default_template contains 'default_template'

  Scenario: GET default templates sort by medium
    Given path '/beta/notification-template-defaults'
    And param sorters = 'medium'
    When method get
    Then status 200
    * def size = karate.sizeOf(response)
    * def first_template = response[0].medium
    * def last_template = response[size-1].medium
    * match first_template contains 'EMAIL'
    * match last_template contains 'TEAMS'

  Scenario: GET default templates sort by name
    Given path '/beta/notification-template-defaults'
    And param sorters = 'name'
    When method get
    Then status 200
    * def size = karate.sizeOf(response)
    * def first_template = response[0].name
    * def second_template = response[1].name
    * assert first_template <= second_template

  Scenario: GET default templates filter by name contains
    Given path '/beta/notification-template-defaults'
    And param filters = 'name co "e2e"'
    When method get
    Then status 200
    * match response[0].name contains 'e2e-template-test'



  Scenario: GET default templates filter by description start with
    Given path '/beta/notification-template-defaults'
    And param filters = 'description sw "Default template for rendered emails"'
    When method get
    Then status 200
    * match response[0].name contains 'Default Template'
