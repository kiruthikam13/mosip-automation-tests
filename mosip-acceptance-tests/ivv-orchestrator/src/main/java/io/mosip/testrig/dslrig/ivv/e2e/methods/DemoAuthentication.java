package io.mosip.testrig.dslrig.ivv.e2e.methods;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import io.mosip.testrig.apirig.utils.AdminTestException;
import io.mosip.testrig.apirig.dto.TestCaseDTO;
import io.mosip.testrig.apirig.testrunner.JsonPrecondtion;
import io.mosip.testrig.apirig.utils.AuthenticationTestException;
import io.mosip.testrig.apirig.testrunner.BaseTestCase;
import io.mosip.testrig.apirig.auth.testscripts.DemoAuth;
import io.mosip.testrig.dslrig.ivv.core.base.StepInterface;
import io.mosip.testrig.dslrig.ivv.core.exceptions.RigInternalError;
import io.mosip.testrig.dslrig.ivv.e2e.constant.E2EConstants;
import io.mosip.testrig.dslrig.ivv.orchestrator.BaseTestCaseUtil;
import io.mosip.testrig.dslrig.ivv.orchestrator.dslConfigManager;

public class DemoAuthentication extends BaseTestCaseUtil implements StepInterface {
	static Logger logger = Logger.getLogger(DemoAuthentication.class);
	private static final String DEMOPATH = "idaData/DemoAuth/DemoAuth.yml";

	DemoAuth demoAuth = new DemoAuth();

	static {
		if (dslConfigManager.IsDebugEnabled())
			logger.setLevel(Level.ALL);
		else
			logger.setLevel(Level.ERROR);
	}

	@Override
	public void run() throws RigInternalError {
		String demofields = null;
		List<String> demofieldList = null;
		String uins = null;
		String vids = null;
		List<String> uinList = null;
		List<String> vidList = null;
		String demoResponse = null;
		String addressResponse = null;
		String _personFilePath = null;
		Object[] casesListUIN = null;
		List<String> idType = BaseTestCase.getSupportedIdTypesValueFromActuator();
		Object[] casesListVID = null;
		String updateAgeFlag = null;

		if (step.getParameters().isEmpty() || step.getParameters().size() < 1) {
			logger.error("Parameter is  missing from DSL step");
			this.hasError = true;
			throw new RigInternalError("Modality paramter is  missing in step: " + step.getName());
		} else {
			demofields = step.getParameters().get(0);
			if (!StringUtils.isBlank(demofields))
				demofieldList = new ArrayList<>(Arrays.asList(demofields.split("@@")));

		}
		if (step.getParameters().size() == 2) {
			uins = step.getParameters().get(1);
			if (!StringUtils.isBlank(uins))
				uinList = new ArrayList<>(Arrays.asList(uins.split("@@")));
		} else if (step.getParameters().size() > 2) {
			uins = step.getParameters().get(1);
			_personFilePath = step.getParameters().get(2);
			if (uins.startsWith("$$") && _personFilePath.startsWith("$$")) {
				uins = step.getScenario().getVariables().get(uins);
				_personFilePath = step.getScenario().getVariables().get(_personFilePath);
				uinList = new ArrayList<>(Arrays.asList(uins.split("@@")));
			}
		} else
			uinList = new ArrayList<>(step.getScenario().getUinPersonaProp().stringPropertyNames());

		if (step.getParameters().size() == 2) {
			vids = step.getParameters().get(1);
			if (!StringUtils.isBlank(vids))
				vidList = new ArrayList<>(Arrays.asList(vids.split("@@")));
		} else if (step.getParameters().size() > 2) {
			vids = step.getParameters().get(3);
			_personFilePath = step.getParameters().get(2);
			if (vids.startsWith("$$") && _personFilePath.startsWith("$$")) {
				vids = step.getScenario().getVariables().get(vids);
				_personFilePath = step.getScenario().getVariables().get(_personFilePath);
				vidList = new ArrayList<>(Arrays.asList(vids.split("@@")));
			}
		} else
			vidList = new ArrayList<>(step.getScenario().getVidPersonaProp().stringPropertyNames());

		if (step.getParameters().size() > 4)
			updateAgeFlag = step.getParameters().get(4);

		for (String uin : uinList) {
			String personFilePathvalue = null;

			if (step.getParameters().size() > 2) {
				personFilePathvalue = _personFilePath;
			} else if (step.getScenario().getUinPersonaProp().containsKey(uin))
				personFilePathvalue = step.getScenario().getUinPersonaProp().getProperty(uin);
			else {
				this.hasError = true;
				throw new RigInternalError("Persona doesn't exist for the given UIN " + uin);
			}

			List<String> demoFetchList = new ArrayList<String>();
			demoFetchList.add(E2EConstants.DEMOFETCH);
			demoResponse = packetUtility.retrieveBiometric(personFilePathvalue, demoFetchList, step);
			List<String> addressFetchList = new ArrayList<String>();
			addressFetchList.add(E2EConstants.DEMOADDRESSFETCH);
			addressResponse = packetUtility.retrieveBiometric(personFilePathvalue, addressFetchList, step);
			Object[] testObj = demoAuth.getYmlTestData(DEMOPATH);
			TestCaseDTO test = (TestCaseDTO) testObj[0];
			String input = test.getInput();
			input = JsonPrecondtion.parseAndReturnJsonContent(input, uin, "individualId");
			JSONObject inputJson = new JSONObject(input);

			for (String demoField : demofieldList) {
				String demoFieldValueKey = null;
				String demoValue = null;

				switch (demoField) {
				case E2EConstants.DEMOAGE:
					demoFieldValueKey = E2EConstants.DEMODOB;
					demoValue = JsonPrecondtion.getValueFromJson(demoResponse,
							E2EConstants.DEMOFETCH + "." + demoFieldValueKey); // array fill all the values

					if (updateAgeFlag != null && updateAgeFlag.contains("ageDecrease")) {
						demoValue = decreaseAge(demoValue);
					} else {
						DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
						LocalDate birthDate = LocalDate.parse(demoValue, formatter);
						LocalDate currentDate = LocalDate.now();

						demoValue = String.valueOf(ChronoUnit.YEARS.between(birthDate, currentDate));
					}

					if (demoValue == null) {
						this.hasError = true;
						throw new RigInternalError(

								"Unable to get the Demo value for field " + demoField + " from Persona");
					}
					inputJson.getJSONObject("identityRequest").put(demoField, demoValue);
					break;

				case E2EConstants.DEMOEMAIL:
					demoFieldValueKey = E2EConstants.DEMOEMAIL;
					demoValue = JsonPrecondtion.getValueFromJson(demoResponse,
							E2EConstants.DEMOFETCH + "." + demoFieldValueKey); // array fill all the values
					if (demoValue == null) {
						this.hasError = true;
						throw new RigInternalError(
								"Unable to get the Demo value for field " + demoField + " from Persona");
					}
					inputJson.getJSONObject("identityRequest").put(demoField, demoValue);
					break;

				case E2EConstants.DEMOYMLPHONE:
					demoFieldValueKey = E2EConstants.DEMOPHONE;
					demoValue = JsonPrecondtion.getValueFromJson(demoResponse,
							E2EConstants.DEMOFETCH + "." + demoFieldValueKey); // array fill all the values
					if (demoValue == null) {
						this.hasError = true;
						throw new RigInternalError(
								"Unable to get the Demo value for field " + demoField + " from Persona");
					}
					inputJson.getJSONObject("identityRequest").put(demoField, demoValue);
					break;

				case E2EConstants.DEMOADDRESSFETCH:
					String addLine1 = null, addLine2 = null, addLine3 = null;
					try {
						addLine1 = JsonPrecondtion.JsonObjSimpleParsing(addressResponse, E2EConstants.DEMOADDRESSFETCH,
								E2EConstants.DEMOADDRESSLINE1);
						addLine2 = JsonPrecondtion.JsonObjSimpleParsing(addressResponse, E2EConstants.DEMOADDRESSFETCH,
								E2EConstants.DEMOADDRESSLINE2);
						addLine3 = JsonPrecondtion.JsonObjSimpleParsing(addressResponse, E2EConstants.DEMOADDRESSFETCH,
								E2EConstants.DEMOADDRESSLINE3);
					} catch (Exception e) {
						this.hasError = true;
						throw new RigInternalError(e.getMessage());
					}
					if (addLine1 == null || addLine2 == null || addLine3 == null) {
						this.hasError = true;
						throw new RigInternalError(

								"Unable to get the Demo value for field " + demoField + " from Persona");
					}
					JSONArray addressLine1Array = new JSONArray();
					JSONObject addressLine1Obj = new JSONObject();
					addressLine1Obj.put("language", BaseTestCase.getLanguageList().get(0));
					addressLine1Obj.put("value", addLine1);
					addressLine1Array.put(addressLine1Obj);
					inputJson.put(E2EConstants.DEMOADDRESSLINE1, addressLine1Array);

					JSONArray addressLine2Array = new JSONArray();
					JSONObject addressLine2Obj = new JSONObject();
					addressLine2Obj.put("language", BaseTestCase.getLanguageList().get(0));
					addressLine2Obj.put("value", addLine2);
					addressLine2Array.put(addressLine2Obj);
					inputJson.put(E2EConstants.DEMOADDRESSLINE2, addressLine2Array);

					JSONArray addressLine3Array = new JSONArray();
					JSONObject addressLine3Obj = new JSONObject();
					addressLine3Obj.put("language", BaseTestCase.getLanguageList().get(0));
					addressLine3Obj.put("value", addLine3);
					addressLine3Array.put(addressLine3Obj);
					inputJson.getJSONObject("identityRequest").put(E2EConstants.DEMOADDRESSLINE3, addressLine3Array);
					break;
				case E2EConstants.DEMONAME:

					String firstNm = null, midNm = null, lastNm = null, fullname = null;
					firstNm = JsonPrecondtion.getValueFromJson(demoResponse,
							E2EConstants.DEMOFETCH + "." + E2EConstants.DEMOFNAME);
					midNm = JsonPrecondtion.getValueFromJson(demoResponse,
							E2EConstants.DEMOFETCH + "." + E2EConstants.DEMOMNAME);
					lastNm = JsonPrecondtion.getValueFromJson(demoResponse,
							E2EConstants.DEMOFETCH + "." + E2EConstants.DEMOLNAME);
					if (firstNm == null || midNm == null || lastNm == null) {
						this.hasError = true;
						throw new RigInternalError(
								"Unable to get the Demo value for field " + demoField + " from Persona");
					}
					fullname = firstNm + " " + midNm + " " + lastNm;
					JSONArray nameArray = new JSONArray();
					JSONObject nameObj = new JSONObject();
					nameObj.put("language", BaseTestCase.getLanguageList().get(0));
					nameObj.put("value", fullname);
					nameArray.put(nameObj);
					inputJson.getJSONObject("identityRequest").put(demoField, nameArray);
					break;

				case E2EConstants.DEMOGENDER:
					demoFieldValueKey = E2EConstants.DEMOGENDER;
					demoValue = JsonPrecondtion.getValueFromJson(demoResponse,
							E2EConstants.DEMOFETCH + "." + demoFieldValueKey); // array fill all the values
					JSONArray genArray = new JSONArray();
					JSONObject genderObj = new JSONObject();
					genderObj.put("language", BaseTestCase.getLanguageList().get(0));
					genderObj.put("value", demoValue);
					genArray.put(genderObj);
					if (demoValue == null) {
						this.hasError = true;
						throw new RigInternalError(
								"Unable to get the Demo value for field " + demoField + " from Persona");
					}
					inputJson.getJSONObject("identityRequest").put(demoField, genArray);
					break;

				default:
					throw new RigInternalError("Given DEMO doesn't match with the options in the script");
				}
			}

			if (BaseTestCase.getSupportedIdTypesValueFromActuator().contains("UIN")
					|| BaseTestCase.getSupportedIdTypesValueFromActuator().contains("uin")) {

				casesListUIN = demoAuth.getYmlTestData(DEMOPATH);

			}

			else if (BaseTestCase.getSupportedIdTypesValueFromActuator().contains("VID")
					|| BaseTestCase.getSupportedIdTypesValueFromActuator().contains("vid")) {
				casesListVID = demoAuth.getYmlTestData(DEMOPATH);
			}

			else {
				casesListUIN = demoAuth.getYmlTestData(DEMOPATH);
				casesListVID = demoAuth.getYmlTestData(DEMOPATH);
			}

			// inputJson.put("identityRequest", identityReqJson.toString());

			if (idType.contains("UIN") || idType.contains("uin")) {
				casesListUIN = demoAuth.getYmlTestData(DEMOPATH);
			}

			if (casesListUIN != null) {
				for (Object object : casesListUIN) {
					test = (TestCaseDTO) object;
					test.setInput(inputJson.toString());
					try {
						demoAuth.test(test);
					} catch (AuthenticationTestException | AdminTestException e) {
						logger.error(e.getMessage());
						this.hasError = true;
						throw new RigInternalError(e.getMessage());
					}
				}
			}

		}

		for (String vid : vidList) {
			String personFilePathvalue = null;

			if (step.getParameters().size() > 2) {
				personFilePathvalue = _personFilePath;
			} else if (step.getScenario().getVidPersonaProp().containsKey(vid))
				personFilePathvalue = step.getScenario().getVidPersonaProp().getProperty(vid);
			else {

				this.hasError = true;
				throw new RigInternalError("Persona doesn't exist for the given UIN " + vid);
			}

			List<String> demoFetchList = new ArrayList<String>();
			demoFetchList.add(E2EConstants.DEMOFETCH);
			demoResponse = packetUtility.retrieveBiometric(personFilePathvalue, demoFetchList, step);
			List<String> addressFetchList = new ArrayList<String>();
			addressFetchList.add(E2EConstants.DEMOADDRESSFETCH);
			addressResponse = packetUtility.retrieveBiometric(personFilePathvalue, addressFetchList, step);
			Object[] testObj = demoAuth.getYmlTestData(DEMOPATH);
			TestCaseDTO test = (TestCaseDTO) testObj[0];
			String input = test.getInput();
			input = JsonPrecondtion.parseAndReturnJsonContent(input, vid, "individualId");
			input = JsonPrecondtion.parseAndReturnJsonContent(input, "VID", "individualIdType");
			JSONObject inputJson = new JSONObject(input);
			for (String demoField : demofieldList) {
				String demoFieldValueKey = null;
				String demoValue = null;
				int age = 0;

				switch (demoField) {
				case E2EConstants.DEMOAGE:
					demoFieldValueKey = E2EConstants.DEMODOB;
					demoValue = JsonPrecondtion.getValueFromJson(demoResponse,
							E2EConstants.DEMOFETCH + "." + demoFieldValueKey); // array fill all the values

					if (updateAgeFlag != null && updateAgeFlag.contains("ageDecrease")) {
						demoValue = decreaseAge(demoValue);
					} else {
						DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
						LocalDate birthDate = LocalDate.parse(demoValue, formatter);
						LocalDate currentDate = LocalDate.now();

						demoValue = String.valueOf(ChronoUnit.YEARS.between(birthDate, currentDate));
					}
					if (demoValue == null) {

						this.hasError = true;
						throw new RigInternalError(
								"Unable to get the Demo value for field " + demoField + " from Persona");
					}
					inputJson.getJSONObject("identityRequest").put(demoField, demoValue);
					break;

				case E2EConstants.DEMOEMAIL:
					demoFieldValueKey = E2EConstants.DEMOEMAIL;
					demoValue = JsonPrecondtion.getValueFromJson(demoResponse,
							E2EConstants.DEMOFETCH + "." + demoFieldValueKey); // array fill all the values
					if (demoValue == null) {
						this.hasError = true;
						throw new RigInternalError(
								"Unable to get the Demo value for field " + demoField + " from Persona");
					}
					inputJson.getJSONObject("identityRequest").put(demoField, demoValue);
					break;

				case E2EConstants.DEMOYMLPHONE:
					demoFieldValueKey = E2EConstants.DEMOPHONE;
					demoValue = JsonPrecondtion.getValueFromJson(demoResponse,
							E2EConstants.DEMOFETCH + "." + demoFieldValueKey); // array fill all the values
					if (demoValue == null) {
						this.hasError = true;
						throw new RigInternalError(
								"Unable to get the Demo value for field " + demoField + " from Persona");
					}
					inputJson.getJSONObject("identityRequest").put(demoField, demoValue);
					break;

				case E2EConstants.DEMOADDRESSFETCH:
					String addLine1 = null, addLine2 = null, addLine3 = null;
					try {
						addLine1 = JsonPrecondtion.JsonObjSimpleParsing(addressResponse, E2EConstants.DEMOADDRESSFETCH,
								E2EConstants.DEMOADDRESSLINE1);
						addLine2 = JsonPrecondtion.JsonObjSimpleParsing(addressResponse, E2EConstants.DEMOADDRESSFETCH,
								E2EConstants.DEMOADDRESSLINE2);
						addLine3 = JsonPrecondtion.JsonObjSimpleParsing(addressResponse, E2EConstants.DEMOADDRESSFETCH,
								E2EConstants.DEMOADDRESSLINE3);
					} catch (Exception e) {
						this.hasError = true;
						throw new RigInternalError(e.getMessage());
					}
					if (addLine1 == null || addLine2 == null || addLine3 == null) {
						this.hasError = true;
						throw new RigInternalError(
								"Unable to get the Demo value for field " + demoField + " from Persona");
					}
					JSONArray addressLine1Array = new JSONArray();
					JSONObject addressLine1Obj = new JSONObject();
					addressLine1Obj.put("language", BaseTestCase.getLanguageList().get(0));
					addressLine1Obj.put("value", addLine1);
					addressLine1Array.put(addressLine1Obj);
					inputJson.put(E2EConstants.DEMOADDRESSLINE1, addressLine1Array);

					JSONArray addressLine2Array = new JSONArray();
					JSONObject addressLine2Obj = new JSONObject();
					addressLine2Obj.put("language", BaseTestCase.getLanguageList().get(0));
					addressLine2Obj.put("value", addLine2);
					addressLine2Array.put(addressLine2Obj);
					inputJson.put(E2EConstants.DEMOADDRESSLINE2, addressLine2Array);

					JSONArray addressLine3Array = new JSONArray();
					JSONObject addressLine3Obj = new JSONObject();
					addressLine3Obj.put("language", BaseTestCase.getLanguageList().get(0));
					addressLine3Obj.put("value", addLine3);
					addressLine3Array.put(addressLine3Obj);
					inputJson.getJSONObject("identityRequest").put(E2EConstants.DEMOADDRESSLINE3, addressLine3Array);
					break;
				case E2EConstants.DEMONAME:

					String firstNm = null, midNm = null, lastNm = null, fullname = null;
					firstNm = JsonPrecondtion.getValueFromJson(demoResponse,
							E2EConstants.DEMOFETCH + "." + E2EConstants.DEMOFNAME);
					midNm = JsonPrecondtion.getValueFromJson(demoResponse,
							E2EConstants.DEMOFETCH + "." + E2EConstants.DEMOMNAME);
					lastNm = JsonPrecondtion.getValueFromJson(demoResponse,
							E2EConstants.DEMOFETCH + "." + E2EConstants.DEMOLNAME);
					if (firstNm == null || midNm == null || lastNm == null) {
						this.hasError = true;
						throw new RigInternalError(
								"Unable to get the Demo value for field " + demoField + " from Persona");
					}
					fullname = firstNm + " " + midNm + " " + lastNm;
					JSONArray nameArray = new JSONArray();
					JSONObject nameObj = new JSONObject();
					nameObj.put("language", BaseTestCase.getLanguageList().get(0));
					nameObj.put("value", fullname);
					nameArray.put(nameObj);
					inputJson.getJSONObject("identityRequest").put(demoField, nameArray);
					break;

				case E2EConstants.DEMOGENDER:
					demoFieldValueKey = E2EConstants.DEMOGENDER;
					demoValue = JsonPrecondtion.getValueFromJson(demoResponse,
							E2EConstants.DEMOFETCH + "." + demoFieldValueKey); // array fill all the values
					JSONArray genArray = new JSONArray();
					JSONObject genderObj = new JSONObject();
					genderObj.put("language", BaseTestCase.getLanguageList().get(0));
					genderObj.put("value", demoValue);
					genArray.put(genderObj);
					if (demoValue == null) {
						this.hasError = true;
						throw new RigInternalError(
								"Unable to get the Demo value for field " + demoField + " from Persona");
					}
					inputJson.getJSONObject("identityRequest").put(demoField, genArray);
					break;

				default:
					throw new RigInternalError("Given DEMO doesn't match with the options in the script");
				}
			}

			if (BaseTestCase.getSupportedIdTypesValueFromActuator().contains("UIN")
					|| BaseTestCase.getSupportedIdTypesValueFromActuator().contains("uin")) {

				casesListUIN = demoAuth.getYmlTestData(DEMOPATH);

			}

			else if (BaseTestCase.getSupportedIdTypesValueFromActuator().contains("VID")
					|| BaseTestCase.getSupportedIdTypesValueFromActuator().contains("vid")) {
				casesListVID = demoAuth.getYmlTestData(DEMOPATH);
			}

			else {
				casesListUIN = demoAuth.getYmlTestData(DEMOPATH);
				casesListVID = demoAuth.getYmlTestData(DEMOPATH);
			}

			// inputJson.put("identityRequest", identityReqJson.toString());

			if (idType.contains("VID") || idType.contains("vid")) {
				casesListVID = demoAuth.getYmlTestData(DEMOPATH);
			}

			if (casesListVID != null) {
				for (Object object : casesListVID) {
					test = (TestCaseDTO) object;
					test.setInput(inputJson.toString());

					try {
						demoAuth.test(test);
					} catch (AuthenticationTestException | AdminTestException e) {
						logger.error(e.getMessage());
						this.hasError = true;
						throw new RigInternalError(e.getMessage());
					}
				}
			}

		}

	}

	public static String decreaseAge(String dateString) {
		// Assuming dateString comes in the format "YYYY/MM/DD"
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
		LocalDate birthDate = LocalDate.parse(dateString, formatter);
		LocalDate currentDate = LocalDate.now();

		long currentAge = ChronoUnit.YEARS.between(birthDate, currentDate);

		// If the age is less than 1 year, we cannot decrease it
		if (currentAge < 1) {
			return "0"; // Return "0" as a string
		}
		Random random = new Random();
		int yearsToSubtract = random.nextInt((int) currentAge) + 1; // Random number between 1 and currentAge
		int decreasedAge = (int) currentAge - yearsToSubtract;
		return String.valueOf(decreasedAge);
	}

	public static String increaseAge(String dateString) {

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
		LocalDate birthDate = LocalDate.parse(dateString, formatter);
		Random random = new Random();
		int yearsToAdd = random.nextInt(10) + 1;
		LocalDate newDate = birthDate.plusYears(yearsToAdd);
		LocalDate currentDate = LocalDate.now();
		long newAge = ChronoUnit.YEARS.between(newDate, currentDate);
		return String.valueOf(newAge);
	}

}