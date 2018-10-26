package aut.common.utils;

import java.io.FileInputStream;
import java.util.ArrayList;
import org.apache.poi.xssf.usermodel.*;
import org.testng.annotations.Test;


/**
 * This class contains utility methods for getting data from an Excel file.
 * 
 * @author mlabbe
 *
 */
public class ExcelDataUtil {
	protected String sTestName;
	protected Test test;
	
	// Location of Excel Data Files
	private static final String dataFolder = "src/test/resources/Data/";

		
	/*
	 * Returns the path for the Excel Data file for the given test environment
	 * @return
	 */
	private String getExcelFile(String file, String currentEnv) {
		String returnFile = "";
		String userDirectory = System.getProperty("user.dir") + "/";
		//String currentEnv = TestProperties.testProperties.getString(TestProperties.TEST_ENV);		
		if (file.isEmpty()) {
			returnFile = userDirectory+dataFolder+currentEnv+"ExcelData.xlsx";
		} else {
			returnFile = userDirectory+dataFolder+file;
		}
		return returnFile;
	}
	
	
	/*
	 * Returns ALL rows in a given Excel file, given Sheet, and for a given search key
	 * Expects to find search key in column A of the sheet.
	 * @param filePath
	 * @param sheet
	 * @param searchKey
	 * @return
	 * @throws Exception
	 */
	private ArrayList<ArrayList<Object>> fetchExcelSheetData(String filePath, String sheet, String searchKey) throws Exception {
		ArrayList<ArrayList<Object>> returnData = new ArrayList<ArrayList<Object>>();
		
		XSSFWorkbook excelBook;
		XSSFSheet excelSheet;
		XSSFRow excelRow;
		int searchKeyColumn=0;

		try
		{
			FileInputStream excelFile = new FileInputStream(filePath);
			excelBook = new XSSFWorkbook(excelFile); // Fetch Excel File
			excelSheet = excelBook.getSheet(sheet); // Fetch Given Sheet
					
			// Loop through each row in the Sheet
			for (int i=1; i <= excelSheet.getLastRowNum(); i++)
			{	
				excelRow = excelSheet.getRow(i); // Fetch current row
				// Check for the specified searchKey
				if (excelRow.getCell(searchKeyColumn).toString().equalsIgnoreCase(searchKey)){
					ArrayList<Object> currentRowData = new ArrayList<Object>();
					// Add each cell to array
					for(int j=0;j<excelRow.getLastCellNum();j++)
					{
						currentRowData.add(excelRow.getCell(j).toString());
					}
					returnData.add(currentRowData);				
				}			
			}
			return returnData;		
		} catch(Exception e) {
			e.printStackTrace();
			throw e;
		}
	}
	
	/*
	 * Converts the Excel ArrayList<ArrayList<Object>> into an Object[][]
	 * @param excelData
	 * @return
	 */
	private Object[][] listArrayConvert(ArrayList<ArrayList<Object>> excelData){		
		Object[][] returnObjectArray= new Object[excelData.size()][excelData.get(0).size()];
		Object[] aDataArray= new Object[excelData.get(0).size()];
		for (int i=0; i<excelData.size(); i++)
		{
			aDataArray=  excelData.get(i).toArray();
			returnObjectArray[i]= aDataArray;
		}
		return returnObjectArray;		
	}


	/**
	 * Returns Object[][] of data from Excel.
	 * File is determined by test environment.
	 * DataProvider specifies Sheet and SearchKey.
	 * @param file		Specific excel file to read
	 * @param sheet		The sheet name to select
	 * @param searchKey		The search key
	 * @return 	List of value pairs
	 * @throws Exception	throws Exception
	 */
	public Object[][] getExcelData(String env, String file, String sheet, String searchKey) throws Exception {
		String filePath = getExcelFile(file, env);
		ArrayList<ArrayList<Object>> excelDataArray= fetchExcelSheetData(filePath, sheet, searchKey);
		return listArrayConvert(excelDataArray);
	}
	
	/**
	 * Fetch the locale string of a given item for a given locale
	 * @param file		The file to read
	 * @param sheet		The app sheet to read.
	 * @param searchKey		The item to search for
	 * @param String		The column to get value in
	 * @return String		The localized value
	 * @throws Exception		throws exception
	 */
	public String getColumnKeyValue(String env, String file, String sheet, String searchKey, int column) throws Exception {
		String returnString = "";
		String filePath = getExcelFile(file, env);
		
		XSSFWorkbook excelBook;
		XSSFSheet excelSheet;
		XSSFRow excelRow;
		int searchKeyColumn = 0;
		
		/*switch (myLocale) {
		case EN:
			localeColumn = 1;
			break;
		case ES:
			localeColumn = 2;
			break;
		}*/
		FileInputStream excelFile = new FileInputStream(filePath);
		excelBook = new XSSFWorkbook(excelFile); // Fetch Excel File
		excelSheet = excelBook.getSheet(sheet); // Fetch Given Sheet
		int rowCount = excelSheet.getLastRowNum() - excelSheet.getFirstRowNum();
		for (int i=0; i<=rowCount; i++) {
			excelRow = excelSheet.getRow(i);
			if (excelRow.getCell(searchKeyColumn).toString().equalsIgnoreCase(searchKey)) {
				returnString = excelRow.getCell(column).toString();
				break;
			}
		}
		excelFile.close();
		return returnString;
	}
}
