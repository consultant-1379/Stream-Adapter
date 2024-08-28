package com.ericsson.streamAdapter.client.torstreaming.controller.sources;

import com.ericsson.streamAdapter.client.torstreaming.controller.Source;
import com.ericsson.streamAdapter.client.torstreaming.controller.meassage.EventMessage;
import com.ericsson.streamAdapter.client.torstreaming.controller.meassage.Message;
import com.ericsson.streamAdapter.server.MultiSourceIPProvider;

public class ENBFaultSource extends Source {
	public ENBFaultSource(int sourceId)
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
		Message message = new EventMessage();
		byte[] messageBytes = message.createMessage(this);
		for(int i = 0 ; i < getNoOfEventsToGenerate() ; i++)
		{
			writeBuffer(messageBytes);
			noOfEvents ++;
		}
	}	
}
