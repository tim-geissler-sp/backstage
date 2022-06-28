/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.interest.matcher.repository.impl.json;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sailpoint.atlas.AtlasConfig;
import com.sailpoint.atlas.featureflag.FeatureFlagService;
import com.sailpoint.iris.client.Event;
import com.sailpoint.iris.server.EventHandlerContext;
import com.sailpoint.notification.sender.common.event.interest.matching.NotificationInterestMatchedBuilder;
import com.sailpoint.notification.sender.common.event.interest.matching.dto.NotificationInterestMatched;
import com.sailpoint.notification.sender.common.repository.BaseJsonRepository;
import com.sailpoint.notification.interest.matcher.interest.Interest;
import com.sailpoint.notification.interest.matcher.repository.InterestRepository;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;


/**
 * JSON implementation for InterestRepository.
 */
@Singleton
public class InterestRepositoryJsonImpl extends BaseJsonRepository<Interest> implements InterestRepository {

	private static final Log _log = LogFactory.getLog(InterestRepositoryJsonImpl.class);

	public static final String ATLAS_INTEREST_MATCHED_REPOSITORY_LOCATION = "ATLAS_INTEREST_MATCHED_REPOSITORY_LOCATION";

	private static final String DEFAULT_INTEREST_REPOSITORY_FILE = "interestsRepository.json";

	private final List<Interest> _interests;

	private final String RECIPIENT_ID_FIELD = "recipientId";
	private final String RECIPIENT_EMAIL_FIELD = "recipientEmail";
	private final String NOTIFICATION_KEY = "notificationKey";

	/*
	 * Load and initialize interest matching repository.
	 * By default we will use resource file 'interestsRepository.json' from jar file
	 * but external configurations can be define in atlas config as needed.
	 */
	@Inject
	public InterestRepositoryJsonImpl(AtlasConfig atlasConfig) {
		super(atlasConfig.getString(ATLAS_INTEREST_MATCHED_REPOSITORY_LOCATION), DEFAULT_INTEREST_REPOSITORY_FILE, Interest.class);
		_interests = getRepository();
		_interests.forEach(Interest::init);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Interest> getInterests() {
		return _interests;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<NotificationInterestMatched> processInterestMatch(EventHandlerContext context, Interest interest) {
		Event event = context.getEvent();
		List<Map<String, String>> fields =  interest.getFieldsDiscovery()
				.discover(event.getContentJson());

		return processFields(fields, event, interest);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean test(EventHandlerContext context) {
		return _interests.stream()
				.anyMatch(i -> i.test(context));
	}

	private List<NotificationInterestMatched> processFields(List<Map<String, String>> fields,
															Event event, Interest interest) {
		List<NotificationInterestMatched> result = new ArrayList<>();
		for (Map<String, String> values : fields) {
			String notificationKey = values.get(NOTIFICATION_KEY);
			if(notificationKey == null) {
				notificationKey = interest.getNotificationKey();
			}
			NotificationInterestMatchedBuilder builder = new NotificationInterestMatchedBuilder(
					UUID.randomUUID().toString(), event)
					.withCategoryName(interest.getCategoryName())
					.withInterestName(interest.getInterestName())
					.withEnabled(interest.isEnabled())
					.withNotificationKey(notificationKey);
			values.forEach((k, v)-> {
				switch (k) {
					case RECIPIENT_ID_FIELD:
						builder.withRecipientId(v);
						break;
					case RECIPIENT_EMAIL_FIELD:
						builder.withRecipientEmail(v);
						break;
				}
			});
			result.add(builder.build());
		}
		return result;
	}
}
