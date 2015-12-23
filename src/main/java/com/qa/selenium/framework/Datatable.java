
package com.qa.selenium.framework;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import nl.fountain.xelem.excel.Cell;
import nl.fountain.xelem.excel.Workbook;
import nl.fountain.xelem.excel.Worksheet;
import nl.fountain.xelem.lex.ExcelReader;

import org.xml.sax.SAXException;

import com.qa.selenium.framework.Configuration;

public class Datatable {
	private String dataReferenceIdentifier = "#";
	private String currentTestcase;
	private int currentIteration = 0;
	private int currentSubIteration = 0;
	private Workbook workbook;
	private Worksheet commonDataWs;

	public void setDataReferenceIdentifier(String dataReferenceIdentifier) {
		this.dataReferenceIdentifier = dataReferenceIdentifier;
	}

	public Datatable(String datatablePath, String datatableName)
			throws IOException, SAXException, ParserConfigurationException {
		workbook = new ExcelReader().getWorkbook(datatablePath + "/"
				+ datatableName + ".xml");

		commonDataWs = new ExcelReader().getWorkbook(
				datatablePath + "/" + "Common Testdata.xml").getWorksheet(
				"Common_Testdata");
	}

	public void setCurrentRow(String currentTestcase, int currentIteration,
			int currentSubIteration) {
		this.currentTestcase = currentTestcase;
		this.currentIteration = currentIteration;
		this.currentSubIteration = currentSubIteration;
	}

	private void checkPreRequisites() throws Exception {
		if (this.currentTestcase == null) {
			throw new Exception(
					"CraftDataTable.currentTestCase is not set!");
		}
		if (this.currentIteration == 0) {
			throw new Exception(
					"CraftDataTable.currentIteration is not set!");
		}
		if (this.currentSubIteration == 0)
			throw new Exception(
					"CraftDataTable.currentSubIteration is not set!");
	}

	public synchronized String getData(String datasheetName, String fieldName) throws Exception {
		checkPreRequisites();

		Worksheet testDataWs = workbook.getWorksheet(datasheetName);

		int rowNum = getRowByNameAndColumn(testDataWs, this.currentTestcase, 1);
		if (rowNum == -1) {
			throw new Exception("The test case \""
					+ this.currentTestcase
					+ "\" is not found in the test data sheet \""
					+ datasheetName + "\"!");
		}
		if (!Integer.toString(this.currentIteration).equals(
				testDataWs.getCellAt(rowNum, 2).getData$())) {
			throw new Exception("The iteration number \""
					+ this.currentIteration + "\" of the test case \""
					+ this.currentTestcase
					+ "\" is not found in the test data sheet \""
					+ datasheetName + "\"!");
		}
		if (!Integer.toString(this.currentSubIteration).equals(
				testDataWs.getCellAt(rowNum, 3).getData$())) {
			throw new Exception("The sub iteration number \""
					+ this.currentSubIteration + "\" under iteration number \""
					+ this.currentIteration + "\" of the test case \""
					+ this.currentTestcase
					+ "\" is not found in the test data sheet \""
					+ datasheetName + "\"!");
		}

		String dataValue = testDataWs.getCellAt(rowNum,
				getColumnByName(testDataWs, fieldName)).getData$();

		if (dataValue.startsWith(this.dataReferenceIdentifier)) {
			dataValue = getCommonData(fieldName, dataValue);
		}

		return dataValue;
	}

	private String getCommonData(String fieldName, String dataValue) throws Exception {
		String dataReferenceId = dataValue.split(this.dataReferenceIdentifier)[1];

		int rowNum = getRowByNameAndColumn(commonDataWs, dataReferenceId, 1);
		if (rowNum == -1) {
			throw new Exception(
					"The common test data row identified by \""
							+ dataReferenceId
							+ "\" is not found in the common test data sheet!");
		}

		dataValue = commonDataWs.getCellAt(rowNum,
				getColumnByName(commonDataWs, fieldName)).getData$();

		return dataValue;
	}

	public synchronized void putData(String datasheetName, String fieldName,
			String dataValue) throws Exception {
		checkPreRequisites();

		Worksheet testDataWs = null;
		testDataWs = workbook.getWorksheet(datasheetName);

		int rowNum = getRowByNameAndColumn(testDataWs, this.currentTestcase, 1);
		if (rowNum == -1) {
			throw new Exception("The test case \""
					+ this.currentTestcase
					+ "\" is not found in the test data sheet \""
					+ datasheetName + "\"!");
		}
		if (!Integer.toString(this.currentIteration).equals(
				testDataWs.getCellAt(rowNum, 2).getData$())) {
			throw new Exception("The iteration number \""
					+ this.currentIteration + "\" of the test case \""
					+ this.currentTestcase
					+ "\" is not found in the test data sheet \""
					+ datasheetName + "\"!");
		}
		if (!Integer.toString(this.currentSubIteration).equals(
				testDataWs.getCellAt(rowNum, 3).getData$())) {
			throw new Exception("The sub iteration number \""
					+ this.currentSubIteration + "\" under iteration number \""
					+ this.currentIteration + "\" of the test case \""
					+ this.currentTestcase
					+ "\" is not found in the test data sheet \""
					+ datasheetName + "\"!");
		}

		int colNum = getColumnByName(testDataWs, fieldName);

		testDataWs.getCellAt(rowNum, colNum).setData(dataValue);
	}

	public synchronized String getExpectedResult(String fieldName) throws Exception {
		checkPreRequisites() ;

		Worksheet expectedResultsWs = null;
		expectedResultsWs = workbook.getWorksheet("Parametrized_Checkpoints");

		int rowNum = getRowByNameAndColumn(expectedResultsWs,
				this.currentTestcase, 1);
		if (rowNum == -1) {
			throw new Exception("The test case \""
					+ this.currentTestcase
					+ "\" is not found in the parametrized checkpoints sheet!");
		}
		if (!Integer.toString(this.currentIteration).equals(
				expectedResultsWs.getCellAt(rowNum, 2).getData$())) {
			throw new Exception("The iteration number \""
					+ this.currentIteration + "\" of the test case \""
					+ this.currentTestcase
					+ "\" is not found in the parametrized checkpoints sheet!");
		}
		if (!Integer.toString(this.currentSubIteration).equals(
				expectedResultsWs.getCellAt(rowNum, 3).getData$())) {
			throw new Exception("The sub iteration number \""
					+ this.currentSubIteration + "\" under iteration number \""
					+ this.currentIteration + "\" of the test case \""
					+ this.currentTestcase
					+ "\" is not found in the parametrized checkpoints sheet!");
		}

		String dataValue = expectedResultsWs.getCellAt(rowNum,
				getColumnByName(expectedResultsWs, fieldName)).getData$();

		return dataValue;
	}

	public List<String> getBusinessFlow() throws Exception{
		Worksheet businessFlowWs = workbook.getWorksheet("Business_Flow");

		int rowNum = getRowByNameAndColumn(businessFlowWs,
				this.currentTestcase, 1);
		if (rowNum == -1) {
			throw new Exception("The test case \""
					+ this.currentTestcase
					+ "\" is not found in the Business Flow sheet!");
		}

		String dataValue;
		List<String> businessFlowData = new ArrayList<String>();
		int currentColumnNum = 2;
		while (!"".equals((dataValue = businessFlowWs.getCellAt(rowNum,
				currentColumnNum).getData$()))) {
			businessFlowData.add(dataValue);
			currentColumnNum++;
		}

		if (businessFlowData.size() == 0) {
			throw new Exception(
					"No business flow found for the test case \""
							+ this.currentTestcase + "\"");
		}

		return businessFlowData;
	}

	public int getTestEndIteration() {
		Worksheet testDataWs = workbook.getWorksheet(Configuration
				.getProperty("DefaultDataSheet"));

		int startRowNum = getRowByNameAndColumn(testDataWs, currentTestcase, 1);
		int nTestcaseRows = getRowCount(testDataWs, currentTestcase, 1,
				startRowNum);
		// Assumption: Every test case will have at least one iteration
		int nSubIterations = getRowCount(testDataWs, "1", 2, startRowNum);
		int nIterations = nTestcaseRows / nSubIterations;

		if (nIterations > 0) {
			return nIterations;
		} else {
			return 1;
		}
	}

	private static int getRowCount(Worksheet worksheet, String key,
			int columnNum, int startRowNum) {
		int rowCount = 0;
		Boolean keyFound = Boolean.FALSE;

		for (int currentRowNum = startRowNum; currentRowNum <= Worksheet.lastRow; ++currentRowNum) {
			String currentValue = worksheet.getCellAt(currentRowNum, columnNum)
					.getData$();

			if (currentValue.equals(key)) {
				++rowCount;
				keyFound = Boolean.TRUE;
			} else {
				if (keyFound.booleanValue()) {
					break;
				}
			}
		}

		return rowCount;
	}

	private static int getRowByNameAndColumn(Worksheet worksheet, String name,
			int column) {
		for (int r = 1; r <= Worksheet.lastRow; r++) {
			if (name.equals(worksheet.getCellAt(r, column).getData$())) {
				return r;
			}
		}
		return -1;
	}

	private static int getColumnByName(Worksheet worksheet, String name) throws Exception {
		for (Cell cell : worksheet.getRowAt(1).getCells()) {
			if (name.equals(cell.getData$())) {
				return cell.getIndex();
			}
		}
		throw new Exception("Could not find column by name: " + name);
	}
}