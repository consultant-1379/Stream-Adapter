package com.ericsson.streamAdapter.util.cache;

import com.codahale.metrics.Meter;
import com.ericsson.streamAdapter.util.StreamedRecord;
import com.ericsson.streamAdapter.util.Utils;
import com.ericsson.streamAdapter.util.config.Config;
import com.ericsson.streamAdapter.util.config.StreamAdapterConstants;
import com.ericsson.streamAdapter.util.config.StreamAdapterDefaults;
import com.ericsson.streamAdapter.util.statistics.StatisticsHandler;

import java.util.Hashtable;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class BlockingQueueCache implements Cache {

    // The percentage at which the queue must empty to before allowing events back in after queue filling
    private static final int QUEUE_RELEASE_PERCENTAGE = 90;
    private BlockingQueue<StreamedRecord> outputQList[];
    private Hashtable<String, BlockingQueue<StreamedRecord>> queueMap = new Hashtable<String, BlockingQueue<StreamedRecord>>();
    private Hashtable<Integer, Boolean> queueFullTracker = new Hashtable<>();
    private int queueReleaseSize = 0;// The number of events below which a full queue must drop before allowing events back in
    private int numberOfQueues;
    private int queueSize;
    private Config config;
    private static boolean statisticsOn;
    private StatisticsHandler handler;
    private Meter recordsMeter;
    private Meter dropEventMeter;
    private Meter cacheFullMeter;

    public BlockingQueueCache(Config config) {
        this.config = config;
        String type = config.getValue(StreamAdapterConstants.OUTPUT_TYPE, StreamAdapterDefaults.OUTPUT_TYPE);
        this.numberOfQueues = config.getValue(StreamAdapterConstants.NUM_OF_THREADS, StreamAdapterDefaults.NUM_OF_THREADS);
        if ("MZ".contains(type)) {
            String propsFile = config.getValue(StreamAdapterConstants.CTRS_PROPS, StreamAdapterDefaults.CTRS_PROPS);

            // Override the number of threads to use one per port
            numberOfQueues = Utils.getMZPortsCount(
                    "CTUM".equalsIgnoreCase(config.getValue(StreamAdapterConstants.DATA_TYPE, StreamAdapterDefaults.DATA_TYPE)), propsFile);
        }
        queueSize = config.getValue(StreamAdapterConstants.MIN_QUEUE_SIZE, StreamAdapterDefaults.MIN_QUEUE_SIZE);
        queueReleaseSize = queueSize * QUEUE_RELEASE_PERCENTAGE / 100;
        handler = new StatisticsHandler(config);
        statisticsOn = handler.isStatisticsOn();
        if (statisticsOn) {
            recordsMeter = handler.createMeter(BlockingQueueCache.class,"Cache", "Records Received");
            dropEventMeter = handler.createMeter(BlockingQueueCache.class,"Cache", "Dropped Records");
            cacheFullMeter = handler.createMeter(BlockingQueueCache.class,"Cache", "Cache full");
        }

        initalizeQueue();

    }

    @Override
    public boolean offer(StreamedRecord o, String topic) {
        BlockingQueue<StreamedRecord> queue = queueMap.get(topic);
        if (queue != null) {
            if (queueFullTracker.get(queue.hashCode())) {
            	if (statisticsOn){
            		dropEventMeter.mark();
            	}
                return false;
            }
            if (queue.size() < queueSize) {
            	if (statisticsOn){
            		recordsMeter.mark();
            	}
                return queue.offer(o);
            } else {
                queueFullTracker.put(queue.hashCode(), true);
                if (statisticsOn){
                	cacheFullMeter.mark();
                }
                return false;
            }
        } else {
            return false;
        }
    }

    private void initalizeQueue() {
        if (numberOfQueues > 0) {
            // known error with the way Java handles generics and arrays
            outputQList = new BlockingQueue[numberOfQueues];
            for (int i = 0; i < numberOfQueues; i++) {
                outputQList[i] = new LinkedBlockingQueue<StreamedRecord>(queueSize);
                queueMap.put(config.getSection() + "(" + i + ")", outputQList[i]);
                queueFullTracker.put(outputQList[i].hashCode(), false);
            }
        }
    }

    @Override
    public boolean offer(StreamedRecord record) {
        if (outputQList != null) {
            int sourceId = record.getSourceId();
            int index = sourceId % numberOfQueues;
            if (queueFullTracker.get(outputQList[index].hashCode())) {
            	if (statisticsOn){
            		dropEventMeter.mark();
            	}
                return false;
            }
            if (outputQList[index].size() < queueSize) {
            	if (statisticsOn){
            		recordsMeter.mark();
            	}
                return outputQList[index].offer(record);
            } else {
                queueFullTracker.put(outputQList[index].hashCode(), true);
                if (statisticsOn){
                	cacheFullMeter.mark();
                }
                return false;
            }
        } else {
            return false;
        }

    }

    @Override
    public StreamedRecord take(String topic) {
        BlockingQueue<StreamedRecord> queue = queueMap.get(topic);
        if (queue != null) {
           // synchronized (queue) {
                try {
                    StreamedRecord record = queue.take();
                    // Check if the queue is full, if so, check if it should be released
                    if (queueFullTracker.get(queue.hashCode())) {
                        if (queue.size() < queueReleaseSize) {
                            queueFullTracker.put(queue.hashCode(), false);
                        }
                    }
                    return record;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
           // }
        }
        return null;
    }


    public  BlockingQueue<StreamedRecord> getQueue(String topic){
           return queueMap.get(topic);
    }



    @Override
    public Set<String> getTopics() {
        return queueMap.keySet();
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < numberOfQueues; i++) {
            if (!outputQList[i].isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int size() {
        int size = 0;
        for (int i = 0; i < numberOfQueues; i++) {
            size += outputQList[i].size();
        }
        return size;
    }

}
