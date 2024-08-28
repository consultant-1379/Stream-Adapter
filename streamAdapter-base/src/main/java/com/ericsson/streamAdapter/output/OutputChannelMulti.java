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

import com.ericsson.streamAdapter.util.StreamedRecord;

import com.ericsson.streamAdapter.output.exception.NoConnectionException;

import java.util.HashMap;
import java.util.Map;

import com.ericsson.streamAdapter.util.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The class handles each event in the queue.
 * 
 * @author esmipau based on work by eeimho
 */
public abstract class OutputChannelMulti extends OutputChannel {
    private static final Logger logger = LoggerFactory.getLogger(OutputChannelMulti.class);
    private Map<Integer, EventOutputChannel> srcMap = new HashMap<Integer, EventOutputChannel>();

    public OutputChannelMulti(Config config) {
        super(config);
    }

    public void handleConnect(StreamedRecord streamedRecord) throws OutputThreadException {
        int sourceId = streamedRecord.getSourceId();
        EventOutputChannel channel = getOutputHandler();
        channel.connect(streamedRecord);
        getSrcMap().put(sourceId, channel);
    }

    public void handleDisconnect(StreamedRecord streamedRecord) throws OutputThreadException {
        int sourceId = streamedRecord.getSourceId();
        EventOutputChannel eo = getSrcMap().get(sourceId);
        if (eo != null) {
            eo.close(); // frees up resources
            getSrcMap().remove(sourceId);
        } else {
            logger.warn("Disconnect recieved for sourceId " + sourceId + " but no matching connect");
        }
    }

    public boolean handleEvent(StreamedRecord streamedRecord) throws NoConnectionException {
        int srcId = streamedRecord.getSourceId();
        EventOutputChannel eoc = getSrcMap().get(srcId);

        if (eoc == null && srcId != 0) { // Don't know where to put it
            logger.warn("Event from sourceId " + streamedRecord.getSourceId() + " but no matching connect");
            throw new NoConnectionException("No matching connect for Source Id : " + srcId);
        }
        return eoc.write(streamedRecord);
    }

    public void handleRefresh(StreamedRecord streamedRecord) {
        long timenow = streamedRecord.getTimeNow();
        for (EventOutputChannel channel : getSrcMap().values()) {
            channel.refresh(timenow);
        }
    }

    public void handleClose(StreamedRecord streamedRecord) {
        for (EventOutputChannel channel : getSrcMap().values()) {
            channel.close();
        }
    }

    protected Map<Integer, EventOutputChannel> getSrcMap() {
        return srcMap;
    }

}
