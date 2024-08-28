package com.ericsson.streamAdapter.server.logger;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class AutomationLogger {
	static Logger  logger;
	static {
        PropertyConfigurator.configure("log4j.properties");
        logger = Logger.getLogger("Automation");
	}
	public static Logger getLogger()
	{
		return logger;
	}
}
