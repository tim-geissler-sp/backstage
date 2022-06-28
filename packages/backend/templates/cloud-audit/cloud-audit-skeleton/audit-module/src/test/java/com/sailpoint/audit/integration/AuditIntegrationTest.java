/*
 *
 *  * Copyright (c) 2019.  SailPoint Technologies, Inc. All rights reserved.
 *
 */

package com.sailpoint.audit.integration;

import com.sailpoint.atlas.event.AtlasEventPlugin;
import com.sailpoint.atlas.event.EventService;
import com.sailpoint.atlas.event.idn.IdnTopic;
import com.sailpoint.atlas.health.AtlasHealthPlugin;
import com.sailpoint.atlas.idn.DevOrgDataProvider;
import com.sailpoint.atlas.idn.IdnMessageScope;
import com.sailpoint.atlas.logging.AtlasDynamicLoggingPlugin;
import com.sailpoint.atlas.messaging.client.Job;
import com.sailpoint.atlas.messaging.client.MessagePriority;
import com.sailpoint.atlas.messaging.client.Payload;
import com.sailpoint.atlas.messaging.client.SendMessageOptions;
import com.sailpoint.atlas.metrics.AtlasMetricsPlugin;
import com.sailpoint.atlas.search.util.JsonUtils;
import com.sailpoint.atlas.service.AtomicMessageService;
import com.sailpoint.atlas.service.MessageClientService;
import com.sailpoint.atlas.service.ServiceFactory;
import com.sailpoint.atlas.task.schedule.service.TaskScheduleClientModule;
import com.sailpoint.atlas.test.integration.kafka.EnableKafkaServer;
import com.sailpoint.audit.AuditEventPlugin;
import com.sailpoint.audit.AuditModulePlugin;
import com.sailpoint.audit.event.DomainEventPlugin;
import com.sailpoint.audit.message.AuditEventHandler;
import com.sailpoint.audit.message.AuditEventPayload;
import com.sailpoint.audit.service.DeletedOrgsCacheService;
import com.sailpoint.audit.service.model.AddAthenaPartitionsDTO;
import com.sailpoint.audit.service.model.AuditEventDTO;
import com.sailpoint.audit.utils.TestUtils;
import com.sailpoint.featureflag.impl.MockFeatureFlagClient;
import com.sailpoint.iris.client.EventBuilder;
import com.sailpoint.mantis.event.MantisEventHandlerModule;
import com.sailpoint.mantis.platform.MantisApplication;
import com.sailpoint.mantis.test.integration.IntegrationTestApplication;
import com.sailpoint.mantis.test.integration.MantisIntegrationTest;
import com.sailpoint.mantisclient.IdnAtlasClient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@EnableKafkaServer(topics = {"org_lifecycle"})
public class AuditIntegrationTest extends MantisIntegrationTest {

	AtomicMessageService _atomicMessageService;

	EventService _eventService;

	IdnAtlasClient _idnAtlasClient;

	DeletedOrgsCacheService _deletedOrgsCache;

	@Override
	protected MantisApplication createMantisApplication() {
		return new IntegrationTestApplication() {{
			registerPlugin(new AtlasEventPlugin());
			registerPlugin(new AtlasHealthPlugin());
			registerPlugin(new AtlasMetricsPlugin());

			registerPlugin(new AuditEventPlugin());
			registerPlugin(new AuditModulePlugin());

			registerPlugin(new DomainEventPlugin());
			registerPlugin(new AtlasDynamicLoggingPlugin());

			addServiceModule(new MantisEventHandlerModule());
			addServiceModule(new TaskScheduleClientModule());
		}};
	}

	@Before
	public void initializeApplication() throws Exception {
		super.initializeApplication();

		_application.getRestPort().ifPresent(restPort -> {
			_idnAtlasClient = new IdnAtlasClient(
					String.format("http://localhost:%d", restPort),
					"dev",
					"acme-solar",
					DevOrgDataProvider.DEV_API_KEY,
					null,
					getHttpClient());
		});

		_restClient = _idnAtlasClient;

		_eventService = ServiceFactory.getService(EventService.class);

		_atomicMessageService = ServiceFactory.getService(AtomicMessageService.class);

		_deletedOrgsCache = ServiceFactory.getService(DeletedOrgsCacheService.class);

		MockFeatureFlagClient featureFlagClient = getFeatureFlagClient();

		featureFlagClient.setBoolean("WRITE_AUDIT_DATA_IN_PARQUET", true);

		TestUtils.setDummyRequestContext();
	}

	@Test
	public void testAuditEventDeletedOrg() {
		sendOrgDeleteEvent();
		try { Thread.sleep(10000); } catch (InterruptedException ie) {}

		AuditEventDTO event = TestUtils.getTestEvent("id0", "acme-solar", "dev");
		AuditEventPayload payload = new AuditEventPayload();
		payload.setAuditEventJson(event);

		sendMessage(AuditEventHandler.MessageType.AUDIT_EVENT.toString(),
				payload);
		try { Thread.sleep(10000); } catch (InterruptedException ie) {}

		Map map = _restClient.getJson(Map.class, "/audit/auditEvents");
		//There is already one audit event in another test
		Assert.assertTrue((Long)map.get("count") == 1);
		ArrayList items = (ArrayList) map.get("items");
		String action = (String) ((Map) items.get(0)).get("action");
		Assert.assertTrue(!"USER_ACTIVATE".equals(action));
	}

	private void sendOrgDeleteEvent() {
		com.sailpoint.iris.client.Event event = EventBuilder
				.withTypeAndContentJson("ORG_DELETED", "{}")
				.build();

		_eventService.publish(IdnTopic.ORG_LIFECYCLE, event);
	}

	private void sendMessage(String payloadType, AuditEventPayload auditEventPayload) {
		auditEventPayload.setUseAerStorage(true);

		_atomicMessageService.send(IdnMessageScope.AUDIT,
				new Payload(payloadType, auditEventPayload),
				new SendMessageOptions(MessagePriority.LOW));
	}

	@Test
	public void testAddAthenaPartitions() {
		AddAthenaPartitionsDTO athenaPartitionsDTO = new AddAthenaPartitionsDTO();
		athenaPartitionsDTO.setPartitionDate("2021-12-10");

		String response = _restClient.post("audit/data/add-partitions", null);
		Job jobResponse = JsonUtils.parse(Job.class, response);
		Assert.assertNotNull(jobResponse.getId());
		Assert.assertEquals(jobResponse.getPayload().getType(), "ADD_ATHENA_PARTITIONS" );

		List<Job> jobs = getMessageClientService().getActiveJobs();
		Assert.assertEquals(1, jobs.size());
	}

	private MessageClientService getMessageClientService() {

		return ServiceFactory.getService(MessageClientService.class);
	}
}
