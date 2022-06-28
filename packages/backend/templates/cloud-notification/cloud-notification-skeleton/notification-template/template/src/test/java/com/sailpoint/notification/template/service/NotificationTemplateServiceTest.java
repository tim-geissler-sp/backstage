/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.template.service;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Provider;
import com.sailpoint.atlas.event.EventService;
import com.sailpoint.atlas.featureflag.FeatureFlagService;
import com.sailpoint.notification.api.event.dto.SlackNotificationRendered;
import com.sailpoint.notification.api.event.dto.TeamsNotificationRendered;
import com.sailpoint.notification.template.engine.impl.velocity.TemplateEngineVelocity;
import com.sailpoint.notification.template.util.EscapeUtil;
import com.sailpoint.utilities.JsonUtil;
import com.sailpoint.iris.client.Event;
import com.sailpoint.iris.client.EventBuilder;
import com.sailpoint.iris.client.TopicDescriptor;
import com.sailpoint.notification.api.event.dto.NotificationRendered;
import com.sailpoint.notification.context.service.GlobalContextService;
import com.sailpoint.notification.template.common.model.NotificationTemplate;
import com.sailpoint.notification.template.common.repository.TemplateRepository;
import com.sailpoint.notification.template.context.TemplateContext;
import com.sailpoint.notification.template.context.impl.velocity.TemplateContextVelocity;
import com.sailpoint.notification.template.engine.TemplateEngine;
import com.sailpoint.notification.userpreferences.repository.UserPreferencesRepository;
import org.apache.velocity.app.VelocityEngine;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static com.sailpoint.notification.template.service.NotificationTemplateService.HERMES_TEMPLATES_HTML_SANITIZE_ENABLED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

/**
 * Unit test for NotificationTemplateService
 */
@RunWith(MockitoJUnitRunner.class)
public class NotificationTemplateServiceTest {

	private static final String TEMPLATE_REPOSITORY_JSON_1 =
			"{\n"+
			"    \"tenant\": \"acme-lunar\",\n"+
			"    \"name\" : \"velocity-welcome\",\n"+
			"    \"medium\": \"sms\",\n"+
			"    \"description\": \"testTemplateDescription3\",\n"+
			"    \"subject\" : \"$event.get('medium') notification!\",\n"+
			"    \"header\": \"Welcome to SailPoint.\",\n"+
			"    \"body\": \"Hello $event.get('recipient.name')\",\n"+
			"    \"footer\": \"See you soon!\",\n"+
			"	 \"from\": \"no-reply@sailpoint.com\",\n"+
			"	 \"replyTo\": \"no-reply@sailpoint.com\",\n" +
			"    \"locale\": \"en\"\n"+
			"  }";
	private static final String TEMPLATE_REPOSITORY_JSON_2 =
			"  {\n" +
			"    \"key\" : \"default_template\",\n" +
			"    \"medium\": \"email\",\n" +
			"    \"description\": \"Default template for rendered emails\",\n" +
			"    \"subject\" : \"Subject = '\",\n" +
			"    \"header\":\"\",\n" +
			"    \"body\": \"$domainEvent.get('body')\",\n" +
			"    \"footer\": \"\",\n" +
			"    \"from\": \"$domainEvent.get('from')\",\n" +
			"    \"replyTo\": \"$domainEvent.get('replyTo')\",\n" +
			"    \"locale\": \"en\"\n" +
			"  }";

	private static final String TEMPLATE_REPOSITORY_JSON_SLACK =
			"  {\n" +
					"    \"key\" : \"default_template_slack\",\n" +
					"    \"medium\": \"slack\",\n" +
					"    \"description\": \"Default template for rendered slacks\",\n" +
					"    \"slackTemplate\": { \n" +
					"	 \"text\" : \"$domainEvent.get('text')\",\n" +
					"	 \"blocks\" : \"$domainEvent.get('blocks')\",\n" +
					"	 \"attachments\" : \"$domainEvent.get('attachments')\",\n" +
					"    \"autoApprovalData\": { \n" +
					"    \"itemId\": \"itemId\",\n" +
					"    \"itemType\": \"itemType\",\n" +
					"    \"autoApprovalMessageJSON\": \"autoApprovalMessageJSON\",\n" +
					"    \"autoApprovalTitle\": \"autoApprovalTitle\"\n" +
					"     }, \n" +
					"    \"requestId\":\"requestId\", \n" +
					"    \"approvalId\":\"approvalId\", \n" +
					"    \"customFields\": { \n" +
					"    \"campaignId\": \"campaignId\",\n" +
					"    \"campaignStatus\": \"campaignStatus\"\n" +
					"     } \n" +
					"     }, \n" +
					"    \"locale\": \"en\"\n" +
					"  }";

	private static final String TEMPLATE_SPECIAL_SYMBOLS_JSON_SLACK =
			"  {\n" +
					"    \"key\" : \"default_template_slack\",\n" +
					"    \"medium\": \"slack\",\n" +
					"    \"description\": \"Default template for rendered slacks\",\n" +
					"    \"slackTemplate\": { \n" +
					"	 \"text\" : \"$__esc.java($text)\",\n" +
					"	 \"blocks\" : \"$__esc.java($blocks)\",\n" +
					"	 \"attachments\" : \"$__esc.java($attachments)\",\n" +
					"    \"autoApprovalData\": { \n" +
					"    \"itemId\": \"itemId\",\n" +
					"    \"itemType\": \"itemType\",\n" +
					"    \"autoApprovalMessageJSON\": \"autoApprovalMessageJSON\",\n" +
					"    \"autoApprovalTitle\": \"autoApprovalTitle\"\n" +
					"     }, \n" +
					"    \"requestId\":\"requestId\", \n" +
					"    \"approvalId\":\"approvalId\", \n" +
					"    \"customFields\": { \n" +
					"    \"campaignId\": \"campaignId\",\n" +
					"    \"campaignStatus\": \"campaignStatus\"\n" +
					"     } \n" +
					"     }, \n" +
					"    \"locale\": \"en\"\n" +
					"  }";

	private static final String TEMPLATE_REPOSITORY_JSON_TEAMS =
			"  {\n" +
					"    \"key\" : \"default_template_teams\",\n" +
					"    \"medium\": \"teams\",\n" +
					"    \"description\": \"Default template for rendered teams\",\n" +
					"    \"teamsTemplate\": { \n" +
					"	 \"title\" : \"$domainEvent.get('title')\",\n" +
					"	 \"text\" : \"$domainEvent.get('text')\",\n" +
					"	 \"messageJson\" : \"$domainEvent.get('messageJson')\",\n" +
					"    \"autoApprovalData\": { \n" +
					"    \"itemId\": \"itemId\",\n" +
					"    \"itemType\": \"itemType\",\n" +
					"    \"autoApprovalMessageJSON\": \"autoApprovalMessageJSON\",\n" +
					"    \"autoApprovalTitle\": \"autoApprovalTitle\"\n" +
					"     }, \n" +
					"    \"customFields\": { \n" +
					"    \"campaignId\": \"campaignId\",\n" +
					"    \"campaignStatus\": \"campaignStatus\"\n" +
					"     } \n" +
					"     }, \n" +
					"    \"locale\": \"en\"\n" +
					"  }";

	private static final String TEMPLATE_SPECIAL_SYMBOLS_JSON_TEAMS =
			"  {\n" +
					"    \"key\" : \"default_template_teams\",\n" +
					"    \"medium\": \"teams\",\n" +
					"    \"description\": \"Default template for rendered teams\",\n" +
					"    \"teamsTemplate\": { \n" +
					"	 \"title\" : \"$__esc.javascript($title)\",\n" +
					"	 \"text\" : \"$__esc.javascript($text)\",\n" +
					"	 \"messageJson\" : \"$__esc.javascript($messageJson)\",\n" +
					"    \"autoApprovalData\": { \n" +
					"    \"itemId\": \"itemId\",\n" +
					"    \"itemType\": \"itemType\",\n" +
					"    \"autoApprovalMessageJSON\": \"autoApprovalMessageJSON\",\n" +
					"    \"autoApprovalTitle\": \"autoApprovalTitle\"\n" +
					"     }, \n" +
					"    \"customFields\": { \n" +
					"    \"campaignId\": \"campaignId\",\n" +
					"    \"campaignStatus\": \"campaignStatus\"\n" +
					"     } \n" +
					"     }, \n" +
					"    \"locale\": \"en\"\n" +
					"  }";

	private static final String DOMAIN_EVENT = "{\"headers\":" +
			"{\"pod\":\"dev\",\"org\":\"acme-solar\"," +
			"\"requestId\":\"21a4d4fcbb534feba6a6db369942a587\"}," +
			"\"id\":\"a900a78a463842e59d2f16c8bdf6e0d7\",\"timestamp\":\"2019-02-15T12:40:36.555-06:00\"," +
			"\"type\":\"NOTIFICATION_INTEREST_MATCHED\"," +
			"\"contentJson\":\"{\\\"notificationId\\\":\\\"64e67a99-1869-4da6-97ac-a68f5f57466b\\\"," +
			"\\\"recipientId\\\":\\\"2c928090672c901401672c92d33c02ef\\\",\\\"recipientEmail\\\":" +
			"\\\"vasil.shlapkou@sailpoint.com\\\",\\\"interestName\\\":\\\"Extended Notification Event\\\"," +
			"\\\"categoryName\\\":\\\"email\\\",\\\"domainEvent\\\":{\\\"headers\\\":{\\\"pod\\\":" +
			"\\\"dev\\\",\\\"org\\\":\\\"acme-solar\\\"},\\\"id\\\":\\\"50755c7e6089492788fecf2cc7d40904\\\"," +
			"\\\"timestamp\\\":\\\"2019-02-15T18:40:36.165Z\\\",\\\"type\\\":\\\"EXTENDED_NOTIFICATION_EVENT\\\"," +
			"\\\"contentJson\\\":\\\"{\\\\\\\"recipient\\\\\\\":{\\\\\\\"id\\\\\\\":\\\\\\\"2c928090672c901401672c92d33c02ef\\\\\\\"," +
			"\\\\\\\"name\\\\\\\":null,\\\\\\\"phone\\\\\\\":null,\\\\\\\"email\\\\\\\":\\\\\\\"vasil.shlapkou@sailpoint.com\\\\\\\"}," +
			"\\\\\\\"medium\\\\\\\":\\\\\\\"email\\\\\\\",\\\\\\\"from\\\\\\\":\\\\\\\"no-reply@sailpoint.com\\\\\\\"," +
			"\\\\\\\"subject\\\\\\\":\\\\\\\"ATTENTION: Your SailPoint password update was successful\\\\\\\"," +
			"\\\\\\\"body\\\\\\\":\\\\\\\"\\\\\\\"," +
			"\\\\\\\"replyTo\\\\\\\":\\\\\\\"ggg@ffff\\\\\\\"," +
			"\\\\\\\"orgId\\\\\\\":3,\\\\\\\"org\\\\\\\":" +
			"\\\\\\\"acme-solar\\\\\\\",\\\\\\\"notificationKey\\\\\\\":\\\\\\\"cloud_user_app_password_changed" +
			"\\\\\\\",\\\\\\\"isTemplateEvaluated\\\\\\\":true,\\\\\\\"" +
			"requestId\\\\\\\":\\\\\\\"e00239ee-311a-4e5e-8026-9ee3ced8692a\\\\\\\"}" +
			"\\\"},\\\"notificationKey\\\":\\\"cloud_user_app_password_changed\\\"}\"}";

	private static final String JSON_CONTENT =	"\"recipient\": {  \n" +
			"            \"id\": \"70e7cde5-3473-46ea-94ea-90bc8c605a6c\",  \n" +
			"            \"name\": \"james.smith\",  \n" +
			"            \"email\": \"james.smith@sailpoint.com\",  \n" +
			"            \"phone\": \"512-888-8888\"  \n" +
			"        },  \n" +
			"        \"medium\": \"sms\",  \n" +
			"        \"template\": \"approval-template-request-sms\"  \n" +
			"    }  ";

	private static final String JSON_CONTENT_1 = "{  \n" +
			"        \"domainEvent\": " + DOMAIN_EVENT + ","
			+ JSON_CONTENT;


	private static final String JSON_CONTENT_2 = "{  \n" +
			"        \"domainEvent\": \"(copy of original event)\",  \n"
			+ JSON_CONTENT;

	private static final String GLOBAL_CONTEXT_PRODUCT_NAME = "Acme Solar";

	@Mock
	private Provider<EventService> _eventServiceProvider;

	@Mock
	private EventService _eventService;

	@Captor
	private ArgumentCaptor<Event> _eventArgumentCaptor;

	@Mock
	private TemplateRepository _templateRepository;

	@Mock
	private TemplateEngineVelocity _templateEngine;

	@Mock
	private GlobalContextService _globalContextService;

	@Mock
	private UserPreferencesRepository _userPreferencesRepository;

	@Mock
	public FeatureFlagService _featureFlagService;

	private NotificationTemplateService _notificationTemplateService;

	@Before
	public void setUp() {

		when(_globalContextService.findOneByTenant(anyString())).thenReturn(Optional.empty());

		when(_templateRepository.findOneByTenantAndKeyAndMediumAndLocale(anyString(), anyString(), anyString(), any()))
				.thenAnswer(invocation -> JsonUtil.parse(NotificationTemplate.class, TEMPLATE_REPOSITORY_JSON_2));

		when(_featureFlagService.getBoolean(HERMES_TEMPLATES_HTML_SANITIZE_ENABLED, false)).thenReturn(true);

		_notificationTemplateService =
				new NotificationTemplateService(_templateRepository, _eventServiceProvider, _templateEngine,
						_userPreferencesRepository, _featureFlagService);
	}

	@Test
	public void globalContextTest() {

		Map<String, Object> globalContext = givenGlobalContext();

		// When getTemplateContext is called
		final TemplateContext templateContext1 = _notificationTemplateService.getTemplateContext(null, JSON_CONTENT_1, globalContext);

		// Then
		assertEquals(GLOBAL_CONTEXT_PRODUCT_NAME, ((TemplateContext)templateContext1.get("global")).get("productName"));
	}

	@Test
	public void templateContextTest() {
		// When getTemplateContext is called
		final TemplateContext templateContext1 = _notificationTemplateService.getTemplateContext(null, JSON_CONTENT_1, Collections.emptyMap());

		//Then
		assertEquals(((TemplateContext)templateContext1.get("event")).get("recipient.id"), "70e7cde5-3473-46ea-94ea-90bc8c605a6c");
		assertEquals(((TemplateContext)templateContext1.get("event")).get("medium"), "sms");

		assertEquals(((TemplateContext)templateContext1.get("domainEvent")).get("recipient.id"), "2c928090672c901401672c92d33c02ef");
		assertEquals(((TemplateContext)templateContext1.get("domainEvent")).get("subject"), "ATTENTION: Your SailPoint password update was successful");

		// When getTemplateContext is called
		final TemplateContext templateContext2 = _notificationTemplateService.getTemplateContext(null, JSON_CONTENT_2, Collections.emptyMap());

		// Then
		assertEquals(((TemplateContext)templateContext2.get("event")).get("recipient.id"), "70e7cde5-3473-46ea-94ea-90bc8c605a6c");
		assertEquals(((TemplateContext)templateContext2.get("event")).get("medium"), "sms");
	}

	@Test
	public void globalContextTestV2() {

		Map<String, Object> globalContext = givenGlobalContext();

		// When getTemplateContext is called
		final TemplateContext templateContext1 = _notificationTemplateService.getTemplateContextV2(null, JSON_CONTENT_1, globalContext);

		// Then
		assertEquals(GLOBAL_CONTEXT_PRODUCT_NAME, ((Map<String, Object>)templateContext1.get("__global")).get("productName"));
	}

	@Test
	public void templateContextTestV2() {
		// When getTemplateContext is called
		final TemplateContext templateContext1 = _notificationTemplateService.getTemplateContextV2(null, JSON_CONTENT_1, Collections.emptyMap());

		//Then Recipient in UserPreferencesMatchedEvent should be available in __recipient namespace
		assertEquals(((Map<String, Object>)templateContext1.get("__recipient")).get("id"), "70e7cde5-3473-46ea-94ea-90bc8c605a6c");

		//Domain event contents should be available in outer context
		assertEquals(((Map<String, Object>)templateContext1.get("recipient")).get("id"), "2c928090672c901401672c92d33c02ef");
		assertEquals(templateContext1.get("subject"), "ATTENTION: Your SailPoint password update was successful");

		// When getTemplateContext is called
		final TemplateContext templateContext2 = _notificationTemplateService.getTemplateContextV2(null, JSON_CONTENT_2, Collections.emptyMap());

		// Then
		assertEquals(((Map<String, Object>)templateContext1.get("__recipient")).get("id"), "70e7cde5-3473-46ea-94ea-90bc8c605a6c");
	}


	@Test
	public void contextChainingTest() {
		Map<String, Object> globalContext = givenGlobalContext();

		// When getTemplateContext is called
		final TemplateContext templateContext1 = _notificationTemplateService.getTemplateContext(null, JSON_CONTENT_1, globalContext);
		final TemplateContext templateContext2 = _notificationTemplateService.getTemplateContextV2(templateContext1, JSON_CONTENT_1, globalContext);

		// Then
		assertEquals(GLOBAL_CONTEXT_PRODUCT_NAME, ((TemplateContext)templateContext1.get("global")).get("productName"));
		assertEquals(GLOBAL_CONTEXT_PRODUCT_NAME, ((Map<String, Object>)templateContext2.get("__global")).get("productName"));
	}

	@Test
	public void renderTemplateByKeyTest() {

		givenTemplateEngineEvaluateMock();

		// Given TemplateContext
		final TemplateContext templateContext = givenTemplateContext(
				ImmutableMap.of("medium", "highSpeedRail",
						"recipient.name", "curiousJorge",
						"subject", "Subject = '"));

		// When renderTemplate
		NotificationRendered notificationRendered = _notificationTemplateService
				.renderByNotificationKey("templateName", "templateName", "highSpeedRail",
						Locale.ENGLISH, templateContext);

		assertEquals("Subject = '", notificationRendered.getSubject());
		assertEquals("$domainEvent.get(&#39;body&#39;)", notificationRendered.getBody());
	}

	@Test
	public void renderSlackTemplateByKeyTest() {

		givenTemplateEngineEvaluateMock();

		when(_templateRepository.findOneByTenantAndKeyAndMediumAndLocale(anyString(), anyString(), anyString(), any()))
				.thenAnswer(invocation -> JsonUtil.parse(NotificationTemplate.class, TEMPLATE_REPOSITORY_JSON_SLACK));

		_notificationTemplateService =
				new NotificationTemplateService(_templateRepository, _eventServiceProvider, _templateEngine,
						_userPreferencesRepository, _featureFlagService);

		// Given TemplateContext
		final TemplateContext templateContext = givenTemplateContext(
				ImmutableMap.of("medium", "highSpeedRail", "recipient.name", "curiousJorge", "text", "my text"));

		// When renderTemplate
		SlackNotificationRendered notificationRendered = _notificationTemplateService
				.renderSlackByNotificationKey("templateName", "templateName", "highSpeedRail",
						Locale.ENGLISH, templateContext);

		assertEquals("$domainEvent.get('attachments')", notificationRendered.getAttachments());
		assertEquals("$domainEvent.get('text')", notificationRendered.getText());
	}

	@Test
	public void renderTeamsTemplateByKeyTest() {

		givenTemplateEngineEvaluateMock();

		when(_templateRepository.findOneByTenantAndKeyAndMediumAndLocale(anyString(), anyString(), anyString(), any()))
				.thenAnswer(invocation -> JsonUtil.parse(NotificationTemplate.class, TEMPLATE_REPOSITORY_JSON_TEAMS));

		_notificationTemplateService =
				new NotificationTemplateService(_templateRepository, _eventServiceProvider, _templateEngine,
						_userPreferencesRepository, _featureFlagService);

		// Given TemplateContext
		final TemplateContext templateContext = givenTemplateContext(
				ImmutableMap.of("medium", "highSpeedRail", "recipient.name", "curiousJorge"));

		// When renderTemplate
		TeamsNotificationRendered notificationRendered = _notificationTemplateService
				.renderTeamsMessageByNotificationKey("templateName", "templateName", "highSpeedRail",
						Locale.ENGLISH, templateContext);

		assertEquals("$domainEvent.get('title')", notificationRendered.getTitle());
		assertEquals("$domainEvent.get('text')", notificationRendered.getText());
		assertEquals("$domainEvent.get('messageJson')", notificationRendered.getMessageJSON());
	}

	@Test
	public void renderTeamsTemplate_SpecialSymbolsArePresent_ShouldEscapeSymbols() {
		TemplateEngineVelocity templateEngineVelocity = new TemplateEngineVelocity();
		when(_templateRepository.findOneByTenantAndKeyAndMediumAndLocale(anyString(), anyString(), anyString(), any()))
				.thenAnswer(invocation -> JsonUtil.parse(NotificationTemplate.class, TEMPLATE_SPECIAL_SYMBOLS_JSON_TEAMS));

		_notificationTemplateService =
				new NotificationTemplateService(_templateRepository, _eventServiceProvider, templateEngineVelocity,
						_userPreferencesRepository, _featureFlagService);

		// Given TemplateContext
		final TemplateContext templateContext = givenTemplateContext(
				ImmutableMap.of("title", "Hi O'Neil \n _\\_",
						"text", "Text/\"\t",
						"messageJson", "'text'o-\\_(^_^)_/-o",
						"__esc", new EscapeUtil()));

		// When renderTemplate
		TeamsNotificationRendered notificationRendered = _notificationTemplateService
				.renderTeamsMessageByNotificationKey("templateName", "templateName", "highSpeedRail",
						Locale.ENGLISH, templateContext);

		assertEquals("Hi O'Neil \\n _\\\\_", notificationRendered.getTitle());
		assertEquals("Text\\/\\\"\\t", notificationRendered.getText());
		assertEquals("'text'o-\\\\_(^_^)_\\/-o", notificationRendered.getMessageJSON());
	}

	@Test
	public void renderSlackTemplate_SpecialSymbolsArePresent_ShouldEscapeSymbols() {
		TemplateEngineVelocity templateEngineVelocity = new TemplateEngineVelocity();
		when(_templateRepository.findOneByTenantAndKeyAndMediumAndLocale(anyString(), anyString(), anyString(), any()))
				.thenAnswer(invocation -> JsonUtil.parse(NotificationTemplate.class, TEMPLATE_SPECIAL_SYMBOLS_JSON_SLACK));

		_notificationTemplateService =
				new NotificationTemplateService(_templateRepository, _eventServiceProvider, templateEngineVelocity,
						_userPreferencesRepository, _featureFlagService);

		final TemplateContext templateContext = givenTemplateContext(
				ImmutableMap.of("text", "Hi O'Neil \n _\\_",
						"blocks", "Text/\"\t",
						"attachments", "'text'o-\\_(^_^)_/-o",
						"__esc", new EscapeUtil()));

		SlackNotificationRendered notificationRendered = _notificationTemplateService
				.renderSlackByNotificationKey("templateName", "templateName", "highSpeedRail",
						Locale.ENGLISH, templateContext);

		assertEquals("Hi O'Neil \\n _\\\\_", notificationRendered.getText());
		assertEquals("Text/\\\"\\t", notificationRendered.getBlocks());
		assertEquals("'text'o-\\\\_(^_^)_/-o", notificationRendered.getAttachments());
	}

	@Test
	public void noTemplateFound() {
		when(_templateRepository.findOneByTenantAndKeyAndMediumAndLocale(anyString(), anyString(),
				anyString(), any())).thenReturn(null);

		// Given TemplateContext
		final TemplateContext templateContext = givenTemplateContext(
				ImmutableMap.of("medium", "highSpeedRail", "recipient.name", "curiousJorge"));

		// When renderTemplate
		NotificationRendered notificationRendered = _notificationTemplateService
				.renderByNotificationKey("templateName","templateName",
						"highSpeedRail", Locale.ENGLISH, templateContext);

		assertNull(notificationRendered);
	}

	@Test
	public void testPublishEvent() {
		// Given Mocks and event
		givenEventService();
		Event event = givenEvent();

		// When
		_notificationTemplateService.publishEvent(event);

		// Then verify publishAsync argument
		verify(_eventService, times(1)).publishAsync(any(TopicDescriptor.class), _eventArgumentCaptor.capture());
		Event publishedEvent = _eventArgumentCaptor.getValue();
		assertEquals(event.getType(), publishedEvent.getType());
	}

	private void givenTemplateEngineEvaluateMock() {
		when(_templateEngine.evaluate(anyString(), any())).thenAnswer(invocation -> {
			Object[] args = invocation.getArguments();
			String templateText = (String) args[0];
			TemplateContext<String, Object> templateContext = (TemplateContext) args[1];

			for(String key : templateContext.keySet()) {
				String pattern = "$event.get('" + key + "')";

				if (templateText.contains(pattern)) {
					templateText = templateText.replace(pattern, templateContext.get(key).toString());
				}
			}

			return templateText;
		});
	}

	private Event givenEvent() {
		return EventBuilder.withTypeAndContentJson("ACCESS_APPROVAL_REQUESTED", "{}").build();
	}

	private void givenEventService() {
		when(_eventServiceProvider.get()).thenReturn(_eventService);
	}

	private TemplateContext givenTemplateContext(Map<String, Object> entries) {
		final TemplateContext templateContext = new TemplateContextVelocity();
		entries.entrySet().stream().forEach(entry -> templateContext.put(entry.getKey(), entry.getValue()));

		return templateContext;
	}

	private Map<String, Object> givenGlobalContext() {
		Map<String, Object> attributes = Collections.singletonMap("productName", GLOBAL_CONTEXT_PRODUCT_NAME);
		when(_globalContextService.getContext(anyString(), any())).thenReturn(attributes);
		when(_globalContextService.getContext(anyString(), any())).thenReturn(attributes);
		return attributes;
	}
}
