package com.ericsson.streamAdapter.output;

import com.ericsson.streamAdapter.util.Metrics;
import com.ericsson.streamAdapter.util.StreamedRecord;
import com.ericsson.streamAdapter.util.Utils;
import com.ericsson.streamAdapter.util.config.Config;
import com.ericsson.streamAdapter.util.config.StreamAdapterConstants;
import com.ericsson.streamAdapter.util.config.StreamAdapterDefaults;
import com.ericsson.streamAdapter.util.statistics.StatisticsHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OutputThreadHandler implements Metrics {

    private static final Logger logger = LoggerFactory.getLogger(OutputThreadHandler.class);
    private int numberOfThreads;
    private OutputThread outputThreadList[];
    private Config config;
    private String type;
    private static boolean statisticsOn;
    private StatisticsHandler statisticsHandler;

    public OutputThreadHandler(Config config) {
        this.config = config;
        type = config.getValue(StreamAdapterConstants.OUTPUT_TYPE, StreamAdapterDefaults.OUTPUT_TYPE);
        this.numberOfThreads = config.getValue(StreamAdapterConstants.NUM_OF_THREADS, StreamAdapterDefaults.NUM_OF_THREADS);
        if ("MZ".contains(type)) {
            String propsFile = config.getValue(StreamAdapterConstants.CTRS_PROPS, StreamAdapterDefaults.CTRS_PROPS);

            // Override the number of threads to use one per port
            numberOfThreads = Utils.getMZPortsCount(
                    "CTUM".equalsIgnoreCase(config.getValue(StreamAdapterConstants.DATA_TYPE, StreamAdapterDefaults.DATA_TYPE)), propsFile);
        }
        outputThreadList = new OutputThread[numberOfThreads];
        statisticsHandler = new StatisticsHandler(config);
        statisticsOn = statisticsHandler.isStatisticsOn();

        if (statisticsOn) {
            createGlobalStatistics();
        }
    }

    private void createGlobalStatistics(){
        if (statisticsHandler.isStatisticsOn()) {
            statisticsHandler.createMeter(OutputThread.class,"@" + config.getSection(), "Events Received");
            statisticsHandler.createMeter(OutputThread.class,"@" + config.getSection(), "Failed Writes");
            statisticsHandler.createMeter(OutputThread.class,"@" + config.getSection(), "Connections Received");
            statisticsHandler.createMeter(OutputThread.class,"@" + config.getSection(), "Disconnects Received");
        }
    }

    public void start() {
        for (int out = 0; out < numberOfThreads; out++) {
            logger.debug("Creating Output Threads:{}", (out + 1));
            OutputChannel outputChannel = getOutputChannel(type, out);
            outputThreadList[out] = new OutputThread(outputChannel, config, (out + 1));
            outputThreadList[out].start();
        }
    }

    public void close() {
        // Close handling on each thread
        for (OutputThread outputThread : outputThreadList) {
            outputThread.close();
        }
    }

    @Override
    public void outputMetrics() {
        for (OutputThread outputThread : outputThreadList) {
            outputThread.outputMetrics();
        }
    }

    private OutputChannel getOutputChannel(String myTypeParam, int outputChannelNumber) {
        OutputChannel outputChannel = null;

        if ("File".equals(myTypeParam)) {
            boolean isCtum = "CTUM".equalsIgnoreCase(config.getValue(StreamAdapterConstants.DATA_TYPE, StreamAdapterDefaults.DATA_TYPE));

            if (isCtum) {
                outputChannel = new CTUMOutputChannel(config);
            } else {
                outputChannel = new CTRSOutputChannel(config);
            }
        } else if ("MZ".equals(myTypeParam)) {
            outputChannel = new MzOutputChannel(config, (outputChannelNumber + 1));
        } else {
            outputChannel = new DummyOutputChannel(config);
        }
        return outputChannel;
    }

    public boolean processRecord(StreamedRecord streamedRecord) {
        if (outputThreadList == null || outputThreadList.length == 0) {
            return false;
        }
        return outputThreadList[streamedRecord.getSourceId() % outputThreadList.length].processRecord(streamedRecord);
    }

}
