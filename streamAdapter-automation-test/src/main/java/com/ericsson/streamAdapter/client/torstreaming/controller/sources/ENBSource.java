package com.ericsson.streamAdapter.client.torstreaming.controller.sources;

import java.util.Map;

import com.ericsson.streamAdapter.client.torstreaming.controller.Source;
import com.ericsson.streamAdapter.client.torstreaming.controller.meassage.ConnectMessage;
import com.ericsson.streamAdapter.client.torstreaming.controller.meassage.CustomEventMessage;
import com.ericsson.streamAdapter.client.torstreaming.controller.meassage.DisconnectMessage;
import com.ericsson.streamAdapter.client.torstreaming.controller.meassage.EventMessage;
import com.ericsson.streamAdapter.client.torstreaming.controller.meassage.InitilizationMessage;
import com.ericsson.streamAdapter.client.torstreaming.controller.meassage.Message;
import com.ericsson.streamAdapter.server.MultiSourceIPProvider;
import com.ericsson.streamAdapter.server.utils.EventGenrator;

public class ENBSource extends Source{
	public ENBSource(int sourceId)
	{
		this.sourceId = sourceId;
		this.setSourceBytes(MultiSourceIPProvider.generateThreeBytesSource(sourceId));
		this.setIpBytes(MultiSourceIPProvider.generateFourBytesIP(sourceId));
	}

	public void run() {
			createEventMessage();	
	}
	
	@Override
	public void createEventMessage() {
		Map<Integer, Byte[]> customeventMessage = null;
		Message message = new InitilizationMessage();
		writeBuffer(message.createMessage(this));
		message = new ConnectMessage();
		writeBuffer(message.createMessage(this));
		noOfConnects++;
		message = new EventMessage();
		byte[] messageBytes = message.createMessage(this);
		if(customEvent)
		{
			message = new CustomEventMessage();
			EventGenrator customEvents = EventGenrator.getInstance();
			customeventMessage = customEvents.getEventMessage();
		}
		for(int i = 0 ; i < getNoOfEventsToGenerate() ; i++)
		{
			if(!customEvent){
				writeBuffer(messageBytes);
			}
			else{
				 messageBytes = ((CustomEventMessage)message).createMessage(this,customeventMessage.get(i%customeventMessage.size()));
				 writeBuffer(messageBytes);
			}
				
			noOfEvents ++;
		}
		message = new DisconnectMessage();
		writeBuffer(message.createMessage(this));
		noOfDisConnects++;
	}
}

