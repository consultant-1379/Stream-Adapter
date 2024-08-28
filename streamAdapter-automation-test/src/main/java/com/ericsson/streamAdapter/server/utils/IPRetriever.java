package com.ericsson.streamAdapter.server.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class IPRetriever {
	public static String getIP(byte[] ipAddress) throws UnknownHostException {
		InetAddress a = InetAddress.getByAddress(ipAddress);
		return a.toString().replace("/", "");
	}
}
