package io.mosip.testrig.dslrig.packetcreator.dto;
import java.util.List;

import lombok.Data;

@Data
public class PreRegisterRequestDto {
	
	private List<String> personaFilePath;
	private String additionalInfoReqId;
	private boolean getRidFromSync;

}