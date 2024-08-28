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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.streamAdapter.util.StreamedRecord;

/**
 * This class is a container for outgoing channels for events for each source
 * Id, the information about where and how to output the record is stored here
 * 
 * @author esmipau
 */
public class EventOutputToMZ implements EventOutputChannel,Runnable {

	private static final Logger logger = LoggerFactory
			.getLogger(EventOutputToMZ.class);
	private SocketChannel socket;
	private String ipAddress;
	private int port;
    private static final long STATE_TRANSITION_DELAY = 10000;
	//private List <StreamedRecord> headersNotSentList;
    private Map <Integer,StreamedRecord> headersNotSentMap;
	private boolean isCtum = true; // if for CTRS records

	public EventOutputToMZ(String ipAddress, int port, boolean isCTUM) {
		this.ipAddress = ipAddress;
		this.port = port;
		createConnection(ipAddress, port);
		new Thread(this,"Reconnect Monitor").start();
		//headersNotSentList = Collections.synchronizedList(new ArrayList<StreamedRecord>());
		headersNotSentMap = new ConcurrentHashMap<Integer,StreamedRecord>();
		this.isCtum = isCTUM;
	}

	public EventOutputToMZ() {
		//headersNotSentList = Collections.synchronizedList(new ArrayList<StreamedRecord>());
		headersNotSentMap = new ConcurrentHashMap<Integer,StreamedRecord>();
		; // used by test tools
	}

	public void connect(StreamedRecord record) {
		if (!write(record)) {
			byte[] remoteIP = record.getRemoteIP();
			if (remoteIP != null) {
				headersNotSentMap.put(record.getSourceId(), record);
			}
			//headersNotSentList.add(record);			
		}
	}

	@Override
	public boolean write(StreamedRecord record) {

		byte[] remoteIP = record.getRemoteIP();
		if (remoteIP == null) {
			return false; // records can't be sent before a connect message is received
		}

		try {
			int sourceId = record.getSourceId();
			final int SOURCE_ID = 4;
			final int REMOTE_IP = 16;
			final int RECORD_LENGTH = 2;
			int RECORD_TYPE = 2;
			if (isCtum) {
				RECORD_TYPE = 1;
			}

			int recordLength = record.getDataSize() + RECORD_LENGTH
					+ RECORD_TYPE + REMOTE_IP;

			if (!isCtum) {
				recordLength = recordLength + SOURCE_ID;
			}
			ByteBuffer buffer = null;
			buffer = ByteBuffer.allocate(recordLength); 

			buffer.putShort((short) ((recordLength) & (short) 0xFFFF));
			buffer.put((byte) record.getData()[2]);
			if (!isCtum) {
				buffer.put((byte) record.getData()[3]);
				buffer.putInt(sourceId);				
			}			
			buffer.put(remoteIP);
			buffer.put(record.getData());
			buffer.flip();
			return writeSocket(record, recordLength, buffer);

		} catch (Exception e) {
			logger.error("Error in writing byte buffer", e.getMessage());
			return false;
		}
	}

	public boolean writeSocket(StreamedRecord record, int recordLength,
			ByteBuffer buffer) {
		if (getSocket().isConnected()) {
			try {
				int bytesWritten = 0;
				bytesWritten = getSocket().write(buffer);
				if(bytesWritten < recordLength){
					logger.debug("Unable to write all bytes , event lost");
					return false;
				}
				return true;
			} catch (IOException e) {
				logger.error("Unable to send events to remote port, closing connection!", e.getMessage());
					close();  // stop trying to write (socket will be automatically reopened in 10secs
			}
		}
		return false;
	}

	/*
	 * refresh the output channel if needed
	 */
	@Override
	public void refresh(long timeNow) {
		; // nothing needs doing
	}

	/*
	 * close the buffer, release any resources. output buffer no longer valid
	 */
	@Override
	public void close() {
		try {
			getSocket().close();
		} catch (IOException e) {
			logger.warn("Error in closeing socket with IP: " + ipAddress
					+ " Port: " + port, e.getMessage());
		}
	}

	public boolean createConnection(String ipAddress, int port) {
		try {
			setSocket(getSocketChannel());
            setConfigureBlocking();
			getSocket().connect(new InetSocketAddress(ipAddress, port));
			getSocket().finishConnect();
		} catch (IOException e) {
			logger.error("Error in connecting to socket with IP: " + ipAddress
					+ " Port: " + port, e.getMessage());
			return false;

		}
		return true;
	}

	public SocketChannel getSocketChannel() throws IOException {
		return SocketChannel.open();
	}

	protected void setConfigureBlocking() throws IOException {
		getSocket().configureBlocking(true);
	}

	public SocketChannel getSocket() {
		return socket;
	}

	public void setSocket(SocketChannel socket) {
		this.socket = socket;
	}


    @Override
	public void run() {
		
		while (true) {
			if (!getSocket().isConnected()) {
				boolean connected = createConnection(ipAddress, port); // attempt to reopen the connection
				logger.debug("Reconnected to client : " + connected + " Socket status : "+getSocket().isConnected()); //added for debugging , shall be removed.
				if (getSocket().isConnected() && !headersNotSentMap.keySet().isEmpty()) { // send any outstanding unsent headers
					Set<Integer> headerSet = headersNotSentMap.keySet();
					for (Integer header: headerSet) {
						if (write(headersNotSentMap.get(header))) {
							headersNotSentMap.remove(header);
						}
					}
				}
			}
			try {
				Thread.sleep(STATE_TRANSITION_DELAY);
			} catch (InterruptedException e) {

			}
		}
	}
}
