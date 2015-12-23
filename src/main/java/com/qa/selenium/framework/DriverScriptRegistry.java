package com.qa.selenium.framework;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

/**
 * Registry for retrieving the driver script based on the a unique identifier.
 * 
 * @author Matthew DeTullio
 */
public class DriverScriptRegistry {

	private static Map<String, MainScript> registry = new HashMap<String, MainScript>();

	/**
	 * Retrieves a driver script from the registry based on a unique identifier.
	 * If not present, the driver script is created using the given scenario.
	 * 
	 * @param uid
	 *            the unique identifier
	 * @param scenario
	 *            used to create the new driver script if not present
	 * @return the driver script for the given uid
	 * @throws IOException
	 *             from {@link MainScript#DriverScript(String)}
	 * @throws SAXException
	 *             from {@link MainScript#DriverScript(String)}
	 * @throws ParserConfigurationException
	 *             from {@link MainScript#DriverScript(String)}
	 */
	public static synchronized MainScript getDriverScript(String uid,
			String scenario) throws IOException, SAXException,
			ParserConfigurationException {
		if (!registry.containsKey(uid)) {
			registry.put(uid, new MainScript(scenario));
		}
		return registry.get(uid);
	}
}