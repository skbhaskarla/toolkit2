/**
 This software was developed at the National Institute of Standards and Technology by employees
of the Federal Government in the course of their official duties. Pursuant to title 17 Section 105 of the
United States Code this software is not subject to copyright protection and is in the public domain.
This is an experimental system. NIST assumes no responsibility whatsoever for its use by other parties,
and makes no guarantees, expressed or implied, about its quality, reliability, or any other characteristic.
We would appreciate acknowledgement if the software is used. This software can be redistributed and/or
modified freely provided that any derivative works bear some notice that they are derived from it, and any
modified versions bear some notice that they have been modified.

Project: NWHIN-DIRECT
Authors: Frederic de Vaulx
		Diane Azais
		Julien Perugini
 */


package gov.nist.direct.test.java.validation.impl.validationOptionalHeaders;

import static org.junit.Assert.assertTrue;
import gov.nist.toolkit.errorrecording.TextErrorRecorder;
import gov.nist.direct.validation.impl.DirectMimeMessageValidatorFacade;
import gov.nist.toolkit.errorrecording.ErrorRecorder;
 

import org.junit.Test;

public class DirectMessageValidatorSignatureHeadersTest {
	
	// DTS 160, Content-Type Name, Optional
	@Test
	public void testContentTypeName() {
		ErrorRecorder er = new TextErrorRecorder();
		DirectMimeMessageValidatorFacade validator = new DirectMimeMessageValidatorFacade();
		validator.validateContentTypeNameOptional(er, "smime.p7s");
		assertTrue(!er.hasErrors());
	}	
		
	// DTS 183, EncapsulatedContentInfo.eContent, Optional
	@Test
	public void testEncapsulatedContentInfo() {
		ErrorRecorder er = new TextErrorRecorder();
		DirectMimeMessageValidatorFacade validator = new DirectMimeMessageValidatorFacade();
		validator.validateEncapsuledInfo2(er, "");
		assertTrue(!er.hasErrors());
	}
	
	// DTS 167, Certificates, Optional
	@Test
	public void testCertificates() {
		ErrorRecorder er = new TextErrorRecorder();
		DirectMimeMessageValidatorFacade validator = new DirectMimeMessageValidatorFacade();
		validator.validateCertificates(er, "");
		assertTrue(!er.hasErrors());
	}
	
	// DTS 168, Crls, Optional
	@Test
	public void testCrls() {
		ErrorRecorder er = new TextErrorRecorder();
		DirectMimeMessageValidatorFacade validator = new DirectMimeMessageValidatorFacade();
		validator.validateCrls(er, "");
		assertTrue(!er.hasErrors());
	}
	
	// DTS 169, SignerInfos, Optional
	@Test
	public void testSignerInfos() {
		ErrorRecorder er = new TextErrorRecorder();
		DirectMimeMessageValidatorFacade validator = new DirectMimeMessageValidatorFacade();
		validator.validateSignerInfos(er, "2012-05-05T08:15:30-05:00");
		assertTrue(!er.hasErrors());
	}
	
	@Test
	public void testSignerInfos2() {
		ErrorRecorder er = new TextErrorRecorder();
		DirectMimeMessageValidatorFacade validator = new DirectMimeMessageValidatorFacade();
		validator.validateSignerInfos(er, "May, 5 2012  8:15:30");   // Not valid, not in UTC format
		assertTrue(er.hasErrors());
	}
	
	// DTS 173, SignerInfos.sid, Optional
	@Test
	public void testSignerInfosSid() {
		ErrorRecorder er = new TextErrorRecorder();
		DirectMimeMessageValidatorFacade validator = new DirectMimeMessageValidatorFacade();
		validator.validateSignerInfosSid(er, "");
		assertTrue(!er.hasErrors());
	}
	
	// DTS 181, SignerInfos.unsignedAttrs, Optional
	@Test
	public void testSignerInfosUnsignedAttrs() {
		ErrorRecorder er = new TextErrorRecorder();
		DirectMimeMessageValidatorFacade validator = new DirectMimeMessageValidatorFacade();
		validator.validateSignerInfosUnsignedAttrs(er, "");
		assertTrue(!er.hasErrors());
	}
	
	
}
