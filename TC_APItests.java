package com.qaproject.testcases.api;

import org.openqa.selenium.lift.Matchers;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import com.qaproject.util.TestBase;
import com.qaproject.util.TestUtil;

import io.restassured.RestAssured;
import io.restassured.builder.ResponseBuilder;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;

import com.google.common.base.Verify;
import com.qaproject.apiObjects.*;
import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import org.testng.asserts.SoftAssert;

@Test(groups ={"NetezzaToBigQueryTestSuite", "TeradataToHiveTestSuite", "TeradataToBigQueryTestSuite", "NetezzaToHiveTestSuite"})
public class TC_ValidateResultAPIs extends TestBase {

	private SoftAssert softAssert = new SoftAssert();
	HashSet getTestDataIntoHashSet = new HashSet();
	private static ValidatableResponse validatableresponse=null;
	private static Response response=null;
	private static CreateMappingObjects mapobj = null;
	String uniqueTableMapping_Id=null;
	String mapping_Id=null;
	String mapping_Name=null;
	private static Map<String,String> dataprovidermap = null;
	private static String request_Id=null;
	String data_set=null;
	String dataset_sheet=null;
	
	@Parameters({"dataset", "datasetsheet"})
	@BeforeClass
	public void beforeClass(String dataset, String datasetsheet) throws IOException {
	initConfig();	
	RestAssured.baseURI=config.getProperty("api_baseuri");
	RestAssured.basePath=config.getProperty("api_basepath");	
	data_set=dataset;
	System.out.println("Dataset row name for the test is "+data_set);
	dataset_sheet=datasetsheet;
	System.out.println("Datasetsheet name for the test is "+dataset_sheet);	
	}
	
	@Test
	public void getAuthTokenTest() {
	createAuthAccessToken();
	System.out.println("auth token in test is -> "+access_token);
	Assert.assertNotNull(access_token);
	}
	
	
	@Test(dataProvider="TC_ValidateResultAPIs", priority=1)
	public void getTestDataIntoHashSet(Hashtable<String,String> testdataArray) {
		
		if(!TestUtil.isExecutable("TC_ValidateResultAPIs", xls))	{
			throw new SkipException("Skipping the Test");
		}	
		//add the hashtables into hashset
		System.out.println(testdataArray);
		getTestDataIntoHashSet.add(testdataArray);
		
	}
	
	@Test(priority=2, dependsOnMethods="getTestDataIntoHashSet")
	public void validateMappingAPI() {
		
		if(!TestUtil.isExecutable("TC_ValidateResultAPIs", xls))	{
			throw new SkipException("Skipping the Test");
		}			
		
		dataprovidermap = (Map)getTestDataIntoHashSet.toArray()[0];
		System.out.println("(Map)getTestDataIntoHashSet.toArray()[0] -> " + dataprovidermap.get("expressionName"));
		
		//create json request body only once for a table	
		CreateMappingObjects mapobj = new CreateMappingObjects();
		mapobj.setSourceDatastoreName(dataprovidermap.get("sourceDatastoreName"));
		mapobj.setSourceDatabaseName(dataprovidermap.get("sourceDatabaseName"));
		mapobj.setSourceTableName(dataprovidermap.get("sourceTableName"));
		mapobj.setTargetDatastoreName(dataprovidermap.get("targetDatastoreName"));
		mapobj.setTargetDatabaseName(dataprovidermap.get("targetDatabaseName"));
		mapobj.setTargetTableName(dataprovidermap.get("targetTableName"));
		mapobj.setPersistMapping(dataprovidermap.get("persistMapping"));
		mapobj.setUniqueKeyColumn(dataprovidermap.get("uniqueKeyColumn"));

	
		 if((dataprovidermap.get("persistMapping").equalsIgnoreCase("TRUE") && 
				dataprovidermap.get("howTableMapped").equalsIgnoreCase("partial") &&
				dataprovidermap.get("saveAsNewMapping").equalsIgnoreCase("FALSE")) ||
				
				(dataprovidermap.get("persistMapping").equalsIgnoreCase("FALSE") && 
				dataprovidermap.get("howTableMapped").equalsIgnoreCase("full") &&
				dataprovidermap.get("saveAsNewMapping").equalsIgnoreCase("TRUE")) ||
				
				(dataprovidermap.get("persistMapping").equalsIgnoreCase("FALSE") && 
				dataprovidermap.get("howTableMapped").equalsIgnoreCase("partial") &&
				dataprovidermap.get("saveAsNewMapping").equalsIgnoreCase("TRUE"))	
				) {
			
			//Mapping api
			response=
			given()
			.contentType(ContentType.JSON)
			.auth()
			.oauth2(access_token)
			.when()
			.body(mapobj)
			.post("/mapping")
			;	

			System.out.println("Mapping did not persist already for the partial mapped Table "+dataprovidermap.get("sourceTableName")+" , and return code is 200 for new non-persisting mapping api");
			
		}
	
		
		 else if((dataprovidermap.get("persistMapping").equalsIgnoreCase("TRUE") && 
					dataprovidermap.get("howTableMapped").equalsIgnoreCase("full") &&
					dataprovidermap.get("saveAsNewMapping").equalsIgnoreCase("FALSE")) ||
				 
					(dataprovidermap.get("persistMapping").equalsIgnoreCase("TRUE") && 
					dataprovidermap.get("howTableMapped").equalsIgnoreCase("full") &&
					dataprovidermap.get("saveAsNewMapping").equalsIgnoreCase("TRUE")) 	
					) {
			
		//Get the mapping api response
			 response=	given()
						.contentType(ContentType.JSON)
						.auth()
						.oauth2(access_token)
						.when()
						.body(mapobj)
						.post("/mapping")						
						;	
		
			 
			 uniqueTableMapping_Id = response.jsonPath().getString("uniqueTableMappingId");
			 Assert.assertNotNull(uniqueTableMapping_Id);			
			 System.out.println("Mapping ID not persist already for the fully mapped Table "+dataprovidermap.get("sourceTableName")+" , and return code is 200 for newly persisted mappings");	

		}		
		
		else {
			System.out.println("Test data not found");
		}
		
	}
	
	
	
	@Test(priority=3, dependsOnMethods="validateMappingAPI")
	public void validateEditMappingAPI() {
		
		if(!TestUtil.isExecutable("TC_ValidateResultAPIs", xls))	{
			throw new SkipException("Skipping the Test");
		}			
		
		dataprovidermap = (Map)getTestDataIntoHashSet.toArray()[0];
		System.out.println("in validateEditMappingAPI test");
		
		
		//Save mapping scenario
		if((dataprovidermap.get("persistMapping").equalsIgnoreCase("TRUE") && 
				dataprovidermap.get("howTableMapped").equalsIgnoreCase("partial") &&
				dataprovidermap.get("saveAsNewMapping").equalsIgnoreCase("FALSE"))	
				) {
			
			String preeditmapping = "{\"saveAsNewMapping\": false, \"qaprojectMappingResult\":";
			System.out.println(response.getBody().asString());
		//	JsonPath js = new JsonPath(validatableresponse.toString());
			
			
			//Edit Mapping api	
			validatableresponse=
			given()
			.contentType(ContentType.JSON)
			.auth()
			.oauth2(access_token)
			.when()
			.body(preeditmapping+response.getBody().asString()+"}")
			.post("/mapping/edit")
			.then()
			.log()
			.all()
			.statusCode(200)
			.body("mappingId", notNullValue())
			.body("mappingName", notNullValue())
			;	
			
			System.out.println("Save mapping scenario of edit mapping api test passed.... ");
		
			mapping_Id=validatableresponse.extract().path("mappingId");
			mapping_Name=validatableresponse.extract().path("mappingName");
			
		}
	
	
		
		//Save mapping scenarios
		else if(	
						(dataprovidermap.get("persistMapping").equalsIgnoreCase("FALSE") && 
						dataprovidermap.get("howTableMapped").equalsIgnoreCase("full") &&
						dataprovidermap.get("saveAsNewMapping").equalsIgnoreCase("TRUE")) ||
						
						(dataprovidermap.get("persistMapping").equalsIgnoreCase("FALSE") && 
						dataprovidermap.get("howTableMapped").equalsIgnoreCase("partial") &&
						dataprovidermap.get("saveAsNewMapping").equalsIgnoreCase("TRUE"))	
						) {
					
					String preeditmapping = "{\"saveAsNewMapping\": true, \"qaprojectMappingResult\":";
					System.out.println(response.getBody().asString());
				//	JsonPath js = new JsonPath(validatableresponse.toString());
					
					
					//Edit Mapping api
					
					
					validatableresponse=
					given()
					.contentType(ContentType.JSON)
					.auth()
					.oauth2(access_token)
					.when()
					.body(preeditmapping+response.getBody().asString()+"}")
					.post("/mapping/edit")
					.then()
					.log()
					.all()
					.statusCode(200)
					.body("mappingId", notNullValue())
					.body("mappingName", notNullValue())
					;	
					
					System.out.println("Save mapping scenario of edit mapping api test passed.... ");
				
					mapping_Id=validatableresponse.extract().path("mappingId");
					mapping_Name=validatableresponse.extract().path("mappingName");
					
				}
			
		
		//Edit mapping scenario
		else if(dataprovidermap.get("persistMapping").equalsIgnoreCase("TRUE") && 
				dataprovidermap.get("howTableMapped").equalsIgnoreCase("full") &&
				dataprovidermap.get("saveAsNewMapping").equalsIgnoreCase("FALSE")) {
			
			String preeditmapping = "{\"saveAsNewMapping\": false, \"qaprojectMappingResult\":";
			System.out.println(response.getBody().asString());
		//	JsonPath js = new JsonPath(validatableresponse.toString());
			
			//Edit Mapping api			
			validatableresponse=
			given()
			.contentType(ContentType.JSON)
			.auth()
			.oauth2(access_token)
			.when()
			.body(preeditmapping+response.getBody().asString()+"}")
			.post("/mapping/edit")
			.then()
			.log()
			.all()
			.statusCode(200)
			.body("mappingId", notNullValue())
			.body("mappingName", notNullValue())
			;	

			//Edit mapping mapping id and name to be used further
			mapping_Id=validatableresponse.extract().path("mappingId").toString();
			mapping_Name=validatableresponse.extract().path("mappingName").toString();
			
			//Compare mapping id from mapping api mapping id
			Assert.assertEquals(mapping_Id, uniqueTableMapping_Id);
			
			System.out.println("Edit mapping scenario of edit mapping api test passed... ");
		}
				
		//Clone mapping scenario
		else if(dataprovidermap.get("persistMapping").equalsIgnoreCase("TRUE") && 
				dataprovidermap.get("howTableMapped").equalsIgnoreCase("full") &&
				dataprovidermap.get("saveAsNewMapping").equalsIgnoreCase("TRUE")) {
			
			String preeditmapping = "{\"saveAsNewMapping\": true, \"qaprojectMappingResult\":";
			System.out.println(response.getBody().asString());
		//	JsonPath js = new JsonPath(validatableresponse.toString());
			
			
			//Edit Mapping api		
			validatableresponse=
			given()
			.contentType(ContentType.JSON)
			.auth()
			.oauth2(access_token)
			.when()
			.body(preeditmapping+response.getBody().asString()+"}")
			.post("/mapping/edit")
			.then()
			.log()
			.all()
			.statusCode(200)
			.body("mappingId", notNullValue())
			.body("mappingName", notNullValue())
			;	

	
			//Edit mapping mapping id and name to be used further
			mapping_Id=validatableresponse.extract().path("mappingId").toString();
			mapping_Name=validatableresponse.extract().path("mappingName").toString();
			

			//Compare mapping id from mapping api mapping id
			validatableresponse.body("mappingId", not(uniqueTableMapping_Id));
			
			System.out.println("\n"+"Clone mapping scenario of edit mapping api test passed ......");
			
		}
		
		else {
			System.out.println("Test data not found");
		}
		
	}
	
	
	
	@Test(priority=4, dependsOnMethods="validateEditMappingAPI")
	public void validateCompareTableAPI() {
		
		if(!TestUtil.isExecutable("TC_ValidateResultAPIs", xls))	{
			throw new SkipException("Skipping the Test");
		}			
		
		System.out.println("\n"+"In compare test......."+dataprovidermap.get("validationpackage"));
		System.out.println("\n"+"mapping id in compare test ..."+mapping_Id);

	if(mapping_Id != null) {		
		String compareapibody = "{\"tableMappingId\":\""+mapping_Id+"\", \"validationpackage\":\""+dataprovidermap.get("validationpackage")+"\"}";
		System.out.println("compareapibody "+compareapibody);
		
		given()
		.contentType(ContentType.JSON)
		.auth()
		.oauth2(access_token)
	//	.queryParams("tableMappingId", mapping_Id, "validationpackage", dataprovidermap.get("validationpackage"))
		.body(compareapibody)
		.when()
		.post("/compare")
		.then()
		.statusCode(202)
		.log()
		.all()
		.body("content", startsWith("requestId="))
		.body("links[0].href", startsWith("http://10.200.99.201:8200/api/rest/v2/qaproject/validationresult"))
		;
			
		System.out.print("\n"+"Compare table api test passed...");
		
		request_Id=given()
		.contentType(ContentType.JSON)
		.auth()
		.oauth2(access_token)
	//	.queryParams("tableMappingId", mapping_Id, "validationpackage", dataprovidermap.get("validationpackage"))
		.body(compareapibody)
		.when()
		.post("/compare")
		.then()
		.extract()
		.path("content")
		.toString()
		.substring(10, 55)
		;
		
		System.out.println("\n"+"Request id is -> "+request_Id);
	}

	else {
		System.out.println("????????????????????????????????????????????????????????????????????????????????????????????????????????????????????");	
		System.out.println("Since mapping_id found null for the given table- "+dataprovidermap.get("sourceTableName")+" ,so TC_ValidateResultAPIs tests are BLOCKED"
					+ ", and remaing tests should be marked NOT COMPLETE !!");
		System.out.println("????????????????????????????????????????????????????????????????????????????????????????????????????????????????????");
		}
	}
		
	
	
	@Test(priority=5, dependsOnMethods="validateCompareTableAPI")
	public void validateResultAPI() throws InterruptedException {
		
		if(!TestUtil.isExecutable("TC_ValidateResultAPIs", xls))	{
			throw new SkipException("Skipping the Test");
		}			
		
		
		System.out.println("\n"+"In validate result test");
		
		String status1;
		do {

				status1=given()
				.contentType(ContentType.JSON)
				.auth()
				.oauth2(access_token)
				.when()
				.get("/validationresult/"+request_Id)
				.then()
				.log()
				.all()
				.statusCode(200)
				.extract()
				.path("status")
				.toString();
				;	
				System.out.println("Result API status is running, so putting some wait............");
				TimeUnit.SECONDS.sleep(10);
		}
		while(status1.equalsIgnoreCase("Running"));
			
		System.out.print("Validate result API status code is 200 as expected...");	
		
		
				validatableresponse=given()
				.contentType(ContentType.JSON)
				.auth()
				.oauth2(access_token)
				.when()
				.get("/validationresult/"+request_Id)
				.then()
				;
				
				
				ArrayList<Map<String,String>> qaprojectResultProperties_array=
						validatableresponse
						.extract()
						.path("qaprojectExecutionDetails[0].qaprojectResults[1].qaprojectResultProperties")
						;
				
if(validatableresponse.extract().path("qaprojectExecutionDetails[0].status").equals("Failed")) {
					
	if(validatableresponse.extract().path("qaprojectExecutionDetails[0].qaprojectResults[0].status").equals("Successful")) {
		validatableresponse
		.body("qaprojectExecutionDetails[0].qaprojectResults[0].resultType", equalTo("DATA TRANSFER RESULT"))
		.body("qaprojectExecutionDetails[0].qaprojectResults[0]", hasEntry("status", "Successful"));			
		
		if(validatableresponse.extract().path("qaprojectExecutionDetails[0].qaprojectResults[1].status").equals("Failed")
				&& (dataprovidermap.get("validationpackage").equalsIgnoreCase("QUICK") || dataprovidermap.get("validationpackage").equalsIgnoreCase("FAST"))
				) {
			validatableresponse
			.body("qaprojectExecutionDetails[0].qaprojectResults[1].resultType", equalTo("VALIDATION RESULT"))
			.body("qaprojectExecutionDetails[0].qaprojectResults[1]", hasEntry("status", "Failed"))
			.body("qaprojectExecutionDetails[0].qaprojectResults[1].qaprojectResultProperties[0]", hasEntry("keyName", "FullMismatchData"))
			.body("qaprojectExecutionDetails[0].qaprojectResults[1].qaprojectResultProperties[0].referenceValue", startsWith(config.getProperty("temporary_database")+".source_"+dataprovidermap.get("sourceTableName")))
			.body("qaprojectExecutionDetails[0].qaprojectResults[1].qaprojectResultProperties[0].destinationValue", startsWith(config.getProperty("temporary_database")+".target_"+dataprovidermap.get("targetTableName")))
			;
			
			//Get array index for keyName=MismatchedRowCount
			int k=1;
			if(dataprovidermap.get("keyNameMISMATCH").equalsIgnoreCase("TRUE")) {
				k=k+1;
			};
				if(dataprovidermap.get("keyNameEXTRA").equalsIgnoreCase("TRUE")) {
					k=k+1;
				};		
					if(dataprovidermap.get("keyNameMISSING").equalsIgnoreCase("TRUE")) {
						k=k+1;
					};
				
			
			
					//Validate total mismatch count
					validatableresponse
					.body("qaprojectExecutionDetails[0].qaprojectResults[1].qaprojectResultProperties["+k+"]", hasEntry("keyName", "MismatchedRowCount"),
					      "qaprojectExecutionDetails[0].qaprojectResults[1].qaprojectResultProperties["+k+"]", hasEntry("referenceValue", "0"),				
					      "qaprojectExecutionDetails[0].qaprojectResults[1].qaprojectResultProperties["+k+"]", hasEntry("destinationValue", dataprovidermap.get("valueMismatchedRowCount").substring(0, dataprovidermap.get("valueMismatchedRowCount").length()-2)));
					
					
					for(int i=0; i< qaprojectResultProperties_array.size(); i++) {
						System.out.println("===========For loop starts for qaprojectResultProperties with index - "+i+"=========================");
						
						
						//Validate mismatch type
						if(validatableresponse.extract().path("qaprojectExecutionDetails[0].qaprojectResults[1].qaprojectResultProperties["+i+"].keyName").toString().equalsIgnoreCase("MISMATCH")
							&&	dataprovidermap.get("keyNameMISMATCH").equalsIgnoreCase("TRUE")) {
						validatableresponse
						.body("qaprojectExecutionDetails[0].qaprojectResults[1].qaprojectResultProperties["+i+"]", hasEntry("keyName", "MISMATCH"),
						      "qaprojectExecutionDetails[0].qaprojectResults[1].qaprojectResultProperties["+i+"]", hasEntry("referenceValue", "0"),				
						      "qaprojectExecutionDetails[0].qaprojectResults[1].qaprojectResultProperties["+i+"]", hasEntry("destinationValue", dataprovidermap.get("valueMISMATCH").substring(0, dataprovidermap.get("valueMISMATCH").length()-2)));
						System.out.println("MISMATCH record found under 'data validation' results");
						}
						else{System.out.println("No MISMATCH record found under 'data validation' results");}
						if(validatableresponse.extract().path("qaprojectExecutionDetails[0].qaprojectResults[1].qaprojectResultProperties["+i+"].keyName").toString().equalsIgnoreCase("EXTRA")
							&& dataprovidermap.get("keyNameEXTRA").equalsIgnoreCase("TRUE")) {
						validatableresponse	
						.body("qaprojectExecutionDetails[0].qaprojectResults[1].qaprojectResultProperties["+i+"]", hasEntry("keyName", "EXTRA"),
						      "qaprojectExecutionDetails[0].qaprojectResults[1].qaprojectResultProperties["+i+"]", hasEntry("referenceValue", "0"),				
						      "qaprojectExecutionDetails[0].qaprojectResults[1].qaprojectResultProperties["+i+"]", hasEntry("destinationValue", dataprovidermap.get("valueEXTRA").substring(0, dataprovidermap.get("valueEXTRA").length()-2)));	
						System.out.println("EXTRA record found under 'data validation' results");
						}
						else{System.out.println("No EXTRA record found under 'data validation' results");}
						if(validatableresponse.extract().path("qaprojectExecutionDetails[0].qaprojectResults[1].qaprojectResultProperties["+i+"].keyName").toString().equalsIgnoreCase("MISSING")
							&& dataprovidermap.get("keyNameMISSING").equalsIgnoreCase("TRUE")) {
						validatableresponse	
						.body("qaprojectExecutionDetails[0].qaprojectResults[1].qaprojectResultProperties["+i+"]", hasEntry("keyName", "MISSING"),
						      "qaprojectExecutionDetails[0].qaprojectResults[1].qaprojectResultProperties["+i+"]", hasEntry("referenceValue", "0"),				
						      "qaprojectExecutionDetails[0].qaprojectResults[1].qaprojectResultProperties["+i+"]", hasEntry("destinationValue", dataprovidermap.get("valueMISSING").substring(0, dataprovidermap.get("valueMISSING").length()-2)));	
						System.out.println("MISSING record found under 'data validation' results");
						}
						else{System.out.println("No MISSING record found under 'data validation' results");}


			}			
		}
		
	
		
		else if(validatableresponse.extract().path("qaprojectExecutionDetails[0].qaprojectResults[1].status").equals("Failed")
				&& (dataprovidermap.get("validationpackage").equalsIgnoreCase("SWIFT") || dataprovidermap.get("validationpackage").equalsIgnoreCase("RAPID"))
				) {
			validatableresponse
			.body("qaprojectExecutionDetails[0].qaprojectResults[1].resultType", equalTo("VALIDATION RESULT"))
			.body("qaprojectExecutionDetails[0].qaprojectResults[1]", hasEntry("status", "Failed"))
			.body("qaprojectExecutionDetails[0].qaprojectResults[1].description", 
			equalTo("Data validation failed.Source rowcount:"+dataprovidermap.get("Sourcerowcount").substring(0, dataprovidermap.get("Sourcerowcount").length()-2)+" Target Rowcount:"
			+dataprovidermap.get("TargetRowcount").substring(0, dataprovidermap.get("TargetRowcount").length()-2)))
			;						
		}

	
		
		
		
		else{System.out.println("\n"+"'Validation result' is 'successful'. Congratulations, there is no difference found in data");}	
	}	
	else{System.out.println("\n"+"'Data transfer' itself failed. So there is no data to validate...........!!");
	System.out.println(validatableresponse.extract().path("qaprojectExecutionDetails[0].qaprojectResults[0].description").toString());
	validatableresponse
	.body("qaprojectExecutionDetails[0].qaprojectResults[0].description", equalTo("Successful"));
    
	}	
}
else {
System.out.println("\n"+"qaprojectExecutionDetails status is 'Successful', congratulations!, 'data transfer' and 'data validation' results are 'successful'");	
}	
;
				
				
System.out.print("\n"+"Validate result test passed finally..."+"\n");			
		
		
	}
	
	
	
	@Test(priority=6, dependsOnMethods="validateResultAPI")
	public void validateGetMappingAPI() {
		
		if(!TestUtil.isExecutable("TC_ValidateResultAPIs", xls))	{
			throw new SkipException("Skipping the Test");
		}			
		
		System.out.println("\n"+"In validateGetMappingAPI test.......");
		
		
		given()
		.contentType(ContentType.JSON)
		.auth()
		.oauth2(access_token)
		.queryParams("targetDsName", dataprovidermap.get("targetDatastoreName"), "targetDbName", dataprovidermap.get("targetDatabaseName"), "targetTableName", dataprovidermap.get("targetTableName"))
		.when()
		.get("/mapping/"+dataprovidermap.get("sourceDatastoreName")+"/"+dataprovidermap.get("sourceDatabaseName")+"/"+dataprovidermap.get("sourceTableName"))
		.then()
		.statusCode(200)
		.log()
		.all()
		.body("mappingId", notNullValue())
		.body("mappingName", notNullValue())
		;
			
		System.out.print("\n"+"validateGetMappingAPI test passed...");
		
		
	}
	
	
	@Test(priority=7, dependsOnMethods="validateGetMappingAPI")
	public void validateGetMappingDetailsAPI() {
		
		if(!TestUtil.isExecutable("TC_ValidateResultAPIs", xls))	{
			throw new SkipException("Skipping the Test");
		}			
		
		System.out.println("\n"+"In validateGetMappingDeatilsAPI test.......");
		
		
		given()
		.contentType(ContentType.JSON)
		.auth()
		.oauth2(access_token)
	//	.queryParams("targetDsName", dataprovidermap.get("targetDatastoreName"), "targetDbName", dataprovidermap.get("targetDatabaseName"), "targetTableName", dataprovidermap.get("targetTableName"))
		.when()
		.get("/mapping/"+mapping_Id)
		.then()
		.statusCode(200)
		.log()
		.all()
		.body("uniqueTableMappingId", notNullValue())
		;
			
		System.out.print("\n"+"validateGetMappingDetailsAPI test passed...");	
	}
	
	
	@Test(priority=8, dependsOnMethods="validateGetMappingDetailsAPI")
	public void validateGetExecutionResultsAPI() {
		
		if(!TestUtil.isExecutable("TC_ValidateResultAPIs", xls))	{
			throw new SkipException("Skipping the Test");
		}			
		
		System.out.println("\n"+"In validateGetExecutionResultsAPI test.......");
		
		
		given()
		.contentType(ContentType.JSON)
		.auth()
		.oauth2(access_token)
	//	.queryParams("targetDsName", dataprovidermap.get("targetDatastoreName"), "targetDbName", dataprovidermap.get("targetDatabaseName"), "targetTableName", dataprovidermap.get("targetTableName"))
		.when()
		.get("/executiondetails?executioncount=1&needsample=true&page=1&size=10")
		.then()
		.statusCode(200)
		.log()
		.all()
	//	.body("uniqueTableMappingId", notNullValue())
		;
			
		System.out.print("\n"+"validateGetExecutionResultsAPI test passed...");	
	}
	
	
	
	@Test(priority=9, dependsOnMethods="validateGetExecutionResultsAPI")
	public void validateGetComparisonResultsByMappingIdAPI() {
		
		if(!TestUtil.isExecutable("TC_ValidateResultAPIs", xls))	{
			throw new SkipException("Skipping the Test");
		}			
		
		System.out.println("\n"+"In validateGetComparisonResultsByMappingIdAPI test.......");
		
		
		given()
		.contentType(ContentType.JSON)
		.auth()
		.oauth2(access_token)
	//	.queryParams("targetDsName", dataprovidermap.get("targetDatastoreName"), "targetDbName", dataprovidermap.get("targetDatabaseName"), "targetTableName", dataprovidermap.get("targetTableName"))
		.when()
		.get("/validationresult/mappingid/"+mapping_Id)
		.then()
		.statusCode(200)
		.log()
		.all()
	//	.body("uniqueTableMappingId", notNullValue())
		;
			
		System.out.print("\n"+"validateGetComparisonResultsByMappingIdAPI test passed...");	
	}
	

	
	@Test(priority=10, dependsOnMethods="validateGetComparisonResultsByMappingIdAPI")
	public void deleteMappingIdAPI() {
		
		if(!TestUtil.isExecutable("TC_ValidateResultAPIs", xls))	{
			throw new SkipException("Skipping the Test");
		}			
		
		System.out.println("\n"+" In deleteMappingIdAPI test.......");
		
		
		given()
		.contentType(ContentType.JSON)
		.auth()
		.oauth2(access_token)
	//	.queryParams("targetDsName", dataprovidermap.get("targetDatastoreName"), "targetDbName", dataprovidermap.get("targetDatabaseName"), "targetTableName", dataprovidermap.get("targetTableName"))
		.when()
		.delete("/mapping/"+mapping_Id+"?force=true")
		.then()
		.statusCode(200)
		.log()
		.all()
		.body("tableMappingId", notNullValue())
		.body("deletionStatus", equalTo("Success"))
		;
			
		System.out.print("\n"+"deleteMappingIdAPI test passed..."+"\n");
		System.out.print("\n"+"=============================================================================================="+"\n");
		System.out.print("\n"+"All the tests of ResultAPIs are executed for the table..."+dataprovidermap.get("sourceTableName")+"\n");	
		System.out.print("\n"+"=============================================================================================="+"\n");
	}
	
	
	
	@DataProvider(parallel = true)
	public Object[][] TC_ValidateResultAPIs(){
		return TestUtil.getData(data_set, dataset_sheet, xls);
	}
	
}
