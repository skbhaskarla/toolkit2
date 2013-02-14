package gov.nist.registry.common2.direct;


import gov.nist.direct.messageProcessor.MessageProcessor;
import gov.nist.direct.messageProcessor.MessageProcessorInterface;
import gov.nist.direct.messageProcessor.direct.DirectMessageProcessorInterface;
import gov.nist.direct.messageProcessor.direct.directImpl.DirectMimeMessageProcessor;
import gov.nist.toolkit.errorrecording.ErrorRecorder;
import gov.nist.toolkit.errorrecording.factories.ErrorRecorderBuilder;
import gov.nist.toolkit.utilities.io.Io;
import gov.nist.toolkit.valsupport.client.ValidationContext;
import gov.nist.toolkit.valsupport.engine.MessageValidatorEngine;
import gov.nist.toolkit.valsupport.message.MessageValidator;

import java.io.IOException;
import java.io.InputStream;

import org.apache.log4j.Logger;
import org.mortbay.util.IO;

public class DirectDecoder extends MessageValidator {
	InputStream in, certificate;
	ErrorRecorderBuilder erBuilder;
	static Logger logger = Logger.getLogger(DirectDecoder.class);
	
	public DirectDecoder(ValidationContext vc, ErrorRecorderBuilder erBuilder, InputStream directInputStream, InputStream directCertificate) {
		
		super(vc);
		this.erBuilder = erBuilder;
		in = directInputStream;
		certificate = directCertificate;
		logger.debug("ValidationContext is " + vc.toString());
	}

	@Override
	public void run(ErrorRecorder er, MessageValidatorEngine mvc) {
		
	
		byte[] directMessage = null;
		byte[] directCertificate = null;
		try {
			directMessage = IO.readBytes(in);
			directCertificate = IO.readBytes(certificate);
			//in.read(directMessage);
		} catch (IOException e) {
			er.err("Error loading file", "Can't load the certificate file or message file", "", "", "Loading file");
			e.printStackTrace();
		}
		
		try {
			logger.debug(Io.getStringFromInputStream(in));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// Calls the higher-level validation function from the Direct package
		MessageProcessorInterface msgProcessor = new MessageProcessor();
		msgProcessor.processMessage(er, directMessage, directCertificate, vc.privKeyPassword, vc);

		
	}

	
	
}