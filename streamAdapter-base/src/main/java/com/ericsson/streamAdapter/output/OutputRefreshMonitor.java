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

import java.util.Timer;
import com.ericsson.streamAdapter.util.StreamedRecord;
import com.ericsson.streamAdapter.util.config.Config;
import com.ericsson.streamAdapter.util.config.StreamAdapterConstants;
import com.ericsson.streamAdapter.util.config.StreamAdapterDefaults;

/**
 * This class is used to trigger output stream flushing, refreshing etc.
 * 
 * @author eeimho
 * 
 */
public class OutputRefreshMonitor extends java.util.TimerTask {
    private static long refreshPeriod;
    private OutputThread outputThread;

    /**
     * Constructor
     * 
     * @param outputThread
     * @param config
     */
    public OutputRefreshMonitor(OutputThread outputThread, Config config) {
        this.outputThread = outputThread;
        refreshPeriod = config.getValue(StreamAdapterConstants.REFRESH_PERIOD, StreamAdapterDefaults.REFRESH_PERIOD);
        new Timer(OutputRefreshMonitor.class.getSimpleName()).schedule(this, refreshPeriod, refreshPeriod);
    }

    /**
     * Output the metrics for stream loading
     */
    @Override
    public void run() {
        StreamedRecord refreshRecord = new StreamedRecord();
        refreshRecord.setAction(StreamedRecord.Actions.REFRESH);
        refreshRecord.setTimeNow(System.currentTimeMillis());
        outputThread.processRecord(refreshRecord);
    }
}
