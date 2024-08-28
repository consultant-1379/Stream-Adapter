package com.ericsson.streamAdapter.util;

import static org.junit.Assert.*;
import org.junit.Test;

public class UtilsTest {

	@Test
	public void testBytesToHex() {
		byte[] b = { 127, 23, 2, 10 };
		String convertedBytes = Utils.bytesToHex(b);
		assertEquals("7F17020A", convertedBytes);
	}

	@Test
	public void testByteToIPConversion() {
		byte[] b = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, (byte) 255,
				(byte) 255, (byte) 255, 1 };
		String convertedIP = Utils.getIP(b);
		assertEquals("255.255.255.1", convertedIP);
	}

	@Test
	public void testMZPortCountCTUM() {
		Utils.setPortCounts(-1);
		String workingDirectory = System.getProperty("user.dir");
		// set isCTUM value to true to get number of ctum ports (ctum_ports_tor)
		Utils.getMZPortsCount(true, workingDirectory
				+ "/src/test/resources/TESTctrs.prop");
		assertEquals(1, Utils.getPortCounts());
	}

	@Test
	public void testMZPortCountCTRS() {
		Utils.setPortCounts(-1);
		String workingDirectory = System.getProperty("user.dir");
		// set isCTUM value to false to get number of ctrs ports (ctr_ports_tor)
		Utils.getMZPortsCount(false, workingDirectory
				+ "/src/test/resources/TESTctrs.prop");
		assertEquals(10, Utils.getPortCounts());
	}

	@Test
	public void testMZPortCTUM() {// ctum_ports_tor=4010,4011
		Utils.setPortCounts(-1);
		String workingDirectory = System.getProperty("user.dir");
		Utils.getMZPortsCount(true, workingDirectory
				+ "/src/test/resources/TESTctrs.prop");
		int portNumber = 5010;
		portNumber = comparePortNumber(portNumber);
	}

	@Test
	public void testMZPortCTRS() {// ctr_ports_tor=4000,4001,4002,4003,4004,4005,4006,4007,4008,4009
		Utils.setPortCounts(-1);
		String workingDirectory = System.getProperty("user.dir");
		Utils.getMZPortsCount(false, workingDirectory
				+ "/src/test/resources/TESTctrs.prop");
		int portNumber = 4000;
		portNumber = comparePortNumber(portNumber);
	}

	private int comparePortNumber(int portNumber) {
		for (int i = 0; i < Utils.getPortsArray().length; i++) {
			String valToCompare = String.valueOf(portNumber);
			assertEquals(valToCompare, Utils.getPortsArray()[i]);
			portNumber++;
		}
		return portNumber;
	}

	@Test
	public void testSafeParseInt() {
		int parseInt = Utils.safeParseInt("10");
		assertEquals(10, parseInt);
	}

	@Test
	public void testSafeParseIntNull() {
		String s = null;
		int parseInt = Utils.safeParseInt(s);
		assertEquals(0, parseInt);
	}

	@Test
	public void testSafeParseIntExpectZero() {
		int parseInt = Utils.safeParseInt("Zero");
		assertEquals(0, parseInt);

	}

}
