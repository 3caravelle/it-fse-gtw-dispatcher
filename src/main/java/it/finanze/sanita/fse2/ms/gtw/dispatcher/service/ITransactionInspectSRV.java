package it.finanze.sanita.fse2.ms.gtw.dispatcher.service;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.TransactionInspectResDTO;

public interface ITransactionInspectSRV {

	TransactionInspectResDTO callSearchEventByWorkflowInstanceId(String workflowInstanceId);

	TransactionInspectResDTO callSearchEventByTraceId(String traceId);

}
