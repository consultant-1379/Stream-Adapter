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

package com.ericsson.streamAdapter;

import com.ericsson.streamAdapter.output.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.streamAdapter.torstreaming.controller.TORStreamingController;
import com.ericsson.streamAdapter.util.Metrics;
import com.ericsson.streamAdapter.util.StreamLoadMonitor;
import com.ericsson.streamAdapter.util.config.Config;
import com.ericsson.streamAdapter.util.config.StreamAdapterConstants;
import com.ericsson.streamAdapter.util.config.StreamAdapterDefaults;
import com.ericsson.streamAdapter.util.statistics.StatisticsHandler;

/**
 * This class starts a streaming service De-Multiplier.
 * 
 * @author eeimho
 */

public class StreamService implements Metrics {
    private static final Logger logger = LoggerFactory.getLogger(StreamService.class);
    private static StreamService demuxer1;
    private Config config;
    private TORStreamingController controller;
    private OutputThreadHandler outputThreadHandler;
    private StreamLoadMonitor monitor;
    private StatisticsHandler statisticsHandler;
    private static boolean statisticsOn;

    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new JVMShutDownMonitor());
        demuxer1 = new StreamService();
        demuxer1.start(args);
    }

    public void start(String[] args) {
        String iniFile = "MyStreamAdapter.ini";
        String section = "TorStreamCTUM";

        for (int i = 0; i < (args.length - 1); i++) {
            if (args[i].equals("-f")) { // iniFile
                iniFile = args[i + 1];
            }
            if (args[i].equals("-i")) { // input section
                section = args[i + 1];
            }
        }
        Config config = new Config(iniFile, section);
        int die_after = config.getValue(StreamAdapterConstants.RUN_FOR_MINUTES, StreamAdapterDefaults.RUN_FOR_MINUTES);

        this.init(config, section);
        this.execute();

        if (die_after > 0) {
            System.out.println("Only going to run for " + die_after + " minutes.");
            try {
                Thread.sleep(die_after * 60 * 1000);
            } catch (InterruptedException e) {
                ; // go and terminate normally
            }
            System.out.println("Terminating, " + die_after + " minutes is up.");
            demuxer1.terminate();
        }

    }

    @Override
    public void outputMetrics() {
        controller.outputMetrics();
        outputThreadHandler.outputMetrics();
    }

    /**
     * This method is responsible for starting the service.
     * 
     */
    public void execute() {
        logger.debug("execute()-->");
        boolean monitorOn = "true".equalsIgnoreCase(config.getValue(StreamAdapterConstants.STREAM_LOAD_MONITOR,
                StreamAdapterDefaults.STREAM_LOAD_MONITOR));
        if (monitorOn) {
            monitor = new StreamLoadMonitor(this, config);
        }
        if (statisticsOn) {
            statisticsHandler.startStatisticsReporting();
            statisticsHandler.registerServiceDeployed();
        }


        outputThreadHandler.start();
        controller.start();

        logger.debug("execute()<--");
    }

    /**
     * 
     * The method is responsible for initialising the service.
     * 
     * @param config
     * @param instanceId
     */
    public void init(Config config, String section) {
        logger.debug("init()-->");
        this.config = config;
        statisticsHandler = new StatisticsHandler(config);
        statisticsOn = statisticsHandler.isStatisticsOn();
        outputThreadHandler = new OutputThreadHandler(config);
        controller = new TORStreamingController(section, config, outputThreadHandler);
        logger.debug("init()<--");
    }







    /**
     * This method is responsible for terminating the service.
     * 
     * Shutdown process for output threads On each thread, call close(). This will add close message to end of Q When processed, HandleClose() this
     * will call <channel>.close() to release any resources and tidy up, <channel>.close() will add Terminate message to the Q. When processed, this
     * will call <channel>.stop() on each channel, and terminate the channel, it will then end this thread.
     * 
     */
    public void terminate() {
        logger.debug("terminate()-->");
        controller.stop();
        outputThreadHandler.close();
        boolean monitorOn = "true".equalsIgnoreCase(config.getValue(StreamAdapterConstants.STREAM_LOAD_MONITOR,
                StreamAdapterDefaults.STREAM_LOAD_MONITOR));
        if (monitorOn) {
        	monitor.cancel();
        }

        if (statisticsOn) {
            statisticsHandler.registerServiceUndeployed();
            statisticsHandler.stopStatisticsReporting();
        }

        logger.debug("terminate()-->");
    }

    public String getName() {
        return "StreamAdapter";
    }

    // a class that extends thread that is to be called when program is exiting
    static class JVMShutDownMonitor extends Thread {
        public void run() {
            logger.info("JVMShutDownMonitor:run() : JVM stop triggered , cleaning up existing tasks.");
            demuxer1.terminate();
        }
    }
}
