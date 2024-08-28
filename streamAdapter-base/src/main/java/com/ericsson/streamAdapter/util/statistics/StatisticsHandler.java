package com.ericsson.streamAdapter.util.statistics;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.ericsson.streamAdapter.StreamService;
import com.ericsson.streamAdapter.util.config.Config;
import com.ericsson.streamAdapter.util.config.StreamAdapterConstants;
import com.ericsson.streamAdapter.util.config.StreamAdapterDefaults;

public class StatisticsHandler {

    private final static String JMX_ATTRIBUTE_SERVICE_DEPLOYED = "ServiceDeployed";

    private static Boolean statisticsOn;
    private final MetricRegistry metricsRegistry;
    private final Counter modulesDeployed;
    private final Config config;

    public StatisticsHandler(Config config) {
        this.config = config;
        metricsRegistry = StatisticsRegistry.getMetricRegistry(config);
        modulesDeployed = metricsRegistry.counter(MetricRegistry.name(StatisticsHandler.class, JMX_ATTRIBUTE_SERVICE_DEPLOYED));
    }

    public Boolean isStatisticsOn() {
        if (statisticsOn == null) {
            statisticsOn = "true".equalsIgnoreCase(config.getValue(StreamAdapterConstants.STATISTIC_ON, StreamAdapterDefaults.STATISTIC_ON));
        }
        return statisticsOn;
    }

    public void startStatisticsReporting() {
        StatisticsRegistry.start();
    }

    public void stopStatisticsReporting() {
        StatisticsRegistry.stop();
    }

    public MetricRegistry getRegistry() {
        return metricsRegistry;
    }

    public Meter createMeter(final Class<?> className,final String name, final String type) {
        if (className == null) {
            throw new IllegalArgumentException("className must not be null");
        }
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("name must not be null or empty");
        }
        if (type == null || type.isEmpty()) {
            throw new IllegalArgumentException("type must not be null or empty");
        }
        return metricsRegistry.meter(MetricRegistry.name(className.getSimpleName(), name, type));
    }

    public Meter createMeter(final String name, final String type, final StreamService service) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("name must not be null or empty");
        }
        if (type == null || type.isEmpty()) {
            throw new IllegalArgumentException("type must not be null or empty");
        }
        if (service == null) {
            throw new IllegalArgumentException("service must not be null");
        }

        return metricsRegistry.meter(MetricRegistry.name(service.getName(), name, type));
    }

    public void registerServiceDeployed() {
        modulesDeployed.inc();
    }

    public void registerServiceUndeployed() {
        modulesDeployed.dec();
    }

}
