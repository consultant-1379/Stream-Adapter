package com.ericsson.streamAdapter.util.statistics;

import com.codahale.metrics.CsvReporter;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.ericsson.streamAdapter.util.config.Config;
import com.ericsson.streamAdapter.util.config.StreamAdapterConstants;
import com.ericsson.streamAdapter.util.config.StreamAdapterDefaults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.concurrent.TimeUnit;

public class StatisticsRegistry {

    private static final String REPORTING_CSV = "CSV";
    private static final String REPORTING_JMX = "JMX";
    private final static Logger logger = LoggerFactory.getLogger(StatisticsRegistry.class);
    private static final MetricRegistry metricRegistry = new MetricRegistry();
    private static JmxReporter jmxReporter;
    private static CsvReporter csvReporter;
    private static Config config;
    private static boolean started = false;

    private static void startStatisticsReporter() {
        final String reportingMethod = config.getValue(StreamAdapterConstants.REPORTER, StreamAdapterDefaults.REPORTER);
        if (reportingMethod != null && !reportingMethod.isEmpty()) {
            logger.info("Reporting method set to {}", reportingMethod);
        }
        if (REPORTING_CSV.equalsIgnoreCase(reportingMethod)) {
            logger.debug("Will use CSV reporter for statistics. Trying to find output location");
            String csvOutputFolder = config.getValue(StreamAdapterConstants.CSV_LOCATION, StreamAdapterDefaults.CSV_LOCATION);
            if (csvOutputFolder.equals(StreamAdapterDefaults.CSV_LOCATION)) {
                logger.debug("key:{} was not set. Will use default output location for CSV files {}", StreamAdapterConstants.CSV_LOCATION, csvOutputFolder);
            }
            startCsvReporter(csvOutputFolder);
        } else if (REPORTING_JMX.equalsIgnoreCase(reportingMethod)) {
            logger.info("Will output statistics to JMX.");
            startJmxReporter();
        } else {
            logger.warn("Will output statistics to JMX. {} has wrong value {}", StreamAdapterConstants.REPORTER, reportingMethod);
            startJmxReporter();
        }
    }

    private static void startJmxReporter() {
        jmxReporter = JmxReporter.forRegistry(metricRegistry).build();
        jmxReporter.start();
        logger.debug("Successfully started statistics JMX reporter");
    }

    private static void startCsvReporter(final String outputFolder) {
        logger.debug("Starting CSV reporting to location [{}]", outputFolder);
        csvReporter = CsvReporter.forRegistry(metricRegistry).build(new File(outputFolder));
        csvReporter.start(1, TimeUnit.SECONDS);
        logger.debug("Successfully started statistics CSV reporter");
    }

    /**
     * Start the metrics Reporter
     */
    static void start() {
        if (!started) {
            startStatisticsReporter();
            started = true;
        }
    }

    /**
     * Shutdown of the metric reporter and registry
     */
    static void stop() {
        if (started) {
            if (jmxReporter != null) {
                jmxReporter.stop();
            }
            if (csvReporter != null) {
                csvReporter.stop();
            }
            started = false;
        }
    }

    /**
     * 
     * @return MetricsRegistry the instance of the MetricsRegistriy for reporting purpose and register more metrics
     */
    static MetricRegistry getMetricRegistry(Config config) {
        StatisticsRegistry.config = config;
        return metricRegistry;
    }

}
