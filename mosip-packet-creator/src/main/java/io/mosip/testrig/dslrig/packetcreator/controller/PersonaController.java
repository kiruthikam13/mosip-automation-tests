package io.mosip.testrig.dslrig.packetcreator.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import io.mosip.testrig.dslrig.dataprovider.models.setup.MosipMachineModel;
import io.mosip.testrig.dslrig.dataprovider.util.DataProviderConstants;
import io.mosip.testrig.dslrig.dataprovider.util.RestClient;
import io.mosip.testrig.dslrig.dataprovider.variables.VariableManager;
import io.mosip.testrig.dslrig.packetcreator.dto.BioExceptionDto;
import io.mosip.testrig.dslrig.packetcreator.dto.MockABISExpectationsDto;
import io.mosip.testrig.dslrig.packetcreator.dto.PersonaRequestDto;
import io.mosip.testrig.dslrig.packetcreator.dto.UpdatePersonaDto;
import io.mosip.testrig.dslrig.packetcreator.service.PacketSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "PersonaController", description = "REST APIs for Persona management")
public class PersonaController {

	@Value("${mosip.test.persona.configpath}")
	private String personaConfigPath;

	private PacketSyncService packetSyncService;
	
	@Value("${mosip.test.persona.Angulipath}")
	private String personaAnguliPath;

	private static final Logger logger = LoggerFactory.getLogger(PersonaController.class);

	public PersonaController(@Lazy PacketSyncService packetSyncService) {
		this.packetSyncService = packetSyncService;
	}

	@Operation(summary = "Update the specified persona record with the provided attribute values")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Successfully updated the persona record") })
	@PutMapping(value = "/persona/{id}/{contextKey}")
	public @ResponseBody String updatePersonaData(@RequestBody List<UpdatePersonaDto> personaRequestDto,
			@PathVariable("id") String id, @PathVariable("contextKey") String contextKey) {
		try {
			if (personaConfigPath != null && !personaConfigPath.equals("")) {
				DataProviderConstants.RESOURCE = personaConfigPath;
			}
			VariableManager.Init(contextKey);

			return packetSyncService.updatePersonaData(personaRequestDto, contextKey);

		} catch (Exception ex) {
			logger.error("updatePersonaData", ex);
		}
		return "{Failed}";

	}
	
	@Operation(summary = "Update the persona data with UIN/RID")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Resident data is successfully updated") })
	@PostMapping(value = "/updateresident/{contextKey}")
	public @ResponseBody String updateResidentData(@RequestBody PersonaRequestDto personaRequestDto,
			// @PathVariable("id") int id,
			@RequestParam(name = "UIN", required = false) String uin,
			@RequestParam(name = "RID", required = false) String rid, @PathVariable("contextKey") String contextKey) {

		try {
			if (personaConfigPath != null && !personaConfigPath.equals("")) {
				DataProviderConstants.RESOURCE = personaConfigPath;
			}
			// String uin = "";
			// String rid = "1234567890";
			return packetSyncService.updateResidentData(personaRequestDto.getRequests(), uin, rid, contextKey);

		} catch (Exception ex) {
			logger.error("registerResident", ex);
		}
		return "{Failed}";

	}

	@Operation(summary = "Update the specified persona record with the provided biometric exceptions")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Successfully updated the persona record") })
	@PutMapping(value = "/persona/bioexceptions/{contextKey}")
	public @ResponseBody String updatePersonaBioExceptions(@RequestBody BioExceptionDto personaBERequestDto,
			// @PathVariable("id") String id,
			@PathVariable("contextKey") String contextKey) {
		try {
			if (personaConfigPath != null && !personaConfigPath.equals("")) {
				DataProviderConstants.RESOURCE = personaConfigPath;
			}
			return packetSyncService.updatePersonaBioExceptions(personaBERequestDto, contextKey);

		} catch (Exception ex) {
			logger.error("updatePersonaData", ex);
		}
		return "{Failed}";

	}

	/*
	 * @Operation(summary =
	 * "Create a persona record based on the provided specifications")
	 * 
	 * @ApiResponses(value = {
	 * 
	 * @ApiResponse(responseCode = "200", description =
	 * "Successfully created the persona record") })
	 * 
	 * @PostMapping(value = "/persona/{contextKey}") public @ResponseBody String
	 * generateResidentData(@RequestBody PersonaRequestDto residentRequestDto,
	 * 
	 * @PathVariable("contextKey") String contextKey) {
	 * 
	 * try { logger.info("Persona Config Path=" + personaConfigPath); if
	 * (personaConfigPath != null && !personaConfigPath.equals("")) {
	 * DataProviderConstants.RESOURCE = personaConfigPath; } if (personaAnguliPath
	 * != null && !personaAnguliPath.equals("")) { DataProviderConstants.ANGULI_PATH
	 * = personaAnguliPath; } logger.info("personaAnguliPath =" +
	 * DataProviderConstants.ANGULI_PATH);
	 * 
	 * logger.info("Resource Path=" + DataProviderConstants.RESOURCE); logger.info(
	 * "DOC_Template Path=" + DataProviderConstants.RESOURCE +
	 * DataProviderConstants.DOC_TEMPLATE_PATH);
	 * 
	 * // clear all tokens // VariableManager.setVariableValue("urlSwitched",
	 * "true");
	 * 
	 * return packetSyncService.generateResidentData(residentRequestDto,
	 * contextKey).toString();
	 * 
	 * } catch (Exception ex) { logger.error("generateResidentData", ex); } return
	 * "{Failed}"; }
	 */
	
	@Operation(summary = "Generate the resident data")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Successfully generated the resident data") })
	@PostMapping(value = "/persona/generate/{contextKey}")
	public @ResponseBody String generateResidentData(@RequestBody PersonaRequestDto residentRequestDto,
			 @PathVariable("contextKey") String contextKey) {

		try {
			RestClient.logInfo(contextKey, "Persona Config Path=" + personaConfigPath);
			if (personaConfigPath != null && !personaConfigPath.equals("")) {
				DataProviderConstants.RESOURCE = personaConfigPath;
			}
			if (personaAnguliPath != null && !personaAnguliPath.equals("")) {
				DataProviderConstants.ANGULI_PATH = personaAnguliPath;
			}
			RestClient.logInfo(contextKey, "personaAnguliPath =" + DataProviderConstants.ANGULI_PATH);

			RestClient.logInfo(contextKey, "Resource Path=" + DataProviderConstants.RESOURCE);
			// logger.info("DOC_Template Path="+
			// VariableManager.getVariableValue(contextKey,"mosip.test.persona.documentsdatapath").toString());

			// clear all tokens
			// VariableManager.setVariableValue("urlSwitched", "true");

			return packetSyncService.generateResidentData(residentRequestDto, contextKey).toString();

		} catch (Exception ex) {
			logger.error("generateResidentData", ex);
			return "{\"" + ex.getMessage() + "\"}";
		}
	}

	@Operation(summary = "Retrieve specified attribute values from the given persona record")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Persona record retrived successfully") })
	@GetMapping(value = "/persona/{contextKey}")
	public @ResponseBody String getPersonaData(@RequestBody List<UpdatePersonaDto> personaRequestDto,
			@PathVariable("contextKey") String contextKey) {
		try {
			if (personaConfigPath != null && !personaConfigPath.equals("")) {
				DataProviderConstants.RESOURCE = personaConfigPath;
			}
			return packetSyncService.getPersonaData(personaRequestDto, contextKey);

		} catch (Exception ex) {
			logger.error("getPersonaData", ex);
		}
		return "{Failed}";

	}

	@Operation(summary = "Extended API to set Persona specific expectations in mock ABIS")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Successfully created the specific expectations in mock ABIS") })
	@PostMapping(value = "/persona/mockabis/v2/expectations/{contextKey}")
	public @ResponseBody String setPersonaMockABISExpectationV2(@RequestBody List<MockABISExpectationsDto> expectations,
			@PathVariable("contextKey") String contextKey) {

		try {
			if (personaConfigPath != null && !personaConfigPath.equals("")) {
				DataProviderConstants.RESOURCE = personaConfigPath;
			}
			return packetSyncService.setPersonaMockABISExpectation(expectations, contextKey);

		} catch (Exception ex) {
			logger.error("setPersonaMockABISExpectation", ex);
		}
		return "{Failed}";

	}

	@Operation(summary = "Delete expectation for a given Id")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Successfully deleted") })
	@DeleteMapping(value = "persona/mock-abis-service/config/expectation/{contextKey}")
	public @ResponseBody String deleteExpectations(@PathVariable("contextKey") String contextKey) {

		return packetSyncService.deleteMockAbisExpectations(contextKey);

	}

	/*
	 * @Operation(summary = "Update the machine details")
	 * 
	 * @ApiResponses(value = {
	 * 
	 * @ApiResponse(responseCode = "200", description =
	 * "Successfully updated the machine details") })
	 * 
	 * @PutMapping(value = "/updateMachine/{contextKey}") public @ResponseBody
	 * String updateMachine(@RequestBody MosipMachineModel machine,
	 * 
	 * @PathVariable("contextKey") String contextKey) { try { if (personaConfigPath
	 * != null && !personaConfigPath.equals("")) { DataProviderConstants.RESOURCE =
	 * personaConfigPath; } return packetSyncService.updateMachine(machine,
	 * contextKey);
	 * 
	 * } catch (Exception ex) { logger.error("updateMachine", ex); } return
	 * "{Failed}"; }
	 */

}
