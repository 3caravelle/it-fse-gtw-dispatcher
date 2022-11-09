/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package it.finanze.sanita.fse2.ms.gtw.dispatcher;


import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.regex.Pattern;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.Constants;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.ValidationCFG;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.JWTHeaderDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.UIDModeEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.logging.LoggerHelper;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.impl.UtilitySRV;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.CfUtility;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.FileUtility;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.PDFUtility;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.StringUtility;
import lombok.extern.slf4j.Slf4j;

/**
 * Junit class for utility methods.
 * 
 */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {"validation.allow-special-fiscal-code=true"})
@ComponentScan(basePackages = { Constants.ComponentScan.BASE })
@ActiveProfiles(Constants.Profile.TEST)
class UtilityTest extends AbstractTest{

    @MockBean
    ValidationCFG validationCfg;

    @Autowired
    LoggerHelper logger;
    
    @Test
    @DisplayName("Fiscal code check test")
    void fiscalCodeTest () {

        final String fiscalCode16 = "RSSMRA72H26F941L";
        final String fiscalCode17 = "RSSMRA72H26F941LA"; // Should drop last char
        final String fiscalCode11 = "RSSMRA72H26";
        final String fiscalCodeEni = "ENI1234567891234";
        final String fiscalCodeStp = "STP1234567891234";
        final String fiscalCodeNull = null;
        
        assertEquals(CfUtility.CF_OK_16, CfUtility.validaCF(fiscalCode16));
        assertEquals(CfUtility.CF_OK_16, CfUtility.validaCF(fiscalCode17));
        assertNotEquals(CfUtility.CF_OK_11, CfUtility.validaCF(fiscalCode11));
        assertEquals(CfUtility.CF_ENI_OK, CfUtility.validaCF(fiscalCodeEni));
        assertEquals(CfUtility.CF_STP_OK, CfUtility.validaCF(fiscalCodeStp));
        assertEquals(CfUtility.CF_NON_CORRETTO, CfUtility.validaCF(fiscalCodeNull));
        
        final String fiscalCodeShort = "RSSMRA72H26F941";
        final String fiscalCodeLong = "RSSMRA72H26F941LAA";
        final String fiscalCodeImproper = "RSSMR172H26F941L";
        final String fcImproperEni = "ENI123456789123A";
        final String fcImproperStp = "STP123456789123A";

        assertEquals(0, CfUtility.validaCF(fiscalCodeShort));
        assertEquals(0, CfUtility.validaCF(fiscalCodeLong));
        assertEquals(0, CfUtility.validaCF(fiscalCodeImproper));
        assertEquals(0, CfUtility.validaCF(fcImproperEni));
        assertEquals(0, CfUtility.validaCF(fcImproperStp));
    }

    @Autowired
    UtilitySRV utilitySrv;

    @Test
    @DisplayName(value = "Validation CF by config")
    void extValidFiscalCode() {
        final String fiscalCode16 = "RSSMRA72H26F941L";
        final String fiscalCode17 = "RSSMRA72H26F941LA"; // Should drop last char
        final String fiscalCode11 = "RSSMRA72H26";
        final String fiscalCodeEni = "ENI1234567891234";
        final String fiscalCodeStp = "STP1234567891234";
        final String fiscalCodeNull = null;
    	
        given(validationCfg.getAllowSpecialFiscalCodes()).willReturn(true);

    	//true validation prop
    	assertEquals(true, utilitySrv.isValidCf(fiscalCode16));
    	assertEquals(true, utilitySrv.isValidCf(fiscalCode17));
    	assertEquals(false, utilitySrv.isValidCf(fiscalCode11));
    	assertEquals(true, utilitySrv.isValidCf(fiscalCodeEni));
    	assertEquals(true, utilitySrv.isValidCf(fiscalCodeStp));
    	assertEquals(false, utilitySrv.isValidCf(fiscalCodeNull));
        
        given(validationCfg.getAllowSpecialFiscalCodes()).willReturn(false);

    	//false validation prop
    	assertEquals(true, utilitySrv.isValidCf(fiscalCode16));
    	assertEquals(false, utilitySrv.isValidCf(fiscalCode17));
    	assertEquals(false, utilitySrv.isValidCf(fiscalCode11));
    	assertEquals(false, utilitySrv.isValidCf(fiscalCodeEni));
    	assertEquals(false, utilitySrv.isValidCf(fiscalCodeStp));
    	assertEquals(false, utilitySrv.isValidCf(fiscalCodeNull));

        given(validationCfg.getAllowSpecialFiscalCodes()).willReturn(null);

        //null validation prop
    	assertEquals(true, utilitySrv.isValidCf(fiscalCode16));
    	assertEquals(false, utilitySrv.isValidCf(fiscalCode17));
    	assertEquals(false, utilitySrv.isValidCf(fiscalCode11));
    	assertEquals(false, utilitySrv.isValidCf(fiscalCodeEni));
    	assertEquals(false, utilitySrv.isValidCf(fiscalCodeStp));
    	assertEquals(false, utilitySrv.isValidCf(fiscalCodeNull));
    }
    
    @ParameterizedTest
    @DisplayName("Transaction UID generation test")
    @ValueSource(ints = { 1, 2, 3, 4})
    void workflowInstanceIdTest(final int idMode) {

        String uidGenerated = StringUtility.generateTransactionUID(UIDModeEnum.get(idMode));
        assertFalse(StringUtility.isNullOrEmpty(uidGenerated));
    }

    @Test
    @DisplayName("Mixed utility")
    void genericUtility() {
        byte[] pdfAttachment = FileUtility.getFileFromInternalResources("Files/attachment/CDA_OK_SIGNED.pdf");
        assertNotNull(StringUtility.encodeBase64(pdfAttachment));
        assertNotNull(StringUtility.encodeHex(pdfAttachment));
        assertNotNull(StringUtility.encodeSHA256(pdfAttachment));
        assertNotNull(StringUtility.encodeSHA256B64(new String(pdfAttachment)));
        assertNotNull(StringUtility.encodeSHA256Hex(new String(pdfAttachment)));

        JWTHeaderDTO head = new JWTHeaderDTO("alg", "typ", "kid", "x5c");
        assertDoesNotThrow(() -> StringUtility.fromJSON(StringUtility.toJSON(head), JWTHeaderDTO.class));
    }
 

    @Test
	void generateJwtFromFiles() {
		final String header = new String(FileUtility.getFileFromInternalResources("Files" + File.separator + "jwt" + File.separator + "header.json"));
		final String payload = new String(FileUtility.getFileFromInternalResources("Files" + File.separator + "jwt" + File.separator + "payload.json"));
		
		assertNotNull(header);
		assertNotNull(payload);
		
		log.info(Base64.getEncoder().encodeToString(header.getBytes()) + "." + Base64.getEncoder().encodeToString(payload.getBytes()));
	}

	@Test
	void patternTest() {
		final String devUrl = "http:localhost:9080";
		final String prodUrl = "https://server-ok.com";
		final Pattern pattern = Pattern.compile("^https://.*");
		assertTrue(pattern.matcher(prodUrl).matches());
		assertFalse(pattern.matcher(devUrl).matches());
	}

    @ParameterizedTest
    @CsvSource({"CDA-utf-16-LE.xml,UTF-16LE", "CDA-utf-16-BE.xml,UTF-16BE", "CDA-UTF-8.xml,UTF-8"})
    @DisplayName("Testing extraction of CDA with different encodings")
    void charsetTest(final String filename, final String charset) throws XMLStreamException, FactoryConfigurationError {

        byte[] filebytes = FileUtility.getFileFromInternalResources("Files" + File.separator + "attachment" + File.separator + "encoded-cda" + File.separator + filename);
        
        final XMLStreamReader xmlStreamReader = XMLInputFactory.newInstance().createXMLStreamReader(new ByteArrayInputStream(filebytes)); 
        final String fileEncoding = xmlStreamReader.getEncoding();
        final Charset detectedCharset = Charset.forName(fileEncoding);

        assertEquals(detectedCharset, Charset.forName(charset), "The encoding type should be the same set in the file");

        final String decodedCda = PDFUtility.detectCharsetAndExtract(filebytes);
        assertTrue(decodedCda.contains("<?xml"), "The bytes should have been decoded correctly");
    }

    
}
