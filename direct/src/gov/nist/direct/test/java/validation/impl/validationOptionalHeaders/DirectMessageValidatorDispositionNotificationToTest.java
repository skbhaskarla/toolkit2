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
import gov.nist.direct.utils.TextErrorRecorderModif;
import gov.nist.direct.validation.impl.DirectMimeMessageValidatorFacade;
import gov.nist.toolkit.errorrecording.ErrorRecorder;
 

import org.junit.Test;

public class DirectMessageValidatorDispositionNotificationToTest {
	
	// DTS 128, Disposition-Notification-To, Optional
	// Result: Success
	@Test
	public void testDispositionNotificationTo() {
		ErrorRecorder er = new TextErrorRecorderModif();
		DirectMimeMessageValidatorFacade validator = new DirectMimeMessageValidatorFacade();
		validator.validateDispositionNotificationTo(er, "test@test.com", false);
		assertTrue(!er.hasErrors());
	}
		
	// Result: Fail
	@Test
	public void testDispositionNotificationTo2() {
		ErrorRecorder er = new TextErrorRecorderModif();
		DirectMimeMessageValidatorFacade validator = new DirectMimeMessageValidatorFacade();
		validator.validateDispositionNotificationTo(er, "test.test.com", false);  // Not valid, not an e-mail address
		assertTrue(er.hasErrors());
	}
	
	// Result: Fail
	@Test
	public void testDispositionNotificationTo3() {
		ErrorRecorder er = new TextErrorRecorderModif();
		DirectMimeMessageValidatorFacade validator = new DirectMimeMessageValidatorFacade();
		validator.validateDispositionNotificationTo(er, "Test <test@test.com> Test", false);  // Not valid
		assertTrue(er.hasErrors());
	}	
}
