/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package it.finanze.sanita.fse2.ms.gtw.dispatcher.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.OperationalContextEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.RegionCodeEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.RoleEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;


/**
 * 	Metadata user INI.
 */
@Data
@EqualsAndHashCode(callSuper=true)
public class MetadataUserDTO extends AbstractDTO {

	/**
	 * Serial version uid.
	 */
	private static final long serialVersionUID = -3879191629857977974L;

	@Schema(description = "Identificativo")
	private final String identificativo;

	@Schema(description = "Ruolo")
	private final RoleEnum ruolo;

	@Schema(description = "Struttura")
	private final String struttura;

	@Schema(description = "Identificativo organizzazione")
	private final RegionCodeEnum idOrganizzazione;

	@Schema(description = "Contesto operativo")
	private final OperationalContextEnum contestoOperativo;

}
