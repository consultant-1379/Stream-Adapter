package com.ericsson.streamAdapter.client.torstreaming.controller.meassage;

import com.ericsson.streamAdapter.client.torstreaming.controller.Source;

public class InitilizationMessage implements Message {

	public byte[] createMessage(Source source) {
        byte[] initializationMessage = new byte[16];

        //byte 1 : message type, 0x01 for initialization message
        initializationMessage[0] = 0x01;

        //byte 2-4 : source id
        initializationMessage[1] = source.getSourceBytes()[0];
        initializationMessage[2] = source.getSourceBytes()[1];
        initializationMessage[3] = source.getSourceBytes()[2];

        //byte 5-6 : length
        initializationMessage[4] = 0x00;
        initializationMessage[5] = 0x0c; //12 bytes (2 bytes for length + 8 bytes for protocol + 2 bytes for reserved for future use)

        //byte 7-14 protocol version
        initializationMessage[6] = 0x00;
        initializationMessage[7] = 0x00;
        initializationMessage[8] = 0x00;
        initializationMessage[9] = 0x00;
        initializationMessage[10] = 0x00;
        initializationMessage[11] = 0x00;
        initializationMessage[12] = 0x00;
        initializationMessage[13] = 0x00;

        //byte 15-16 reserved for future use
        initializationMessage[14] = 0x00;
        initializationMessage[15] = 0x01;
        return initializationMessage;
    }

}
