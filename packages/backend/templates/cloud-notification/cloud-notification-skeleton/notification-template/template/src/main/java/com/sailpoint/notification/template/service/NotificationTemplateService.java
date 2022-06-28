/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.template.service;

import com.github.wnameless.json.flattener.JsonFlattener;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.sailpoint.atlas.event.EventService;
import com.sailpoint.atlas.event.idn.IdnTopic;
import com.sailpoint.atlas.featureflag.FeatureFlagService;
import com.sailpoint.metrics.MetricsUtil;
import com.sailpoint.notification.api.event.dto.Notification;
import com.sailpoint.notification.api.event.dto.Recipient;
import com.sailpoint.notification.api.event.dto.SlackNotificationAutoApprovalData;
import com.sailpoint.notification.api.event.dto.SlackNotificationRendered;
import com.sailpoint.notification.api.event.dto.TeamsNotificationAutoApprovalData;
import com.sailpoint.notification.api.event.dto.TeamsNotificationRendered;
import com.sailpoint.notification.template.common.model.teams.TeamsTemplate;
import com.sailpoint.notification.template.common.model.slack.SlackTemplate;
import com.sailpoint.notification.template.common.util.HtmlUtil;
import com.sailpoint.notification.template.util.EscapeUtil;
import com.sailpoint.utilities.JsonUtil;
import com.sailpoint.iris.client.Event;
import com.sailpoint.notification.api.event.dto.NotificationRendered;
import com.sailpoint.notification.template.common.model.NotificationTemplate;
import com.sailpoint.notification.template.common.repository.TemplateRepository;
import com.sailpoint.notification.template.context.TemplateContext;
import com.sailpoint.notification.template.context.impl.velocity.TemplateContextVelocity;
import com.sailpoint.notification.template.engine.TemplateEngine;
import com.sailpoint.notification.template.util.TemplateUtil;
import com.sailpoint.notification.userpreferences.repository.UserPreferencesRepository;
import com.sailpoint.notification.userpreferences.repository.impl.CachedRepository;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.velocity.tools.generic.DateTool;
import org.apache.velocity.tools.generic.NumberTool;

import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Notification template service.
 * Abstracts the template rendering, the template context extraction and publishing the final NOTIFICATION event.
 */
@Singleton
public class NotificationTemplateService {

	static Log _log = LogFactory.getLog(NotificationTemplateService.class);

	private final Provider<EventService> _eventService;

	private final TemplateRepository _templateRepository;

	private final TemplateEngine _templateEngine;

	private final UserPreferencesRepository _userPreferencesRepository;

	//Deprecated/V1 namespaces
	public static final String CONTEXT_PREFIX = "event";
	public static final String CONTEXT_PREFIX_INTEREST = "interestMatchedEvent";
	public static final String CONTEXT_PREFIX_DOMAIN = "domainEvent";
	public static final String CONTEXT_PREFIX_GLOBAL = "global";
	public static final String CONTEXT_PREFIX_DATETOOL = "date";
	public static final String CONTEXT_PREFIX_UTIL = "util";

	//New/V2 namespaces
	public static final String V2_CONTEXT_PREFIX_RECIPIENT = "__recipient";
	public static final String V2_CONTEXT_PREFIX_GLOBAL = "__global";
	public static final String V2_CONTEXT_PREFIX_DATETOOL = "__dateTool";
	public static final String V2_CONTEXT_PREFIX_NUMBERTOOL = "__numberTool";
	public static final String V2_CONTEXT_PREFIX_UTIL = "__util";
	public static final String V2_CONTEXT_PREFIX_ESCAPETOOL = "__esc";
	public static final String V2_CONTEXT_PREFIX_CONTENT_JSON = "__contentJson";

	private static final String EMBEDDED_EVENT = "domainEvent.contentJson";
	private static final String USER_PREFERENCES_MATCHED_RECIPIENT = "recipient";
	public static final String CONTENT_JSON = "contentJson";
	public static final String DOMAIN_EVENT = "domainEvent";

	private static final String NOTIFICATION_TEMPLATE_SERVICE_PREFIX = NotificationTemplateService.class.getName();

	//TODO: FF for control HTML sanitize, can be removed once confirmed working OK in production.
	private final FeatureFlagService _featureFlagService;
	final static String HERMES_TEMPLATES_HTML_SANITIZE_ENABLED = "HERMES_TEMPLATES_HTML_SANITIZE_ENABLED";

	@Inject
	NotificationTemplateService(TemplateRepository templateRepository,
								Provider<EventService> eventService,
								TemplateEngine templateEngine,
								UserPreferencesRepository userPreferencesRepository,
								FeatureFlagService featureFlagService) {
		_templateRepository = templateRepository;
		_eventService = eventService;
		_templateEngine = templateEngine;
		_featureFlagService = featureFlagService;
		_userPreferencesRepository = new CachedRepository(userPreferencesRepository);
	}

	public NotificationRendered renderByNotificationKey(String tenant, String notificationKey,
														String medium, Locale locale,
														TemplateContext templateContext) {
		return renderTemplate(tenant,
				_templateRepository.findOneByTenantAndKeyAndMediumAndLocale(tenant, notificationKey, medium, locale),
				medium, templateContext);
	}

	public SlackNotificationRendered renderSlackByNotificationKey(String tenant, String notificationKey,
																  String medium, Locale locale,
																  TemplateContext templateContext) {
		return renderSlackTemplate(tenant,
				_templateRepository.findOneByTenantAndKeyAndMediumAndLocale(tenant, notificationKey, medium, locale),
				medium, templateContext);
	}

	public TeamsNotificationRendered renderTeamsMessageByNotificationKey(String tenant, String notificationKey,
																		 String medium, Locale locale,
																		 TemplateContext templateContext) {
		return renderTeamsTemplate(tenant,
				_templateRepository.findOneByTenantAndKeyAndMediumAndLocale(tenant, notificationKey, medium, locale),
				medium, templateContext);
	}

	public TemplateContext getTemplateContext(TemplateContext chainedContext, String content, Map<String, Object> globalContext) {
		final TemplateContext outerContext = new TemplateContextVelocity(chainedContext);

		outerContext.put(CONTEXT_PREFIX_GLOBAL, getContext(globalContext));

		Map<String, Object> flattenContent = JsonFlattener.flattenAsMap(content);
		outerContext.put(CONTEXT_PREFIX, getContext(flattenContent));
		if(flattenContent.get(EMBEDDED_EVENT) == null) return outerContext;

		Map<String, Object> flattenContentInterest = JsonFlattener
				.flattenAsMap(flattenContent.get(EMBEDDED_EVENT).toString());
		outerContext.put(CONTEXT_PREFIX_INTEREST, getContext(flattenContentInterest));

		if(flattenContentInterest.get(EMBEDDED_EVENT) == null) return outerContext;

		Map<String, Object> flattenContentDomain = JsonFlattener
				.flattenAsMap(flattenContentInterest.get(EMBEDDED_EVENT).toString());
		outerContext.put(CONTEXT_PREFIX_DOMAIN, getContext(flattenContentDomain));

		outerContext.put(CONTEXT_PREFIX_DATETOOL, new DateTool());
		outerContext.put(CONTEXT_PREFIX_UTIL, new TemplateUtil(_userPreferencesRepository));

		return outerContext;
	}

	public TemplateContext getTemplateContextV2(TemplateContext chainedContext, String content, Map<String, Object> globalContext) {
		final TemplateContext outerContext = new TemplateContextVelocity(chainedContext);

		//Add global context map to "__global" namespace
		outerContext.put(V2_CONTEXT_PREFIX_GLOBAL, globalContext);

		//Add Apache Velocity DateTool to "__dateTool" namespace
		outerContext.put(V2_CONTEXT_PREFIX_DATETOOL, new DateTool());

		//Add Apache Velocity NumberTool to "__numberTool" namespace
		outerContext.put(V2_CONTEXT_PREFIX_NUMBERTOOL, new NumberTool());

		//Add platform template util to "__util" namespace
		outerContext.put(V2_CONTEXT_PREFIX_UTIL, new TemplateUtil(_userPreferencesRepository));

		outerContext.put(V2_CONTEXT_PREFIX_ESCAPETOOL, new EscapeUtil());

		Map<String, Object> userPreferencesMatchedMap = JsonUtil.parse(Map.class, content);
		//Add recipient alone to "__recipient" namespace
		outerContext.put(V2_CONTEXT_PREFIX_RECIPIENT, userPreferencesMatchedMap.get(USER_PREFERENCES_MATCHED_RECIPIENT));

		Map<String, Object> interestMatchedMap = JsonUtil.parse(Map.class, getEmbeddedContentJson(userPreferencesMatchedMap));
		if(interestMatchedMap == null) return outerContext;

		String domainEventJson = getEmbeddedContentJson(interestMatchedMap);
		Map<String, Object> domainEventMap  = JsonUtil.parse(Map.class, domainEventJson);
		if(domainEventMap == null) return outerContext;

		//Add domain event as JSON to namespace __contentJson
		outerContext.put(V2_CONTEXT_PREFIX_CONTENT_JSON, domainEventJson);

		//Add remaining contents of domain event map directly to outer context
		putAllInContext(outerContext, domainEventMap);
		return outerContext;
	}

	public TemplateContext getTemplateContextV2(Recipient recipient, Map<String, Object> globalContext, Map<String, Object> context) {
		final TemplateContext outerContext = new TemplateContextVelocity();

		//Add global context map to "__global" namespace
		outerContext.put(V2_CONTEXT_PREFIX_GLOBAL, globalContext);

		//Add Apache Velocity DateTool to "__dateTool" namespace
		outerContext.put(V2_CONTEXT_PREFIX_DATETOOL, new DateTool());

		//Add Apache Velocity NumberTool to "__numberTool" namespace
		outerContext.put(V2_CONTEXT_PREFIX_NUMBERTOOL, new NumberTool());

		//Add platform template util to "__util" namespace
		outerContext.put(V2_CONTEXT_PREFIX_UTIL, new TemplateUtil(_userPreferencesRepository));

		//Add recipient alone to "__recipient" namespace
		outerContext.put(V2_CONTEXT_PREFIX_RECIPIENT, recipient);

		outerContext.put(V2_CONTEXT_PREFIX_ESCAPETOOL, new EscapeUtil());

		putAllInContext(outerContext, context);
		return outerContext;
	}

	public void publishEvent(Event event) {
		_eventService.get().publishAsync(IdnTopic.NOTIFICATION, event);
	}

	private NotificationRendered renderTemplate(String tenant, NotificationTemplate template, String medium, TemplateContext templateContext) {

		if (template == null) {
			_log.info("Template for tenant " + tenant + " and medium " + medium + " not found using default.");
			template = _templateRepository.getDefault(tenant);
		}

		if(template == null) {
			_log.error(" No template found");
			return null;
		}

		Map<String, String> tags = metricTagProvider(template);

		try{
			final StringBuilder templateBody = new StringBuilder();
			if (template.getHeader() != null) {
				templateBody.append(_templateEngine.evaluate(template.getHeader(), templateContext));
			}
			if (template.getBody() != null) {
				templateBody.append(_templateEngine.evaluate(template.getBody(), templateContext));
			}
			if (template.getFooter() != null) {
				templateBody.append(_templateEngine.evaluate(template.getFooter(), templateContext));
			}

			final String subject = _templateEngine.evaluate(template.getSubject(), templateContext);

			final String from = _templateEngine.evaluate(template.getFrom(), templateContext);
			final String replyTo = _templateEngine.evaluate(template.getReplyTo(), templateContext);
			boolean isHTMLSanitizeEnabled = _featureFlagService.getBoolean(HERMES_TEMPLATES_HTML_SANITIZE_ENABLED, false);

			MetricsUtil.getCounter(NOTIFICATION_TEMPLATE_SERVICE_PREFIX + ".template_evaluation.success", tags).inc();

			return NotificationRendered.builder()
					.medium(medium)
					.body(isHTMLSanitizeEnabled ? HtmlUtil.sanitize(templateBody.toString()): templateBody.toString())
					.subject(subject)
					.from(from)
					.replyTo(replyTo)
					.build();
		} catch(IllegalStateException e){
			MetricsUtil.getCounter(NOTIFICATION_TEMPLATE_SERVICE_PREFIX + ".template_evaluation.failure", tags).inc();
			throw new IllegalStateException("Error evaluating template", e);
		}
	}

	private SlackNotificationRendered renderSlackTemplate(String tenant, NotificationTemplate template, String medium, TemplateContext templateContext) {

		if (template == null) {
			_log.info("Template for tenant " + tenant + " and medium " + medium + " not found using default.");
			template = _templateRepository.getDefault(tenant);
		}

		if(template == null) {
			_log.error(" No template found");
			return null;
		}

		SlackTemplate slackTemplate = template.getSlackTemplate();
		if(slackTemplate == null) {
			_log.error(" No slack template found");
			return null;
		}

		Map<String, String> tags = metricTagProvider(template);
		try {
			final String text = _templateEngine.evaluate(slackTemplate.getText(), templateContext);
			final String blocks = _templateEngine.evaluate(slackTemplate.getBlocks(), templateContext);
			final String attachments = _templateEngine.evaluate(slackTemplate.getAttachments(), templateContext);
			final String notificationType = slackTemplate.getNotificationType();
			final Boolean isSubscription = slackTemplate.getIsSubscription();

			MetricsUtil.getCounter(NOTIFICATION_TEMPLATE_SERVICE_PREFIX + ".template_evaluation.success", tags).inc();
			SlackNotificationRendered.SlackNotificationRenderedBuilder builder = SlackNotificationRendered.builder()
					.text(text)
					.blocks(blocks)
					.attachments(attachments)
					.notificationType(notificationType)
					.isSubscription(isSubscription)
					.attachments(attachments);

			if (slackTemplate.getAutoApprovalData() != null) {
				final String itemId = _templateEngine.evaluate(slackTemplate.getAutoApprovalData().getItemId(), templateContext);
				final String itemType = _templateEngine.evaluate(slackTemplate.getAutoApprovalData().getItemType(), templateContext);
				final String autoApprovalMessageJSON = _templateEngine.evaluate(slackTemplate.getAutoApprovalData().getAutoApprovalMessageJSON(), templateContext);
				final String autoApprovalTitle = _templateEngine.evaluate(slackTemplate.getAutoApprovalData().getAutoApprovalTitle(), templateContext);
				builder.autoApprovalData(SlackNotificationAutoApprovalData.builder()
						.itemId(itemId)
						.itemType(itemType)
						.autoApprovalMessageJSON(autoApprovalMessageJSON)
						.autoApprovalTitle(autoApprovalTitle)
						.build());
			}

			if (slackTemplate.getRequestId() != null) {
				final String requestId = _templateEngine.evaluate(slackTemplate.getRequestId(), templateContext);
				builder.requestId(requestId);
			}

			if (slackTemplate.getApprovalId() != null) {
				final String approvalId = _templateEngine.evaluate(slackTemplate.getApprovalId(), templateContext);
				builder.approvalId(approvalId);
      }
      
			if (slackTemplate.getCustomFields() != null) {
				final Map<String, Object> customFields = new HashMap<>(slackTemplate.getCustomFields());

				for (Map.Entry<String, Object> entry : customFields.entrySet()) {
					entry.setValue(_templateEngine.evaluate(entry.getValue().toString(), templateContext));
				}

				builder.customFields(customFields);
			}

			return builder.build();
		} catch (IllegalStateException e) {
			MetricsUtil.getCounter(NOTIFICATION_TEMPLATE_SERVICE_PREFIX + ".template_evaluation.failure", tags).inc();
			throw new IllegalStateException("Error evaluating Slack template", e);
		}
	}

	private TeamsNotificationRendered renderTeamsTemplate(String tenant, NotificationTemplate template, String medium, TemplateContext templateContext) {

		if (template == null) {
			_log.info("Template for tenant " + tenant + " and medium " + medium + " not found using default.");
			template = _templateRepository.getDefault(tenant);
		}

		if(template == null) {
			_log.error(" No template found");
			return null;
		}

		TeamsTemplate teamsTemplate = template.getTeamsTemplate();
		if(teamsTemplate == null) {
			_log.error(" No teams template found");
			return null;
		}

		Map<String, String> tags = metricTagProvider(template);
		try {

			final String text = _templateEngine.evaluate(teamsTemplate.getText(), templateContext);
			final String title = _templateEngine.evaluate(teamsTemplate.getTitle(), templateContext);
			final String messageJSON = _templateEngine.evaluate(teamsTemplate.getMessageJson(), templateContext);
			final Boolean isSubscription = teamsTemplate.getIsSubscription();
			final String notificationType = teamsTemplate.getNotificationType();

			MetricsUtil.getCounter(NOTIFICATION_TEMPLATE_SERVICE_PREFIX + ".template_evaluation.success", tags).inc();
			TeamsNotificationRendered.TeamsNotificationRenderedBuilder builder = TeamsNotificationRendered.builder()
					.title(title)
					.text(text)
					.messageJSON(messageJSON)
					.isSubscription(isSubscription)
					.notificationType(notificationType);

			if (teamsTemplate.getCustomFields() != null) {
				final Map<String, Object> customFields = new HashMap<>(teamsTemplate.getCustomFields());

				for (Map.Entry<String, Object> entry : customFields.entrySet()) {
					entry.setValue(_templateEngine.evaluate(entry.getValue().toString(), templateContext));
				}

				builder.customFields(customFields);
			}

			if (teamsTemplate.getAutoApprovalData() != null) {
				final String itemId = _templateEngine.evaluate(teamsTemplate.getAutoApprovalData().getItemId(), templateContext);
				final String itemType = _templateEngine.evaluate(teamsTemplate.getAutoApprovalData().getItemType(), templateContext);
				final String autoApprovalMessageJSON = _templateEngine.evaluate(teamsTemplate.getAutoApprovalData().getAutoApprovalMessageJSON(), templateContext);
				final String autoApprovalTitle = _templateEngine.evaluate(teamsTemplate.getAutoApprovalData().getAutoApprovalTitle(), templateContext);
				builder.autoApprovalData(TeamsNotificationAutoApprovalData.builder()
						.itemId(itemId)
						.itemType(itemType)
						.autoApprovalMessageJSON(autoApprovalMessageJSON)
						.autoApprovalTitle(autoApprovalTitle)
						.build());
			}

			if (teamsTemplate.getApprovalId() != null) {
				final String approvalId = _templateEngine.evaluate(teamsTemplate.getApprovalId(), templateContext);
				builder.approvalId(approvalId);
			}

			if (teamsTemplate.getRequestId() != null) {
				final String requestId = _templateEngine.evaluate(teamsTemplate.getRequestId(), templateContext);
				builder.requestId(requestId);
			}

			return builder.build();
		} catch (IllegalStateException e) {
			MetricsUtil.getCounter(NOTIFICATION_TEMPLATE_SERVICE_PREFIX + ".template_evaluation.failure", tags).inc();
			throw new IllegalStateException("Error evaluating Slack template", e);
		}
	}

	private TemplateContext getContext(Map<String, Object> flattenContent) {
		final TemplateContext innerContext = new TemplateContextVelocity();
		if (flattenContent != null) {
			flattenContent.entrySet()
					.stream()
					.forEach(entry -> {
						if(!entry.getKey().equals(EMBEDDED_EVENT)) {
							innerContext.put(entry.getKey(), entry.getValue());
						}
					});
		}
		return innerContext;
	}

	/**
	 * Puts all entries from a map into the template context
	 * @param context reference to template context
	 * @param map reference to map
	 */
	private void putAllInContext(TemplateContext context, Map<String, Object> map) {
		if (map != null) {
			map.entrySet()
					.stream()
					.forEach(e -> context.put(e.getKey(), e.getValue()));
		}
	}

	/**
	 * Each event in hermes wraps the previous/embedded event in the chain in the key domainEvent.
	 * The function returns the content of wrapped event as JSON
	 *
	 * @return the content as json string
	 */
	private String getEmbeddedContentJson(Map<String, Object> eventMap) {
		if (eventMap != null) {
			Map<String, Object> domainEventMap;
			try {
				domainEventMap = (Map<String, Object>) eventMap.get(DOMAIN_EVENT);
				if (domainEventMap != null) {
					return (String)domainEventMap.get(CONTENT_JSON);
				}
			} catch (ClassCastException cce) {
				return null;
			}
		}
		return null;
	}

	/**
	 * Provide tags for evaluation metrics
	 * @param template The template for evaluation
	 * @return Tags Map
	 */
	private Map<String, String> metricTagProvider (NotificationTemplate template){
		Map<String, String> tags = new HashMap<>();
		tags.put("template_key", template.getKey());
		tags.put("medium", template.getMedium());
		tags.put("locale", template.getLocale().toString());
		Boolean isDefaultTemplate = template.getTenant() == null ? true : false;
		tags.put("is_default", isDefaultTemplate.toString());

		return tags;
	}

	/**
	 * Renders a template and returns a Notification object
	 *
	 * @param tenant the tenant
	 * @param recipient the recipient
	 * @param template the template
	 * @param globalContext the global context
	 * @param context the template context
	 * @return
	 */
	public Notification renderTemplate(String tenant, Recipient recipient, NotificationTemplate template, Map<String, Object> globalContext, Map<String, Object> context) {
		final TemplateContext templateContextV2 =
				getTemplateContextV2(recipient, globalContext, context);

		if (template.getMedium() != null) {
			switch(template.getMedium()) {
				case "EMAIL":
					return renderTemplate(tenant, template, "EMAIL", templateContextV2);
				case "SLACK":
					return renderSlackTemplate(tenant, template, "SLACK", templateContextV2);
				case "TEAMS":
					return renderTeamsTemplate(tenant, template, "TEAMS", templateContextV2);
			}
		}
		return null;
	}
}
