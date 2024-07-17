package com.finalproject;

import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeClass;
import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

import org.json.JSONObject;
import org.testng.ITestContext;

import org.testng.annotations.DataProvider;

import io.restassured.response.Response;

import java.lang.reflect.Method;


public class loanAPITests {
	private JSONObject authPayload;
	
	@BeforeClass()
	public static void setupClass() {
		/*
		 * Set base URI 
		 */
		baseURI = "http://localhost:9000";
	}

	@BeforeMethod()
	public void setUp(Method method, Object[] testData) {
		/* 
		 * Setup method for TestNG Tests:
		 * 1. Extract 'email' and 'password' parameters from the testData array provided by DataProvider
		 * 2. Construct a new JSONObject 'authPayload' and populate it with 'email' and 'password', 
		 *    which are used for setting up API requests in the test methods in this class 
		 * 3. Print the name of the test method before execution, along with the associated 'email' value, 
		 *    to the console for traceability and debugging. 
		 */
		String email = (String) testData[0];
		String password = (String) testData[1];
		authPayload = new JSONObject();
		authPayload.put("email", email);
		authPayload.put("password", password);
		System.out.println("\n\n------" + method.getName() + " method for " + email + "------");
		
	}
	
	@Test(dataProvider = "datasource")
	public void postReqUserAuth(String email, String password, ITestContext context) {
		/*
		 * Send a POST request to api/authenticate containing email and password from authPayload
		 * (from setUp method)
		 */
		
		given() 
			.header("Content-Type", "application/json")
			.body(authPayload.toString())
//			.log().all()
		.when()
			.post("api/authenticate")
		.then()
			.assertThat()
			.statusCode(200)
			.body("code", equalTo(101))
			.body("message", equalTo(email + " authentication success"));
	}
	
	
	@Test(dataProvider = "datasource") 
	public void getCustomerInfo(String email, String password, ITestContext context) {
		/*
		 * Send a GET request to api/customer containing email address from dataProvider
		 */
		
		given()
			.queryParam("email", email)
//			.log().all()
		.when()
			.get("api/customer")
		.then()
			.assertThat()
			.statusCode(200)
			.body("code", equalTo(101))
			.body("message", equalTo(email + " fetch success"))
//			.log().all()
			;
	}
	
	@Test(dataProvider = "datasource")
	public void getCreditScore(String email, String password, ITestContext context) {
		/*
		 * Send a GET request to api/credit-score containing email address from dataProvider,
		 * validate the credit score value
		 */
		
		given() 
			.queryParam("email", email)
//			.log().all()
		.when()
			.get("api/credit-score")
		.then()
			.assertThat()
			.statusCode(200)
			.body("code", equalTo(101))
			.body("message", equalTo("Credit Score Fetched"))
			.body("email", equalTo(email))
			
			// Validate credit score value - must be between 300 and 850
			.body("creditScore.score", greaterThanOrEqualTo(300))
			.body("creditScore.score", lessThanOrEqualTo(850))
//			.log().all()
			;
	}
	
	@Test(dataProvider = "datasource")
	public void getLoanEligibility(String email, String password, ITestContext context) {
		/*
		 * Send a GET request to api/loan-eligibility containing email address from dataProvider
		 */
		given() 
			.queryParam("email", email)
//			.log().all()
		.when()
			.get("api/loan-eligibility")
		.then()
			.assertThat()
			.statusCode(200)
			.body("code", equalTo(101))
			.body("message", containsString("be given"))
			.body("email", equalTo(email))
//			.log().all()
			;
	}
	
	@Test(dataProvider = "datasource")
	public void getRiskAssessmentReport(String email, String password, ITestContext context) {
		/*
		 * Send a GET request to api/risk-report containing email address from dataProvider
		 */
		
		given() 
			.queryParam("email", email)
//			.log().all()
		.when()
			.get("api/risk-report")
		.then()
			.assertThat()
			.statusCode(200)
			.body("code", equalTo(101))
			.body("message", equalTo("Risk Report Fetched"))
			.body("email", equalTo(email))
			.body("riskReport", hasKey("riskFactors"))
			.body("riskReport", hasKey("recommendations"))
//			.log().all()
			;
	}
	
	@Test(dataProvider = "datasource")
	public void submitLoanApplication(String email, String password, ITestContext context) {
		/*
		 * Send a POST request to api/submit-loan-application containing email address from dataProvider
		 * and 2 hard-coded values.
		 * Validate response, and then extract application number from it. 
		 * Save the application number to the test context so it can be used in subsequent test methods.
		 * Validate response.
		 */
		
		JSONObject jdata = new JSONObject(); 
		jdata.put("customerEmail", email);
		jdata.put("requestedAmount", 100000);
		jdata.put("loanTerm", 6);
		
		Response response = given() 
			.header("Content-Type", "application/json")
			.body(jdata.toString())
//			.log().all()
		.when()
			.post("api/submit-loan-application")
//			.prettyPrint();
		.then()
//			.assertThat()
			.statusCode(200)
			.body("code", equalTo(101))
			.body("message", containsString("initiated for " + email))
//			.log().all()
			.extract().response();
		
		String message = response.path("message");
		String[] words = message.split(" ");
		int applicationNumber = Integer.parseInt(words[4]); 
		System.out.println("Application number: " + applicationNumber);

		context.setAttribute(email, applicationNumber); // Save the application number using the email as a key

	}
	
	@Test(dataProvider = "datasource", dependsOnMethods = {"submitLoanApplication"}) 
	public void approveLoanApplication(String email, String password, ITestContext context) {
		/*
		 * Send a PUT request to api/approve-loan-application containing email address from dataProvider
		 * and applicationNumber saved into test context in a previous method.
		 * Validate response. 
		 */
		
		int applicationNumber = (Integer) context.getAttribute(email);

	
		JSONObject jdata = new JSONObject(); 
		jdata.put("applicationID", applicationNumber);
		jdata.put("customerEmail", email);
		
		given() 
			.header("Content-Type", "application/json")
			.body(jdata.toString())
//			.log().all()
		.when()
			.put("api/approve-loan-application")
//			.prettyPrint();
		.then()
			.assertThat()
			.statusCode(200)
			.body("code", equalTo(101))
			.body("message", containsString("Loan Application with ID: " + applicationNumber + " Approved for " + email))
//			.log().all()
			;
	}
	
	@Test(dataProvider = "datasource", dependsOnMethods = {"submitLoanApplication"})
	public void rejectLoanApplication(String email, String password, ITestContext context) {
		/*
		 * Send a PUT request to api/reject-loan-application containing email address from dataProvider
		 * and applicationNumber saved into test context in a previous method.
		 * Validate response. 
		 * 
		 * Note: an application that has already been approved cannot be rejected, and vice-versa.
		 * Set up TestNG tests accordingly, e.g. if you want to reject an application, 
		 * either run the deleteLoanApplication method or run the rejectLoanApplication method 
		 * in a different test.  
		 */
		
		int applicationNumber = (Integer) context.getAttribute(email);
		
		JSONObject jdata = new JSONObject(); 
		jdata.put("applicationID", applicationNumber);
		jdata.put("customerEmail", email);
		
		given() 
			.header("Content-Type", "application/json")
			.body(jdata.toString())
//			.log().all()
		.when()
			.put("api/reject-loan-application")
//			.prettyPrint();
		.then()
			.assertThat()
			.statusCode(200)
			.body("code", equalTo(101))
			.body("message", containsString("Loan Application with ID: " + applicationNumber + " Rejected for " + email))
//			.log().all()
			;
	}
	
	
	@Test(dataProvider = "datasource", dependsOnMethods = {"submitLoanApplication"})
	public void pullLoanApplicationStatus(String email, String password, ITestContext context) {
		/*
		 * Send a POST request to api/get-loan-application containing email address from dataProvider
		 * and applicationNumber saved into test context in a previous method.
		 * Validate response. 
		 */		  
		
		int applicationNumber = (Integer) context.getAttribute(email);

		JSONObject jdata = new JSONObject(); 
		jdata.put("applicationID", applicationNumber);
		jdata.put("customerEmail", email);
		
		given() 
			.header("Content-Type", "application/json")
			.body(jdata.toString())
//			.log().all()
		.when()
			.post("api/get-loan-application")
//			.prettyPrint();
		.then()
			.assertThat()
			.statusCode(200)
			.body("code", equalTo(101))
			.body("message", containsString("Loan Application with ID: " + applicationNumber + " Fetched for " + email))
//			.log().all()
			;		
	}
	
	@Test(dataProvider = "datasource", dependsOnMethods = {"submitLoanApplication"})
	public void deleteLoanApplication(String email, String password, ITestContext context) {
		/*
		 * Send a DELETE request to api/delete-loan-application containing email address from dataProvider
		 * and applicationNumber saved into test context in a previous method.
		 * Validate response. 
		 */
		
		int applicationNumber = (Integer) context.getAttribute(email);

		JSONObject jdata = new JSONObject(); 
		jdata.put("applicationID", applicationNumber);
		jdata.put("customerEmail", email);
		
		given() 
			.header("Content-Type", "application/json")
			.body(jdata.toString())
//			.log().all()
		.when()
			.delete("api/delete-loan-application")
//			.prettyPrint();
		.then()
			.assertThat()
			.statusCode(200)
			.body("code", equalTo(101))
			.body("message", containsString("Loan Application with ID: " + applicationNumber + " Deleted for " + email))
//			.log().all()
			;		
	}
	
	
	
	@DataProvider
	public Object[][] datasource() {
		return new Object[][] { 
			new Object[] {"john@example.com", "john123"},
			new Object[] {"jane@example.com", "jane123"},
			new Object[] {"alice@example.com", "alice123"},
			new Object[] {"bob@example.com", "bob123"},
			new Object[] {"eva@example.com", "eva123"},
				};

		}
		
		
	}
