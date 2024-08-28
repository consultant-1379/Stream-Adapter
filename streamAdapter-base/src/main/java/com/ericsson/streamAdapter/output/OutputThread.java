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

package com.ericsson.streamAdapter.output;

import com.codahale.metrics.Meter;
import com.ericsson.streamAdapter.util.config.Config;
import com.ericsson.streamAdapter.util.config.StreamAdapterConstants;
import com.ericsson.streamAdapter.util.config.StreamAdapterDefaults;
import com.ericsson.streamAdapter.util.statistics.StatisticsHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.streamAdapter.output.exception.NoConnectionException;
import com.ericsson.streamAdapter.util.EventMetrics;
import com.ericsson.streamAdapter.util.Metrics;
import com.ericsson.streamAdapter.util.StreamedRecord;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * The class handles each event in the queue.
 * 
 * @author esmipau based on work by eeimho
 */
public class OutputThread implements Runnable, Metrics {
    private static final Logger logger = LoggerFactory.getLogger(OutputThread.class);
    private static final Logger metricLogger = LoggerFactory.getLogger(METRICS);

    private Thread thread = null;
    private String threadName = null;

    // Loading Metrics
    private EventMetrics eventMetrics = null;
    private OutputChannel outputChannel = null;

    private boolean isWriteSuccess;
    private boolean isFinished;
    private Config config;
    private StatisticsHandler statisticsHandler;
    private static boolean statisticsOn;
    private static boolean monitorOn;

    private static final int QUEUE_RELEASE_PERCENTAGE = 90;
    private BlockingQueue<StreamedRecord> recordQueue;
    private int queueSize;
    private int queueReleaseSize = 0;// The number of events below which a full queue must drop before allowing events back in

    private OutputRefreshMonitor refreshMonitor;
    private String type;
    private boolean queueFull = false;
    private Meter eventMeter;
    private String globalEventMeter;
    private Meter failedWriteMeter;
    private static final String EVENTS_RECEIVED = ".Events Received";
    private String globalFailedWriteMeter;
    private Meter connectionMeter;
    private static final String FAILED_WRITES = ".Failed Writes";
    private String globalConnectioMeter;
    private Meter disconnectMeter;
    private static final String CONN_RECEIVED = ".Connections Received";
    private String globalDisconnectionMeter;
    private static final String DISCON_RECEIVED = ".Disconnects Received";

    public OutputThread(OutputChannel outputChannel, Config config, int index) {
        this.outputChannel = outputChannel;
        this.threadName = OutputThread.class.getSimpleName() + "-" + index;
        this.config = config;
        this.queueSize = config.getValue(StreamAdapterConstants.MIN_QUEUE_SIZE, StreamAdapterDefaults.MIN_QUEUE_SIZE);
        recordQueue = new LinkedBlockingQueue<StreamedRecord>(queueSize);
        queueReleaseSize = queueSize * QUEUE_RELEASE_PERCENTAGE / 100;

        statisticsHandler = new StatisticsHandler(config);
        statisticsOn = statisticsHandler.isStatisticsOn();

        monitorOn = "true".equalsIgnoreCase(config.getValue(StreamAdapterConstants.STREAM_LOAD_MONITOR, StreamAdapterDefaults.STREAM_LOAD_MONITOR));
        if (monitorOn) {
            eventMetrics = new EventMetrics(false);
            eventMetrics.setStartTime(System.currentTimeMillis());
        }

        type = config.getValue(StreamAdapterConstants.OUTPUT_TYPE, StreamAdapterDefaults.OUTPUT_TYPE);

        if ("File".equals(type)) {
            // Now trigger the refresh thread
            refreshMonitor = new OutputRefreshMonitor(this, config);
        }

        if (statisticsOn) {
            eventMeter = statisticsHandler.createMeter(OutputThread.class, "@" + index, EVENTS_RECEIVED);
            globalEventMeter = OutputThread.class.getSimpleName() + ".@" + config.getSection() + EVENTS_RECEIVED;
            failedWriteMeter = statisticsHandler.createMeter(OutputThread.class, "@" + index, FAILED_WRITES);
            globalFailedWriteMeter = OutputThread.class.getSimpleName() + ".@" + config.getSection() + FAILED_WRITES;
            connectionMeter = statisticsHandler.createMeter(OutputThread.class, "@" + index, CONN_RECEIVED);
            globalConnectioMeter = OutputThread.class.getSimpleName() + ".@" + config.getSection() + CONN_RECEIVED;
            disconnectMeter = statisticsHandler.createMeter(OutputThread.class, "@" + index, DISCON_RECEIVED);
            globalDisconnectionMeter = OutputThread.class.getSimpleName() + ".@" + config.getSection() + DISCON_RECEIVED;
        }
    }

    public void start() {
        thread = new Thread(this);
        thread.setName(threadName);
        thread.start();
    }

    public void setChannel(OutputChannel outputChannel) {
        this.outputChannel = outputChannel;
    }

    public boolean processRecord(StreamedRecord streamedRecord) {

        if (queueFull) {
            return false;
        }
        // Check if there is space for this event
        if (recordQueue.size() < queueSize) {
            // Push the record onto the queue
            return recordQueue.offer(streamedRecord);
        } else {
            // Set the queue as full and issue a warning
            queueFull = true;
        }

        return false;

    }

    public BlockingQueue<StreamedRecord> getQueue() {
        return recordQueue;
    }

    public void close() {
        logger.info("OutputThread {} close() : Elements to be flushed before exiting the JVM : {}", threadName);
        if ("File".equals(type)) {
            // Now trigger the refresh thread
            refreshMonitor.cancel();
        }
        StreamedRecord streamedRecord = new StreamedRecord();
        streamedRecord.setAction(StreamedRecord.Actions.CLOSE);
        // The queue might be full.
        boolean done = false;
        int retryCnt = 0;
        do {
            done = processRecord(streamedRecord);
            try {
                Thread.sleep(1000); // wait a sec
                retryCnt++;
            } catch (InterruptedException e) {
                ;
            }
        } while (!done && retryCnt < 10);
        if (!done) {
            logger.warn("Queue full! Failed to send shutdown message after {} attempts.", retryCnt);
        }
    }

    public void outputMetrics() {
        if (monitorOn) {
            eventMetrics.setToTime(System.currentTimeMillis());
            metricLogger.debug(threadName + ";" + eventMetrics.toString());
            eventMetrics.setStartTime(System.currentTimeMillis());
        }
    }

    public EventMetrics getMetrics() {
        return eventMetrics;
    }

    // Check all output buffers for flushing, refresh etc.
    public void refresh() {
        long timeNow = System.currentTimeMillis();
        StreamedRecord streamedRecord;
        streamedRecord = new StreamedRecord();
        streamedRecord.setAction(StreamedRecord.Actions.REFRESH);
        streamedRecord.setTimeNow(timeNow);
        // The queue might be full.
        boolean done = false;
        int retryCnt = 0;
        do {
            done = processRecord(streamedRecord);
            try {
                Thread.sleep(1000); // wait a sec
                retryCnt++;
            } catch (InterruptedException e) {
                ;
            }
        } while (!done && retryCnt < 10);
        if (!done) {
            logger.warn("Queue full! Failed to send refresh message after " + retryCnt + " attempts.");
        }
    }

    @Override
    public void run() {
        // Run the event processing thread
        logger.info("Event thread {} started.", threadName);
        isFinished = false;
        while (!isFinished && thread.isAlive() && !thread.isInterrupted()) {
            try {
                isFinished = processMessage();
            } catch (NoConnectionException e) {
                logger.error("No connectiong Exception", e);
            } catch (Exception e) {
                logger.error("Event thread exception", e);
                thread.interrupt();
            }
        }
        logger.info("Event thread {} Ended", threadName);
    }

    public boolean processMessage() throws InterruptedException, OutputThreadException, NoConnectionException {
        StreamedRecord streamedRecord;
        streamedRecord = recordQueue.take();
        setWriteSuccess(false);
        if (queueFull) {
            if (recordQueue.size() < queueReleaseSize) {
                // OK, Release the queue again and tell users
                queueFull = false;
            }
        }
        if (outputChannel == null) {
            throw new NoConnectionException("No output channel set up");
        }
        switch (streamedRecord.getAction()) {
            case EVENT:
                incrementEvents();
                setWriteSuccess(outputChannel.handleEvent(streamedRecord));
                if (!isWriteSuccess()) {
                    incrementWriteFail();
                }
                break;
            case REFRESH:
                logger.info("Event thread Refresh");
                outputChannel.handleRefresh(streamedRecord);
                break;
            case CONNECT:
                incrementConnects();
                outputChannel.handleConnect(streamedRecord);
                break;
            case DISCONNECT:
                incrementDisconnects();
                outputChannel.handleDisconnect(streamedRecord);
                break;
            case CLOSE:
                outputChannel.handleClose(streamedRecord);
                streamedRecord.setAction(StreamedRecord.Actions.TERMINATE);
                recordQueue.offer(streamedRecord);
                break;
            case TERMINATE:
                logger.info("Event thread Terminate");
                return true;

            default:
                logger.error("Unknown event" + streamedRecord.getAction());
                break;
        }
        return false;
    }

    public boolean isWriteSuccess() {
        return isWriteSuccess;
    }

    public void setWriteSuccess(boolean isWriteSuccess) {
        this.isWriteSuccess = isWriteSuccess;
    }

    private void incrementEvents() {
        if (monitorOn) {
            eventMetrics.incrementEvents();
        }
        if (statisticsOn) {
            eventMeter.mark();
            statisticsHandler.getRegistry().getMeters().get(globalEventMeter).mark();
        }
    }

    private void incrementWriteFail() {
        if (monitorOn) {
            eventMetrics.incrementSocketWriteFail();
        }
        if (statisticsOn) {
            failedWriteMeter.mark();
            statisticsHandler.getRegistry().getMeters().get(globalFailedWriteMeter).mark();
        }
    }

    private void incrementConnects() {
        if (monitorOn) {
            eventMetrics.incrementConnects();
        }
        if (statisticsOn) {
            connectionMeter.mark();
            statisticsHandler.getRegistry().getMeters().get(globalConnectioMeter).mark();
        }
    }

    private void incrementDisconnects() {
        if (monitorOn) {
            eventMetrics.incrementDisconnects();
        }
        if (statisticsOn) {
            disconnectMeter.mark();
            statisticsHandler.getRegistry().getMeters().get(globalDisconnectionMeter).mark();
        }
    }
}