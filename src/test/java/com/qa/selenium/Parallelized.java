package com.qa.selenium;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.junit.rules.TestWatcher;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import com.qa.selenium.framework.Configuration;

/*import com.qa.selenium.junit.rules.CustomTestWatcher;*/
import com.qa.selenium.junit.runners.ParallelParameterized;
import com.qa.selenium.framework.MainScript;
import com.qa.selenium.framework.WebDriverRegistry;
import com.saucelabs.common.SauceOnDemandAuthentication;
import com.saucelabs.common.SauceOnDemandSessionIdProvider;
/*import com.saucelabs.junit.Parallelized;*/
import com.qa.selenium.framework.Browser;


/**
 * This base JUnit class uses the {@link Parallelized} runner to instantiate a
 * thread for each set of parameters and execute all tests against the
 * parameters.
 * <p>
 * Parameters are determined from the "SAUCE_ONDEMAND_BROWSERS" system
 * property/environment variable, which is a JSON array of browser
 * names/versions/platforms. If this is not present, a comma-separated list of
 * browsers is loaded from "LocalBrowsers" in global-settings.properties.
 * <p>
 * For example, if you have defined 10 test methods in your class and 3
 * browsers, then 3 threads will run concurrently. Each thread will execute all
 * 10 methods one at a time. Additional parallelization at the class level can
 * be set in the Maven Surefire Plugin configuration.
 * 
 *  
 */
@RunWith(ParallelParameterized.class)
public abstract class Parallelized implements
		SauceOnDemandSessionIdProvider {

	protected MainScript driver;

	/**
	 * Constructs a {@link SauceOnDemandAuthentication} instance supplied by
	 * environment variables or from an external file.
	 */
	public SauceOnDemandAuthentication authentication = new SauceOnDemandAuthentication();

	/**
	 * JUnit Rule which will mark the Sauce Job as passed/failed when the test
	 * succeeds or fails.
	 */
	/*@Rule
	public TestWatcher testWatcher = new CustomTestWatcher(this, authentication);*/

	/**
	 * JUnit Rule that records the test name of the current test. When this is
	 * referenced during the creation of
	 * {@link org.openqa.selenium.remote.DesiredCapabilities}, the test method
	 * name is assigned to the Sauce Job name and recorded in Jenkins Console
	 * Output and in the Sauce Jobs Report in the Jenkins project's home page.
	 */
	@Rule
	public TestName testName = new TestName();

	/**
	 * Instance variable which contains the Sauce Job Id.
	 */
	private String sessionId;

	/**
	 * Represents the browser to be used as part of the test run.
	 */
	private String browserName;

	/**
	 * Represents the version of the browser to be used as part of the test run.
	 */
	private String version;

	/**
	 * Represents the platform to be used as part of the test run.
	 */
	private String platform;

	/**
	 * Represents the operating system to be used as part of the test run.
	 */
	private String os;

	/**
	 * Constructs a new instance of the test. The constructor requires four
	 * string parameters, which represent the browser name, version, platform,
	 * and os to be used when launching a Sauce VM. The order of the parameters
	 * should be the same as that of the elements within the
	 * {@link #browsersStrings()} method.
	 * 
	 * @param browserName
	 * @param version
	 * @param platform
	 * @param os
	 */
	public Parallelized(String browserName, String version,
			String platform, String os) {
		super();
		this.browserName = browserName;
		this.version = version;
		this.platform = platform;
		this.os = os;
	}

	/**
	 * Defines the parameters for either remote or local execution. If the
	 * system property/environment variable "SAUCE_ONDEMAND_BROWSERS" is
	 * present, remote execution is used.
	 * 
	 * @return a List containing String arrays representing the browser
	 *         combinations the test should be run against. The values in the
	 *         String array are used as part of the invocation of the test
	 *         constructor.
	 */
	@Parameters
	public static List<String[]> browsersStrings() {
		List<String[]> browsers = new ArrayList<String[]>();

		String json = readPropertyOrEnv("SAUCE_ONDEMAND_BROWSERS", null);

		if (StringUtils.isNotBlank(json)) {
			// Read in all settings for remote execution
			try {
				JSONArray browserArray = new JSONArray(json);
				for (int i = 0; i < browserArray.length(); i++) {
					JSONObject browserObject = browserArray.getJSONObject(i);
					browsers.add(new String[] {
							browserObject.getString("browser"),
							browserObject.getString("browser-version"),
							browserObject.getString("platform"),
							browserObject.getString("os") });
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		} else {
			String threads = System
					.getProperty("parallelParameterMethods", "1");
			int numThreads = Integer.parseInt(threads);
			if (numThreads > 1) {
				throw new IllegalStateException(
						"Property parallelParameterMethods > 1 is not supported for local execution");
			}

			// Set only a list of browser names for local execution
			String localBrowsers = Configuration.getProperty("LocalBrowser");

			for (String name : localBrowsers.split(",")) {
				browsers.add(new String[] { name.trim(), null, null, null });
			}
		}

		return browsers;
	}

	/**
	 * For remote execution, constructs a new {@link WebDriver} which is
	 * configured to use the capabilities defined by the {@link #browserName},
	 * {@link #version}, {@link #platform} and {@link #os} instance variables,
	 * and which is configured to run against ondemand.saucelabs.com, using the
	 * username and access key populated by the {@link #authentication}
	 * instance. Otherwise, a driver is retrieved from {@link WebDriverRegistry}
	 * for local execution.
	 * 
	 * @throws MalformedURLException
	 *             if the remote execution URL is invalid
	 */
	@Before
	public void setUp() throws MalformedURLException {
		/*if (StringUtils.isNotBlank(version) && StringUtils.isNotBlank(platform)
				&& StringUtils.isNotBlank(os)) */
			if(Configuration.getProperty("testbed").equals("saucelab")){
			// Parameters for grid provided
			String host = readPropertyOrEnv("SELENIUM_HOST",
					"ondemand.saucelabs.com");
			String port = readPropertyOrEnv("SELENIUM_PORT", "80");

			DesiredCapabilities capabilities = new DesiredCapabilities();
			/*capabilities
					.setCapability("username", authentication.getUsername());
			capabilities.setCapability("accessKey",
					authentication.getAccessKey());*/
			//DesiredCapabilities capabilities = new DesiredCapabilities();
			capabilities
					.setCapability("username", Configuration.getProperty("SAUCE_USER_NAME"));
			capabilities.setCapability("accessKey",
					Configuration.getProperty("SAUCE_API_KEY"));
			capabilities
					.setCapability(CapabilityType.BROWSER_NAME, browserName);
			capabilities.setCapability(CapabilityType.VERSION, version);
			capabilities.setCapability(CapabilityType.PLATFORM, platform);
			capabilities.setCapability("loweswebdev", "parent-tunnel");
			capabilities.setCapability("os", os);
			capabilities.setCapability("name", this.getClass().getName() + "."
					+ testName.getMethodName());

			driver.setWebDriver(new RemoteWebDriver(new URL("http://" + host
					+ ":" + port + "/wd/hub"), capabilities));
			sessionId = ((RemoteWebDriver) driver.getWebDriver())
					.getSessionId().toString();
		} else {
			// Parameters for grid missing, get local driver
			driver.setWebDriver(WebDriverRegistry.getDriver(Browser.valueOf(browserName)));
		}
	}

	@After
	public void tearDown() {
		if (StringUtils.isNotBlank(version) && StringUtils.isNotBlank(platform)
				&& StringUtils.isNotBlank(os)) {
			// Parameters for grid provided
			driver.getWebDriver().quit();
		}
	}

	@AfterClass
	public static void tearDownClass() {
		/*WebDriverRegistry.quitAllDrivers();*/
	}

	/**
	 * 
	 * @return the value of the Sauce Job id.
	 */
	public String getSessionId() {
		return sessionId;
	}

	private static String readPropertyOrEnv(String key, String defaultValue) {
		String v = System.getProperty(key);
		if (v == null) {
			v = System.getenv(key);
		}
		if (v == null) {
			v = defaultValue;
		}
		return v;
	}
}
