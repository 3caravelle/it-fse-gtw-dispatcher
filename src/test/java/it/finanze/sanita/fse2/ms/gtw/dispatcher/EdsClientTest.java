/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package it.finanze.sanita.fse2.ms.gtw.dispatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.client.impl.EdsClient;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.Constants;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request.EdsMetadataUpdateReqDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request.PublicationMetadataReqDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.EdsResponseDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.BusinessException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.ConnectionRefusedException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.ServerResponseException;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ComponentScan(basePackages = {Constants.ComponentScan.BASE})
@ActiveProfiles(Constants.Profile.TEST)
class EdsClientTest {

    @SpyBean
    RestTemplate restTemplate;

    @Autowired
    private EdsClient edsClient;

    private EdsMetadataUpdateReqDTO requestBody;

    @BeforeEach
    void init() {
        requestBody = new EdsMetadataUpdateReqDTO("idDoc", "workflowInstanceId", new PublicationMetadataReqDTO());
    }

    @Test
    @DisplayName("Update - updateConnectionRefusedErrorTest")
    void updateConnectionRefusedErrorTest() {
        Mockito.doThrow(new ConnectionRefusedException("url", "Error: connection refused")).when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(EdsResponseDTO.class));
        assertThrows(BusinessException.class, () -> edsClient.update(requestBody));
    }

    @Test
    @DisplayName("Update - updateRecordGenericExceptionTest")
    void updateRecordGenericExceptionTest() {
        EdsResponseDTO responseMock = new EdsResponseDTO();
        responseMock.setEsito(false);
        Mockito.doThrow(new BusinessException("Error")).when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(EdsResponseDTO.class));
        assertThrows(BusinessException.class, () -> edsClient.update(requestBody));
    }

    @Test
    @DisplayName("Update - updateHttpStatucCodeExceptionTest")
    void updateHttpStatusCodeExceptionTest() {
        EdsResponseDTO responseMock = new EdsResponseDTO();
        responseMock.setEsito(false);
        Mockito.doThrow(new HttpStatusCodeException(HttpStatus.MULTI_STATUS) {
                    @Override
                    public HttpStatus getStatusCode() {
                        return super.getStatusCode();
                    }
                }).when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(EdsResponseDTO.class));
        assertThrows(ServerResponseException.class, () -> edsClient.update(requestBody));
    }

    @Test
    @DisplayName("Update - updateRecord Success test")
    void updateRecordSuccessTest() {
        EdsResponseDTO responseMock = new EdsResponseDTO();
        responseMock.setEsito(true);
        Mockito.doReturn(new ResponseEntity<>(responseMock, HttpStatus.OK)).when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(EdsResponseDTO.class));
        EdsResponseDTO responseDTO = edsClient.update(requestBody);
        assertEquals(responseDTO, responseMock);
    }

    @Test
    @DisplayName("Update - updateRecordHttpBodyNullTest")
    void updateRecordHttpBodyNullTest() {
        Mockito.doReturn(new ResponseEntity<>(null, HttpStatus.BAD_GATEWAY)).when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(EdsResponseDTO.class));
        assertNull(edsClient.update(requestBody));
    }
}
