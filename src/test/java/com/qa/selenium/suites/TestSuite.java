package com.qa.selenium.suites;

import java.io.IOException;
import java.text.MessageFormat;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.xml.sax.SAXException;

import com.qa.selenium.Parallelized;

import com.qa.selenium.framework.DriverScriptRegistry;


public class TestSuite extends Parallelized { 
	
	public TestSuite(String browserName, String version, String platform,
			String os) throws IOException, SAXException,
			ParserConfigurationException {
		super(browserName, version, platform, os);
		driver = DriverScriptRegistry.getDriverScript(MessageFormat.format(
				"{0}:{1}:{2}:{3}:{4}", browserName, version, platform, os, this
						.getClass().getName()), "Sample Data Flow");
	}
	
	@Test
	public void TC_1() throws Exception {
		driver.setCurrentTestCase("TC_1");
		driver.setCurrentTestCaseDescription("First Test Case");
		driver.driveTestExecution();
	}

	
}
