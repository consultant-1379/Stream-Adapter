package com.ericsson.streamAdapter.util;

public class TestConstants {

	public static final byte EVENT_MESSAGE = 0x00;
	public static final byte INITIATION_MESSAGE = 0x01;
	public static final byte CONNECTION_MESSAGE = 0x02;
	public static final byte DISCONNECTION_MESSAGE = 0x03;
	public static final byte DROPPED_EVENTS_MESSAGE = 0x04;
	public static final String DUMMY_DATASET_PATH = "dummy_path";
	public static final byte[] DUMMY_IP_ADDRESS = { 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x0A, 0x2D, 0x5A,
			0x01 };
	public static final int DUMMY_SRC_ID = 123;
	public static final int DUMMY_DISCONNECT_REASON = 456;
	public static final String DUMMY_IP_STRING = "DUMMY IP";
	public static final String DUMMY_THREAD_ID = "DUMMY_THREAD_ID";
	public static final int DUMMY_PORT = 123;

}
