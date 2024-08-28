package com.ericsson.streamAdapter.util;

import org.slf4j.Logger;

/* 
 * Useful utilities 
 */
public class Utils {

	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

	public static String bytesToHex(byte[] bytes) {
		return bytesToHex(bytes, 0, bytes.length);
	}
	public static String bytesToHex(byte[] bytes, int offset, int length) {
		int len = (length > 8 || bytes.length > 8) ? 8 : length; 
	    char[] hexChars = new char[len * 2];
	    int v;
	    if (len > bytes.length + offset) {
	    	len = bytes.length - offset;
	    }
	    for ( int j = 0; j < len; j++ ) {
	        v = bytes[offset+j] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    return new String(hexChars);
	}

	public static Integer safeParseInt(String s, Logger logger) {
		if (s == null || s.isEmpty()) {
			return 0;
		}
		try {
			return Integer.parseInt(s);
		} catch (Exception e) {
			logger.debug("Unparseable int: " + s, e);
			return 0;
		}
	}

	public static Integer safeParseInt(String s) {
		if (s == null || s.isEmpty()) {
			return 0;
		}
		try {
			return Integer.parseInt(s);
		} catch (Exception e) {
			return 0;
		}
	}

	public static String getIP(byte[] ipAddress) {
		StringBuilder ipbuf = new StringBuilder();
		boolean isIPv6 = false;
		int i, j;
		for (i = 0; i < 12; i++) { // strip leading zeros from IPv4
			if (ipAddress[i] != 0) {
				break;
			}
		}
		if (i == 0) { // this is an IPv6 address
			isIPv6 = true;
		}
		for (j = 15; j > 4; j--) { // strip trailing zeros from IPv6
			if (ipAddress[j] != 0) {
				break;
			}
		}
		for (; i <= j; i++) {
			if (isIPv6) {
				if (ipAddress[i] != 0) {
					ipbuf.append((int) (ipAddress[i] & 0x00ff)); // 0-255
				}
				if (i < j)
					ipbuf.append(":"); // Warning - you can't use a colon in a
										// windows file name
			} else {
				ipbuf.append((int) (ipAddress[i] & 0x00ff)); // 0-255
				if (i < j)
					ipbuf.append(".");
			}
		}
		return ipbuf.toString();
	}

	private static String[] portsArray;
	private static int portCounts = -1;

	public static int getMZPortsCount(boolean isCTUM, String file) {
		if (getPortCounts() == -1) { // has not been initialised
			ParseCtrsProps pc = new ParseCtrsProps(isCTUM, file);
			setPortCounts(pc.getPortsCount());

			setPortsArray(new String[getPortCounts()]);
			for (int i = 0; i < getPortCounts(); i++) {
				getPortsArray()[i] = pc.getPort(i);
			}
		}
		return getPortCounts();
	}

	public static String getMZPort(int index) {
		if (getPortCounts() > index) {
			return getPortsArray()[index];
		} else {
			return null;
		}
	}

	public static Boolean isBitSet(byte b, int bit)
	{
	    return (b & (1 << bit)) != 0;
	}
	public static String[] getPortsArray() {
		return portsArray;
	}
	public static void setPortsArray(String[] portsArray) {
		Utils.portsArray = portsArray;
	}
	public static int getPortCounts() {
		return portCounts;
	}
	public static void setPortCounts(int portCounts) {
		Utils.portCounts = portCounts;
	}

}
