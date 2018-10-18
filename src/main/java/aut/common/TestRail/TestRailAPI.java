package aut.common.TestRail;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.testng.Reporter;

/**
 * This class is for integrating with TestRails
 * @author mlabbe
 * @param <APIClient>
 *
 */
public class TestRailAPI {

	// TestRails connection info.
	public static final String testRailURL = "https://fwdavison.testrail.com/";
	public static final String testRailUser = "mlabbe@prismhr.com";
	public static final String testRailPassword = "1234Test";
	private int waitTime = 1500;
	
	TestRailAPIClient client = null;

	// TestRails test result status options
	public final static Integer PASSED = 1;
	public final static Integer BLOCKED = 2;
	public final static Integer UNTESTED = 3;
	public final static Integer RETEST = 4;
	public final static Integer FAILED = 5;
	
	/*
	 * Set the TestRailAPI client
	 */
	private void setClient() {
		client = new TestRailAPIClient(testRailURL);
		client.setUser(testRailUser);
		client.setPassword(testRailPassword);		
	}
	
	/*
	 * Get the Plan, based on Plan ID
	 */
	private JSONObject getPlan(String planID) throws MalformedURLException, IOException, APIException {
		return (JSONObject) client.sendGet("get_plan/"+planID);
	}

	/*
	 * Get the Plan's Entries
	 */
	private JSONArray getPlanEntries(JSONObject plan) {
		return (JSONArray) plan.get("entries");
	}

	public JSONObject getTestCase(String caseID) throws Exception {
		return (JSONObject) client.sendGet("get_case/"+caseID);
	}
	
	/*
	 * Get the desired Run, based on passing in
	 * Entries and the string to search for in Run Name
	 */
	private Long getRunID(JSONArray entries, String runString) {
		Long returnID = (long) 0;
		for (int i=0; i < entries.size(); i++) {
			JSONObject thisEntry = (JSONObject) entries.get(i);
			//System.out.println("*** TestRailAPI entry -'"+thisEntry.get("name").toString().toLowerCase()+"'");
			if (thisEntry.get("name").toString().toLowerCase().contains(runString.toLowerCase())) {
				//System.out.println("** TestRailAPI: Found Run for '"+runString+"'");
				JSONObject thisRun = (JSONObject) ((JSONArray) thisEntry.get("runs")).get(0);
				returnID = (Long) thisRun.get("id");
				break;
			}
		}
		return returnID;
	}
	
	/*
	 * Returns true/false if the given Test Case ID
	 * is found in list of tests for the given Run ID.
	 */
	private boolean runTestCaseIDFound(Long runID, Long testCaseID) throws Exception {
		boolean foundIt = false;
		JSONArray run = (JSONArray) client.sendGet("get_tests/"+runID);
		if (run.size() > 0) {
			for (int i=0; i<run.size(); i++) {
				JSONObject thisTest = (JSONObject) run.get(i);
				if ((Long)thisTest.get("case_id") == testCaseID.longValue()) {
					foundIt = true;
					break;
				}
			}
			Thread.sleep(waitTime);
		} else {
			throw new Exception("!!! TestRail Exception: runTestCaseIDFound get_tests returned size 0 !!!");
		}
		return foundIt;
	}
	
	/*
	 * Updates a result for a given run and test
	 * Specify the status and any comment.
	 */
	@SuppressWarnings("rawtypes")
	private JSONObject updateTestResult(Long runID, Long testID, Map resultData) throws MalformedURLException, IOException, APIException {
		String postString = "add_result_for_case/"+runID+"/"+testID;
		//System.out.println("*** TestRailAPI posting string: '"+postString+"'");
		return (JSONObject) client.sendPost(postString, resultData);
	}
	
	/*
	 * Gets list of Test IDs for tests in a run that are marked Failed or Retest
	 */
	public ArrayList<String> getRunFailedTests(String runID) throws Exception {
		ArrayList<String> returnList = new ArrayList<String>();
		
		// Fetch All Tests
		ArrayList<String> runTestCaseIdList = getRunTestIds(runID);
		setClient();
		for (String singleId : runTestCaseIdList) {
			JSONObject thisCase = getTestCase(singleId);
			// Only check those marked as Automated or Automated-Needs Update
			if (String.valueOf((long) thisCase.get("custom_executionmethod")).equals("1")
					|| String.valueOf((long) thisCase.get("custom_executionmethod")).equals("3")) {
				
				JSONArray runResults = (JSONArray) client.sendGet("get_results_for_case/"+runID+"/"+singleId+"&limit=1");
				if (runResults != null) {
					try {
						if (((JSONObject) runResults.get(0)).get("status_id") != null) {
							if (((JSONObject) runResults.get(0)).get("status_id").toString().equals("5") || ((JSONObject) runResults.get(0)).get("status_id").toString().equals("4")) {
								returnList.add(singleId);
								//Reporter.log("ID: "+singleId, true);
							}
						}
					} catch (Exception e) {
						// nothing -skip this result
					}
				}
			}
			Thread.sleep(waitTime);
		}
		//Reporter.log("NUMBER: "+returnList.size(), true);
		return returnList;
	}
	
	public ArrayList<String> getRunUntestedTests(String runID) throws Exception {
		ArrayList<String> returnList = new ArrayList<String>();
		
		// Fetch All Tests
		ArrayList<String> runTestCaseIdList = getRunTestIds(runID);
		setClient();
		for (String singleId : runTestCaseIdList) {
			JSONObject thisCase = getTestCase(singleId);
			
			// Only check those marked as Automated or Automated-Needs Update
			if (String.valueOf((long) thisCase.get("custom_executionmethod")).equals("1")
					|| String.valueOf((long) thisCase.get("custom_executionmethod")).equals("3")) {
				
				JSONArray runResults = (JSONArray) client.sendGet("get_results_for_case/"+runID+"/"+singleId+"&limit=1");
				//If runResults is null, then is Untested
				
				if (runResults == null) {
					//Reporter.log("** 	Untested: "+singleId, true);
					returnList.add(singleId);
				} else {
					if (runResults.size() == 0) {
						//Reporter.log("** 	Untested 2: "+singleId, true);
						returnList.add(singleId);
					} else {
						try {
							if (((JSONObject) runResults.get(0)).get("status_id").toString().equals("3")) {
								//Reporter.log("** 	Status 3: "+singleId, true);	
								returnList.add(singleId);
							}
						} catch (Exception e) {
							//Reporter.log("** EXCEPTION ID: "+singleId, true);
							returnList.add(singleId);
						}
					}
				}
			}
			Thread.sleep(waitTime);
		}
		//Reporter.log("NUMBER:  "+returnList.size(), true);
		return returnList;
	}
	
	
	/**
	 * Submit a Test Run Result to TestRail
	 * @param ID
	 * 		The ID (plan or run)
	 * @param planOrRun
	 * 		'plan' or 'run' for recording results
	 * @param runSearch
	 * 		The String to search for in Run Title
	 * @param testCases
	 * 		The list of test case IDs
	 * @param status
	 * 		The status to set
	 * @param comment
	 * 		Comment to set
	 * @param browser
	 * 		Browser in use
	 * @throws Exception
	 * 		throws Exception
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void addRunTestResult(String ID, String planOrRun, String runSearch, Long[] testCases, Integer status, String comment, String browser) throws Exception {
		Long runID;
		Map newResult = new HashMap();
		newResult.put("status_id", status);
		if (!browser.isEmpty()) {
			newResult.put("custom_browser", browser);
		}
		newResult.put("comment", comment);
				
		// Set Client
		setClient();
		
		// If PlanID, then need these steps to get plan, and plan entries(runs), find my run
		if (planOrRun.toLowerCase().equals("plan")) {
			// Get Plan
			JSONObject thisPlan = getPlan(ID);
			// Get Entries
			JSONArray thisEntries = getPlanEntries(thisPlan);
			// Get RunID
			runID = getRunID(thisEntries, runSearch);
		} else {
			runID = Long.parseLong(ID);
		}
			
		// For each Test String, get ID and record result
		for (Long thisTest : testCases) {
			// Update Result
			boolean foundMe = runTestCaseIDFound(runID, thisTest);
			if (foundMe) { // Only record if Test is Found.
				Reporter.log("***** TestRail ADD: "+planOrRun+":"+ID+" / test("+thisTest+") / "+runSearch+" / status("+status+") / comment("+comment+")", true);
				try {
					JSONObject resultResponse = updateTestResult(runID, thisTest, newResult);
				} catch (Exception e) {
					Reporter.log("***** TestRail API ERROR: "+e.getMessage(), true);
				}
			} else {
				Reporter.log("***** TestRail ERROR: Test Case ID "+thisTest+" NOT found in Run ID "+runID, true);
			}
			Thread.sleep(waitTime);
		}
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void addRunTestResult(String ID, String planOrRun, String runSearch, Long[] testCases, Integer status, String comment) throws Exception {
		Long runID;
		Map newResult = new HashMap();
		newResult.put("status_id", status);
		newResult.put("comment", comment);
				
		// Set Client
		setClient();
		
		// If PlanID, then need these steps to get plan, and plan entries(runs), find my run
		if (planOrRun.toLowerCase().equals("plan")) {
			// Get Plan
			JSONObject thisPlan = getPlan(ID);
			// Get Entries
			JSONArray thisEntries = getPlanEntries(thisPlan);
			// Get RunID
			runID = getRunID(thisEntries, runSearch);
		} else {
			runID = Long.parseLong(ID);
		}
			
		// For each Test String, get ID and record result
		for (Long thisTest : testCases) {
			// Update Result
			boolean foundMe = runTestCaseIDFound(runID, thisTest);
			if (foundMe) { // Only record if Test is Found.
				Reporter.log("***** TestRail ADD: "+planOrRun+":"+ID+" / test("+thisTest+") / "+runSearch+" / status("+status+") / comment("+comment+")", true);
				try {
					JSONObject resultResponse = updateTestResult(runID, thisTest, newResult);
				} catch (Exception e) {
					Reporter.log("***** TestRail API ERROR: "+e.getMessage(), true);
				}
			} else {
				Reporter.log("***** TestRail ERROR: Test Case ID "+thisTest+" NOT found in Run ID "+runID, true);
			}
			Thread.sleep(waitTime);
		}
	}

	public ArrayList<String> getRunTestIds(String runId) throws Exception {
		ArrayList<String> returnList = new ArrayList<String>();
		setClient();
		JSONArray run = (JSONArray) client.sendGet("get_tests/"+runId);
		for (int i=0; i<run.size(); i++) {
			JSONObject thisTest = (JSONObject) run.get(i);
			returnList.add(Long.toString((Long)thisTest.get("case_id")));
			Thread.sleep(waitTime);
		}
		return returnList;
	}
	
}
