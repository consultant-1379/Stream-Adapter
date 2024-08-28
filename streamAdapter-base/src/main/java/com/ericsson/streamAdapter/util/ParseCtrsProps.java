package com.ericsson.streamAdapter.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.streamAdapter.util.config.*;

/* This is not a public class!
 *  It is used by utils to populate statics that can then be referenced on demand. 
 *  
 *  The class itself parses the Ctrs.prop file used by MZ to extract the PORT numbers TOR client will need to connect to.
 */
final class ParseCtrsProps {

	private static final Logger logger = LoggerFactory
			.getLogger(ParseCtrsProps.class);
	private String[] portsArray;
	private int portCounts;
	private String ipAddress;

	public ParseCtrsProps(boolean isCTUM, String file) {
		Config config =new Config();
		String service = config.getValue(StreamAdapterConstants.EC, StreamAdapterDefaults.EC);
		String ctumPorts = null;
		String prop;
		if (isCTUM) {
			prop = "ctum_ports_tor";
		} else {
			prop = "ctr_ports_tor";
		}
		prop=prop.concat("_").concat(service).toLowerCase();
		ctumPorts = readConfigProp(prop, file);
		if (ctumPorts != null) {
			ctumPorts = ctumPorts.substring(ctumPorts.indexOf("=") + 1);
			portsArray = ctumPorts.split(",");
			portCounts = portsArray.length;

		}

	}

	public int getPortsCount() {
		return portCounts;
	}

	public String getPort(int index) {
		if (portCounts > index) {
			return portsArray[index];
		} else {
			return null;
		}
	}

	private String readConfigProp(String prop, String file) {
		BufferedReader br = null;
		String val = null;

		try {

			String sCurrentLine;

			br = new BufferedReader(new FileReader(file));
			while ((sCurrentLine = br.readLine()) != null) {
				if (sCurrentLine.startsWith(prop) == true) {
					val = sCurrentLine;
					break;
				}

			}

		} catch (IOException e) {
			logger.error("Unable to read ctrs.prop file", e.getMessage());
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (IOException e) {
				logger.error("Unable to close buffer", e.getMessage());
			}
		}
		return val;
	}

}
