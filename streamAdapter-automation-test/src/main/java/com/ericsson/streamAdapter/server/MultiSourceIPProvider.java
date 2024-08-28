package com.ericsson.streamAdapter.server;

public class MultiSourceIPProvider {
	public MultiSourceIPProvider()
	{
		
	}
	public static byte[] generateFourBytesIP(int sourceId)
	{	
		byte [] byteBuffer = new byte[16];
		//Added for testing IPv6
		byteBuffer[0] = 0;
		byteBuffer[1] = 0;
		byteBuffer[2] = 0;
		byteBuffer[3] = 0;
		byteBuffer[4] = 0;
		byteBuffer[5] = 0;
		byteBuffer[6] = 0;
		byteBuffer[7] = 0;

		byteBuffer[8] = 0;
		byteBuffer[9] = 0;
		byteBuffer[10] = 0;
		byteBuffer[11] = 0;
		byteBuffer[12] = (byte)((sourceId & 0xff000000) >> 24);
		byteBuffer[13] = (byte)((sourceId & 0x00ff0000) >> 16);
		byteBuffer[14] = (byte) ((sourceId & 0x0000ff00) >> 8);
		byteBuffer[15] = (byte) ((sourceId & 0x000000ff));
		return byteBuffer;
	}

	public static byte[] generateThreeBytesSource(int sourceId) {
		byte [] byteBuffer = new byte[3];
		byteBuffer[0] = (byte)((sourceId & 0x00ff0000) >> 16);
		byteBuffer[1] = (byte) ((sourceId & 0x0000ff00) >> 8);
		byteBuffer[2] = (byte) ((sourceId & 0x000000ff));
		return byteBuffer;
	}
}
