package com.ericsson.streamAdapter.util.config;

import org.junit.Test;

import static org.junit.Assert.*;

public class ConfigTest {

	private static final String DEFAULT_INI_FILE = "StreamAdapter.ini";
	private static final String TEST_INI_FILE = "TestTORClient.ini";
	private static final String DEFAULT_SECTION = "General";
	private static final String TEST_SECTION = "TorStreamCTUM";
	private static final String DEFALUT_VALUE = "false";
	private static final String INPUTIP = "inputIP";
	private static final String IPADDRESS = "10.45.207.208";

	@Test
	public void testDefaultConfig() {
		Config config = new Config();
		assertTrue(
				"Expected INI File: " + DEFAULT_INI_FILE + " Got :"
						+ config.getFilename(),
				DEFAULT_INI_FILE.equals(config.getFilename()));
		assertTrue(
				"Expected Section: " + DEFAULT_SECTION + " Got :"
						+ config.getSection(),
				DEFAULT_SECTION.equals(config.getSection()));
		assertTrue("Expected (Statistic_On): " + DEFALUT_VALUE + " Got :"
				+ config.getValue("Statistic_On", "false"),
				DEFALUT_VALUE.equalsIgnoreCase(config.getValue("Statistic_On",
						"false")));

	}

	@Test
	public void testConfig() {
		Config config = new Config("TestTORClient.ini", "TorStreamCTUM");
		assertTrue(
				"Expected INI File: " + TEST_INI_FILE + " Got :"
						+ config.getFilename(),
				TEST_INI_FILE.equals(config.getFilename()));
		assertTrue(
				"Expected Section: " + TEST_SECTION + " Got :"
						+ config.getSection(),
				TEST_SECTION.equals(config.getSection()));
		assertTrue("Expected (" + INPUTIP + ") " + IPADDRESS + " Got :"
				+ config.getValue(INPUTIP, "0.0.0.0"),
				IPADDRESS.equals(config.getValue(INPUTIP, "0.0.0.0")));

	}

	@Test
	public void testConfig_Expect_Valid() {
		Config config = new Config("TestTORClient.ini", "TorStreamCTUM");
		String ip = config.getValue("inputIP", "dummy_ip");
		String inputPort = config.getValue("inputPort", "dummy port");
		assertEquals("11102", inputPort);
		assertEquals("10.45.207.208", ip);
	}
}
