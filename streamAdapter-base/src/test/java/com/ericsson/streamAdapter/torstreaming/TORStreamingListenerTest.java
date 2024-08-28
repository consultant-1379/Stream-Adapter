package com.ericsson.streamAdapter.torstreaming;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;
import static com.ericsson.streamAdapter.util.TestConstants.*;

import com.ericsson.streamAdapter.output.OutputThreadHandler;
import com.ericsson.streamAdapter.torstreaming.controller.TORStreamingController;
import com.ericsson.streamAdapter.torstreaming.listener.TORStreamingListener;
import com.ericsson.streamAdapter.output.OutputThreadException;
import com.ericsson.streamAdapter.util.cache.BlockingQueueCache;
import com.ericsson.streamAdapter.util.config.Config;
import com.ericsson.streamAdapter.util.statistics.StatisticsHandler;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ericsson.streamAdapter.util.StreamedRecord;
import com.ericsson.streamAdapter.util.StreamData;
import com.ericsson.streamAdapter.util.Utils;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class TORStreamingListenerTest {

    private static final String INSTANCE_ID = "streamAdapter";
    private static TORStreamingListener objectToTest_torStreamingListener;
    private static OutputThreadHandler handler;
    private static MessageEvent event;
    private static StatisticsHandler statisticsHandler;
    private ChannelHandlerContext channelHandler;
    private ChannelBuffer channelBuffer;
    private static String topic;

    @BeforeClass
    public static void oneTimeSetUp() {
    	Utils.setPortCounts(-1);
        String workingDirectory = System.getProperty("user.dir");
        Config fileconfig = new Config(workingDirectory
                + "/src/test/resources/TestTORClient1.ini", "TorStreamCTUM");

        statisticsHandler = new StatisticsHandler(fileconfig);


        statisticsHandler.startStatisticsReporting();



        handler = new OutputThreadHandler(fileconfig);

        TORStreamingController theTorStreamingController = new TORStreamingController(INSTANCE_ID, fileconfig, handler);
        TORStreamingListener.setTorStreamingController(theTorStreamingController);
        event = mock(MessageEvent.class);
        objectToTest_torStreamingListener = new TORStreamingListener();

    }

    @AfterClass
    public static void oneTimeTearDown() {
        statisticsHandler.stopStatisticsReporting();
        statisticsHandler = null;
        objectToTest_torStreamingListener = null;
        event = null;
    }

    @Test
    public void test_handleInitiation() {
        channelBuffer = ChannelBuffers.wrappedBuffer(new StreamData().getInitilizationMessage());
        when(event.getMessage()).thenReturn(channelBuffer);
        objectToTest_torStreamingListener.messageReceived(channelHandler, event);
        assertTrue("Expected Initiation Message: " + INITIATION_MESSAGE + " Got :" + objectToTest_torStreamingListener.getEventType(),
                INITIATION_MESSAGE == objectToTest_torStreamingListener.getEventType());
    }

    @Test
    public void test_handleConnect() throws InterruptedException {

        test_handleInitiation();//This will set the variable length size in the connect message.
        byte[] eventdata = new StreamData().getConnectionMessage();

        byte[] ipAddress = Arrays.copyOfRange(eventdata, 14, 30);
        byte[] eventPayload = Arrays.copyOfRange(eventdata, 31, eventdata.length);

        channelBuffer = ChannelBuffers.wrappedBuffer(eventdata);
        when(event.getMessage()).thenReturn(channelBuffer);
        objectToTest_torStreamingListener.messageReceived(channelHandler, event);
//        StreamedRecord streamRecord = handler.
//
//        assertTrue("Expected Connection Message: " + CONNECTION_MESSAGE + " Got :" + streamRecord.getEventType(),
//                CONNECTION_MESSAGE == streamRecord.getEventType());
//        assertTrue("Expected Action: " + StreamedRecord.Actions.CONNECT + " Got :" + streamRecord.getAction(),
//                StreamedRecord.Actions.CONNECT == streamRecord.getAction());
//        assertTrue("Expected Source Id: " + 1 + " Got :" + streamRecord.getSourceId(), 1 == streamRecord.getSourceId());
//
//        assertTrue("Expected IP Address: " + ipAddress + " Got :" + streamRecord.getRemoteIP(), Arrays.equals(ipAddress, streamRecord.getRemoteIP()));
//
//        byte[] payload = streamRecord.getData();
//        assertArrayEquals(eventPayload, payload);
    }

    @Test
    public void test_handleEvent() throws InterruptedException {
        byte[] eventdata = new StreamData().getEventMessage();
        byte[] eventPayload = Arrays.copyOfRange(eventdata, 4, eventdata.length);
        channelBuffer = ChannelBuffers.wrappedBuffer(eventdata);
        when(event.getMessage()).thenReturn(channelBuffer);
        objectToTest_torStreamingListener.messageReceived(channelHandler, event);

//        StreamedRecord streamRecord = outputQList.take(topic);
//        assertTrue("Expected Event Message: " + EVENT_MESSAGE + " Got :" + streamRecord.getEventType(),
//                EVENT_MESSAGE == streamRecord.getEventType());
//        assertTrue("Expected Action: " + StreamedRecord.Actions.EVENT + " Got :" + streamRecord.getAction(),
//                StreamedRecord.Actions.EVENT == streamRecord.getAction());
//
//        assertTrue("Expected Source Id: " + 1 + " Got :" + streamRecord.getSourceId(), 1 == streamRecord.getSourceId());
//        byte[] payload = streamRecord.getData();
//        assertArrayEquals(eventPayload, payload);
    }

    @Test
    public void test_handleDisconnect() throws InterruptedException {
        byte[] eventdata = new StreamData().getDisconnectMessage();
        byte[] reason = Arrays.copyOfRange(eventdata, 14, eventdata.length);
        channelBuffer = ChannelBuffers.wrappedBuffer(eventdata);
        when(event.getMessage()).thenReturn(channelBuffer);
        objectToTest_torStreamingListener.messageReceived(channelHandler, event);
//        StreamedRecord streamRecord = outputQList.take(topic);
//
//        assertTrue("Expected Disconnect Message: " + DISCONNECTION_MESSAGE + " Got :" + streamRecord.getEventType(),
//                DISCONNECTION_MESSAGE == streamRecord.getEventType());
//        assertTrue("Expected Action: " + StreamedRecord.Actions.DISCONNECT + " Got :" + streamRecord.getAction(),
//                StreamedRecord.Actions.DISCONNECT == streamRecord.getAction());
//
//        assertTrue("Expected Source Id: " + 1 + " Got :" + streamRecord.getSourceId(), 1 == streamRecord.getSourceId());
//
//        assertTrue(streamRecord.getDisconnectReason() == ByteBuffer.wrap(reason).getShort());

    }

    @Test
    public void test_handleEventsDropped() throws OutputThreadException {
        channelBuffer = ChannelBuffers.wrappedBuffer(new StreamData().getDroppedEventMessage());
        when(event.getMessage()).thenReturn(channelBuffer);
        objectToTest_torStreamingListener.messageReceived(channelHandler, event);
        assertTrue("Expected Events Dropped Message: " + DROPPED_EVENTS_MESSAGE + " Got :" + objectToTest_torStreamingListener.getEventType(),
                DROPPED_EVENTS_MESSAGE == objectToTest_torStreamingListener.getEventType());
    }
}
