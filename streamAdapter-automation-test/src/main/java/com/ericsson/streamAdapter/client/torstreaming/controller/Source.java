package com.ericsson.streamAdapter.client.torstreaming.controller;

import com.ericsson.streamAdapter.server.logger.AutomationLogger;
import com.ericsson.streamAdapter.server.utils.EventDetails;
import com.ericsson.streamAdapter.server.utils.IPRetriever;
import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.Future;

public abstract class Source implements Runnable{
	Logger logger = AutomationLogger.getLogger();
	private static Map<String,List<EventDetails>> eventDetailsMap = Collections.synchronizedMap(new HashMap<String,List<EventDetails>>());
	private byte[] ipBytes;
	private String ipAsString;
	private byte[] sourceBytes;
	protected int sourceId;
	protected int noOfConnects;
	protected int noOfDisConnects;
	protected int noOfEvents;
	protected boolean customEvent;
	private int noOfEventsToGenerate;
	private Future<?> task;
	Channel channel;
	File outputFile;
	FileOutputStream fo;
	BufferedOutputStream bo;
	protected boolean connectSent = false;
	public abstract void createEventMessage();
	public byte[] getIpBytes() {
		return ipBytes;
	}
	public void setIpBytes(byte[] ip) {
		this.ipBytes = ip;
	}
	public byte[] getSourceBytes() {
		return sourceBytes;
	}
	public void setSourceBytes(byte[] sourceId) {
		this.sourceBytes = sourceId;
	}
	public int getNoOfEventsToGenerate() {
		return noOfEventsToGenerate;
	}
	public void setNoOfEventsToGenerate(int noOfEventsToGenerate) {
		this.noOfEventsToGenerate = noOfEventsToGenerate;
	}
	public Future<?> getTask() {
		return task;
	}
	public void setTask(Future<?> task) {
		this.task = task;
	}
	public void setChannel(Channel channel) {
		this.channel = channel;
	}
	public synchronized void writeBuffer(byte[] message){
		ChannelBuffer buffer = ChannelBuffers.wrappedBuffer(message);
		if(channel.isConnected()){
			channel.write(buffer);
		}
		else{
			System.out.println("Channel Not connected to send event ...");
			logger.error("Channel Not connected to send event ...");
		}
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			logger.error("Thread Interupted : " + e.getMessage());
		}
		updateEventsDetails(buffer);
	}
    private void updateEventsDetails(ChannelBuffer buffer)
    {
        if((buffer.getByte(0)==0x00))
        {
            EventDetails event = EventDetails.createSimulatorEventDetails(buffer);
            processEvent(event);
        }
        else if((buffer.getByte(0)==0x02))
        {
            EventDetails event = EventDetails.createSimulatorConnectDetails(buffer);
            processEvent(event);

        }
    }
    private void processEvent(EventDetails event) {
        event.setIpAddress(ipBytes);
        try {
            setIpAsString(IPRetriever.getIP(ipBytes));
        } catch (UnknownHostException e) {
            logger.error("Problem Reading IP : " + e.getMessage());
        }
        event.setIpAddressAsString(getIpAsString());
        if(getEventDetailsMap().get(getIpAsString())==null)
        {
            List<EventDetails> eventsDetailsList = new ArrayList<EventDetails>();
            eventsDetailsList.add(event);
            getEventDetailsMap().put(getIpAsString(), eventsDetailsList);
        }
        else{
            List<EventDetails> eventsDetailsList = getEventDetailsMap().get(getIpAsString());
            eventsDetailsList.add(event);
        }
        event.setSourceId(sourceId);
        logger.debug(Source.class.getSimpleName() +
                ": Event sent for source : " + getIpAsString() +
                " Total number of events sent : " + getEventDetailsMap().get(getIpAsString()).size() +
                " Event details " + event);
    }

	public void setCustomEvent(boolean customEvent) {
		this.customEvent = customEvent;
	}
	public static Map<String,List<EventDetails>> getEventDetailsMap() {
		return eventDetailsMap;
	}
	public static void setEventDetailsMap(Map<String,List<EventDetails>> eventDetailsMap) {
		Source.eventDetailsMap = eventDetailsMap;
	}
	public String getIpAsString() {
		return ipAsString;
	}
	public void setIpAsString(String ipAsString) {
		this.ipAsString = ipAsString;
	}
}
