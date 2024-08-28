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

package com.ericsson.streamAdapter.torstreaming.listener;

import com.codahale.metrics.Meter;
import com.ericsson.streamAdapter.output.OutputThreadHandler;
import com.ericsson.streamAdapter.torstreaming.controller.TORStreamingController;
import com.ericsson.streamAdapter.util.EventMetrics;
import com.ericsson.streamAdapter.util.Metrics;
import com.ericsson.streamAdapter.util.StreamedRecord;
import com.ericsson.streamAdapter.util.config.Config;
import com.ericsson.streamAdapter.util.config.StreamAdapterConstants;
import com.ericsson.streamAdapter.util.config.StreamAdapterDefaults;
import com.ericsson.streamAdapter.util.statistics.StatisticsHandler;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * The class handles TOR streaming Protocol for event streaming.
 * 
 * @author eeimho
 * 
 *         Every event streamed out from the streaming solution will be wrapped in a StreamOut message.
 * 
 *         Each message contains a header portion followed by the original event.
 * 
 *         The header part will have a defined structure according to the message type.
 * 
 *         The message type will be the 1st byte in the header.
 * 
 *         -------------------------- StreamOut Header -------------------------
 * 
 *         ----------------- Event type ------------ Source ID -----------------
 * 
 *         ----------------- (1 byte) ------------ (3 bytes) -------------------
 * 
 * 
 *         Event type ::
 * 
 *         1 byte length
 * 
 *         Describes type of message the Streaming solution is sending to client.
 * 
 *         Possible values are 0 to 4 as described below:
 * 
 *         0: Event message.
 * 
 *         1: Protocol initialization message.
 * 
 *         2: Connection message.
 * 
 *         3: Disconnection message.
 * 
 *         4: Dropped events message.
 * 
 * 
 *         Source ID ::
 * 
 *         3 bytes length
 * 
 *         0 < source id < 16777216
 * 
 *         identifies the NE that produced this event.
 * 
 */
public class TORStreamingListener extends SimpleChannelUpstreamHandler implements Metrics {

    private static final Logger logger = LoggerFactory.getLogger(TORStreamingListener.class);
    private static final Logger metricLogger = LoggerFactory.getLogger(METRICS);
    // Constants for messages
    private static final byte EVENT_MESSAGE = 0x00;
    private static final byte INITIATION_MESSAGE = 0x01;
    private static final byte CONNECTION_MESSAGE = 0x02;
    private static final byte DISCONNECTION_MESSAGE = 0x03;
    private static final byte DROPPED_EVENTS_MESSAGE = 0x04;
    // Constants for field lengths
    private static final int IP_ADDRESS_LENGTH = 16;
    // Constants for field values
    private static final byte PREAMBLE_FIELD = 0x000c;
    private static final long PROTOCOL_VERSION_FIELD = 281479271743489L;
    private static boolean statisticsOn;
    private static boolean monitorOn;
    // The TOR Streaming controller that is connected to the TOR streaming server
    private static TORStreamingController torStreamingController;
    private OutputThreadHandler outputhandler;
    // Values set by protocol messages
    private long protocolVersion;
    private short preamble;
    private short variableLengthSize;
    private long latestTimestamp = 0;
    // Loading metrics for this thread
    private EventMetrics receivedMetrics = null;
    private byte eventType;
    private String instanceId;
    private Meter recordsMeter;
    private Meter eventMeter;
    private Meter dropEventMeter;
    private Meter lostEventMeter;
    private Meter connectionMeter;
    private Meter disconnectMeter;
    private Meter queueMeter;
    private Meter initMeter;
    private Meter invalidRecordsMeter;
    private Meter noSourceIdMeter;
    private StatisticsHandler handler;
    private HashMap<Integer, byte[]> sourceId_IpAddress = new HashMap<Integer, byte[]>();
    private Set<Integer> srcSet = new TreeSet<Integer>(); // set of currently valid source IDs
    private Set<Integer> invalidSrcSet = new TreeSet<Integer>(); // set of currently known invalid source IDs

    /**
     * Constructor, initialize the TOR streaming listener
     */
    public TORStreamingListener() {
        instanceId = torStreamingController.getInstanceId();
        logger.debug("TORStreamingListener({})->", instanceId);
        outputhandler = torStreamingController.getHandler();
        Config config = torStreamingController.getConfig();
        handler = new StatisticsHandler(config);
        statisticsOn = handler.isStatisticsOn();
        // Register with TOR Streaming Controller
        torStreamingController.setListener(this);

        monitorOn = "true".equalsIgnoreCase(config.getValue(StreamAdapterConstants.STREAM_LOAD_MONITOR, StreamAdapterDefaults.STREAM_LOAD_MONITOR));
        if (monitorOn) {
            receivedMetrics = new EventMetrics(false);
            receivedMetrics.setStartTime(System.currentTimeMillis());
        }
        if (statisticsOn) {
            recordsMeter = handler.createMeter(TORStreamingListener.class, "@" + instanceId, "Records Received");
            eventMeter = handler.createMeter(TORStreamingListener.class, "@" + instanceId, "Events Received");
            dropEventMeter = handler.createMeter(TORStreamingListener.class, "@" + instanceId, "Dropped events");
            lostEventMeter = handler.createMeter(TORStreamingListener.class, "@" + instanceId, "Lost events");
            connectionMeter = handler.createMeter(TORStreamingListener.class, "@" + instanceId, "Connections Received");
            disconnectMeter = handler.createMeter(TORStreamingListener.class, "@" + instanceId, "Disconnects Received");
            initMeter = handler.createMeter(TORStreamingListener.class, "@" + instanceId, "Initialisations Received");
            queueMeter = handler.createMeter(TORStreamingListener.class, "@" + instanceId, "Failed Queue(offer) Attempts");
            invalidRecordsMeter = handler.createMeter(TORStreamingListener.class, "@" + instanceId, "Invalid Records Received");
            noSourceIdMeter = handler.createMeter(TORStreamingListener.class, "@" + instanceId, "No SourceId Received");
        }
        logger.debug("TORStreamingListener({})<-", instanceId);
    }

    /**
     * Set the TOR streaming controller being used
     * 
     * @param theTorStreamingController
     */
    public static void setTorStreamingController(TORStreamingController theTorStreamingController) {
        torStreamingController = theTorStreamingController;
    }

    private void incrementRecords() {
        if (monitorOn) {
            receivedMetrics.incrementRecords();
        }
        if (statisticsOn) {
            recordsMeter.mark();
        }
    }

    private void incrementEvents() {
        if (monitorOn) {
            receivedMetrics.incrementEvents();
        }
        if (statisticsOn) {
            eventMeter.mark();
        }
    }

    private void incrementInits() {
        if (monitorOn) {
            receivedMetrics.incrementInits();
        }
        if (statisticsOn) {
            initMeter.mark();
        }
    }

    private void incrementConnects() {
        if (monitorOn) {
            receivedMetrics.incrementConnects();
        }
        if (statisticsOn) {
            connectionMeter.mark();
        }
    }

    private void incrementDisconnects() {
        if (monitorOn) {
            receivedMetrics.incrementDisconnects();
        }
        if (statisticsOn) {
            disconnectMeter.mark();
        }
    }

    private void incrementDrops() {
        if (monitorOn) {
            receivedMetrics.incrementDrops();
        }
        if (statisticsOn) {
            dropEventMeter.mark();
        }
    }

    private void incrementQueueFull() {
        if (monitorOn) {
            receivedMetrics.incrementQueueFull();
        }
        if (statisticsOn) {
            queueMeter.mark();
        }
    }

    private void invalidRecords(final long incrementAmount) {
        if (monitorOn) {
            receivedMetrics.incrementInvalidRecords(incrementAmount);
        }
        if (statisticsOn) {
            invalidRecordsMeter.mark();
        }
    }

    private void noSourceId() {
        if (monitorOn) {
            receivedMetrics.incrementNoSrc();
        }
        if (statisticsOn) {
            noSourceIdMeter.mark();
        }
    }

    private void lostRecords(final long incrementAmount) {
        if (monitorOn) {
            receivedMetrics.incrementLostRecords(incrementAmount);
        }
        if (statisticsOn) {
            lostEventMeter.mark();
        }
    }

    /**
     * Method to receive and process an event from NETTY
     * 
     * @param ctx
     *            : The channel handler context
     * @param event
     *            : The received event
     */
    @Override
    public void messageReceived(final ChannelHandlerContext ctx, final MessageEvent event) {
        // Get the channel buffer of the event
        ChannelBuffer channelBuffer = (ChannelBuffer) event.getMessage();

        // Get the stream ID
        // Use int as it is 4 bytes
        // EventType (1 byte) + SourceId (3 bytes)
        int streamId = channelBuffer.readInt();

        incrementRecords();

        // We're waiting for the message type field, its the highest byte
        eventType = (byte) (streamId >> 24);
        Integer sourceId = Integer.valueOf((streamId & 0x00ffffff));

        // Check for each type of message and call the appropriate handler
        try {
            switch (eventType) {
                case EVENT_MESSAGE: {
                    if (srcSet.contains(sourceId)) {
                        handleEvent(channelBuffer, sourceId);
                        incrementEvents();
                    } else {
                        if (invalidSrcSet.contains(sourceId)) {
                            // do nothing - it has already been reported
                        } else {
                            logger.error("Got message for invalid sourceId : " + sourceId);
                            invalidSrcSet.add(sourceId);
                        }
                        noSourceId();
                    }
                    break;
                }
                case INITIATION_MESSAGE: {
                    handleInitiation(channelBuffer);
                    incrementInits();
                    break;
                }
                case CONNECTION_MESSAGE: {
                    if (srcSet.contains(sourceId)) {
                        logger.warn("Got another connection message for " + sourceId);
                    } else {
                        srcSet.add(sourceId);
                        if (invalidSrcSet.contains(sourceId)) {
                            invalidSrcSet.remove(sourceId);
                        }
                    }
                    handleConnect(channelBuffer, sourceId);
                    incrementConnects();
                    break;
                }
                case DISCONNECTION_MESSAGE: {
                    if (srcSet.contains(sourceId)) {
                        srcSet.remove(sourceId);
                    } else {
                        logger.warn("Got disconnect for unknown sourceID " + sourceId);
                    }
                    handleDisconnect(channelBuffer, sourceId);
                    incrementDisconnects();
                    break;
                }
                case DROPPED_EVENTS_MESSAGE: {
                    handleEventsDropped(channelBuffer, sourceId);
                    incrementDrops();
                    break;
                }
                default: {
                    invalidRecords(1);
                    logger.error("Unexpected event type received: " + eventType);
                }
            }
        } catch (Exception e) {

            logger.error("Processing of event failed" + eventType);

        }

        // Clear the channel buffer
        channelBuffer.clear();
    }

    /**
     * Netty channelConnected event. In this method the connection metrics are updated.
     * 
     * @param ctx
     *            : The channel handler context
     * @param event
     *            : The channel state event
     * 
     * @see org.jboss.netty.channel.SimpleChannelHandler#channelConnected(org.jboss .netty.channel.ChannelHandlerContext,
     *      org.jboss.netty.channel.ChannelStateEvent)
     */
    @Override
    public void channelConnected(final ChannelHandlerContext ctx, final ChannelStateEvent event) throws Exception {
        logger.debug("TOR Streaming channel connected[{}]", instanceId);
    }

    /**
     * Netty channelDisconnected event. In this method the connection metrics are updated.
     * 
     * @param ctx
     *            : The channel handler context
     * @param event
     *            : The channel state event
     * 
     * @see org.jboss.netty.channel.SimpleChannelHandler#channelDisconnected(org. jboss.netty.channel.ChannelHandlerContext,
     *      org.jboss.netty.channel.ChannelStateEvent)
     */
    @Override
    public void channelDisconnected(final ChannelHandlerContext ctx, final ChannelStateEvent event) throws Exception {
        logger.debug("TOR Streaming channel disconnected[{}]", instanceId);
        torStreamingController.reset();
    }

    /**
     * Invoked when an exception was raised by an I/O thread or a ChannelHandler.
     */
    public void exceptionCaught(final ChannelHandlerContext ctx, final ExceptionEvent e) throws Exception {
        logger.error("TOR Streaming exception thrown[{}], {}", instanceId, e.getCause());
        torStreamingController.reset();
    }

    /**
     * Offers the StreamRecord to the Cache.
     * 
     * @param streamedRecord
     * 
     */
    private void offer(final StreamedRecord streamedRecord) {
        if (!(outputhandler.processRecord(streamedRecord))) {
            incrementQueueFull();
            lostRecords(1);
        }
    }

    /**
     * This method handles an event received on a stream
     * 
     * @param inputStream
     *            The input stream to read from
     * @param sourceId
     *            The source node of the message
     * @throws Exception
     *             on errors
     */
    private void handleEvent(final ChannelBuffer channelBuffer, final int sourceId) throws Exception {
        // Holder for the incoming record
        StreamedRecord streamedRecord = new StreamedRecord(sourceId);
        int readable = channelBuffer.readableBytes();
        streamedRecord.setEventType(eventType);
        byte[] data = new byte[readable];
        channelBuffer.readBytes(data);
        streamedRecord.setData(data);
        streamedRecord.setAction(StreamedRecord.Actions.EVENT);
        streamedRecord.setRemoteIP(sourceId_IpAddress.get(Integer.valueOf(sourceId)));
        offer(streamedRecord);
    }

    /**
     * This method handles initiation of an event stream
     * 
     * @param inputStream
     *            The input stream to read from
     * @throws Exception
     *             on errors
     */
    private void handleInitiation(final ChannelBuffer channelBuffer) throws Exception {
        // The preamble is 2 bytes long
        preamble = channelBuffer.readShort();

        // Check the preambles match
        if (preamble != PREAMBLE_FIELD) {
            logger.debug("preamble mismatch, sent " + PREAMBLE_FIELD + ", received " + preamble);
        }

        // The protocol version is 8 bytes long
        protocolVersion = channelBuffer.readLong();

        // Check the protocol versions match
        if (protocolVersion != PROTOCOL_VERSION_FIELD) {
            logger.debug("protocol version mismatch, sent " + PROTOCOL_VERSION_FIELD + ", received " + protocolVersion);
        }

        // Variable length size is 2 bytes long
        // Reserved for future use
        variableLengthSize = channelBuffer.readShort();
    }

    /**
     * This method handles connection of a node on an event stream
     * 
     * @param inputStream
     *            The input stream to read from
     * @param sourceId
     *            The source node of the message
     * @throws Exception
     *             on errors
     */
    private void handleConnect(final ChannelBuffer channelBuffer, final int sourceId) throws Exception {
        // The length of this message is 2 bytes
        short length = channelBuffer.readShort();
        //specifies size of  the rest of the message, i.e. length from this field forward (including the field itself)
        //Length(2)+latestTimestamp(8)+IPAddress(16)=26
        length = (short) (length - 26);

        // The time stamp is 8 bytes
        latestTimestamp = channelBuffer.readLong();

        // The IP address of the node is 16 bytes
        byte[] ipAddress = new byte[IP_ADDRESS_LENGTH];
        channelBuffer.readBytes(ipAddress);

        // Variable length field is of the length specified in the initiation message
        // For future used.
        byte[] variableLengthField = new byte[variableLengthSize];
        channelBuffer.readBytes(variableLengthField);

        //Handle a node connection
        StreamedRecord streamedRecord = new StreamedRecord(sourceId);
        streamedRecord.setEventType(eventType);
        streamedRecord.setRemoteIP(ipAddress);
        // Pass the header message (FFV,FFI) into the connection message.
        int readable = channelBuffer.readableBytes();
        byte[] data = new byte[readable];
        channelBuffer.readBytes(data);
        streamedRecord.setData(data);
        streamedRecord.setAction(StreamedRecord.Actions.CONNECT);
        sourceId_IpAddress.put(Integer.valueOf(sourceId), ipAddress);
        offer(streamedRecord);
        logger.debug("IP ADDRESS[{}] {}", instanceId, ipAddress);
    }

    /**
     * This method handles disconnection of a node on an event stream
     * 
     * @param inputStream
     *            The input stream to read from
     * @param sourceId
     *            The source node of the message
     * @throws Exception
     *             on errors
     */
    private void handleDisconnect(final ChannelBuffer channelBuffer, final int sourceId) throws Exception {
        // The preamble is 2 bytes long
        preamble = channelBuffer.readShort();

        // The time stamp is 8 bytes
        latestTimestamp = channelBuffer.readLong();

        // Reason is 2 bytes long
        int disconnectReason = channelBuffer.readShort();

        StreamedRecord streamedRecord = new StreamedRecord(sourceId);
        streamedRecord.setEventType(eventType);
        streamedRecord.setAction(StreamedRecord.Actions.DISCONNECT);
        streamedRecord.setDisconnectReason(disconnectReason);
        streamedRecord.setRemoteIP(sourceId_IpAddress.get(Integer.valueOf(sourceId)));
        // Handle a node connection
        offer(streamedRecord);
    }

    /**
     * This method handles events dropped from a node on an event stream
     * 
     * @param inputStream
     *            The input stream to read from
     * @param sourceId
     *            The source node of the message
     * @throws Exception
     *             on errors
     */
    private void handleEventsDropped(final ChannelBuffer channelBuffer, final int sourceId) throws Exception {
        // The preamble is 2 bytes long
        preamble = channelBuffer.readShort();

        // The time stamp is 8 bytes
        latestTimestamp = channelBuffer.readLong();

        //        // Reason is 2 bytes long
        int droppedReason = channelBuffer.readShort();

        // Dropped event count is 8 bytes long
        lostRecords(channelBuffer.readLong());
        logger.debug("Events dropped Reason :: {}", droppedReason);
    }

    /**
     *
     */
    @Override
    public void outputMetrics() {
        if (monitorOn) {
            receivedMetrics.setToTime(System.currentTimeMillis());
            metricLogger.info(TORStreamingListener.class.getSimpleName() + "-" + instanceId + ";" + receivedMetrics.toString());
            receivedMetrics.setStartTime(System.currentTimeMillis());
        }
    }

    /**
     * Getters from here
     */
    public long getProtocolVersion() {
        return protocolVersion;
    }

    public short getPreamble() {
        return preamble;
    }

    public short getVariableLengthSize() {
        return variableLengthSize;
    }

    public long getLatestTimestamp() {
        return latestTimestamp;
    }

    public byte getEventType() {
        return eventType;
    }
}
