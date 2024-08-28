package com.ericsson.streamAdapter.output;

import com.ericsson.streamAdapter.util.StreamedRecord;

public class DummyEventOutputDummy implements EventOutputChannel {

    @Override
    public boolean write(StreamedRecord record) {
        return true; // dummy class does nothing
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