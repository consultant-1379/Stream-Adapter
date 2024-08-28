package com.ericsson.streamAdapter.server.utils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class EventGenrator {
	public static EventGenrator instance;
	private Map<Integer, Byte[]> eventMessage = new HashMap<Integer, Byte[]>();
	private int eventsCount;
	
	private EventGenrator(){
	}

	public static EventGenrator getInstance() {
		if(instance == null)
		{
			instance = new EventGenrator();
			instance.loadEventsFile();
		}
		return instance;
	}
	
	private void loadEventsFile() {
		try {
			File fXmlFile = new File("Events.xml");
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder;
			dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fXmlFile);
			NodeList nList = doc.getElementsByTagName("EventId");
			setEventsCount(nList.getLength());
			for (int temp = 0; temp < getEventsCount() ; temp++) {
				Node nNode = nList.item(temp);
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					Element eElement = (Element) nNode;
					getEventMessage().put(temp,
							getByteArray(eElement.getElementsByTagName("Event_Message").item(0).getTextContent()));				
				}
			}
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	private Byte[] getByteArray(String textContent) {
		String[] messageArray = textContent.split(",");
		Byte[] messageBytes = new Byte[messageArray.length];
		for(int i = 0 ; i < messageArray.length ; i++)
		{
			String message = messageArray[i].substring(messageArray[i].indexOf("x")+1,messageArray[i].length());
			messageBytes[i] = (byte) (Integer.parseInt(message,16) & 0xff);
		}
		return messageBytes;
	}
	public Map<Integer, Byte[]> getEventMessage() {
		return eventMessage;
	}

	public int getEventsCount() {
		return eventsCount;
	}
	public void setEventsCount(int eventsCount) {
		this.eventsCount = eventsCount;
	}
	
	public void setEventMessage(Map<Integer, Byte[]> eventMessage) {
		this.eventMessage = eventMessage;
	}
	
	public static void main(String[] args) {
		System.out.println(EventGenrator.getInstance().getEventMessage());
	}
}
