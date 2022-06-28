/*
 * Copyright (c) 2018. SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.notification.interest.matcher;

import com.sailpoint.atlas.event.EventRegistry;
import com.sailpoint.atlas.event.idn.IdnTopic;
import com.sailpoint.atlas.plugin.AtlasPlugin;
import com.sailpoint.atlas.plugin.PluginConfigurationContext;
import com.sailpoint.atlas.plugin.PluginDeploymentContext;
import com.sailpoint.atlas.rest.RestConfig;
import com.sailpoint.atlas.rest.RestDeployment;
import com.sailpoint.iris.client.SimpleTopicDescriptor;
import com.sailpoint.iris.client.TopicDescriptor;
import com.sailpoint.iris.client.TopicScope;
import com.sailpoint.notification.interest.matcher.event.InterestMatcherEventHandler;
import com.sailpoint.notification.interest.matcher.interest.Interest;
import com.sailpoint.notification.interest.matcher.repository.InterestRepository;
import com.sailpoint.notification.interest.matcher.rest.NotificationInterestMatcherRestApplication;
import com.sailpoint.notification.interest.matcher.service.NotificationInterestMatcherModule;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class NotificationInterestMatcherPlugin implements AtlasPlugin {

	private static final Log _log = LogFactory.getLog(NotificationInterestMatcherPlugin.class);

	@Override
	public void configure(PluginConfigurationContext context) {
		context.addGuiceModule(new NotificationInterestMatcherModule());
	}

	@Override
	public void deploy(PluginDeploymentContext context) {
		RestConfig restConfig = context.getInstance(RestConfig.class);
		restConfig.addDeployment(new RestDeployment("/interest-matcher",
				NotificationInterestMatcherRestApplication.class));

		EventRegistry eventRegistry = context.getInstance(EventRegistry.class);


		InterestRepository interestRepository = context.getInstance(InterestRepository.class);

		for (Interest interest: interestRepository.getInterests()) {
			TopicDescriptor descriptor;
			try {
				descriptor = IdnTopic.valueOf(interest.getTopicName().toUpperCase());
			} catch (IllegalArgumentException ignored) {
				_log.warn("no descriptor found for topic: " + interest.getTopicName() + ", defaulting to POD scope");
				descriptor = new SimpleTopicDescriptor(TopicScope.POD, interest.getTopicName());
			}
			InterestMatcherEventHandler interestMatcherEventHandler = context.getInstance(InterestMatcherEventHandler.class);
			interestMatcherEventHandler.setInterest(interest);

			eventRegistry.register(descriptor, interest.getEventType(),
					interestMatcherEventHandler);
		}

	}
}
