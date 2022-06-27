Feature: Test Phase: Interest Matched

  Background:
    * url baseUrl
    * def creds = callonce read('classpath:com/sailpoint/util/get-oauth-internal-client.js') {name: 'hermes'}
    * def clientCredentialsTypeResponse = call read('classpath:com/sailpoint/authentication/oauth/oathkeeper/client_credentials_grant_type.feature') {apiUser: '#(creds.clientId)', apiKey: '#(creds.secret)'}
    * def fromEmailAddress = 'no-reply-e2e-test@identitysoon.com'
    * header Authorization = 'Bearer ' + clientCredentialsTypeResponse.response.access_token

  Scenario: E2E to sent kafka event EXTENDED_NOTIFICATION_EVENT known by interest matcher and verify it was matched
    Given path 'hermes/interest-matcher/debug/publish/event/notification/EXTENDED_NOTIFICATION_EVENT'
    And request {"recipient":{"id":"2c928090672c901401672c92d33c02ef","name":null,"phone":null,"email":"vasil.shlapkou@sailpoint.com"},"medium":"email","notificationKey":"cloud_user_app_password_changed", "from":"#(fromEmailAddress)","subject":"[Original recipient: cloud-support@sailpoint.com] ATTENTION: Your SailPoint password update was successful","body":"\u003cfont face\u003d\"helvetica,arial,sans-serif\"\u003e\tDear Cloud Support,\u003cbr /\u003e\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\u003c/font\u003e\u003cp\u003eIf you did not make this change please contact your IT administrator immediately.\u003cbr /\u003e\u003c/p\u003e\t\u003cp\u003eThanks,\u003cbr /\u003eThe SailPoint Team\u003c/p\u003e","replyTo":"#(fromEmailAddress)","orgId":3,"org":"acme-solar","template":"Cloud User App Password Changed","isTemplateEvaluated":true,"requestId":"e27b0172-94eb-4000-b9b3-6e603de96374"}
    When method post
    Then status 200
    * def result = call read('classpath:com/sailpoint/notification/e2e/read-redis.js') {key: '#(response)', token: '#(clientCredentialsTypeResponse.response.access_token)'}
    * print result
    * match result contains '2c928090672c901401672c92d33c02efvasil.shlapkou@sailpoint.com'
  Scenario: E2E to sent kafka event ACCESS_APPROVAL_REQUESTED and verify it was matched and split into 2 events
    Given path 'hermes/interest-matcher/debug/publish/event/notification/ACCESS_APPROVAL_REQUESTED'
    And request {"content": { "approvers": [{ "id": "314cf125-f892-4b16-bcbb-bfe4afb01f85", "name": "james.smith@test" }, { "id": "70e7cde5-3473-46ea-94ea-90bc8c605a6c", "name": "jane.doe@test" }], "requester_id": "46ec3058-eb0a-41b2-8df8-1c3641e4d771", "requester_name": "boss.man", "accessItems": [{ "type": "ROLE", "name": "Engineering Administrator" }] }}
    When method post
    Then status 200
    * def result = call read('classpath:com/sailpoint/notification/e2e/read-redis.js') {key: '#(response)', token: '#(clientCredentialsTypeResponse.response.access_token)'}
    * print result
    * match result contains '314cf125-f892-4b16-bcbb-bfe4afb01f8570e7cde5-3473-46ea-94ea-90bc8c605a6cjames.smith@testjane.doe@test'
