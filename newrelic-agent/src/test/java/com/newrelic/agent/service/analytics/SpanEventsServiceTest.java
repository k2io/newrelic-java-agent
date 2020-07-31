/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.service.analytics;

import com.newrelic.agent.AgentHelper;
import com.newrelic.agent.MockDispatcherTracer;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.MockSpanEventReservoirManager;
import com.newrelic.agent.ThreadService;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.TransactionDataTestBuilder;
import com.newrelic.agent.TransactionService;
import com.newrelic.agent.attributes.AttributesService;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.environment.EnvironmentService;
import com.newrelic.agent.errors.ErrorAnalyzerImpl;
import com.newrelic.agent.errors.ErrorMessageReplacer;
import com.newrelic.agent.interfaces.SamplingPriorityQueue;
import com.newrelic.agent.interfaces.backport.Consumer;
import com.newrelic.agent.model.SpanCategory;
import com.newrelic.agent.model.SpanEvent;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.tracers.Tracer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SpanEventsServiceTest {

    private final String appName = "Unit Test";

    MockServiceManager serviceManager;
    @Mock
    public SpanEventCreationDecider spanEventCreationDecider;

    @Before
    public void before() throws Exception {
        MockitoAnnotations.initMocks(this);
        serviceManager = new MockServiceManager();

        Map<String, Object> localSettings = new HashMap<>();
        localSettings.put(AgentConfigImpl.APP_NAME, appName);
        localSettings.put("distributed_tracing", Collections.singletonMap("enabled", true));
        localSettings.put("span_events", Collections.singletonMap("collect_span_events", true));
        when(spanEventCreationDecider.shouldCreateSpans(any(TransactionData.class))).thenReturn(true);

        AgentConfig agentConfig = AgentHelper.createAgentConfig(true, localSettings, new HashMap<String, Object>());
        ConfigService configService = ConfigServiceFactory.createConfigService(agentConfig, localSettings);
        serviceManager.setConfigService(configService);
        ServiceFactory.setServiceManager(serviceManager);

        serviceManager.setTransactionService(new TransactionService());
        serviceManager.setThreadService(new ThreadService());
        final MockSpanEventReservoirManager reservoirManager = new MockSpanEventReservoirManager(configService);
        Consumer<SpanEvent> backendConsumer = new Consumer<SpanEvent>() {
            @Override
            public void accept(SpanEvent spanEvent) {
                reservoirManager.getOrCreateReservoir().add(spanEvent);
            }
        };

        Map<String, SpanErrorBuilder> map = new HashMap<>();
        map.put(agentConfig.getApplicationName(), new SpanErrorBuilder(
                new ErrorAnalyzerImpl(agentConfig.getErrorCollectorConfig()),
                new ErrorMessageReplacer(agentConfig.getStripExceptionConfig())
        ));

        EnvironmentService environmentService = mock(EnvironmentService.class, RETURNS_DEEP_STUBS);
        TransactionDataToDistributedTraceIntrinsics transactionDataToDistributedTraceIntrinsics = mock(TransactionDataToDistributedTraceIntrinsics.class);
        when(transactionDataToDistributedTraceIntrinsics.buildDistributedTracingIntrinsics(any(TransactionData.class), anyBoolean()))
                .thenReturn(Collections.<String, Object>emptyMap());
        TracerToSpanEvent tracerToSpanEvent = new TracerToSpanEvent(map, environmentService, transactionDataToDistributedTraceIntrinsics);
        SpanEventsServiceImpl spanEventsService = SpanEventsServiceImpl.builder()
                .agentConfig(agentConfig)
                .reservoirManager(reservoirManager)
                .collectorSender(mock(CollectorSpanEventSender.class))
                .eventBackendStorage(backendConsumer)
                .spanEventCreationDecider(spanEventCreationDecider)
                .tracerToSpanEvent(tracerToSpanEvent)
                .build();
        serviceManager.setSpansEventService(spanEventsService);
        serviceManager.setAttributesService(new AttributesService());
    }

    @After
    public void after() {
        ServiceFactory.setServiceManager(null);
    }

    @Test
    public void testSpanEvent() {
        TransactionData transactionData = new TransactionDataTestBuilder(
                appName,
                ServiceFactory.getConfigService().getDefaultAgentConfig(),
                new MockDispatcherTracer())
                .setTracers(Collections.<Tracer>emptyList())
                .build();

        Transaction mockTransaction = transactionData.getTransaction();
        when(mockTransaction.sampled()).thenReturn(true);
        when(mockTransaction.getPriority()).thenReturn(1.5f);

        SpanEventsServiceImpl spanEventsService = (SpanEventsServiceImpl) ServiceFactory.getSpanEventService();
        spanEventsService.dispatcherTransactionFinished(transactionData, new TransactionStats());

        SamplingPriorityQueue<SpanEvent> reservoir = spanEventsService.getOrCreateDistributedSamplingReservoir();
        assertEquals(1, reservoir.getSampled());
    }

    @Test
    public void testMaxSamplesStored() {
        SpanEventsService spanEventsService = serviceManager.getSpanEventsService();

        spanEventsService.setMaxSamplesStored(0);

        final SpanEvent event = new SpanEventFactory(appName)
                .setCategory(SpanCategory.generic)
                .setDecider(true)
                .setPriority(1.23f)
                .setDurationInSeconds(1.3f)
                .setHostName("yourHost")
                .setTraceId("gnisnacirema")
                .setGuid("globallyuniqueidentifier")
                .setSampled(true)
                .build();
        spanEventsService.storeEvent(event);

        SamplingPriorityQueue<SpanEvent> reservoir = spanEventsService.getOrCreateDistributedSamplingReservoir();
        assertEquals(0, reservoir.size());

        spanEventsService.setMaxSamplesStored(2);

        spanEventsService.storeEvent(event);
        spanEventsService.storeEvent(event);
        spanEventsService.storeEvent(event);
        spanEventsService.storeEvent(event);
        spanEventsService.storeEvent(event);

        reservoir = spanEventsService.getOrCreateDistributedSamplingReservoir();
        assertEquals(2, reservoir.size());

        spanEventsService.setMaxSamplesStored(13);

        for (int i = 0; i < 100; i++) {
            spanEventsService.storeEvent(event);
        }

        reservoir = spanEventsService.getOrCreateDistributedSamplingReservoir();
        assertEquals(13, reservoir.size());
    }

    @Test
    public void testDoesNotCreateSpansIfToldNotTo() {
        TransactionData transactionData = new TransactionDataTestBuilder(
                appName,
                ServiceFactory.getConfigService().getDefaultAgentConfig(),
                new MockDispatcherTracer())
                .setTracers(Collections.<Tracer>emptyList())
                .build();

        Transaction mockTransaction = transactionData.getTransaction();
        when(mockTransaction.sampled()).thenThrow(new AssertionError("should not have been called"));
        when(mockTransaction.getPriority()).thenThrow(new AssertionError("should not have been called"));

        when(spanEventCreationDecider.shouldCreateSpans(transactionData)).thenReturn(false);

        SpanEventsServiceImpl spanEventsService = (SpanEventsServiceImpl) ServiceFactory.getSpanEventService();
        spanEventsService.dispatcherTransactionFinished(transactionData, null);

        SamplingPriorityQueue<SpanEvent> reservoir = spanEventsService.getOrCreateDistributedSamplingReservoir();
        assertEquals(0, reservoir.getSampled());
    }
}
