package com.ericsson.streamAdapter.server.utils;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class EventDetails {
	//In the protocol order.
	private short recordLength;
	private byte recordType;
	private byte[] ipAddress;
	private String ipAddressAsString;
	private ChannelBuffer data;
	private int sourceId;
	
	public String getIpAddressAsString() {
		return ipAddressAsString;
	}

	public void setIpAddressAsString(String ipAddressAsString) {
		this.ipAddressAsString = ipAddressAsString;
	}

	public byte[] getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(byte[] ipAddress) {
		this.ipAddress = ipAddress;
	}

	public short getRecordType() {
		return recordType;
	}

	public void setRecordType(byte recordType) {
		this.recordType = recordType;
	}

	public short getRecordLength() {
		return recordLength;
	}

	public void setRecordLength(short recordLength) {
		this.recordLength = recordLength;
	}

	public ChannelBuffer getData() {
		return data;
	}

	public void setData(ChannelBuffer channelBuffer) {
		this.data = channelBuffer;
	}
	
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(System.getProperty("line.separator"));
		sb.append("--------------------------------------------");
		sb.append(System.getProperty("line.separator"));
		sb.append("Source Id \t\t:\t"+sourceId);
		sb.append(System.getProperty("line.separator"));
		sb.append("Record Lenght\t\t:\t"+recordLength);
		sb.append(System.getProperty("line.separator"));
		sb.append("Record Type\t\t:\t"+recordType);
		sb.append(System.getProperty("line.separator"));
		sb.append("IP Address String\t:\t"+ipAddressAsString);
		sb.append(System.getProperty("line.separator"));
		sb.append("IP Address byte[]\t:\t"+bytesToList(ipAddress));
		sb.append(System.getProperty("line.separator"));
		sb.append("Data byte[]\t\t:\t"+channelBufferToList(data));
		sb.append(System.getProperty("line.separator"));
		sb.append("--------------------------------------------");
		return sb.toString();
	}
	
	private static List<Byte> bytesToList(byte[] values)
	{
		List<Byte> list = new ArrayList<Byte>();
		for(int i = 0 ; i < values.length ; i++)
		{
			list.add(values[i]);
		}
		return list;
	}
	
	private List<Byte> channelBufferToList(ChannelBuffer channelBuffer)
	{
		int bytes = channelBuffer.readableBytes();
		List<Byte> list = new ArrayList<Byte>();
		for(int i = 0 ; i < bytes ; i++)
		{
			list.add(channelBuffer.getByte(i));
		}
		return list;
	}
	
	public static EventDetails createEventDetails(ChannelBuffer eventBuffer) {
		byte[] ip = new byte[16];
		eventBuffer.getBytes(3, ip, 0, 16);
		String ipAddress=null;
		try {
			 ipAddress = IPRetriever.getIP(ip);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		EventDetails event = new EventDetails();
		event.setIpAddressAsString(ipAddress);
		event.setIpAddress(ip);
		event.setRecordLength(eventBuffer.readShort());
		event.setRecordType(eventBuffer.readByte());
		eventBuffer.skipBytes(16);
		int dataSize = eventBuffer.readableBytes();
		event.setData(eventBuffer.readBytes(dataSize));
		return event;
	}
	
	public static EventDetails createSimulatorEventDetails(ChannelBuffer eventBuffer) {
		EventDetails event = new EventDetails();
		event.setRecordLength(eventBuffer.getShort(4));
		event.setRecordType(eventBuffer.getByte(6));
		eventBuffer.skipBytes(4);
		byte[] data = new byte[eventBuffer.readableBytes()];
		eventBuffer.readBytes(data);
		event.setData(ChannelBuffers.wrappedBuffer(data));
		return event;
	}
	
	public static EventDetails createSimulatorConnectDetails(ChannelBuffer eventBuffer) {
		EventDetails event = new EventDetails();
		eventBuffer.skipBytes(31);
		byte[] data = new byte[eventBuffer.readableBytes()];
		eventBuffer.readBytes(data);
		event.setData(ChannelBuffers.wrappedBuffer(data));
		return event;
	}

	public void setSourceId(int sourceId) {
		this.sourceId = sourceId;
	}
	public int getSourceId()
	{
		return sourceId;
	}
}
