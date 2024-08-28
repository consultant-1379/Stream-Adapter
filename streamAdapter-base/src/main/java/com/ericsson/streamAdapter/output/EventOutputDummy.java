package com.ericsson.streamAdapter.output;

import com.ericsson.streamAdapter.util.StreamedRecord;

public class EventOutputDummy implements EventOutputChannel {

    @Override
    public boolean write(StreamedRecord record) {
        //Some random processing.
        for(int i=0 ; i < 1000; i++){
           //some really complicated stuff here.
            record.getSourceId();
        }
        return true;
    }

    @Override
    public void refresh(long timeNowInMilliSecs) {

    }

    @Override
    public void close() {

    }

    @Override
    public void connect(StreamedRecord record) {

    }

}