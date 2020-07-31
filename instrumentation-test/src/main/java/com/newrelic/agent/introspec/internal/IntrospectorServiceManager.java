/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.introspec.internal;

import com.newrelic.agent.*;
import com.newrelic.agent.attributes.AttributesService;
import com.newrelic.agent.browser.BrowserService;
import com.newrelic.agent.cache.CacheService;
import com.newrelic.agent.circuitbreaker.CircuitBreakerService;
import com.newrelic.agent.commands.CommandParser;
import com.newrelic.agent.config.*;
import com.newrelic.agent.core.CoreService;
import com.newrelic.agent.database.DatabaseService;
import com.newrelic.agent.environment.EnvironmentService;
import com.newrelic.agent.environment.EnvironmentServiceImpl;
import com.newrelic.agent.extension.ExtensionService;
import com.newrelic.agent.instrumentation.ClassTransformerService;
import com.newrelic.agent.instrumentation.PointCutClassTransformer;
import com.newrelic.agent.instrumentation.classmatchers.ClassAndMethodMatcher;
import com.newrelic.agent.instrumentation.context.ClassMatchVisitorFactory;
import com.newrelic.agent.instrumentation.context.InstrumentationContextManager;
import com.newrelic.agent.instrumentation.custom.ClassRetransformer;
import com.newrelic.agent.interfaces.ReservoirManager;
import com.newrelic.agent.interfaces.backport.Consumer;
import com.newrelic.agent.jmx.JmxService;
import com.newrelic.agent.language.SourceLanguageService;
import com.newrelic.agent.logging.IAgentLogger;
import com.newrelic.agent.model.SpanEvent;
import com.newrelic.agent.normalization.NormalizationService;
import com.newrelic.agent.normalization.NormalizationServiceImpl;
import com.newrelic.agent.profile.ProfilerService;
import com.newrelic.agent.reinstrument.RemoteInstrumentationService;
import com.newrelic.agent.reinstrument.RemoteInstrumentationServiceImpl;
import com.newrelic.agent.rpm.RPMConnectionService;
import com.newrelic.agent.samplers.SamplerService;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.Service;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.ServiceManager;
import com.newrelic.agent.service.analytics.*;
import com.newrelic.agent.service.async.AsyncTransactionService;
import com.newrelic.agent.service.module.JarCollectorService;
import com.newrelic.agent.service.module.JarCollectorServiceImpl;
import com.newrelic.agent.sql.SqlTraceService;
import com.newrelic.agent.sql.SqlTraceServiceImpl;
import com.newrelic.agent.stats.StatsService;
import com.newrelic.agent.trace.TransactionTraceService;
import com.newrelic.agent.tracing.DistributedTraceService;
import com.newrelic.agent.tracing.DistributedTraceServiceImpl;
import com.newrelic.agent.utilization.UtilizationService;

import java.lang.instrument.Instrumentation;
import java.util.*;

class IntrospectorServiceManager extends AbstractService implements ServiceManager {

    private volatile RPMServiceManager rpmServiceManager;
    private volatile CoreService coreService;
    private volatile ConfigService configService;
    private volatile ThreadService threadService;
    private volatile EnvironmentService environmentService;
    private volatile TransactionTraceService transactionTraceService;
    private volatile TransactionService transactionService;
    private volatile ProfilerService profilerService;
    private volatile RPMConnectionService rpmConnectionService;
    private volatile StatsService statsService;
    private volatile HarvestService harvestService;
    private volatile SqlTraceService sqlTraceService;
    private volatile BrowserService browserService;
    private volatile CacheService cacheService;
    private volatile SamplerService samplerService;
    private volatile DatabaseService dbService;
    private volatile JarCollectorService jarCollectorService;
    private volatile TransactionEventsService transactionEventsService;
    private volatile NormalizationService normalizationService;
    private volatile ExtensionService extensionService;
    private volatile TracerService tracerService;
    private volatile CommandParser commandParser;
    private volatile RemoteInstrumentationService remoteInstrumentationService;
    private volatile ClassTransformerService classTransformerService;
    private volatile AttributesService attributesService;
    private volatile UtilizationService utilizationService;
    private volatile JmxService jmxService;
    private volatile AsyncTransactionService asyncTxService;
    private volatile CircuitBreakerService circuitBreakerService;
    private volatile InsightsService insightsService;
    private volatile DistributedTraceServiceImpl distributedTraceService;
    private volatile SpanEventsService spanEventsService;
    private volatile SourceLanguageService sourceLanguageService;
    private ExpirationService expirationService;

    private IntrospectorServiceManager(String name) {
        super(name);
    }

    public static IntrospectorServiceManager createAndInitialize(Map<String, Object> configOverrides) {
        final IntrospectorServiceManager manager = new IntrospectorServiceManager("ServiceManager");
        Map<String, Object> config = new HashMap<>();
        config.put(AgentConfigImpl.APP_NAME, "TestApp");
        config.put("trim_stats", false);
        Map<String, Object> crossProcessConfig = new HashMap<>();
        config.put("cross_application_tracing", Boolean.TRUE);
        config.put("cross_application_tracer", crossProcessConfig);
        crossProcessConfig.put("cross_process_id", "1xyz234#1xyz3333");
        crossProcessConfig.put("application_id", "1xyz3333");
        crossProcessConfig.put("encoding_key", "cafebabedeadbeef8675309babecafe1beefdead");
        List<String> trusted = new ArrayList<>();
        trusted.add("1xyz234");
        crossProcessConfig.put("trusted_account_ids", trusted);
        config.put(AgentConfigImpl.OPEN_TRACING, Collections.singletonMap("enabled", false));

        Map<String, Object> dt = new HashMap<>();
        dt.put("enabled", false);

        // These come down on connect
        dt.put(DistributedTracingConfig.ACCOUNT_ID, "1xyz234");
        // Trusted account key is different from account id on purpose to test this scenario
        dt.put(DistributedTracingConfig.TRUSTED_ACCOUNT_KEY, "123456789");
        dt.put(DistributedTracingConfig.PRIMARY_APPLICATION_ID, "1xyz3333");
        config.put("distributed_tracing", dt);
        Map<String, Object> spanConfig = new HashMap<>();
        spanConfig.put("collect_span_events", true);
        config.put("span_events", spanConfig);

        if (configOverrides != null) {
            config = deepMerge(config, configOverrides);
        }
        manager.setup(config);
        return manager;
    }

    private void setup(Map<String, Object> config) {
        configService = ConfigServiceFactory.createConfigService(AgentConfigImpl.createAgentConfig(config), Collections.<String, Object>emptyMap());
        ServiceFactory.setServiceManager(this);
        coreService = new IntrospectorCoreService();
        threadService = new ThreadService();
        environmentService = new EnvironmentServiceImpl();
        transactionService = new TransactionService();
        rpmConnectionService = new IntrospectorRPMConnectService();
        rpmServiceManager = new IntrospectorRPMServiceManager();
        transactionTraceService = new IntrospectorTransactionTraceService();
        asyncTxService = new AsyncTransactionService();
        profilerService = new ProfilerService();
        statsService = new IntrospectorStatsService();
        harvestService = new IntrospectorHarvestService();
        sqlTraceService = new SqlTraceServiceImpl();
        insightsService = new IntrospectorInsightsService();
        expirationService = new ExpirationService();
        dbService = new DatabaseService();
        jarCollectorService = new JarCollectorServiceImpl();
        distributedTraceService = new DistributedTraceServiceImpl();

        TransactionDataToDistributedTraceIntrinsics transactionDataToDistributedTraceIntrinsics = new TransactionDataToDistributedTraceIntrinsics(distributedTraceService);
        transactionEventsService = new TransactionEventsService(transactionDataToDistributedTraceIntrinsics);

        normalizationService = new NormalizationServiceImpl();
        extensionService = new ExtensionService(configService);
        tracerService = new TracerService();
        commandParser = new CommandParser();
        remoteInstrumentationService = new RemoteInstrumentationServiceImpl();
        sourceLanguageService = new SourceLanguageService();
        classTransformerService = new ClassTransformerService() {
            @Override
            public void stop() {
            }

            @Override
            public void start() {
            }

            @Override
            public boolean isStoppedOrStopping() {
                return false;
            }

            @Override
            public boolean isStopped() {
                return false;
            }

            @Override
            public boolean isStartedOrStarting() {
                return true;
            }

            @Override
            public boolean isStarted() {
                return true;
            }

            @Override
            public boolean isEnabled() {
                return true;
            }

            @Override
            public String getName() {
                return "ClassTransformer";
            }

            @Override
            public IAgentLogger getLogger() {
                return Agent.LOG;
            }

            @Override
            public void retransformMatchingClassesImmediately(Class<?>[] loadedClasses, Collection<ClassMatchVisitorFactory> classMatchers) {
            }

            @Override
            public void retransformMatchingClasses(Collection<ClassMatchVisitorFactory> classMatchers) {
            }

            @Override
            public ClassRetransformer getRemoteRetransformer() {
                return null;
            }

            @Override
            public ClassRetransformer getLocalRetransformer() {
                return null;
            }

            @Override
            public Instrumentation getExtensionInstrumentation() {
                return null;
            }

            @Override
            public InstrumentationContextManager getContextManager() {
                return null;
            }

            @Override
            public PointCutClassTransformer getClassTransformer() {
                return null;
            }

            @Override
            public void checkShutdown() {
            }

            @Override
            public boolean addTraceMatcher(ClassAndMethodMatcher matcher, String metricPrefix) {
                return false;
            }
        };
        jmxService = new JmxService();
        attributesService = new AttributesService();
        circuitBreakerService = new CircuitBreakerService();
        AgentConfig agentConfig = createAgentConfig(config, (Map) config.get("distributed_tracing"));
        distributedTraceService.connected(null, agentConfig);
        ReservoirManager<SpanEvent> reservoirManager = new CollectorSpanEventReservoirManager(configService);
        ReservoirManager.EventSender<SpanEvent> collectorSender = new CollectorSpanEventSender(rpmServiceManager);
        Consumer<SpanEvent> infiniteTracing = new Consumer<SpanEvent>() {
            @Override
            public void accept(SpanEvent spanEvent) {
            }
        };
        SpanEventCreationDecider spanEventCreationDecider = new SpanEventCreationDecider(configService);
        spanEventsService = new IntrospectorSpanEventService(
                agentConfig,
                reservoirManager,
                collectorSender,
                infiniteTracing,
                spanEventCreationDecider,
                environmentService,
                transactionDataToDistributedTraceIntrinsics);
        configService.addIAgentConfigListener((IntrospectorSpanEventService)spanEventsService);
        transactionService.addTransactionListener((IntrospectorSpanEventService)spanEventsService);

        try {
            transactionTraceService.start();
            transactionEventsService.start();
            transactionService.start();
        } catch (Exception e) {
            // fall through
        }
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void addService(Service service) {
    }

    @Override
    public CommandParser getCommandParser() {
        return commandParser;
    }

    @Override
    public ExtensionService getExtensionService() {
        return extensionService;
    }

    @Override
    public JmxService getJmxService() {
        return jmxService;
    }

    @Override
    public ProfilerService getProfilerService() {
        return profilerService;
    }

    @Override
    public RPMServiceManager getRPMServiceManager() {
        return rpmServiceManager;
    }

    @Override
    public HarvestService getHarvestService() {
        return harvestService;
    }

    @Override
    public SamplerService getSamplerService() {
        return samplerService;
    }

    @Override
    public Service getService(String name) {
        return null;
    }

    @Override
    public Map<String, Map<String, Object>> getServicesConfiguration() {
        Map<String, Map<String, Object>> config = new HashMap<>();

        Map<String, Object> serviceInfo = new HashMap<>();
        serviceInfo.put("enabled", true);
        config.put("TransactionService", serviceInfo);

        serviceInfo = new HashMap<>();
        config.put("TransactionTraceService", serviceInfo);
        serviceInfo.put("enabled", true);

        serviceInfo = new HashMap<>();
        serviceInfo.put("enabled", true);
        config.put("ProfilerService", serviceInfo);

        serviceInfo = new HashMap<>();
        serviceInfo.put("enabled", true);
        config.put("TracerService", serviceInfo);

        serviceInfo = new HashMap<>();
        serviceInfo.put("enabled", true);
        config.put("CommandParser", serviceInfo);

        serviceInfo = new HashMap<>();
        serviceInfo.put("enabled", true);
        config.put("JmxService", serviceInfo);

        serviceInfo = new HashMap<>();
        serviceInfo.put("enabled", true);
        config.put("ThreadService", serviceInfo);

        return config;
    }

    @Override
    public ThreadService getThreadService() {
        return threadService;
    }

    @Override
    public TracerService getTracerService() {
        return tracerService;
    }

    @Override
    public TransactionService getTransactionService() {
        return transactionService;
    }

    @Override
    public TransactionTraceService getTransactionTraceService() {
        return transactionTraceService;
    }

    @Override
    public CoreService getCoreService() {
        return coreService;
    }

    @Override
    protected void doStart() {
    }

    @Override
    protected void doStop() {
    }

    @Override
    public ConfigService getConfigService() {
        return configService;
    }

    @Override
    public RPMConnectionService getRPMConnectionService() {
        return rpmConnectionService;
    }

    @Override
    public EnvironmentService getEnvironmentService() {
        return environmentService;
    }

    @Override
    public ClassTransformerService getClassTransformerService() {
        return classTransformerService;
    }

    @Override
    public StatsService getStatsService() {
        return statsService;
    }

    @Override
    public SqlTraceService getSqlTraceService() {
        return sqlTraceService;
    }

    @Override
    public BrowserService getBrowserService() {
        return browserService;
    }

    @Override
    public CacheService getCacheService() {
        return cacheService;
    }

    @Override
    public DatabaseService getDatabaseService() {
        return dbService;
    }

    @Override
    public NormalizationService getNormalizationService() {
        if (normalizationService == null) {
            normalizationService = new NormalizationServiceImpl();
        }
        return normalizationService;
    }

    @Override
    public JarCollectorService getJarCollectorService() {
        return jarCollectorService;
    }

    @Override
    public TransactionEventsService getTransactionEventsService() {
        return transactionEventsService;
    }

    @Override
    public RemoteInstrumentationService getRemoteInstrumentationService() {
        return remoteInstrumentationService;
    }

    @Override
    public AttributesService getAttributesService() {
        return attributesService;
    }

    @Override
    public InsightsService getInsights() {
        return insightsService;
    }

    @Override
    public CircuitBreakerService getCircuitBreakerService() {
        return circuitBreakerService;
    }

    @Override
    public AsyncTransactionService getAsyncTxService() {
        return asyncTxService;
    }

    @Override
    public UtilizationService getUtilizationService() {
        return utilizationService;
    }

    @Override
    public DistributedTraceService getDistributedTraceService() {
        return distributedTraceService;
    }

    @Override
    public SpanEventsService getSpanEventsService() {
        return spanEventsService;
    }

    @Override
    public SourceLanguageService getSourceLanguageService() {
        return sourceLanguageService;
    }

    @Override
    public ExpirationService getExpirationService() {
        return expirationService;
    }

    private AgentConfig createAgentConfig(Map<String, Object> settings, Map<String, Object> serverData) {
        settings = new HashMap<>(settings);
        serverData = new HashMap<>(serverData);
        AgentConfigFactory.mergeServerData(settings, serverData, null);
        return AgentConfigImpl.createAgentConfig(settings);
    }

    // Override entries in original
    private static Map deepMerge(Map original, Map toOverride) {
        for (Object key : toOverride.keySet()) {
            if (toOverride.get(key) instanceof Map && original.get(key) instanceof Map) {
                Map originalChild = (Map) original.get(key);
                Map newChild = (Map) toOverride.get(key);
                original.put(key, deepMerge(originalChild, newChild));
            } else {
                original.put(key, toOverride.get(key));
            }
        }
        return original;
    }
}
