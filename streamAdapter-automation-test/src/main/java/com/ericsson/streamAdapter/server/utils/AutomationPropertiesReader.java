package com.ericsson.streamAdapter.server.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.ericsson.streamAdapter.server.logger.AutomationLogger;

public class AutomationPropertiesReader {
	static Properties prop;
	public static AutomationPropertiesReader instance;
	static Logger logger = AutomationLogger.getLogger();
	
	public static void loadProperties()
	{
		prop = new Properties();
		try {
			prop.load(new FileInputStream("Automation.properties"));
		    for (String name : prop.stringPropertyNames()) {
		        String value = prop.getProperty(name);
		        System.setProperty(name, value);
		    }
		} catch (IOException ex) {
			logger.error(AutomationPropertiesReader.class.getSimpleName()+" : Error Reading Properties File : "+ex.getMessage());
		}
	}	
}
