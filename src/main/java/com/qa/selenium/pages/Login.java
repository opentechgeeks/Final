package com.qa.selenium.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.sql.Timestamp;


import com.qa.selenium.framework.*;
import com.thoughtworks.selenium.Selenium;
import com.thoughtworks.selenium.webdriven.WebDriverBackedSelenium;
import com.qa.selenium.framework.ObjectsReference;
import com.qa.selenium.framework.ReusableLibrary;

import com.thoughtworks.selenium.Selenium;


public class Login  extends ReusableLibrary {

	String baseurl = dataTable.getData("General_Data", "URL");
	/*Selenium selenium = new WebDriverBackedSelenium(driver, baseurl);*/
	
	
	
	public Login(ObjectsReference objectsReference) throws Exception {
		super(objectsReference);
		
		// TODO Auto-generated constructor stub
	}
	
public void test() throws Exception
	
	{
		
		System.out.println("Success");
		/*WebDriver driver = new FirefoxDriver();
		String appUrl = "https://www.google.com";*/
		driver.get("http://www.google.com");
						
	}

public void test2() throws Exception
{
System.out.println("success");	  
}
}
	


