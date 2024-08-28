package com.ericsson.streamAdapter.output;

import com.ericsson.streamAdapter.util.StreamedRecord;

public interface EventOutputChannel {

    boolean write(StreamedRecord record);

    void refresh(long timeNowInMilliSecs);

    void close();

    void connect(StreamedRecord record);

}