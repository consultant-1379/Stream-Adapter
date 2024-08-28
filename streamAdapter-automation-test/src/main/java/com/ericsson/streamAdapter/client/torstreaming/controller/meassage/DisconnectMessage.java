package com.ericsson.streamAdapter.client.torstreaming.controller.meassage;

import com.ericsson.streamAdapter.client.torstreaming.controller.Source;

public class DisconnectMessage implements Message {

	public byte[] createMessage(Source source) {
        // ************************
        // Disconnection Message
        // ************************

        byte[] disconnectionMessage = new byte[16];

        // message type
        disconnectionMessage[0] = 0x03;

        // source_id
        disconnectionMessage[1] = source.getSourceBytes()[0];
        disconnectionMessage[2] = source.getSourceBytes()[1];
        disconnectionMessage[3] = source.getSourceBytes()[2];

        // length (excluding header but including this length part)
        disconnectionMessage[4] = 0x00;
        disconnectionMessage[5] = 0x0C;

        // timestamp
        disconnectionMessage[6] = 0x00;
        disconnectionMessage[7] = 0x00;
        disconnectionMessage[8] = 0x01;
        disconnectionMessage[9] = 0x3C;
        disconnectionMessage[10] = 0x0A;
        disconnectionMessage[11] = 0x1C;
        disconnectionMessage[12] = 0x32;
        disconnectionMessage[13] = 0x08;

        // Reason
        disconnectionMessage[14] = 0x00;
        disconnectionMessage[15] = 0x03;
        return disconnectionMessage;
    }

}
