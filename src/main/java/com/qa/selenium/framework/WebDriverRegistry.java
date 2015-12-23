package com.qa.selenium.framework;

import java.util.HashMap;
import java.util.Map;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriverService;
import org.openqa.selenium.remote.DesiredCapabilities;

import com.qa.selenium.framework.Configuration;
import com.opera.core.systems.OperaDriver;

/**
 * Registry for retrieving the driver object based on the required browser.
 * Intended for driving local browsers, not remote browsers.
 * 
 * @author Matthew DeTullio
 */
public final class WebDriverRegistry {

	private static volatile Map<Browser, WebDriver> registry = new HashMap<Browser, WebDriver>();

	private WebDriverRegistry() {
		// Prevent outside instantiation
	}

	/**
	 * Function to return the appropriate {@link WebDriver} object based on the
	 * {@link Browser} passed
	 * 
	 * @param browser
	 *            The {@link Browser} to be used for the test execution
	 * @return The {@link WebDriver} object corresponding to the {@link Browser}
	 *         specified
	 */
	public static synchronized WebDriver getDriver(Browser browser) {
		switch (browser) {
		case chrome:
			System.setProperty("webdriver.chrome.driver",
					Configuration.getProperty("ChromeDriverPath"));

			if (!registry.containsKey(browser)) {
				registry.put(browser, new ChromeDriver());
			}
			break;
		case firefox:
			FirefoxProfile profile = new FirefoxProfile();
			// driver = new FirefoxDriver(new FirefoxBinary(new
			// File("C:\\Program Files\\Mozilla Firefox\\firefox.exe")),
			// profile);
			if (!registry.containsKey(browser)) {
				registry.put(browser, new FirefoxDriver(profile));
			}
			break;
		case htmlunit:
			if (!registry.containsKey(browser)) {
				registry.put(browser, new HtmlUnitDriver());
			}
			break;
		case iexplore:
			System.setProperty("webdriver.ie.driver",
					Configuration.getProperty("InternetExplorerDriverPath"));
			if (!registry.containsKey(browser)) {
				registry.put(browser, new InternetExplorerDriver());
			}
			break;
		case opera:
			if (!registry.containsKey(browser)) {
				registry.put(browser, new OperaDriver());
			}
			break;
		/*case phantomjs:
			DesiredCapabilities caps = new DesiredCapabilities();
			caps.setCapability("takesScreenshot", true);
			caps.setCapability(
					PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY,
					Configuration.getProperty("PhantomJSDriverPath"));
			if (!registry.containsKey(browser)) {
				registry.put(browser, new PhantomJSDriver(caps));
			}
			break;*/
		default:
			throw new IllegalArgumentException("Browser " + browser.name()
					+ " is not supported for local execution");
		}

		return registry.get(browser);
	}

	public static synchronized void quitAllDrivers() {
		for (WebDriver webDriver : registry.values()) {
			webDriver.quit();
		}
		registry.clear();
	}
}