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
package com.ericsson.streamAdapter.util;

/**
 * This class is a container for incoming records
 * 
 * @author eeimho
 */
public class StreamedRecord {

    private int sourceId;
    private byte[] data;
    private byte[] remoteIP;
    private int dataSize;
    private int disconnectReason;
    private Actions action = Actions.UNSET;
    private long timeNow; // time when refresh was triggered
    private byte messageType;

    public StreamedRecord() {

    }

    public StreamedRecord(int sourceId) {
        this.sourceId = sourceId;
    }

    public int getDisconnectReason() {
        return disconnectReason;
    }

    public void setDisconnectReason(int disconnectReason) {
        this.disconnectReason = disconnectReason;
    }

    public long getTimeNow() {
        return timeNow;
    }

    public void setTimeNow(long timeNow) {
        this.timeNow = timeNow;
    }

    public Actions getAction() {
        return this.action;
    }

    public void setAction(Actions action) {
        this.action = action;
    }

    public int getSourceId() {
        return sourceId;
    }

    public void setSourceId(int sourceId) {
        this.sourceId = sourceId;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.action = Actions.EVENT;
        this.data = data;
        this.dataSize = data.length;
    }

    public int getDataSize() {
        return dataSize;
    }

    public byte[] getRemoteIP() {
        return remoteIP;
    }

    public void setRemoteIP(byte[] remoteIP) {
        this.remoteIP = remoteIP;
    }

    public byte getEventType() {
        return messageType;

    }

    public void setEventType(byte messageType) {
        this.messageType = messageType;

    }

    /* A record will be added to the processing queue to control the output channel in the same way an event is handled */
    public enum Actions {
        UNSET, EVENT, REFRESH, CLOSE, TERMINATE, CONNECT, DISCONNECT
    }
}
