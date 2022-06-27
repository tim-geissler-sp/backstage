Feature: Test Template Config REST API

  Background:
    * url baseUrl
    * def creds = callonce read('classpath:com/sailpoint/util/get-oauth-internal-client.js') {name: 'hermes'}
    * def clientCredentialsTypeResponse = call read('classpath:com/sailpoint/authentication/oauth/oathkeeper/client_credentials_grant_type.feature') {apiUser: '#(creds.clientId)', apiKey: '#(creds.secret)'}
    * header Authorization = 'Bearer ' + clientCredentialsTypeResponse.response.access_token
    * def TemplateBulkDeleteDto = [{ "key": "default_template", "medium": "sms", "locale": "fr" }]
    * def TemplateBulkDeleteDtoNameOnly = [{ "key": "default_template" , "medium": "", "locale": ""}]

  Scenario: Verify create, delete custom template
    Given path '/beta/notification-template-defaults'
    And param filters = 'key eq "default_template" and medium eq "email" and locale eq "en"'
    And param count = 'true'
    When method get
    Then status 200
    * print result
    * def TemplateDto = response[0]

    #Create custom template
    * eval TemplateDto['body'] = 'test create template'
    Given path '/beta/notification-templates'
    And header Content-Type = 'application/json'
    * header Authorization = 'Bearer ' + clientCredentialsTypeResponse.response.access_token
    And request TemplateDto
    When method POST
    Then status 201
    * print result

    #Verify created
    Given path '/beta/notification-templates'
    * header Authorization = 'Bearer ' + clientCredentialsTypeResponse.response.access_token
    And param filters = 'key eq "default_template"'
    And param count = 'true'
    When method get
    Then status 200
    * def resultDto = response[0]
    * print resultDto
    * match resultDto.key contains 'default_template'
    * match resultDto.body contains 'test create template'

    #Get by id
    Given path '/beta/notification-templates/' + resultDto.id
    * header Authorization = 'Bearer ' + clientCredentialsTypeResponse.response.access_token
    When method get
    Then status 200
    * def resultDto = response
    * print resultDto
    * match resultDto.key contains 'default_template'
    * match resultDto.body contains 'test create template'

     #Create custom template for different locale and medium
    * eval TemplateDto['locale'] = 'fr'
    * eval TemplateDto['medium'] = 'SMS'
    * eval TemplateDto['body'] = 'test create template french and sms'
    Given path '/beta/notification-templates'
    And header Content-Type = 'application/json'
    * header Authorization = 'Bearer ' + clientCredentialsTypeResponse.response.access_token
    And request TemplateDto
    When method POST
    Then status 201
    * print result

    #Verify created
    Given path '/beta/notification-templates'
    * header Authorization = 'Bearer ' + clientCredentialsTypeResponse.response.access_token
    And param filters = 'key eq "default_template" and medium eq "sms" and locale eq "fr"'
    And param count = 'true'
    When method get
    Then status 200
    * def resultDto = response[0]
    * print resultDto
    * match resultDto.key contains 'default_template'
    * match resultDto.body contains 'test create template french and sms'

    #Delete template by medium, locale, name
    Given path '/beta/notification-templates/bulk-delete'
    * header Authorization = 'Bearer ' + clientCredentialsTypeResponse.response.access_token
    And request TemplateBulkDeleteDto
    When method POST
    Then status 204

    #Verify deleted
    Given path '/beta/notification-templates'
    * header Authorization = 'Bearer ' + clientCredentialsTypeResponse.response.access_token
    And param filters = 'key eq "default_template" and medium eq "sms" and locale eq "fr"'
    And param count = 'true'
    When method get
    Then status 200
    * match response == '#[0]'

    #Try to delete template by name, already deleted should fail
    Given path '/beta/notification-templates/bulk-delete'
    * header Authorization = 'Bearer ' + clientCredentialsTypeResponse.response.access_token
    And request TemplateBulkDeleteDtoNameOnly
    When method POST
    Then status 400

    #Verify all deleted
    Given path '/beta/notification-templates'
    * header Authorization = 'Bearer ' + clientCredentialsTypeResponse.response.access_token
    When method get
    Then status 200
    * match response == '#[0]'
    * match response.size() == 0


  Scenario: Verify sorter=name
    #Create custom template 1 with name "Non-Employee Request Created"
    Given path '/beta/notification-template-defaults'
    And param filters = 'key eq "non_employee_request_created" and medium eq "email" and locale eq "en"'
    And param count = 'true'
    And header Authorization = 'Bearer ' + clientCredentialsTypeResponse.response.access_token
    When method get
    Then status 200
    * print result
    * def TemplateDto1 = response[0]

    * eval TemplateDto1['body'] = 'test create template'
    Given path '/beta/notification-templates'
    And header Content-Type = 'application/json'
    * header Authorization = 'Bearer ' + clientCredentialsTypeResponse.response.access_token
    And request TemplateDto1
    When method POST
    Then status 201
    * print result

    #Create custom template 2 with name "Task Manager Subscription"
    Given path '/beta/notification-template-defaults'
    And param filters = 'key eq "cloud_manual_work_item_summary" and medium eq "email" and locale eq "en"'
    And param count = 'true'
    And header Authorization = 'Bearer ' + clientCredentialsTypeResponse.response.access_token
    When method get
    Then status 200
    * print result
    * def TemplateDto1 = response[0]

    * eval TemplateDto1['body'] = 'test create template'
    Given path '/beta/notification-templates'
    And header Content-Type = 'application/json'
    * header Authorization = 'Bearer ' + clientCredentialsTypeResponse.response.access_token
    And request TemplateDto1
    When method POST
    Then status 201
    * print result

    #Verify sort order between two custom templates
    Given path '/beta/notification-templates'
    * header Authorization = 'Bearer ' + clientCredentialsTypeResponse.response.access_token
    And param sorters = 'name'
    When method get
    Then status 200
    * def resultDto1 = response[0].name
    * def resultDto2 = response[1].name
    * match resultDto1 contains 'Non-Employee Request Created'
    * match resultDto2 contains 'Task Manager Subscription'

    #Verify name filter contains
    Given path '/beta/notification-templates'
    * header Authorization = 'Bearer ' + clientCredentialsTypeResponse.response.access_token
    And param filters = 'name co "Non-Employee"'
    When method get
    Then status 200
    * def resultDto1 = response[0].name
    * match resultDto1 contains 'Non-Employee Request Created'

    #Verify description filter start with
    Given path '/beta/notification-templates'
    * header Authorization = 'Bearer ' + clientCredentialsTypeResponse.response.access_token
    And param filters = 'description sw "Daily"'
    When method get
    Then status 200
    * def resultDto1 = response[0].name
    * match resultDto1 contains 'Task Manager Subscription'

    #Try to delete template by name, already deleted should fail
    Given path '/beta/notification-templates/bulk-delete'
    * header Authorization = 'Bearer ' + clientCredentialsTypeResponse.response.access_token
    And request [{ "key": "cloud_manual_work_item_summary" , "medium": "email", "locale": "en"}, { "key": "non_employee_request_created" , "medium": "email", "locale": "en"}]
    When method POST
    Then status 204

    #Verify all deleted
    Given path '/beta/notification-templates'
    * header Authorization = 'Bearer ' + clientCredentialsTypeResponse.response.access_token
    When method get
    Then status 200
    * match response == '#[0]'
    * match response.size() == 0
