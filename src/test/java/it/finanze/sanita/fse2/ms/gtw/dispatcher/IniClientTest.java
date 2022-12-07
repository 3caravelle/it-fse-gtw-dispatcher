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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
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
import org.springframework.web.client.RestTemplate;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.client.impl.IniClient;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.Constants;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.request.IniMetadataUpdateReqDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.response.IniTraceResponseDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.BusinessException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.ConnectionRefusedException;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ComponentScan(basePackages = {Constants.ComponentScan.BASE})
@ActiveProfiles(Constants.Profile.TEST)
class IniClientTest {

    @SpyBean
    RestTemplate restTemplate;

    @Autowired
    private IniClient iniClient;

    private IniMetadataUpdateReqDTO requestBody;

    @Test
    @DisplayName("Update - updateConnectionRefusedErrorTest")
    void updateConnectionRefusedErrorTest() {
        Mockito.doThrow(new ConnectionRefusedException("url", "Error: connection refused")).when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(IniTraceResponseDTO.class));
        assertThrows(ConnectionRefusedException.class, () -> iniClient.update(requestBody));
    }

    @Test
    @DisplayName("Update - updateRecordGenericErrorTest")
    void updateRecordGenericErrorTest() {
        IniTraceResponseDTO responseMock = new IniTraceResponseDTO();
        responseMock.setErrorMessage("Failed to update on INI");
        Mockito.doReturn(new ResponseEntity<>(responseMock, HttpStatus.OK))
                .when(restTemplate).exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), ArgumentMatchers.eq(IniTraceResponseDTO.class));
        assertEquals("Failed to update on INI", iniClient.update(requestBody).getErrorMessage());
    }

    @Test
    @DisplayName("Update - updateRecordGenericExceptionTest")
    void updateRecordGenericExceptionTest() {
        IniTraceResponseDTO responseMock = new IniTraceResponseDTO();
        responseMock.setEsito(false);
        Mockito.doThrow(new BusinessException("Error")).when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(IniTraceResponseDTO.class));
        assertThrows(BusinessException.class, () -> iniClient.update(requestBody));
    }

//    @Test
//    @DisplayName("Update - updateHttpStatucCodeExceptionTest")
//    void updateHttpStatusCodeExceptionTest() {
//        IniTraceResponseDTO responseMock = new IniTraceResponseDTO();
//        responseMock.setEsito(false);
//        Mockito.doThrow(new HttpStatusCodeException(HttpStatus.MULTI_STATUS) {
//                    @Override
//                    public HttpStatus getStatusCode() {
//                        return super.getStatusCode();
//                    }
//                }).when(restTemplate)
//                .exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(IniTraceResponseDTO.class));
//        assertThrows(ServerResponseException.class, () -> iniClient.update(requestBody));
//    }

    @Test
    @DisplayName("Update - updateRecord Success test")
    void updateRecordSuccessTest() {
        IniTraceResponseDTO responseMock = new IniTraceResponseDTO();
        responseMock.setEsito(true);
        Mockito.doReturn(new ResponseEntity<>(responseMock, HttpStatus.OK)).when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(IniTraceResponseDTO.class));
        IniTraceResponseDTO responseDTO = iniClient.update(requestBody);
        assertEquals(responseDTO, responseMock);
    }

    @Test
    @DisplayName("Update - updateRecordHttpErrorTest")
    void updateRecordHttpErrorTest() {
        IniTraceResponseDTO responseMock = new IniTraceResponseDTO();
        responseMock.setEsito(true);
        Mockito.doReturn(new ResponseEntity<>(responseMock, HttpStatus.BAD_GATEWAY)).when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(IniTraceResponseDTO.class));
        assertNull(iniClient.update(requestBody).getErrorMessage());
    }

    @Test
    @DisplayName("Update - updateRecordHttpBodyNullTest")
    void updateRecordHttpBodyNullTest() {
        Mockito.doReturn(new ResponseEntity<>(null, HttpStatus.BAD_GATEWAY)).when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(IniTraceResponseDTO.class));
        assertNull(iniClient.update(requestBody));
    }
}
