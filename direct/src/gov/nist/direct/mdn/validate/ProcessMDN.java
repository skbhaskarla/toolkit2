package gov.nist.direct.mdn.validate;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import javax.mail.MessagingException;
import javax.mail.Part;
import org.apache.commons.io.IOUtils;
import gov.nist.toolkit.errorrecording.ErrorRecorder;

/**
 * This class calls the full MDN message validation
 * @author jnp3
 * @author dazais
 *
 */
public class ProcessMDN {
	
	private String dispNotifTo;
	private String originalRecipient;
	private String reportingUA;
	private String mdnGateway;
	private String finalRecipient;
	private String originalMessageID;
	private String disposition;
	private String failure;
	private String error;
	private String warning;
	private String extension;
	
	public ProcessMDN(Part p){
		InputStream mdnStream = null;

		try {
			mdnStream = (InputStream) p.getContent();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		StringWriter writer = new StringWriter();
		try {
			IOUtils.copy(mdnStream, writer, "UTF-8");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String mdnPart = writer.toString();
		mdnPart = mdnPart.toLowerCase();

		dispNotifTo = getMDNHeader(mdnPart, "disposition-notification-to");
		originalRecipient = getMDNHeader(mdnPart, "original-recipient");
		reportingUA = getMDNHeader(mdnPart, "reporting-ua");
		mdnGateway = getMDNHeader(mdnPart, "mdn-gateway");
		finalRecipient = getMDNHeader(mdnPart, "final-recipient");
		originalMessageID = getMDNHeader(mdnPart, "original-message-id");
		disposition = getMDNHeader(mdnPart, "disposition");
		failure = getMDNHeader(mdnPart, "failure");
		error = getMDNHeader(mdnPart, "error");
		warning = getMDNHeader(mdnPart, "warning");
		extension = getMDNHeader(mdnPart, "extension");
	}

	public void validate(ErrorRecorder er){

		MDNValidator validator = new MDNValidatorImpl();

		// DTS 452, Disposition-Notification-To, Required
		validator.validateMDNRequestHeader(er, dispNotifTo);
		
		// DTS 454, Original-Recipient-Header, warning
		validator.validateOriginalRecipientHeader(er, originalRecipient);
		
		// DTS 456, Disposition-Notification-Content, warning
		validator.validateDispositionNotificationContent(er, reportingUA, mdnGateway, originalRecipient, finalRecipient, originalMessageID, disposition, failure, error, warning, extension);
		
		// DTS 457, Reporting-UA-Field, warning
		validator.validateReportingUAField(er, reportingUA);
		
		// DTS 458, mdn-gateway-field, Required
		validator.validateMDNGatewayField(er, mdnGateway);
		
		// DTS 459, original-recipient-field, Required
		validator.validateOriginalRecipientField(er, originalRecipient);
		
		// DTS 460, final-recipient-field, Required
		validator.validateFinalRecipientField(er, finalRecipient);
		
		// DTS 461, original-message-id-field, Required
		validator.validateOriginalMessageIdField(er, originalMessageID);
		
		// DTS 462, disposition-field, Required
		validator.validateDispositionField(er, disposition);
		
		// DTS 463, failure-field, Required
		validator.validateFailureField(er, failure);
		
		// DTS 464, error-field, Required
		validator.validateErrorField(er, error);
		
		// DTS 465, warning-field, Required
		validator.validateWarningField(er, warning);
		
		// DTS 466, extension-field, Required
		validator.validateExtensionField(er, extension);		
	}
	
	public String getMDNHeader(String part, String header) {
		String res = "";
		if(checkPresent(part, header)) {
			String[] partSplit = part.split(header + ": ");
			String[] partSplitRight = partSplit[1].split("\r\n");
			res = partSplitRight[0];
			return res;
		}
		return res;
	}
	
	public boolean checkPresent(String part, String header) {
		if(part.contains(header)) {
			return true;
		} else {
			return false;
		}
	}
	
	public String getDispositionField() {
		return this.disposition;
	}

}
