/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package it.finanze.sanita.fse2.ms.gtw.dispatcher.controller.impl;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request.*;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.*;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.google.gson.Gson;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.client.IEdsClient;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.client.IIniClient;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.Constants;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.ValidationCFG;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.controller.IPublicationCTL;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.IndexerValueDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.JWTPayloadDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.JWTTokenDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.ResourceDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.ValidationCreationInputDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.ValidationDataDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.DestinationTypeEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.ErrorInstanceEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.EventStatusEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.OperationLogEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.PriorityTypeEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.ProcessorOperationEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.RestExecutionResultEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.ResultLogEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.BusinessException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.ConnectionRefusedException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.EdsException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.IniException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.MockEnabledException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.ValidationException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.logging.LoggerHelper;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.IDocumentReferenceSRV;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.IErrorHandlerSRV;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.IKafkaSRV;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.facade.ICdaFacadeSRV;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.impl.IniEdsInvocationSRV;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.CdaUtility;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.CfUtility;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.DateUtility;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.ProfileUtility;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.StringUtility;
import lombok.extern.slf4j.Slf4j;

import static it.finanze.sanita.fse2.ms.gtw.dispatcher.config.Constants.*;
import static it.finanze.sanita.fse2.ms.gtw.dispatcher.config.Constants.App.*;
import static it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.EventStatusEnum.*;
import static it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.StringUtility.*;

/**
 *
 * @author CPIERASC
 *
 *  Publication controller.
 */
@Slf4j
@RestController
public class PublicationCTL extends AbstractCTL implements IPublicationCTL {
	/**
	 * Serial version uid.
	 */
	private static final long serialVersionUID = 1711126466058952723L;

	@Autowired
	private IKafkaSRV kafkaSRV;

	@Autowired
	private IDocumentReferenceSRV documentReferenceSRV;

	@Autowired
	private IniEdsInvocationSRV iniInvocationSRV;

	@Autowired
	private transient LoggerHelper logger;

	@Autowired
	private ICdaFacadeSRV cdaSRV;

	@Autowired
	private transient IErrorHandlerSRV errorHandlerSRV;
		
	@Autowired
	private IIniClient iniClient;
	
	@Autowired
	private IEdsClient edsClient;

	@Autowired
	private ProfileUtility profileUtils;
	
	@Autowired
	private ValidationCFG validationCFG;

	@Override
	public ResponseEntity<PublicationResDTO> create(final PublicationCreationReqDTO requestBody, final MultipartFile file, final HttpServletRequest request) {
		final Date startDateOperation = new Date();
		final LogTraceInfoDTO traceInfoDTO = getLogTraceInfo();

		ValidationCreationInputDTO validationInfo = new ValidationCreationInputDTO();
		validationInfo.setValidationData(new ValidationDataDTO(null, false, MISSING_WORKFLOW_PLACEHOLDER, null, null, new Date()));

		String role = Constants.App.JWT_MISSING_SUBJECT_ROLE;
		String subjectFiscalCode = Constants.App.JWT_MISSING_SUBJECT;
		try {
			validationInfo = validateInput(file, request, false,traceInfoDTO);

			if (validationInfo.getValidationError() != null) {
				throw validationInfo.getValidationError();
			}

			iniInvocationSRV.insert(validationInfo.getValidationData().getWorkflowInstanceId(), validationInfo.getFhirResource(), validationInfo.getJwtToken());
			
			PriorityTypeEnum priorityType = PriorityTypeEnum.NULL;
			if (validationInfo.getJsonObj().getPriorita() != null) {
				priorityType = Boolean.TRUE.equals(validationInfo.getJsonObj().getPriorita()) ? PriorityTypeEnum.HIGH : PriorityTypeEnum.LOW;
			}

			final IndexerValueDTO kafkaValue = new IndexerValueDTO();
			kafkaValue.setWorkflowInstanceId(validationInfo.getValidationData().getWorkflowInstanceId());
			kafkaValue.setIdDoc(validationInfo.getJsonObj().getIdentificativoDoc());
			kafkaValue.setEdsDPOperation(ProcessorOperationEnum.PUBLISH);

			kafkaSRV.notifyChannel(validationInfo.getKafkaKey(), new Gson().toJson(kafkaValue), priorityType, validationInfo.getJsonObj().getTipoDocumentoLivAlto(), DestinationTypeEnum.INDEXER);
			kafkaSRV.sendPublicationStatus(traceInfoDTO.getTraceID(), validationInfo.getValidationData().getWorkflowInstanceId(), SUCCESS, null, validationInfo.getJsonObj(), validationInfo.getJwtToken() != null ? validationInfo.getJwtToken().getPayload() : null);
			
			role = validationInfo.getJwtToken().getPayload().getSubject_role();
			subjectFiscalCode = CfUtility.extractFiscalCodeFromJwtSub(validationInfo.getJwtToken().getPayload().getSub());
			logger.info(String.format("Publication CDA completed for workflow instance id %s", validationInfo.getValidationData().getWorkflowInstanceId()), OperationLogEnum.PUB_CDA2, ResultLogEnum.OK, startDateOperation, validationInfo.getJwtToken().getPayload().getIss(), CdaUtility.getDocumentType(validationInfo.getDocument()), role, subjectFiscalCode);
		} catch (ConnectionRefusedException ce) {
			errorHandlerSRV.connectionRefusedExceptionHandler(startDateOperation, validationInfo.getValidationData(), validationInfo.getJwtToken(), validationInfo.getJsonObj(), traceInfoDTO, ce, true, CdaUtility.getDocumentType(validationInfo.getDocument()));
		} catch (final ValidationException e) {
			errorHandlerSRV.publicationValidationExceptionHandler(startDateOperation, validationInfo.getValidationData(), validationInfo.getJwtToken(), validationInfo.getJsonObj(), traceInfoDTO, e, true, CdaUtility.getDocumentType(validationInfo.getDocument()));
		}

		String warning = null;
		
		if (validationInfo.getJsonObj().getMode() == null) {
			warning = Misc.WARN_EXTRACTION_SELECTION;
		}
		
		return new ResponseEntity<>(new PublicationResDTO(traceInfoDTO, warning, validationInfo.getValidationData().getWorkflowInstanceId()), HttpStatus.CREATED);
	}

	@Override
	public ResponseEntity<PublicationResDTO> replace(final String idDoc, final PublicationUpdateReqDTO requestBody, final MultipartFile file, final HttpServletRequest request) {
		
			final Date startDateOperation = new Date();
			final LogTraceInfoDTO traceInfoDTO = getLogTraceInfo();

			ValidationCreationInputDTO validationInfo = new ValidationCreationInputDTO();
			validationInfo.setValidationData(new ValidationDataDTO(null, false, MISSING_WORKFLOW_PLACEHOLDER, null, null, new Date()));

			String role = Constants.App.JWT_MISSING_SUBJECT_ROLE;
			String subjectFiscalCode = Constants.App.JWT_MISSING_SUBJECT;
			try {
				validationInfo = validateInput(file, request, true,traceInfoDTO);

				if (validationInfo.getValidationError() != null) {
					throw validationInfo.getValidationError();
				}
				
				IniReferenceRequestDTO iniReq = new IniReferenceRequestDTO(idDoc, validationInfo.getJwtToken().getPayload());
				IniReferenceResponseDTO response = iniClient.getReference(iniReq);
				
				kafkaSRV.sendReplaceStatus(traceInfoDTO.getTraceID(), validationInfo.getValidationData().getWorkflowInstanceId(), SUCCESS, null, validationInfo.getJsonObj(), validationInfo.getJwtToken() != null ? validationInfo.getJwtToken().getPayload() : null);
				
				if(!isNullOrEmpty(response.getErrorMessage())) {
					log.error("Errore. Nessun riferimento trovato.");
					throw new IniException("Errore. Nessun riferimento trovato.");
				}
				

				log.debug("Executing replace of document: {}", idDoc);
				iniInvocationSRV.replace(validationInfo.getValidationData().getWorkflowInstanceId(), validationInfo.getFhirResource(), validationInfo.getJwtToken(), idDoc);
				
				final IndexerValueDTO kafkaValue = new IndexerValueDTO();
				kafkaValue.setWorkflowInstanceId(validationInfo.getValidationData().getWorkflowInstanceId());
				kafkaValue.setIdDoc(idDoc);
				kafkaValue.setEdsDPOperation(ProcessorOperationEnum.REPLACE);
				
				kafkaSRV.notifyChannel(validationInfo.getKafkaKey(), new Gson().toJson(kafkaValue), PriorityTypeEnum.LOW, validationInfo.getJsonObj().getTipoDocumentoLivAlto(), DestinationTypeEnum.INDEXER);
				kafkaSRV.sendReplaceStatus(traceInfoDTO.getTraceID(), validationInfo.getValidationData().getWorkflowInstanceId(), SUCCESS, null, validationInfo.getJsonObj(), validationInfo.getJwtToken() != null ? validationInfo.getJwtToken().getPayload() : null);

				role = validationInfo.getJwtToken().getPayload().getSubject_role();
				subjectFiscalCode = CfUtility.extractFiscalCodeFromJwtSub(validationInfo.getJwtToken().getPayload().getSub());

				logger.info(String.format("Replace CDA completed for workflow instance id %s", validationInfo.getValidationData().getWorkflowInstanceId()), OperationLogEnum.REPLACE_CDA2, ResultLogEnum.OK, startDateOperation, validationInfo.getJwtToken().getPayload().getIss(), CdaUtility.getDocumentType(validationInfo.getDocument()), role, subjectFiscalCode);
			} catch (ConnectionRefusedException ce) {
				errorHandlerSRV.connectionRefusedExceptionHandler(startDateOperation, validationInfo.getValidationData(), validationInfo.getJwtToken(), validationInfo.getJsonObj(), traceInfoDTO, ce, false, CdaUtility.getDocumentType(validationInfo.getDocument()));
			} catch (final ValidationException e) {
				errorHandlerSRV.publicationValidationExceptionHandler(startDateOperation, validationInfo.getValidationData(), validationInfo.getJwtToken(), validationInfo.getJsonObj(), traceInfoDTO, e, false, CdaUtility.getDocumentType(validationInfo.getDocument()));
			}
	
			String warning = null;
			
			if (validationInfo.getJsonObj().getMode() == null) {
				warning = Misc.WARN_EXTRACTION_SELECTION;
			}
			
			return new ResponseEntity<>(new PublicationResDTO(traceInfoDTO, warning, validationInfo.getValidationData().getWorkflowInstanceId()), HttpStatus.OK);
	}


	@Override
	public ResponseEntity<ResponseDTO> updateMetadata(final String idDoc, final PublicationMetadataReqDTO requestBody, final HttpServletRequest request) {
		
		final boolean isTestEnvironment = profileUtils.isDevOrDockerProfile();
		
		// Estrazione token
		JWTTokenDTO jwtToken = null;
		final Date startDateOperation = new Date();

		String role = Constants.App.JWT_MISSING_SUBJECT_ROLE;
		String subjectFiscalCode = Constants.App.JWT_MISSING_SUBJECT;	
		try {
			if (Boolean.TRUE.equals(msCfg.getFromGovway())) {
				jwtToken = extractAndValidateJWT(request.getHeader(Headers.JWT_GOVWAY_HEADER), msCfg.getFromGovway());
			} else {
				jwtToken = extractAndValidateJWT(request.getHeader(Headers.JWT_HEADER), msCfg.getFromGovway());
			}

			role = jwtToken.getPayload().getSubject_role();
			subjectFiscalCode = CfUtility.extractFiscalCodeFromJwtSub(jwtToken.getPayload().getSub());
			PublicationMetadataReqDTO jsonObj = StringUtility.fromJSONJackson(request.getParameter("requestBody"), PublicationMetadataReqDTO.class);

			final IniTraceResponseDTO iniResponse = iniClient.updateMetadati(new IniMetadataUpdateReqDTO(idDoc, jwtToken.getPayload(), jsonObj));
			
			EdsResponseDTO edsResponse = null;
			boolean iniMockMessage = !StringUtility.isNullOrEmpty(iniResponse.getErrorMessage()) && iniResponse.getErrorMessage().contains("Invalid region ip");
			if (Boolean.TRUE.equals(iniResponse.getEsito()) || (isTestEnvironment && iniMockMessage)) {
				log.debug("Ini response is OK, proceeding with EDS");
			    edsResponse = edsClient.update(new EdsMetadataUpdateReqDTO(idDoc, null, jsonObj));
			} else {
				throw new BusinessException("Error encountered while sending update information to INI client");
			}

			if (!isTestEnvironment && (edsResponse == null || !edsResponse.getEsito())) {
				throw new EdsException("Error encountered while sending update information to EDS client");
			}

			if (isTestEnvironment && iniMockMessage) {
				throw new MockEnabledException(iniResponse.getErrorMessage(), edsResponse != null ? edsResponse.getErrorMessage() : null);
			}

			logger.info(String.format("Update of CDA metadata completed for document with identifier %s", idDoc), OperationLogEnum.UPDATE_METADATA_CDA2, ResultLogEnum.OK, startDateOperation, jwtToken.getPayload().getIss(), MISSING_DOC_TYPE_PLACEHOLDER, role, subjectFiscalCode);
		} catch (MockEnabledException me) {
			throw me;
		} catch (Exception e) {
			final String issuer = jwtToken != null ? jwtToken.getPayload().getIss() : JWT_MISSING_ISSUER_PLACEHOLDER;
			RestExecutionResultEnum errorInstance = RestExecutionResultEnum.GENERIC_ERROR;
			if (e instanceof ValidationException) {
				errorInstance = RestExecutionResultEnum.get(((ValidationException) e).getError().getType());
			}
			
			log.error(String.format("Error encountered while updating CDA metadata with identifier %s", idDoc), e);
			logger.error(String.format("Error while updating CDA metadata of document with identifier %s", idDoc), OperationLogEnum.UPDATE_METADATA_CDA2, ResultLogEnum.KO, startDateOperation, errorInstance.getErrorCategory(), issuer, MISSING_DOC_TYPE_PLACEHOLDER, role, subjectFiscalCode);
			throw e;
		}
		
		return new ResponseEntity<>(new ResponseDTO(getLogTraceInfo()), HttpStatus.OK);
	}

	private ValidationCreationInputDTO validateInput(final MultipartFile file, final HttpServletRequest request, final boolean isReplace,
			final LogTraceInfoDTO traceInfoDTO) {

		final ValidationCreationInputDTO validation = new ValidationCreationInputDTO();
		ValidationDataDTO validationInfo = new ValidationDataDTO();
		validationInfo.setCdaValidated(false);
		validationInfo.setWorkflowInstanceId(MISSING_WORKFLOW_PLACEHOLDER);
		
		validation.setValidationData(validationInfo);

		String transformId = ""; 
		String xsltID = ""; 
		
		try {
			final JWTTokenDTO jwtToken;
			if (Boolean.TRUE.equals(msCfg.getFromGovway())) {
				jwtToken = extractAndValidateJWT(request.getHeader(Headers.JWT_GOVWAY_HEADER), msCfg.getFromGovway());
			} else {
				jwtToken = extractAndValidateJWT(request.getHeader(Headers.JWT_HEADER), msCfg.getFromGovway());
			}
			validation.setJwtToken(jwtToken);

			PublicationCreationReqDTO jsonObj = getAndValidatePublicationReq(request.getParameter("requestBody"), isReplace);
			validation.setJsonObj(jsonObj);

			final byte[] bytePDF = getAndValidateFile(file);
			validation.setFile(bytePDF);
			
			final String cda = extractCDA(bytePDF, jsonObj.getMode());
			validation.setCda(cda);
			
			validateJWT(validation.getJwtToken(), cda);
			
			final org.jsoup.nodes.Document docT = Jsoup.parse(cda);
			final String key = CdaUtility.extractFieldCda(docT);
			validation.setDocument(docT);
			validation.setKafkaKey(key);
	
			if (!Boolean.TRUE.equals(jsonObj.isForcePublish()) || isReplace) {
				validationInfo = getValidationInfo(cda, jsonObj.getWorkflowInstanceId());
			} else {
				validationInfo.setWorkflowInstanceId(CdaUtility.getWorkflowInstanceId(docT));
			}
	
			validation.setValidationData(validationInfo); // Updating validation info

			ValidationDataDTO validatedDocument = cdaSRV.getByWorkflowInstanceId(validationInfo.getWorkflowInstanceId()); 
			
			if (!Boolean.TRUE.equals(jsonObj.isForcePublish()) || isReplace) {				
				transformId = validatedDocument.getTransformID(); 
				xsltID = validatedDocument.getXsltID(); 
				cdaSRV.consumeHash(validationInfo.getHash()); 
								
				if(DateUtility.getDifferenceDays(validatedDocument.getInsertionDate(), new Date()) > validationCFG.getDaysAllowToPublishAfterValidation()) {
					final ErrorResponseDTO error = ErrorResponseDTO.builder()
							.type(RestExecutionResultEnum.OLDER_DAY.getType())
							.title(RestExecutionResultEnum.OLDER_DAY.getTitle())
							.instance(ErrorInstanceEnum.OLDER_DAY.getInstance())
							.detail("Error: cannot publish documents older than" + validationCFG.getDaysAllowToPublishAfterValidation() + " days").build();
					throw new ValidationException(error); 
				} 
				
			}
			
			final String documentSha256 = encodeSHA256(bytePDF);
			validation.setDocumentSha(documentSha256);
	
			validateDocumentHash(documentSha256, validation.getJwtToken());
	
			final ResourceDTO fhirResourcesDTO = documentReferenceSRV.createFhirResources(cda, jsonObj, bytePDF.length, documentSha256,
				validation.getJwtToken().getPayload().getPerson_id(), transformId,xsltID);
	
			validation.setFhirResource(fhirResourcesDTO);
			
			if(!isNullOrEmpty(fhirResourcesDTO.getErrorMessage())) {
				final ErrorResponseDTO error = ErrorResponseDTO.builder()
					.type(RestExecutionResultEnum.FHIR_MAPPING_ERROR.getType())
					.title(RestExecutionResultEnum.FHIR_MAPPING_ERROR.getTitle())
					.instance(ErrorInstanceEnum.FHIR_RESOURCE_ERROR.getInstance())
					.detail(RestExecutionResultEnum.FHIR_MAPPING_ERROR.getTitle()).build();
	
				throw new ValidationException(error);
			}
			
			kafkaSRV.sendFhirMappingStatus(traceInfoDTO.getTraceID(),validation.getValidationData().getWorkflowInstanceId(), SUCCESS, "Fhir mapping language done", validation.getJsonObj(), validation.getJwtToken() != null ? validation.getJwtToken().getPayload() : null);
		} catch (final ValidationException ve) {
			cdaSRV.consumeHash(validationInfo.getHash());
			validation.setValidationError(ve);
		}

		return validation;
	}
	
	@Override
	public ResponseDTO delete(String idDoc, HttpServletRequest request) {
		// Create request tracking
		LogTraceInfoDTO log = getLogTraceInfo();

		boolean isTestEnv = profileUtils.isDevOrDockerProfile();

		JWTTokenDTO token = null;
		Date startOperation = new Date();
		String role = Constants.App.JWT_MISSING_SUBJECT_ROLE;
		String subjectFiscalCode = Constants.App.JWT_MISSING_SUBJECT;
		boolean error;
		try {
			// Extract token
			token = extractFromReqJWT(request);
			// Extract subject role
			role = token.getPayload().getSubject_role();
			subjectFiscalCode = CfUtility.extractFiscalCodeFromJwtSub(token.getPayload().getSub());

			// ==============================
			// [1] Retrieve reference from INI
			// ==============================
			IniReferenceResponseDTO iniReference = iniClient.getReference(
				new IniReferenceRequestDTO(idDoc, token.getPayload())
			);
			// Check for errors
			error = !isNullOrEmpty(iniReference.getErrorMessage());
			// Update transaction status
			kafkaSRV.sendDeleteStatus(log.getTraceID(), MISSING_WORKFLOW_PLACEHOLDER, idDoc, iniReference, error ? BLOCKING_ERROR : SUCCESS, token.getPayload());
			// Exit if necessary
			if(error) throw new IniException(iniReference.getErrorMessage());

			// ==============================
			// [2] Send delete request to EDS
			// ==============================
			EdsResponseDTO edsResponse = edsClient.delete(idDoc);
			// Check for errors
			error = !isTestEnv && (edsResponse == null || !edsResponse.getEsito());
			// Update transaction status
			kafkaSRV.sendDeleteStatus(log.getTraceID(), MISSING_WORKFLOW_PLACEHOLDER, idDoc, edsResponse, error ? BLOCKING_ERROR : SUCCESS, token.getPayload());
			// Exit if necessary
			if (error) throw new EdsException("Error encountered while sending delete information to EDS client");

			// ==============================
			// [3] Send delete request to INI
			// ==============================
			IniTraceResponseDTO iniResponse = iniClient.delete(
				buildRequestForIni(idDoc, iniReference.getUuid(), token)
			);
			// Check any errors
			error = !isNullOrEmpty(iniResponse.getErrorMessage());
			// Update transaction status
			kafkaSRV.sendDeleteStatus(log.getTraceID(), MISSING_WORKFLOW_PLACEHOLDER, idDoc, iniResponse, error ? BLOCKING_ERROR : SUCCESS, token.getPayload());
			// Check mock errors
			boolean iniMockMessage = !isNullOrEmpty(iniResponse.getErrorMessage()) && iniResponse.getErrorMessage().contains("Invalid region ip");
			error = isTestEnv && iniMockMessage;
			// Exit if necessary
			if (error) {
				throw new MockEnabledException(iniResponse.getErrorMessage(), edsResponse != null ? edsResponse.getErrorMessage() : null);
			}
			// Check res errors
			error = !isNullOrEmpty(iniResponse.getErrorMessage());
			// Check response errors
			if(error) throw new IniException(iniResponse.getErrorMessage());

			logger.info(String.format("Deletion of CDA completed for document with identifier %s", idDoc), OperationLogEnum.DELETE_CDA2, ResultLogEnum.OK, startOperation, token.getPayload().getIss(), MISSING_DOC_TYPE_PLACEHOLDER, role, subjectFiscalCode);
		} catch(MockEnabledException me) {
			throw me;
		} catch(IniException inEx) {
			final String issuer = token != null ? token.getPayload().getIss() : JWT_MISSING_ISSUER_PLACEHOLDER;
			RestExecutionResultEnum errorInstance = RestExecutionResultEnum.INI_EXCEPTION;

			PublicationCTL.log.error(String.format("Error while delete record from ini %s", idDoc), inEx);
			logger.error(String.format("Error while delete record from ini %s", idDoc), OperationLogEnum.DELETE_CDA2, ResultLogEnum.KO, startOperation, errorInstance.getErrorCategory(), issuer, MISSING_DOC_TYPE_PLACEHOLDER, role, subjectFiscalCode);
			throw inEx;
			
		} catch (Exception e) {
			final String issuer = token != null ? token.getPayload().getIss() : JWT_MISSING_ISSUER_PLACEHOLDER;
			RestExecutionResultEnum errorInstance = RestExecutionResultEnum.GENERIC_ERROR;
			if (e instanceof ValidationException) {
				errorInstance = RestExecutionResultEnum.get(((ValidationException) e).getError().getType());
			}

			PublicationCTL.log.error(String.format("Error encountered while deleting CDA with identifier %s", idDoc), e);
			logger.error(String.format("Error while deleting CDA of document with identifier %s", idDoc), OperationLogEnum.DELETE_CDA2, ResultLogEnum.KO, startOperation, errorInstance.getErrorCategory(), issuer, MISSING_DOC_TYPE_PLACEHOLDER, role, subjectFiscalCode);
			throw e;
		}
		
		return new ResponseDTO(log);
	}
	
	private DeleteRequestDTO buildRequestForIni(final String identificativoDocumento, final String uuid, final JWTTokenDTO jwtTokenDTO) {
		DeleteRequestDTO out = null;
		try {
			JWTPayloadDTO jwtPayloadDTO = jwtTokenDTO.getPayload();
			out = DeleteRequestDTO.builder().
					action_id(jwtPayloadDTO.getAction_id()).
					idDoc(identificativoDocumento).
					uuid(uuid).
					iss(jwtPayloadDTO.getIss()).
					locality(jwtPayloadDTO.getLocality()).
					patient_consent(jwtPayloadDTO.getPatient_consent()).
					person_id(jwtPayloadDTO.getPerson_id()).
					purpose_of_use(jwtPayloadDTO.getPurpose_of_use()).
					resource_hl7_type(jwtPayloadDTO.getResource_hl7_type()).
					sub(jwtPayloadDTO.getSub()).
					subject_organization_id(jwtPayloadDTO.getSubject_organization_id()).
					subject_organization(jwtPayloadDTO.getSubject_organization()).
					subject_role(jwtPayloadDTO.getSubject_role()).
					build();
		} catch(Exception ex) {
			log.error("Error while build request delete for ini : " , ex);
			throw new BusinessException("Error while build request delete for ini : " , ex);
		}
		return out;
	}

}
