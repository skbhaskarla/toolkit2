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

public class DirectMessageValidatorOptionalFieldsTest {
	
	
	@Test
	public void testOptionalFields() {
		ErrorRecorder er = new TextErrorRecorder();
		DirectMimeMessageValidatorFacade validator = new DirectMimeMessageValidatorFacade();
		validator.validateOptionalField(er, "test");
		assertTrue(!er.hasErrors());
	}
	
	@Test
	public void testOptionalFields2() {
		ErrorRecorder er = new TextErrorRecorder();
		DirectMimeMessageValidatorFacade validator = new DirectMimeMessageValidatorFacade();
		validator.validateOptionalField(er, "");   // Not present
		assertTrue(!er.hasErrors());
	}
	
}
