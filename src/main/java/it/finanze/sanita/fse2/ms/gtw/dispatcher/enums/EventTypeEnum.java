/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package it.finanze.sanita.fse2.ms.gtw.dispatcher.enums;

import lombok.Getter;

public enum EventTypeEnum {

	VALIDATION("VALIDATION"),
	PUBLICATION("PUBLICATION"),
	FHIR_MAPPING("FHIR_MAPPING"),
	REPLACE("REPLACE"),
	FEEDING("FEEDING"),
	DELETE("DELETE"),
	GENERIC_ERROR("Generic error from dispatcher");

	@Getter
	private String name;

	private EventTypeEnum(String inName) {
		name = inName;
	}

}
