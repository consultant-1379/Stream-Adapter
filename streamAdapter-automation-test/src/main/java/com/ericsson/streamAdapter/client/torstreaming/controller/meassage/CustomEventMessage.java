package com.ericsson.streamAdapter.client.torstreaming.controller.meassage;

import com.ericsson.streamAdapter.client.torstreaming.controller.Source;

public class CustomEventMessage implements Message{

	@Override
	public byte[] createMessage(Source source) {
        // ************************
        // Event Message
        // ************************

        byte[] eventMessage = new byte[14];

        //byte 1 : message type, 0x00 for event message
        eventMessage[0] = 0x00;

        //byte 2-4 : source id
        eventMessage[1] = source.getSourceBytes()[0];
        eventMessage[2] = source.getSourceBytes()[1];
        eventMessage[3] = source.getSourceBytes()[2];

        // length of message excluding header but including these 2 bytes (0x0A = 10 bytes for eventMessage[4] - eventMessage[13]
        eventMessage[4] = 0x00;
        eventMessage[5] = 0x0A;

        // event payload
        eventMessage[6] = 0x01;
        eventMessage[7] = 0x02;
        eventMessage[8] = 0x03;
        eventMessage[9] = 0x04;
        eventMessage[10] = 0x05;
        eventMessage[11] = 0x06;
        eventMessage[12] = 0x07;
        eventMessage[13] = 0x08;
        return eventMessage;
	}

	public byte[] createMessage(Source source, Byte[] customMessage) {
		
	       // ************************
        // Event Message
        // ************************

        byte[] eventMessage = new byte[6+customMessage.length];

        //byte 1 : message type, 0x00 for event message
        eventMessage[0] = 0x00;

        //byte 2-4 : source id
        eventMessage[1] = source.getSourceBytes()[0];
        eventMessage[2] = source.getSourceBytes()[1];
        eventMessage[3] = source.getSourceBytes()[2];

        // length of message excluding header but including these 2 bytes;
        int length = customMessage.length + 2;
        eventMessage[4] = (byte)((length & 0x0000ff00) >> 8);
        eventMessage[5] = (byte)((length & 0x000000ff));

        // event payload
        for(int i = 0 ; i<customMessage.length ; i++)
        {
        	eventMessage[6+i] = customMessage[i];
        }
        return eventMessage;
	}

}
