Feature: Test Notifications Service: Test all steps.

  Background:
    * url baseUrl
    * def creds = callonce read('classpath:com/sailpoint/util/get-oauth-internal-client.js') {name: 'hermes'}
    * def clientCredentialsTypeResponse = call read('classpath:com/sailpoint/authentication/oauth/oathkeeper/client_credentials_grant_type.feature') {apiUser: '#(creds.clientId)', apiKey: '#(creds.secret)'}
    * def TemplateBulkDeleteDto = [{ "key": "cloud_manual_tasks" }]
    * def fromEmailAddress = 'no-reply-e2e-test@identitysoon.com'
    * header Authorization = 'Bearer ' + clientCredentialsTypeResponse.response.access_token

  Scenario: E2E to sent kafka event EXTENDED_NOTIFICATION_EVENT known by interest matcher and verify all phase with default
    Given path 'hermes/interest-matcher/debug/publish/event/full/notification/EXTENDED_NOTIFICATION_EVENT'
    And request {"recipient":{"id":"2c928090672c901401672c92d33c02ef","name":null,"phone":null,"email":"vasil.shlapkou@sailpoint.com"},"medium":"email","notificationKey":"cloud_user_app_password_changed", "from":"#(fromEmailAddress)","subject":"ATTENTION: Karate test Milestone 2","body":"E2E test","replyTo":"#(fromEmailAddress)","orgId":3,"org":"acme-solar","template":"Cloud User App Password Changed","isTemplateEvaluated":true,"requestId":"e27b0172-94eb-4000-b9b3-6e603de96374"}
    When method post
    Then status 200
    * def result = call read('classpath:com/sailpoint/notification/e2e/read-redis.js') {key: '#(response)', token: '#(clientCredentialsTypeResponse.response.access_token)'}
    * print result
    * match result contains { "fromAddress": "#(fromEmailAddress)", "replyToAddress": "#(fromEmailAddress)", "toAddress": "vasil.shlapkou@sailpoint.com", "subject": "ATTENTION: Karate test Milestone 2", "html": "E2E test", "configurationSet": "HermesDevTestEventDesitination" }

  Scenario: Create custom template
    #Clean data if something wrong with old test
    Given path '/beta/notification-templates/bulk-delete'
    And header Content-Type = 'application/json'
    * header Authorization = 'Bearer ' + clientCredentialsTypeResponse.response.access_token
    And request TemplateBulkDeleteDto
    When method POST

    #Get default
    Given path '/beta/notification-template-defaults'
    * header Authorization = 'Bearer ' + clientCredentialsTypeResponse.response.access_token
    And param filters = 'key eq "cloud_manual_tasks" and medium eq "email" and locale eq "en"'
    And param count = 'true'
    When method get
    Then status 200
    * print result
    * def TemplateDto = response[0]

    #Create custom template
    * eval TemplateDto['header'] = 'Custom Template Header '
    * eval TemplateDto['body'] = 'test create template: $domainEvent.get(\'task\')'
    * eval TemplateDto['subject'] = 'ATTENTION: Karate test Milestone 3';
    Given path '/beta/notification-templates'
    And header Content-Type = 'application/json'
    * header Authorization = 'Bearer ' + clientCredentialsTypeResponse.response.access_token
    And request TemplateDto
    When method POST
    Then status 201
    * print result

    #E2E to send kafka event MANUAL_TASKS and verify was rendered with custom template
    Given path 'hermes/interest-matcher/debug/publish/event/full/notification/MANUAL_TASKS_EVENT'
    And header Content-Type = 'application/json'
    * header Authorization = 'Bearer ' + clientCredentialsTypeResponse.response.access_token
    And request {"task":"finish your homework", "recipient":{"id":"2c928090672c901401672c92d33c02ef","name":null,"phone":null,"email":"vasil.shlapkou@sailpoint.com"},"notificationKey":"cloud_manual_tasks","orgId":3,"org":"acme-solar","isTemplateEvaluated":false,"requestId":"e27b0172-94eb-4000-b9b3-6e603de96374"}
    When method post
    Then status 200
    * def result = call read('classpath:com/sailpoint/notification/e2e/read-redis.js') {key: '#(response)', token: '#(clientCredentialsTypeResponse.response.access_token)'}
    * print result
    * match result.html contains "Custom Template Header test create template: finish your homework<br />Don&#39;t ever reply to this email for you shall hear crickets"

    #Delete custom template
    Given path '/beta/notification-templates/bulk-delete'
    And header Content-Type = 'application/json'
    * header Authorization = 'Bearer ' + clientCredentialsTypeResponse.response.access_token
    And request TemplateBulkDeleteDto
    When method POST
    Then status 204
