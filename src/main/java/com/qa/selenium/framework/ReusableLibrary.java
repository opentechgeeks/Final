package com.qa.selenium.framework;

import java.util.Properties;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import com.qa.selenium.framework.*;

/**
 * Abstract base class for reusable libraries created by the user
 * 
 * @author Cognizant
 * @version 3.0
 * @since October 2011
 */
public abstract class ReusableLibrary {
	/**
	 * The {@link DataTable} object (passed from the test script)
	 */
	protected Datatable dataTable;
	/**
	 
	/**
	 * The {@link WebDriver} object
	 */
	protected WebDriver driver;
	/**
	 * The {@link ScriptHelper} object (required for calling one reusable
	 * library from another)
	 */
	protected ObjectsReference objectsReference;

	/**
	 * The {@link Properties} object with settings loaded from the framework
	 * properties file
	 */
	protected Properties properties = Configuration.getInstance();

	/**
	 * Constructor to initialize the {@link ScriptHelper} object and in turn the
	 * objects wrapped by it
	 * 
	 * @param scriptHelper
	 *            The {@link ScriptHelper} object
	 * @param driver
	 *            The {@link WebDriver} object
	 */
	public ReusableLibrary(ObjectsReference objectsReference) {
		this.objectsReference = objectsReference;

		this.dataTable = objectsReference.getDataTable();
		this.driver = objectsReference.getDriver();
	}
	public void ClickCustomize(String l,String s)
	{
	
		if(l.equalsIgnoreCase("xpath"))
		{
			try
			{
			driver.findElement(By.xpath(s)).click();
			}
			catch(Exception e){
				driver.findElement(By.linkText("No, thanks")).click();
	            driver.findElement(By.xpath(s)).click();
			}
		}
		else if(l.equalsIgnoreCase("id"))
		{
			try
			{
			driver.findElement(By.id(s)).click();
			}
			catch(Exception e){
				driver.findElement(By.linkText("No, thanks")).click();
	            
	            driver.findElement(By.id(s)).click();
			}
		}
		else if(l.equalsIgnoreCase("linkText"))
		{
			try
			{
			driver.findElement(By.linkText(s)).click();
			}
			catch(Exception e){
				driver.findElement(By.linkText("No, thanks")).click();
	            driver.findElement(By.linkText(s)).click();
			}
		}
		else if(l.equalsIgnoreCase("name"))
		{
			try
			{
			driver.findElement(By.name(s)).click();
			}
			catch(Exception e){
				driver.findElement(By.linkText("No, thanks")).click();
	            driver.findElement(By.name(s)).click();
			}
		}
		else if(l.equalsIgnoreCase("partialLinkText"))
		{
			try
			{
			driver.findElement(By.partialLinkText(s)).click();
			}
			catch(Exception e){
				driver.findElement(By.linkText("No, thanks")).click();
	            driver.findElement(By.partialLinkText(s)).click();
			}
		}
		else if(l.equalsIgnoreCase("className"))
		{
			try
			{
			driver.findElement(By.className(s)).click();
			}
			catch(Exception e){
				driver.findElement(By.linkText("No, thanks")).click();
	            driver.findElement(By.className(s)).click();
			}
		}
		else if(l.equalsIgnoreCase("cssSelector"))
		{
			try
			{
			driver.findElement(By.cssSelector(s)).click();
			}
			catch(Exception e){
				driver.findElement(By.linkText("No, thanks")).click();
	            driver.findElement(By.cssSelector(s)).click();
			}
		}
		else if(l.equalsIgnoreCase("tagName"))
		{
			try
			{
			driver.findElement(By.tagName(s)).click();
			}
			catch(Exception e){
				driver.findElement(By.linkText("No, thanks")).click();
	            driver.findElement(By.tagName(s)).click();
			}
		}
	}
	public float transformStringToFloatPrice(String s) throws Exception
	   {
		   String c = s.replace("$","");
		   try{	   
		   String g = c.replace(",","");
		   float price = Float.parseFloat(g);
		   return price;
		   }
		   catch(Exception f){
			   float price1 = Float.parseFloat(c);
			   return price1;
		   }
	   }
}