package com.qa.selenium.framework;

import org.openqa.selenium.WebDriver;

import com.qa.selenium.framework.Datatable;

/**
 * Wrapper class for common framework objects, to be used across the entire test
 * case and dependent libraries
**/
public class ObjectsReference {
	private final Datatable dataTable;
	private final WebDriver driver;

	/**
	 * Constructor to initialize all the objects wrapped by the
	 * {@link ObjectsReference} class
	 * 
	 * @param dataTable
	 *            The {@link DataTable} object
	 * @param driver
	 *            The {@link WebDriver} object
	 */
	public ObjectsReference(Datatable dataTable, 
			WebDriver driver) {
		this.dataTable = dataTable;
		this.driver = driver;
	}

	/**
	 * Function to get the {@link DataTable} object of the {@link ObjectsReference}
	 * class
	 * 
	 * @return The {@link DataTable} object
	 */
	public Datatable getDataTable() {
		return dataTable;
	}

	/**
	 * Function to get the {@link SeleniumReport} object of the
	 * {@link WrapperReference} class
	 * 
	 * @return The {@link SeleniumReport} object
	 */
	

	/**
	 * Function to get the {@link WebDriver} object of the {@link ObjectsReference}
	 * class
	 * 
	 * @return The {@link WebDriver} object
	 */
	public WebDriver getDriver() {
		return driver;
	}
}