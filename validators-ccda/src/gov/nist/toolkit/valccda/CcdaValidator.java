package gov.nist.toolkit.valccda;


/*
 * ValidateMessageService#runValidation(ValidationContext, Session, byte[], byte[], GwtErrorRecorderBuilder)

assumes validator plugged into

MessageValidatorFactory#getValidator:80   (not currently plugged in)

example plugin wrappers are getValidatorForXML and getValidatorForDirect (same file) which are the outer wrappers

Inner wrappers are DirectDecoder
 * 
 * 
 */

import gov.nist.toolkit.errorrecording.ErrorRecorder;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.ecore.EClass;
import org.openhealthtools.mdht.uml.cda.ClinicalDocument;
import org.openhealthtools.mdht.uml.cda.mu2consol.Mu2consolPackage;
import org.openhealthtools.mdht.uml.cda.util.CDADiagnostic;
import org.openhealthtools.mdht.uml.cda.util.CDAUtil;
import org.openhealthtools.mdht.uml.cda.util.ValidationResult;

public class CcdaValidator {
	static Logger logger = Logger.getLogger(CcdaValidator.class);

	static Map<String, EClass> typeMap = new HashMap<String, EClass>();

	static {
		typeMap.put("Clinical Office Visit Summary", Mu2consolPackage.eINSTANCE.getClinicalOfficeVisitSummary());
		typeMap.put("Transitions Of Care Ambulatory Summary", Mu2consolPackage.eINSTANCE.getTransitionOfCareAmbulatorySummary());
		typeMap.put("Transitions Of Care Inpatient Summary", Mu2consolPackage.eINSTANCE.getTransitionOfCareInpatientSummary());
		typeMap.put("VDT Ambulatory Summary", Mu2consolPackage.eINSTANCE.getVDTAmbulatorySummary());
		typeMap.put("VDT Inpatient Summary", Mu2consolPackage.eINSTANCE.getVDTInpatientSummary());
		
	}
	
	/**
	 * This method validates the CDA xml input stream using the MDHT validator
	 * 
	 * 09/24/2012 : Rama Ramakrishnan : Making this method static & 
	 * 									removing the source string from the diagnostic message
	 *  
	 * @param is
	 * @param validationType
	 * @param er
	 * @throws Exception
	 */
	public synchronized static void validateCDA(InputStream is, String validationType, ErrorRecorder er) throws Exception {
		Mu2consolPackage.eINSTANCE.eClass();
		ValidationResult result = new ValidationResult();
		EClass type = typeMap.get(validationType);
		
		logger.info("Starting CCDA validation, validate as a " + ((type == null) ? "Plain CCDA" : type));
		long start_time = System.currentTimeMillis();
		
		if (type == null || validationType.trim().equals("")) 
			CDAUtil.load(is);
		else
			CDAUtil.loadAs(is, type, result);
		
		long end_time = System.currentTimeMillis();
		long run_time = end_time - start_time;

		int errors = 0;
		int warnings = 0;
		int details = 0;
		
		for (Diagnostic dq : result.getErrorDiagnostics()) {
			CDADiagnostic cdaDiagnosticq = new CDADiagnostic(dq);
			er.err(null, cdaDiagnosticq.getCode() + "|" + cdaDiagnosticq.getMessage(), cdaDiagnosticq.getPath(), cdaDiagnosticq.getSource());
			errors++;
		}
		for (Diagnostic dq : result.getWarningDiagnostics()) {
			CDADiagnostic cdaDiagnosticq = new CDADiagnostic(dq);
			er.warning("", cdaDiagnosticq.getCode() + "|" + cdaDiagnosticq.getMessage(), cdaDiagnosticq.getPath(), cdaDiagnosticq.getSource());
			warnings++;
		}
		for (Diagnostic dq : result.getInfoDiagnostics()) {
			CDADiagnostic cdaDiagnosticq = new CDADiagnostic(dq);
			er.detail(cdaDiagnosticq.getCode() + "|" + cdaDiagnosticq.getMessage() + "|" + cdaDiagnosticq.getPath() + "|" + cdaDiagnosticq.getSource());
			details++;
		}
		
		logger.info("CCDA Validation complete: " + errors + " errors, " + warnings + " warnings, " + details + " details.");
		logger.info("MHDT run time was " + run_time + " mSec");
		logger.info("Validation, was a " + ((type == null) ? "Plain CCDA" : type));
	}
}
