package com.ericsson.streamAdapter.server.utils;

public class Constants {
	static
	{
		AutomationPropertiesReader.loadProperties();
	}
	public static int EMULATOR_PORT_TO_WRITE = Integer.parseInt(System.getProperty("EMULATOR_PORT_TO_WRITE"));
	public static int MZ_PORT_TO_LISTEN = Integer.parseInt(System.getProperty("MZ_PORT_TO_LISTEN"));
	public static int NUMBER_OF_WORKING_NODES = Integer.parseInt(System.getProperty("NUMBER_OF_WORKING_NODES"));
	public static int NUMBER_OF_WORKING_NODES_SEQUENCE = Integer.parseInt(System.getProperty("NUMBER_OF_WORKING_NODES_SEQUENCE"));
	public static int NUMBER_OF_FAULT_NODES = Integer.parseInt(System.getProperty("NUMBER_OF_FAULT_NODES"));
	public static int NUMBER_OF_EVENTS_PER_NODE = Integer.parseInt(System.getProperty("NUMBER_OF_EVENTS_PER_NODE"));
	public static boolean CUSTOM_EVENT = Boolean.valueOf(System.getProperty("CUSTOM_EVENT"));
	
	public static void reLoad()
	{
		EMULATOR_PORT_TO_WRITE = Integer.parseInt(System.getProperty("EMULATOR_PORT_TO_WRITE"));
		MZ_PORT_TO_LISTEN = Integer.parseInt(System.getProperty("MZ_PORT_TO_LISTEN"));
		NUMBER_OF_WORKING_NODES = Integer.parseInt(System.getProperty("NUMBER_OF_WORKING_NODES"));
		NUMBER_OF_WORKING_NODES_SEQUENCE = Integer.parseInt(System.getProperty("NUMBER_OF_WORKING_NODES_SEQUENCE"));
		NUMBER_OF_FAULT_NODES = Integer.parseInt(System.getProperty("NUMBER_OF_FAULT_NODES"));
		NUMBER_OF_EVENTS_PER_NODE = Integer.parseInt(System.getProperty("NUMBER_OF_EVENTS_PER_NODE"));
		CUSTOM_EVENT = Boolean.valueOf(System.getProperty("CUSTOM_EVENT"));
	}
}
