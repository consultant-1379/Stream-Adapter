/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2013
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.streamAdapter.util;

import com.ericsson.streamAdapter.util.config.*;

import java.util.Timer;

/**
 * This class is used to monitor stream loading and to print some statistics
 *
 * @author eeimho
 *
 */
public class StreamLoadMonitor extends java.util.TimerTask {

    private Metrics metrics = null;

    /**
     * Constructor
     *
     * @param metrics
     */
    public StreamLoadMonitor(Metrics metrics) {
        // Save the event stream handler
        this.metrics = metrics;
        long monitorPeriod = Long.valueOf(StreamAdapterDefaults.MONITOR_PERIOD).longValue();
        new Timer(StreamLoadMonitor.class.getSimpleName()).schedule(this, monitorPeriod, monitorPeriod);
    }

    /**
     * Constructor
     *
     * @param metrics
     */
    public StreamLoadMonitor(Metrics metrics, Config config) {
        // Save the event stream handler
        this.metrics = metrics;
        long monitorPeriod = Long.valueOf(config.getValue(com.ericsson.streamAdapter.util.config.StreamAdapterConstants.MONITOR_PERIOD, StreamAdapterDefaults.MONITOR_PERIOD)).longValue();
        new Timer(StreamLoadMonitor.class.getSimpleName()).schedule(this, monitorPeriod, monitorPeriod);
    }

    /**
     * Overloaded Constructor
     *
     * @param metrics
     * @param period
     */
    public StreamLoadMonitor(Metrics metrics, long period) {

        // Save the event stream handler
        this.metrics = metrics;

        new Timer(StreamLoadMonitor.class.getSimpleName()).schedule(this, period, period);
    }

    /**
     * Output the metrics for stream loading
     */
    @Override
    public void run() {
        metrics.outputMetrics();
    }
}
