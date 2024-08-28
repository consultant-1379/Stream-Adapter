package com.ericsson.streamAdapter.output;

import com.ericsson.streamAdapter.util.StreamedRecord;
import com.ericsson.streamAdapter.output.exception.NoConnectionException;
import com.ericsson.streamAdapter.util.config.Config;

public abstract class OutputChannel {
    protected Config config;
    protected EventOutputChannel eventOutput;

    public OutputChannel(Config config) {
        this.config = config;

    }

    public void handleConnect(StreamedRecord streamedRecord) throws OutputThreadException {
        if(eventOutput == null){
            eventOutput = getOutputHandler();
        }
        eventOutput.connect(streamedRecord);

    }

    public void handleDisconnect(StreamedRecord streamedRecord) throws OutputThreadException {
    	if(eventOutput!=null){
    		eventOutput.close();
    	}
    }

    public boolean handleEvent(StreamedRecord streamedRecord) throws NoConnectionException {
        return eventOutput.write(streamedRecord);
    }

    public void handleRefresh(StreamedRecord streamedRecord) {
        if (eventOutput != null) {
            long timenow = streamedRecord.getTimeNow();
            eventOutput.refresh(timenow);
        }
    }

    public void handleClose(StreamedRecord streamedRecord) {
    	if(eventOutput!=null){
    			eventOutput.close();
    	}
    }

    protected abstract EventOutputChannel getOutputHandler() throws OutputThreadException;

}