package com.qa.selenium.framework;

import java.io.File;
import java.util.Properties;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxBinary;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.remote.*;

import com.opera.core.systems.OperaDriver;
import com.qa.selenium.framework.Configuration;


/**
 * Factory for creating the Driver object based on the required browser
 * @author Cognizant
 * @version 3.0
 * @since October 2011
 */
public class WebDriverFactory
{
	private static Properties properties;
	
	/**
	 * Function to return the appropriate {@link RemoteWebDriver} object based on the {@link Browser} passed
	 * @param browser The {@link Browser} to be used for the test execution
	 * @return The {@link RemoteWebDriver} object corresponding to the {@link Browser} specified
	 */
	@SuppressWarnings("deprecation")
	public static RemoteWebDriver getDriver(Browser browser)
	
	{
		WebDriver driver = null;
		
		switch(browser)
		{
		case chrome:
			properties = Configuration.getInstance();
			System.setProperty("webdriver.chrome.driver",
									properties.getProperty("ChromeDriverPath"));
			driver = new ChromeDriver();
			break;
		case firefox:
			FirefoxProfile profile = new FirefoxProfile();
			driver = new FirefoxDriver(new FirefoxBinary(new File("C:\\Program Files\\Mozilla Firefox\\firefox.exe")), profile);
			break;
		case htmlunit:
			driver = new HtmlUnitDriver();
			break;
		case iexplore:
			properties = Configuration.getInstance();
			System.setProperty("webdriver.ie.driver",
									properties.getProperty("InternetExplorerDriverPath"));
			driver = new InternetExplorerDriver();
			break;
		case opera:
			driver = new OperaDriver();
			break;
		}
					
		return (RemoteWebDriver)driver;
	}
	
	}
