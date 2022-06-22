Feature: Notification rendered test

  Background:
    * url baseUrl
    * def creds = callonce read('classpath:com/sailpoint/util/get-oauth-internal-client.js') {name: 'hermes'}
    * def clientCredentialsTypeResponse = call read('classpath:com/sailpoint/authentication/oauth/oathkeeper/client_credentials_grant_type.feature') {apiUser: '#(creds.clientId)', apiKey: '#(creds.secret)'}
    * def fromAddress = 'no-reply-e2e-test@identitysoon.com'
    * def toAddress = 'narayanan.srinivasan@sailpoint.com'
    * header Authorization = 'Bearer ' + clientCredentialsTypeResponse.response.access_token

  Scenario: Hermes health check
    Given path 'hermes/health/system'
    When method GET
    Then status 200
    And match response contains { status: 'OK' }

  Scenario: E2E to send email and verify it was submitted to SES
    Given path 'hermes/sender/debug/sendmail'
    And request { recipient: { email: '#(toAddress)'}, medium: 'email', from:'#(fromAddress)', subject:'Kafkaaa 2', body:'free beer', replyTo:'#(fromAddress)' }
    When method post
    Then status 200
    * def result = call read('classpath:com/sailpoint/notification/e2e/read-redis.js') {key: '#(response)', token: '#(clientCredentialsTypeResponse.response.access_token)'}
    * print result
    * match result contains { fromAddress: '#(fromAddress)', replyToAddress: '#(fromAddress)', toAddress: '#(toAddress)', subject: 'Kafkaaa 2', html: 'free beer' }

  Scenario: E2E to send email with an invalid recipient address and verify the exception
    Given path 'hermes/sender/debug/sendmail'
    And request { recipient: { email: 'invalidEmail'}, medium: 'email', from:'#(fromAddress)', subject:'Kafkaaa 2', body:'free beer' }
    When method post
    Then status 200
    * def result = call read('classpath:com/sailpoint/notification/e2e/read-redis.js') {key: '#(response)', token: '#(clientCredentialsTypeResponse.response.access_token)'}
    * print result
    * match result contains 'java.lang.IllegalArgumentException: Recipient address invalidEmail is invalid'
