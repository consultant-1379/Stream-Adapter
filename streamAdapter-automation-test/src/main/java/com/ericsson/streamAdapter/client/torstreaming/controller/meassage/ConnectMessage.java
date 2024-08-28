package com.ericsson.streamAdapter.client.torstreaming.controller.meassage;

import com.ericsson.streamAdapter.client.torstreaming.controller.Source;

public class ConnectMessage implements Message {

	public byte[] createMessage(Source source) {
        byte[] connectionMessage = new byte[39];

        //byte 1 : message type, 0x02 for connection message
        connectionMessage[0] = 0x02;

        //byte 2-4 : source id
        connectionMessage[1] = source.getSourceBytes()[0];
        connectionMessage[2] = source.getSourceBytes()[1];
        connectionMessage[3] = source.getSourceBytes()[2];

        //byte 5-6 : length (2 + 8 + 16 + 9 = 35bytes long....35 in decimal is 0x23 in hex)
        connectionMessage[4] = 0x00;
        connectionMessage[5] = 0x23;

        //byte 7-14 timestamp
        connectionMessage[6] = 0x01;
        connectionMessage[7] = 0x02;
        connectionMessage[8] = 0x03;
        connectionMessage[9] = 0x04;
        connectionMessage[10] = 0x05;
        connectionMessage[11] = 0x05;
        connectionMessage[12] = 0x07;
        connectionMessage[13] = 0x08;

        //byte 7-14 ip address (10.45.90.1)
        connectionMessage[14] = 0x00;
        connectionMessage[15] = 0x00;
        connectionMessage[16] = 0x00;
        connectionMessage[17] = 0x00;
        connectionMessage[18] = 0x00;
        connectionMessage[19] = 0x00;
        connectionMessage[20] = 0x00;
        connectionMessage[21] = 0x00;

        connectionMessage[22] = 0x00;
        connectionMessage[23] = 0x00;
        connectionMessage[24] = 0x00;
        connectionMessage[25] = 0x00;
        connectionMessage[26] = source.getIpBytes()[12];
        connectionMessage[27] = source.getIpBytes()[13];
        connectionMessage[28] = source.getIpBytes()[14];
        connectionMessage[29] = source.getIpBytes()[15];

        //variable length = var length from initialization message
        connectionMessage[30] = 0x05;
        connectionMessage[31] = 0x00;
        //connectionMessage[31] = 0x01;

        //header event

        connectionMessage[32] = 0x08;
        connectionMessage[33] = 0x00;
        connectionMessage[34] = 0x01;
        connectionMessage[35] = 0x02;
        connectionMessage[36] = 0x03;
        connectionMessage[37] = 0x04;
        connectionMessage[38] = 0x05;
        return connectionMessage;
    }

}
