/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2013
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.streamAdapter.util.config;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ericsson.streamAdapter.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Config {
	private static final Logger logger = LoggerFactory.getLogger(Config.class);
	private static final String DEFAULT_INI_FILE = "StreamAdapter.ini";
	private static final String GENERAL_SECTION = "General";
	private static Map<String, String> propMap = new HashMap<String, String>();
	private List<String> sections = new ArrayList<String>();
	private String fileName;
	private String section;
	private InputStream fis;

	/**
	 * Default Constructor
	 */
	public Config() {
		fis = Config.class.getClassLoader().getResourceAsStream(
				DEFAULT_INI_FILE);
		fileName = DEFAULT_INI_FILE;
		section = GENERAL_SECTION;
		addSection(GENERAL_SECTION);
		populateCache();
	}

	/**
	 * 
	 * @param filename
	 * @param section
	 */
	public Config(String filename, String section) {
		File file = null;
		try {
			file = new File(filename);
			if (file.exists()) {
				fis = new FileInputStream(file);
			} else {
				// File may be part of your resources. i.e part of your jar.
				fis = Config.class.getClassLoader().getResourceAsStream(
						filename);
			}
			this.fileName = filename;
		} catch (FileNotFoundException e) {
			logger.warn(
					"specified config file {} not found, reverting to default",
					file.getAbsoluteFile());
		}
		if (fis == null) {
			fis = Config.class.getClassLoader().getResourceAsStream(
					DEFAULT_INI_FILE);
			this.fileName = DEFAULT_INI_FILE;
		}
		this.section = section;
		addSection(GENERAL_SECTION);
		addSection(section);
		populateCache();
	}

	/**
	 * This methods adds the section to the section list.
	 * 
	 * @param section
	 */
	private void addSection(String section) {
		String cn = "[" + section + "]";
		sections.add(cn);
	}

	/**
	 * Return a value for a given property.
	 * 
	 * @param property
	 * @param defaultVal
	 * @return
	 */
	public String getValue(String property, String defaultVal) {
		String res = defaultVal;
		if (propMap.containsKey(property)) {
			res = propMap.get(property);
		}
		return res;
	}

	/**
	 * Return a value for a given property.
	 * 
	 * @param property
	 * @param defaultVal
	 * @return
	 */
	public Integer getValue(String property, Integer defaultVal) {
		String res = getValue(property, defaultVal.toString());
		return Utils.safeParseInt(res);
	}

	/**
	 * This method populates the cache based on sections found in the specified
	 * ini file.
	 */
	private void populateCache() {
		String value;
		String property;

		String line; // last line we read
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(fis));
			while ((line = br.readLine()) != null) {
				if (line.startsWith("[")) { // section marker
					for (String section : sections) {

						if (line.startsWith(section)) { // is it the section we
														// want
							while ((line = br.readLine()) != null) {
								if (line.startsWith("[")) { // We have started
															// the next section
									break;
								} else {
									int idx = line.indexOf('=');
									if (idx > 0 && idx < line.length()
											&& !line.startsWith("#")) { // something
																		// on
																		// either
																		// side
																		// of
																		// '='
										property = line.substring(0, idx);
										value = line.substring(idx + 1);
										propMap.put(property, value);
									}
								}
							}
						}
					}
				}
			}
		} catch (FileNotFoundException fnf) {
			logger.warn("Specified config file {} not found!", this.fileName);
			fnf.printStackTrace();
		} catch (Exception e) {
			logger.error("Exception thrown accessing config file!", e);
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (Exception e1) {
					; // do nothing
				}
			}
		}
	}

	/**
	 * Return the section used to populate the cache.
	 * 
	 * @return
	 */
	public String getSection() {
		return section;
	}

	/**
	 * The file used to populate the cache.
	 * 
	 * @return
	 */
	public String getFilename() {
		return new File(this.fileName).getName();
	}
}
