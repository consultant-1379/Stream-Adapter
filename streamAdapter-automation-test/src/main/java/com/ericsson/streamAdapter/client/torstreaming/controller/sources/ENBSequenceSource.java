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
package com.ericsson.streamAdapter.client.torstreaming.controller.sources;

import java.util.Map;

import com.ericsson.streamAdapter.client.torstreaming.controller.meassage.*;
import com.ericsson.streamAdapter.server.utils.EventGenrator;

public class ENBSequenceSource extends ENBSource {

	/**
	 * @param sourceId
	 */
	public ENBSequenceSource(int sourceId) {
		super(sourceId);
		// TODO Auto-generated constructor stub
	}
	@Override
	public void createEventMessage() {
		for(int i = 0 ; i < getNoOfEventsToGenerate() ; i++)
		{
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
			if(!customEvent){
				writeBuffer(messageBytes);
			}
			else{
				 messageBytes = ((CustomEventMessage)message).createMessage(this,customeventMessage.get(i%customeventMessage.size()));
				 writeBuffer(messageBytes);
			}	
			noOfEvents ++;
			message = new DisconnectMessage();
			writeBuffer(message.createMessage(this));
			noOfDisConnects++;
		}
	}
}
