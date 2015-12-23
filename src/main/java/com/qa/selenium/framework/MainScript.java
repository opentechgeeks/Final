package com.qa.selenium.framework;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.xml.sax.SAXException;

import com.qa.selenium.framework.ObjectsReference;

import com.qa.selenium.framework.Datatable;


/**
 * Driver script class which encapsulates the core logic of the CRAFT framework
 * 
 * @author Cognizant
 * @author Matthew DeTullio
 */
public class MainScript {
	private static final String RUNTIME_TABLES_DIR = "target/runtime-datatables";

	private WebDriver webDriver;

	private Datatable dataTable;

	private String scenario;

	private String currentTestCase;
	private int currentTestIteration;
	private int currentTestEndIteration;
	private int currentTestSubIteration;

	private List<String> businessFlowData;

	private Date startTime;
	private Date endTime;

	private ObjectsReference objectsReference;
	

	/**
	 * Constructor to initialize the DriverScript
	 * 
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	public MainScript(String scenario) throws IOException, SAXException,
			ParserConfigurationException {
		this.scenario = scenario;
		initializeDatatable();
	}

	public void setCurrentTestCase(String testCase) {
		currentTestCase = testCase;
		dataTable.setCurrentRow(currentTestCase, 0, 0);
	}

	public void setCurrentTestCaseDescription(String testCaseDescription) {
		// Currently unused
	}

	/**
	 * Function to execute the given test case
	 * @throws Exception 
	 */
	public void driveTestExecution() throws Exception {
		
		initializeTestScript();
		currentTestIteration = 1;
		currentTestEndIteration = dataTable.getTestEndIteration();

		webDriver.manage().deleteAllCookies();
		webDriver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
		webDriver.get(Configuration.getProperty("ApplicationUrl"));
		webDriver.manage().deleteAllCookies();
		// Add cookie to disable ForeSee pop-up invite
		webDriver.manage().addCookie(
				new Cookie("fsr.s", "{\"i\":-1}", ".lowes.com", "/", null,
						false));

		executeTestIterations();

		wrapUp();
	}

	
	private void wrapUp() {
		// TODO Auto-generated method stub
		
	}

	private void initializeDatatable() throws IOException, SAXException,
			ParserConfigurationException {
		File runtimeTable = new File(RUNTIME_TABLES_DIR + "/" + scenario
				+ ".xml");
		if (!runtimeTable.exists()) {
			InputStream table = Object.class.getResourceAsStream("/datatables/"
					+ scenario + ".xml");

			FileUtils.copyInputStreamToFile(table, runtimeTable);
		}

		File runtimeCommon = new File(RUNTIME_TABLES_DIR
				+ "/Common Testdata.xml");
		if (!runtimeCommon.exists()) {
			InputStream common = Object.class
					.getResourceAsStream("/datatables/Common Testdata.xml");

			FileUtils.copyInputStreamToFile(common, runtimeCommon);
		}

		dataTable = new Datatable(RUNTIME_TABLES_DIR, scenario);
	}

	private void initializeTestScript() throws Exception {
		objectsReference = new ObjectsReference(dataTable, webDriver);

		businessFlowData = dataTable.getBusinessFlow();
	}

	private void executeTestIterations()  throws Exception{
		while (currentTestIteration <= currentTestEndIteration) {
			
			try {
				executeTestcase(businessFlowData);
			} catch (Exception fx) {
				exceptionHandler(fx, "Error");
			} /*catch (InvocationTargetException ix) {
				exceptionHandler((Exception) ix.getCause(), "Error");
			}*/ /*catch (Exception ex) {
				exceptionHandler(ex, "Error");
			}*/

			currentTestIteration++;
		}
	}

	private void executeTestcase(List<String> businessFlowData)
			throws IllegalArgumentException, IllegalAccessException,
			InvocationTargetException, ClassNotFoundException,
			InstantiationException {
		HashMap<String, Integer> keywordDirectory = new HashMap<String, Integer>();

		for (int currentKeywordNum = 0; currentKeywordNum < businessFlowData
				.size(); currentKeywordNum++) {
			String[] currentFlowData = businessFlowData.get(currentKeywordNum)
					.split(",");
			String currentKeyword = currentFlowData[0];

			int nKeywordIterations;
			if (currentFlowData.length > 1) {
				nKeywordIterations = Integer.parseInt(currentFlowData[1]);
			} else {
				nKeywordIterations = 1;
			}

			for (int currentKeywordIteration = 0; currentKeywordIteration < nKeywordIterations; currentKeywordIteration++) {
				if (keywordDirectory.containsKey(currentKeyword)) {
					keywordDirectory.put(currentKeyword,
							keywordDirectory.get(currentKeyword) + 1);
				} else {
					keywordDirectory.put(currentKeyword, 1);
				}
				currentTestSubIteration = keywordDirectory.get(currentKeyword);

				dataTable.setCurrentRow(currentTestCase, currentTestIteration,
						currentTestSubIteration);

				if (currentTestSubIteration > 1) {
					/*report.addTestLogSubSection(currentKeyword
							+ " (Sub-Iteration: " + currentTestSubIteration
							+ ")");*/
				} else {
				/*	report.addTestLogSubSection(currentKeyword);*/
				}

				invokeBusinessComponent(currentKeyword);
			}
		}
	}

	private void invokeBusinessComponent(String currentKeyword)
			throws IllegalArgumentException, IllegalAccessException,
			InvocationTargetException, ClassNotFoundException,
			InstantiationException {
		Boolean isMethodFound = false;
		final String javaFileExtension = ".java";
		File[] packageDirectories = { new File(
				"src/main/java/com/qa/selenium/pages") };

		for (File packageDirectory : packageDirectories) {
			File[] packageFiles = packageDirectory.listFiles();
			
			for (int i = 0; i < packageFiles.length; i++) {
				File packageFile = packageFiles[i];
				String fileName = packageFile.getName();
				
				// We only want the .java files
				if (fileName.endsWith(javaFileExtension)) {
					// Remove the .java extension to get the class name
					String className = fileName.substring(0, fileName.length()
							- javaFileExtension.length());
				

					Class<?> reusableComponents = Class
							.forName("com.qa.selenium.pages."
									+ className);
					Method executeComponent;

					try {
						// Convert the first letter of the method to lowercase
						// (in line with java naming conventions)
						currentKeyword = currentKeyword.substring(0, 1)
								.toLowerCase() + currentKeyword.substring(1);
												
						executeComponent = reusableComponents.getMethod(
								currentKeyword, (Class<?>[]) null);
					} catch (NoSuchMethodException ex) {
						// If the method is not found in this class, search the
						// next class
						continue;
					}

					isMethodFound = true;

					Constructor<?> ctor = reusableComponents
							.getDeclaredConstructors()[0];
					
					Object businessComponent = ctor.newInstance(objectsReference);

					executeComponent.invoke(businessComponent, (Object[]) null);
									

					break;
				}
			}
		}

		if (!isMethodFound) {
			throw new IllegalArgumentException("Keyword " + currentKeyword
					+ " not found within any class "
					+ "inside the pages package");
		}
	}

	private void exceptionHandler(Exception ex, String exceptionName) throws Exception {
		// Error reporting
		String exceptionDescription = ex.getMessage();
		if (exceptionDescription == null) {
			exceptionDescription = ex.toString();
		}

		if (ex.getCause() != null) {
			/*report.updateTestLog(exceptionName, exceptionDescription
					+ " <b>Caused by: </b>" + ex.getCause(), Status.FAIL);*/
		} else {
			/*report.updateTestLog(exceptionName, exceptionDescription,
					Status.FAIL);*/
		}
		ex.printStackTrace();

		/*report.updateTestLog(
				"Lowes Info",
				"Test case iteration terminated by user! Proceeding to next iteration (if applicable)...",
				Status.DONE);*/
		currentTestIteration++;
		executeTestIterations();

		// Wrap up execution
		wrapUp();
	}



	public WebDriver getWebDriver() {
		return webDriver;
	}

	public void setWebDriver(WebDriver webDriver) {
		this.webDriver = webDriver;
	}

	public Datatable getDataTable() {
		return dataTable;
	}
}
