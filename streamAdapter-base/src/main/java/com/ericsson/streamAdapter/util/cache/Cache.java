package com.ericsson.streamAdapter.util.cache;

import com.ericsson.streamAdapter.util.StreamedRecord;

import java.util.Collection;
import java.util.Set;

public interface Cache {

    boolean offer(StreamedRecord o);

    boolean offer(StreamedRecord o, String topic);

    StreamedRecord take(String topic);

    int size();

    boolean isEmpty();

    Set<String> getTopics();

    Collection getQueue(String topic);

}
