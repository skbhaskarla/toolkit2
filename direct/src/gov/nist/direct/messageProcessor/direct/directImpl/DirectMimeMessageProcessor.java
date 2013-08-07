/**
 This software was developed at the National Institute of Standards and Technology by employees
of the Federal Government in the course of their official duties. Pursuant to title 17 Section 105 of the
United States Code this software is not subject to copyright protection and is in the public domain.
This is an experimental system. NIST assumes no responsibility whatsoever for its use by other parties,
and makes no guarantees, expressed or implied, about its quality, reliability, or any other characteristic.
We would appreciate acknowledgment if the software is used. This software can be redistributed and/or
modified freely provided that any derivative works bear some notice that they are derived from it, and any
modified versions bear some notice that they have been modified.

Project: NWHIN-DIRECT
Authors: William Majurski
		 Frederic de Vaulx
		 Diane Azais
		 Julien Perugini
		 Antoine Gerardin

 */

package gov.nist.direct.messageProcessor.direct.directImpl;

import gov.nist.direct.directValidator.MessageValidatorFacade;
import gov.nist.direct.directValidator.impl.DirectMimeMessageValidatorFacade;
import gov.nist.direct.directValidator.impl.ProcessEnvelope;
import gov.nist.direct.logger.MessageLogManager;
import gov.nist.direct.messageProcessor.direct.DirectMessageProcessorInterface;
import gov.nist.direct.utils.Utils;
import gov.nist.direct.utils.ValidationSummary;
import gov.nist.direct.utils.ValidationSummary.Status;
import gov.nist.toolkit.MessageValidatorFactory2.MessageValidatorFactoryFactory;
import gov.nist.toolkit.errorrecording.ErrorRecorder;
import gov.nist.toolkit.errorrecording.factories.ErrorRecorderBuilder;
import gov.nist.toolkit.utilities.io.Io;
import gov.nist.toolkit.valccda.CdaDetector;
import gov.nist.toolkit.valregmsg.xdm.XDMException;
import gov.nist.toolkit.valregmsg.xdm.XdmDecoder;
import gov.nist.toolkit.valsupport.client.ValidationContext;
import gov.nist.toolkit.valsupport.engine.MessageValidatorEngine;
import gov.nist.toolkit.valsupport.errrec.GwtErrorRecorder;
import gov.nist.toolkit.xdsexception.ExceptionUtil;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import javax.activation.CommandMap;
import javax.activation.MailcapCommandMap;
import javax.mail.Address;
import javax.mail.Header;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.RecipientId;
import org.bouncycastle.cms.RecipientInformation;
import org.bouncycastle.cms.RecipientInformationStore;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationStore;
import org.bouncycastle.cms.jcajce.JceKeyTransEnvelopedRecipient;
import org.bouncycastle.cms.jcajce.JceKeyTransRecipientId;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.mail.smime.SMIMEEnveloped;
import org.bouncycastle.mail.smime.SMIMEException;
import org.bouncycastle.mail.smime.SMIMESigned;
import org.bouncycastle.mail.smime.SMIMEUtil;
import org.bouncycastle.util.Store;

import com.google.gwt.safehtml.shared.SafeHtmlUtils;

public class DirectMimeMessageProcessor implements DirectMessageProcessorInterface {

	static Logger logger = Logger.getLogger(DirectMimeMessageProcessor.class);

	static{
		setDefaultMailcap();
		Security.addProvider(new BouncyCastleProvider());
	}

	public static void setDefaultMailcap()
	{
		MailcapCommandMap _mailcap =
				(MailcapCommandMap)CommandMap.getDefaultCommandMap();

		_mailcap.addMailcap("application/pkcs7-signature;; x-java-content-handler=org.bouncycastle.mail.smime.handlers.pkcs7_signature");
		_mailcap.addMailcap("application/pkcs7-mime;; x-java-content-handler=org.bouncycastle.mail.smime.handlers.pkcs7_mime");
		_mailcap.addMailcap("application/x-pkcs7-signature;; x-java-content-handler=org.bouncycastle.mail.smime.handlers.x_pkcs7_signature");
		_mailcap.addMailcap("application/x-pkcs7-mime;; x-java-content-handler=org.bouncycastle.mail.smime.handlers.x_pkcs7_mime");
		_mailcap.addMailcap("multipart/signed;; x-java-content-handler=org.bouncycastle.mail.smime.handlers.multipart_signed");

		CommandMap.setDefaultCommandMap(_mailcap);
	}

	private final String BC = BouncyCastleProvider.PROVIDER_NAME;
	private byte[] directCertificate;
	private String password;
	private int attnum = 1;
	ValidationContext vc = new ValidationContext();
	private int partNumber;
	private int shiftNumber;
	private ValidationSummary validationSummary = new ValidationSummary();
	WrappedMessageProcessor wrappedParser = new WrappedMessageProcessor();
	private boolean qpEncoded;
	private boolean wrapped_no_multipart = false;
	private Date logDate;
	private String username;
	private String messageId;
	private int attachmentNumber;



	public void processAndValidateDirectMessage(ErrorRecorder er, byte[] inputDirectMessage, byte[] _directCertificate, String _password, ValidationContext vc){
		directCertificate = _directCertificate;
		password = _password;
		this.vc = vc;
		this.attachmentNumber = 0;

		// New ErrorRecorder to put summary first
		ErrorRecorder mainEr = new GwtErrorRecorder();

		// Parse the message to see if it is wrapped
		wrappedParser.messageParser(er, inputDirectMessage, _directCertificate, _password);
		this.logDate = wrappedParser.getLogDate();
		if(this.logDate == null) {
			this.logDate = new Date();
		}
		this.username = wrappedParser.getUsername();
		this.messageId = wrappedParser.getMessageId();
		this.messageId = Utils.rawMsgId(this.messageId);

		if(this.username.equals("")) {
			this.username = "Unknown-User";
		}

		// Set the part number to 1
		partNumber = 1;
		// Set shift number to 1 (used to display the summary)
		shiftNumber = 1;

		logger.debug("ValidationContext is " + vc.toString());

		MimeMessage mm;
		mm = MimeMessageParser.parseMessage(mainEr, inputDirectMessage);


		// Log Direct Message
		logDirectMessage((Part)mm);

		// Check if valid Direct Message

		try {
			processPart(mainEr, mm);
			er.detail("");
			er.detail("############################Message Content Summary################################");
			er.detail("");

			ErrorRecorder summaryEr = new GwtErrorRecorder();
			validationSummary.writeErrorRecorder(summaryEr);
			er.concat(summaryEr);

			er.detail("");
			er.detail("###############################Detailed Validation#################################");
			er.detail("");

			er.concat(mainEr);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			er.error("No DTS", "Unexpected Error", e.toString(), "", "-");
		}

	}

	/**
	 * 
	 *  Validates a part of the message*/
	public void processPart(ErrorRecorder er, Part p) throws Exception{

		if (p == null)
			return;

		// Decode if quoted printable
		qpEncoded = false;
		String encoding = "";
		ProcessEnvelope procEnv = new ProcessEnvelope();
		encoding = procEnv.searchHeaderSimple(p, "content-transfer-encoding");
		if(encoding.equals("quoted-printable")) {
			//p = decodeQP(p.getInputStream());
			qpEncoded = true;
		} else {
			qpEncoded = false;
		}


		//er.detail("Processing Part");
		// If the Part is a Message then first validate the Envelope
		if (p instanceof Message && !wrapped_no_multipart){
			//er.detail("Detected an Envelope");
			er.detail("====================Outer Enveloped Message==========================");
			processEnvelope(er, (Message)p);
		}

		this.info(p);

		/*
		 * Using isMimeType to determine the content type avoids
		 * fetching the actual content data until we need it.
		 */
		if (p.isMimeType("text/plain")) {			
			//er.detail("This is plain text"+"  Content Name: "+p.getContent().getClass().getName());
			this.processText(er, p);

		} else if (p.isMimeType("text/html")) {
			//er.detail("This is plain text xml"+"  Content Name: "+p.getContent().getClass().getName());
			this.processTextHTML(er, p);

		} else if (p.isMimeType("text/xml")) {
			//er.detail("This is plain text xml"+"  Content Name: "+p.getContent().getClass().getName());
			this.processTextXML(er, p);

		} else if (p.isMimeType("message/rfc822")) {
			//er.detail("This is a nested message"+"  Content Name: "+p.getContent().getClass().getName());

			// Summary
			validationSummary.recordKey(getShiftIndent(shiftNumber) + "Part " + partNumber +": message/rfc822 interpreted as a message", Status.PART, true);

			Part messageRFC = p;
			p = (Part)p.getContent();

			if (p instanceof Message) {
				//er.detail("Detected an Envelope");
				er.detail("====================Message RFC 822==========================");
				ProcessEnvelope process = new ProcessEnvelope();

				// Separate ErrorRecorder
				ErrorRecorder separate = new GwtErrorRecorder();

				process.validateMimeEntity(separate, p, validationSummary, shiftNumber+1);
				er.concat(separate);

				// Separate ErrorRecorder 2
				ErrorRecorder separate2 = new GwtErrorRecorder();

				process.validateMessageHeader(separate2, (Message)p, validationSummary, partNumber, true);
				er.concat(separate2);

				// DTS 151, Validate First MIME Part Body
				MessageValidatorFacade msgValidator = new DirectMimeMessageValidatorFacade();
				msgValidator.validateFirstMIMEPartBody(er, true);

				// Update summary
				separate.concat(separate2);

				validationSummary.updateInfos(getShiftIndent(shiftNumber) + "Part " + partNumber +": message/rfc822 interpreted as a message", separate.hasErrors(), true);
				partNumber++;

				if(qpEncoded) {
					p = decodeQP(messageRFC);
				}

				if(p.getContent() instanceof MimeMultipart) {
					shiftNumber++;
					partNumber=1;

					MimeMultipart mp = (MimeMultipart)p.getContent();

					int count = mp.getCount();
					for (int i = 0; i < count; i++){
						this.processPart(er, mp.getBodyPart(i));
					}
				} else {
					wrapped_no_multipart = true;
					this.processPart(er, p);
				}
			}



		} else if (p.isMimeType("application/pkcs7-signature"+"  Content Name: "+p.getContent().getClass().getName())) {
			//er.detail("This is a signature");
			// DTS 152, Validate Second MIME Part
			MessageValidatorFacade msgValidator = new DirectMimeMessageValidatorFacade();
			msgValidator.validateSecondMIMEPart(er, true);

			// DTS 155, Validate Content-Type
			msgValidator.validateContentType2(er, p.getContentType());


		} else if (p.isMimeType("application/pkcs7-mime")) {
			//er.detail("This is a s/mime"+"  Content Name: "+p.getContent().getClass().getName());
			this.processPart(er, processSMIMEEnvelope(er, p, new ByteArrayInputStream(directCertificate), password));

		} else if (p.isMimeType("application/x-pkcs7-signature")) {
			//er.detail("This is a x signature"+"  Content Name: "+p.getContent().getClass().getName());
			//er.detail("This is a signature");
			// DTS 152, Validate Second MIME Part
			MessageValidatorFacade msgValidator = new DirectMimeMessageValidatorFacade();
			msgValidator.validateSecondMIMEPart(er, true);

			// DTS 155, Validate Content-Type
			msgValidator.validateContentType2(er, p.getContentType());

		} else if (p.isMimeType("application/x-pkcs7-mime")) {
			//er.detail("This is a x s/mime"+"  Content Name: "+p.getContent().getClass().getName());
			this.processPart(er, processSMIMEEnvelope(er, p, new ByteArrayInputStream(directCertificate), password));

		} else if (p.isMimeType("application/zip")) {
			//er.detail("This is a zip"+"  Content Name: "+p.getContent().getClass().getName());
			try {
				this.processZip(er, p);
			}  catch(IOException e) {
				logger.error("The content is not a valid XDM content\n" + ExceptionUtil.exception_details(e));
				validationSummary.recordKey(getShiftIndent(shiftNumber+1) + "The content is not a valid XDM content", Status.ERROR, false);
				er.error("No DTS", "XDM Error", "The content is not a valid XDM content", "", "-");
			} catch(XDMException e) {
				logger.error("The content is not a valid XDM content\n" + ExceptionUtil.exception_details(e));
				validationSummary.recordKey(getShiftIndent(shiftNumber+1) + "The content is not a valid XDM content", Status.ERROR, false);
				er.error("No DTS", "XDM Error", "The content is not a valid XDM content", "", "-");
			} catch(Exception e) {
				e.printStackTrace();
			}

		}  else if (p.isMimeType("application/x-zip-compressed")) {
			//er.detail("This is a zip"+"  Content Name: "+p.getContent().getClass().getName());
			try {
				this.processZip(er, p);
			}  catch(IOException e) {
				logger.error("The content is not a valid XDM content\n" + ExceptionUtil.exception_details(e));
				validationSummary.recordKey(getShiftIndent(shiftNumber+1) + "The content is not a valid XDM content", Status.ERROR, false);
				er.error("No DTS", "XDM Error", "The content is not a valid XDM content", "", "-");
			} catch(XDMException e) {
				logger.error("The content is not a valid XDM content\n" + ExceptionUtil.exception_details(e));
				validationSummary.recordKey(getShiftIndent(shiftNumber+1) + "The content is not a valid XDM content", Status.ERROR, false);
				er.error("No DTS", "XDM Error", "The content is not a valid XDM content", "", "-");
			} catch(Exception e) {
				e.printStackTrace();
			}

		} else if (p.isMimeType("application/octet-stream")) {
			//er.detail("This is a binary"+"  Content Name: "+p.getContent().getClass().getName());
			this.processOctetStream(er, p);

		} else if (p.isMimeType("application/xml")) {
			//er.detail("This is a binary"+"  Content Name: "+p.getContent().getClass().getName());
			this.processApplicationXML(er, p);

		} else if (p.isMimeType("multipart/signed")) {
			//er.detail("This is a signed multipart"+"  Content Name: "+p.getContent().getClass().getName());

			// DTS 129, Validate First MIME Part
			er.detail("====================Process Multipart/signed Part==========================");
			ProcessEnvelope process = new ProcessEnvelope();

			// Separate ErrorRecorder
			ErrorRecorder separate = new GwtErrorRecorder();
			process.validateMimeEntity(separate, p, validationSummary, shiftNumber);
			er.concat(separate);

			// Increase shift number
			shiftNumber++;

			MessageValidatorFacade msgValidator = new DirectMimeMessageValidatorFacade();
			msgValidator.validateFirstMIMEPart(er, true);


			// DTS 152, Validate Second MIME Part
			msgValidator.validateSecondMIMEPart(er, true);

			// DTS 155, Validate Content-Type
			msgValidator.validateContentType2(er, p.getContentType());

			SMIMESigned s = new SMIMESigned((MimeMultipart)p.getContent());

			// Find micalg
			String micalg = p.getContentType().split("micalg=")[1];
			if(micalg.contains(";")) {
				micalg = micalg.split(";")[0];
			}

			//
			// verify signature
			//
			verifySignature(er, s, micalg);
			//
			// extract the content
			//
			this.processPart(er, s.getContent());

		} else if (p.isMimeType("multipart/*")) {
			//er.detail("This is multipart"+"  Content Name: "+p.getContent().getClass().getName());

			// DTS 129, Validate First MIME Part
			er.detail("====================Process Multipart/mixed Part==========================");
			ProcessEnvelope process = new ProcessEnvelope();

			// Separate ErrorRecorder
			ErrorRecorder separate = new GwtErrorRecorder();
			process.validateMimeEntity(separate, p, validationSummary,shiftNumber);
			er.concat(separate);

			// Increase shift number to display indentation
			shiftNumber++;

			MessageValidatorFacade msgValidator = new DirectMimeMessageValidatorFacade();
			msgValidator.validateFirstMIMEPart(er, true);

			MimeMultipart mp;

			if(qpEncoded) {
				p = decodeQP(p);
				mp = (MimeMultipart) p;
			} else {
				mp = (MimeMultipart)p.getContent();
			}
			int count = mp.getCount();
			for (int i = 0; i < count; i++){
				this.processPart(er, mp.getBodyPart(i));
			}

		} else {
			er.detail("===================Unknown Part==========================");
			er.detail("Couldn't figure out the type"+"  Content Name: "+p.getContent().getClass().getName());

			// Log attachment
			String attachmentFilename = "attachment" + this.attachmentNumber + ".txt";
			if(p.getFileName() != null) {
				attachmentFilename = p.getFileName();
			} else {
				attachmentNumber++;
			}

			String attachmentLink = logAttachment(p, attachmentFilename);

			er.detail("#####################Unknown attachment######################");
			er.detail("attachment=" + attachmentLink + ";filename=" + attachmentFilename);
			er.detail("##########################################################");

			// Summary
			validationSummary.recordKey(getShiftIndent(shiftNumber) + "Part " + partNumber +": Unknown part type " + p.getContentType(), Status.PART, true);
			partNumber++;

		}

		//Save Attachments
		//		processAttachments(er, p);

	}

	/**
	 * Validates the envelope of the message
	 * */
	public void processEnvelope(ErrorRecorder er, Message m) throws Exception {
		//er.detail("Processing Envelope");
		ProcessEnvelope process = new ProcessEnvelope();
		MessageValidatorFacade msgValidator = new DirectMimeMessageValidatorFacade();

		// Verifying Outer message checks

		// Update the summary
		validationSummary.recordKey("Encrypted Message", Status.PART, true);


		// Separate ErrorRecorder
		ErrorRecorder separate2 = new GwtErrorRecorder();
		process.validateMessageHeader(separate2, m, validationSummary, 0, !wrappedParser.getWrapped());
		er.concat(separate2);

		// MIME Entity Validation

		// Separate ErrorRecorder
		ErrorRecorder separate = new GwtErrorRecorder();
		process.validateMimeEntity(separate, m, validationSummary, shiftNumber);
		er.concat(separate);

		// DTS 133a, Content-Type
		msgValidator.validateMessageContentTypeA(separate, m.getContentType());

		// DTS 201, Content-Type Name
		msgValidator.validateContentTypeNameOptional(separate, m.getContentType());

		// DTS 202, Content-Type S/MIME Type
		msgValidator.validateContentTypeSMIMETypeOptional(separate, m.getContentType());

		// DTS 203, Content-Disposition
		String contentDisposition = "";
		if(m.getFileName() != null) {
			contentDisposition = m.getFileName();
		}
		msgValidator.validateContentDispositionOptional(separate, contentDisposition);

		// DTS 161-194 Validate Content-Disposition Filename
		separate = new GwtErrorRecorder();
		if(m.getFileName() != null) {
			msgValidator.validateContentDispositionFilename(separate, m.getFileName());
			validationSummary.recordKey(getShiftIndent(shiftNumber) + "Content-Disposition: "+m.getDisposition() +"; filename="+m.getFileName(), separate.hasErrors(), true);
		}
		er.concat(separate);


		// Update the summary
		validationSummary.updateInfos("Encrypted Message", er.hasErrors(), true);
	}


	/**
	 * 
	 * */
	public void processText(ErrorRecorder er, Part p) throws Exception{
		//er.detail("Processing Text");
		er.detail("====================Process Text/plain Part==========================");
		ProcessEnvelope process = new ProcessEnvelope();

		// Summary
		validationSummary.recordKey(getShiftIndent(shiftNumber) + "Part " + partNumber +": text/plain interpreted as a text content", Status.PART, true);

		// Separate ErrorRecorder
		ErrorRecorder separate = new GwtErrorRecorder();
		process.validateMimeEntity(separate, p, validationSummary, shiftNumber+1);
		er.concat(separate);

		MessageValidatorFacade msgValidator = new DirectMimeMessageValidatorFacade();
		msgValidator.validateFirstMIMEPart(er, true);

		if(!qpEncoded) {
			msgValidator.validateBody(er, p, (String)p.getContent());			
		}
		//this.processAttachments(er, p);

		// Log attachment
		String attachmentFilename = "attachment" + this.attachmentNumber + ".txt";
		if(p.getFileName() != null) {
			attachmentFilename = p.getFileName();
		} else {
			attachmentNumber++;
		}

		String attachmentLink = logAttachment(p, attachmentFilename);

		er.detail("#####################text/plain message######################");
		String textContent = p.getContent().toString();
		System.out.println(textContent);
		// Decode if QP encoded
		if(qpEncoded) {
			textContent = decodeQPText(textContent);
		}
		//er.detail(SafeHtmlUtils.htmlEscape(textContent));
		er.detail("attachment=" + attachmentLink + ";filename=" + attachmentFilename);
		er.detail("##########################################################");

		// Update the summary
		validationSummary.updateInfos(getShiftIndent(shiftNumber) + "Part " + partNumber +": text/plain interpreted as a text content", separate.hasErrors(), true);
		partNumber++;
	}

	public void processTextHTML(ErrorRecorder er, Part p) throws Exception {
		er.detail("====================Process Text/html Part==========================");
		ProcessEnvelope process = new ProcessEnvelope();

		// Summary
		validationSummary.recordKey(getShiftIndent(shiftNumber) + "Part " + partNumber +": text/html interpreted as a html content", Status.PART, true);

		// Separate ErrorRecorder
		ErrorRecorder separate = new GwtErrorRecorder();
		process.validateMimeEntity(separate, p, validationSummary, shiftNumber+1);

		MessageValidatorFacade msgValidator = new DirectMimeMessageValidatorFacade();
		msgValidator.validateFirstMIMEPart(separate, true);

		// Only if not quoted printable encoded
		if(!qpEncoded) {
			msgValidator.validateBody(separate, p, (String)p.getContent());
		}
		er.concat(separate);
		//this.processAttachments(er, p);


		// Log attachment
		String attachmentFilename = "attachment" + attachmentNumber + ".html";
		if(p.getFileName() != null) {
			attachmentFilename = p.getFileName();
		} else {
			attachmentNumber++;
		}

		String attachmentLink = logAttachment(p, attachmentFilename);
		er.detail("#####################HTML attachment######################");
		er.detail("attachment=" + attachmentLink + ";filename=" + attachmentFilename);
		er.detail("##########################################################");

		/*
		er.detail("#####################text/html content######################");
		String textContent = p.getContent().toString();
		System.out.println(textContent);
		// Decode if QP encoded
		if(qpEncoded) {
			textContent = decodeQPText(textContent);
		}
		er.detail(SafeHtmlUtils.htmlEscape(textContent));
		er.detail("##########################################################");
		 */

		// Update the summary
		validationSummary.updateInfos(getShiftIndent(shiftNumber) + "Part " + partNumber +": text/html interpreted as a text content", separate.hasErrors(), true);
		partNumber++;
	}

	/**
	 * 
	 * */
	public void processTextXML(ErrorRecorder er, Part p) throws Exception{
		er.detail("====================Processing Text XML==========================");
		logger.info("Processing attachments, Validation context is " + vc.toString());

		// Update the summary
		validationSummary.recordKey(getShiftIndent(shiftNumber) + "Part " + partNumber +": text/xml interpreted as a CCDA content", Status.PART, true);

		ProcessEnvelope process = new ProcessEnvelope();

		// Separate ErrorRecorder
		ErrorRecorder separate = new GwtErrorRecorder();
		process.validateMimeEntity(separate, p, validationSummary, shiftNumber+1);
		er.concat(separate);

		// Send to C-CDA validation tool.
		InputStream attachmentContents;
		if(qpEncoded) {
			attachmentContents = decodeQPInputStream(p.getInputStream());
		} else {
			attachmentContents = p.getInputStream();
		}

		// Log attachment
		String attachmentFilename = "attachment" + attachmentNumber + ".xml";
		if(p.getFileName() != null) {
			attachmentFilename = p.getFileName();
		} else {
			attachmentNumber++;
		}

		String attachmentLink = logAttachment(p, attachmentFilename);
		er.detail("#####################CCDA attachment######################");
		er.detail("attachment=" + attachmentLink + ";filename=" + attachmentFilename);
		er.detail("##########################################################");

		/*
		// Display CCDA Document
		er.detail("#####################CCDA Content######################");
		//String html_formatted_ccda = new OMFormatter(p.getContent().toString()).toHtml();
		String html_formatted_ccda;
		if(qpEncoded) {
			html_formatted_ccda = SafeHtmlUtils.htmlEscape(decodeQPToString(p.getInputStream()));
		} else {
			html_formatted_ccda = SafeHtmlUtils.htmlEscape(p.getContent().toString());
		}
		er.detail(html_formatted_ccda);
		er.detail("####################################################");
		//logger.info(p.getContent().toString());
		 */


		byte[] contents = Io.getBytesFromInputStream(attachmentContents);

		if (new CdaDetector().isCDA(contents)) {
			// Warning: Mandatory for validation report
			er.detail("Input is CDA R2, try validation as CCDA");
			ValidationContext docVC = new ValidationContext();
			docVC.clone(vc);  // this leaves ccdaType in place since that is what is setting the expectations
			docVC.isDIRECT = false;
			docVC.isCCDA = true;

			if(directCertificate!=null) {
				MessageValidatorEngine mve = MessageValidatorFactoryFactory.messageValidatorFactory2I.getValidator((ErrorRecorderBuilder)er, contents, directCertificate, docVC, null);
				mve.run();
			}

			// Warning: Mandatory for validation report
			er.detail("CCDA Validation done");

		} else {
			er.detail("Is not a CDA R2 so no validation attempted");
		}

		// Update Summary
		validationSummary.updateInfos(getShiftIndent(shiftNumber) + "Part " + partNumber +": text/xml interpreted as a CCDA content", separate.hasErrors(), true);
		partNumber++;
	}

	public void processApplicationXML(ErrorRecorder er, Part p) throws Exception {
		if(p.getFileName() != null && p.getFileName().contains(".xsl")) {
			er.detail("\n====================Stylesheet found: " + p.getFileName() + "==========================\n");
			logger.info("Processing attachments application/xml, Validation context is " + vc.toString());
			validationSummary.recordKey(getShiftIndent(shiftNumber) + "Part " + partNumber +": application/xml interpreted stylesheet document", Status.PART, true);

			// Log attachment
			String attachmentFilename = "attachment" + attachmentNumber + ".xml";
			if(p.getFileName() != null) {
				attachmentFilename = p.getFileName();
			} else {
				attachmentNumber++;
			}

			String attachmentLink = logAttachment(p, attachmentFilename);
			er.detail("#####################Stylesheet attachment######################");
			er.detail("attachment=" + attachmentLink + ";filename=" + attachmentFilename);
			er.detail("##########################################################");

			/*
			er.detail("#####################XML Content######################");
			InputStream xsl = MimeUtility.decode(p.getInputStream(), MimeUtility.getEncoding(p.getDataHandler()));
			String xslString = IOUtils.toString(xsl, "UTF-8");
			er.detail(SafeHtmlUtils.htmlEscape(xslString));
			er.detail("####################################################");
			 */

			partNumber++;
		} else {
			er.detail("====================Application/xml==========================");
			logger.info("Processing attachments application/xml, Validation context is " + vc.toString());
			validationSummary.recordKey(getShiftIndent(shiftNumber) + "Part " + partNumber +": application/xml interpreted xml document", Status.PART, true);
			ProcessEnvelope process = new ProcessEnvelope();

			// Separate ErrorRecorder
			ErrorRecorder separate = new GwtErrorRecorder();
			process.validateMimeEntity(separate, p, validationSummary, shiftNumber+1);
			er.concat(separate);

			// Send to C-CDA validation tool.
			InputStream attachmentContents;
			if(qpEncoded) {
				attachmentContents = decodeQPInputStream(p.getInputStream());
			} else {
				attachmentContents = p.getInputStream();
			}

			// Log attachment
			String attachmentFilename = "attachment" + attachmentNumber + ".txt";
			if(p.getFileName() != null) {
				attachmentFilename = p.getFileName();
			} else {
				attachmentNumber++;
			}

			String attachmentLink = logAttachment(p, attachmentFilename);
			er.detail("#####################CCDA attachment######################");
			er.detail("attachment=" + attachmentLink + ";filename=" + attachmentFilename);
			er.detail("##########################################################");

			/*
			// Display CCDA Document
			er.detail("#####################XML Content######################");
			//String html_formatted_ccda = new OMFormatter(p.getContent().toString()).toHtml();
			String html_formatted_ccda;
			if(qpEncoded) {
				html_formatted_ccda = SafeHtmlUtils.htmlEscape(decodeQPToString(p.getInputStream()));
			} else {
				html_formatted_ccda = SafeHtmlUtils.htmlEscape(p.getContent().toString());
			}
			er.detail(html_formatted_ccda);
			er.detail("####################################################");
			//logger.info(p.getContent().toString());
			 */


			byte[] contents = Io.getBytesFromInputStream(attachmentContents);

			if (new CdaDetector().isCDA(contents)) {
				// Warning: Mandatory for validation report
				er.detail("Input is CDA R2, try validation as CCDA");
				ValidationContext docVC = new ValidationContext();
				docVC.clone(vc);  // this leaves ccdaType in place since that is what is setting the expectations
				docVC.isDIRECT = false;
				docVC.isCCDA = true;

				if(directCertificate!=null) {
					MessageValidatorEngine mve = MessageValidatorFactoryFactory.messageValidatorFactory2I.getValidator((ErrorRecorderBuilder)er, contents, directCertificate, docVC, null);
					mve.run();
				}

				// Warning: Mandatory for validation report
				er.detail("CCDA Validation done");

			} else {
				er.detail("Is not a CDA R2 so no validation attempted");
			}

			// Update Summary
			validationSummary.updateInfos(getShiftIndent(shiftNumber) + "Part " + partNumber +": application/xml interpreted xml document", separate.hasErrors(), true);
			partNumber++;

		}

	}

	/**
	 * verify the signature (assuming the cert is contained in the message)
	 */
	@SuppressWarnings("rawtypes")
	private void verifySignature(ErrorRecorder er, SMIMESigned s, String contentTypeMicalg) throws Exception{
		MessageValidatorFacade msgValidator = new DirectMimeMessageValidatorFacade();

		// Separate ErrorRecorder
		ErrorRecorder separate = new GwtErrorRecorder();

		// DTS-164, SignedData exists for the message
		msgValidator.validateSignedData(separate, s.getSignedContent());


		//
		// extract the information to verify the signatures.
		//

		//
		// certificates and crls passed in the signature
		//
		Store certs = s.getCertificates();


		//
		// SignerInfo blocks which contain the signatures
		//
		SignerInformationStore  signers = s.getSignerInfos();

		Collection c = signers.getSigners();
		Iterator it = c.iterator();

		String digestAlgOID = "";

		// DTS 167, SignedData.certificates must contain at least one certificate
		msgValidator.validateSignedDataAtLeastOneCertificate(separate, c);

		//
		// check each signer
		//
		while (it.hasNext())
		{
			SignerInformation   signer = (SignerInformation)it.next();
			// Get digest Algorihm OID
			digestAlgOID = signer.getDigestAlgOID();
			Collection certCollection = certs.getMatches(signer.getSID());

			Iterator certIt = certCollection.iterator();
			X509Certificate cert = null;
			try {
				cert = new JcaX509CertificateConverter().setProvider(BC).getCertificate((X509CertificateHolder)certIt.next());
			} catch (Exception e) {
				separate.error("No DTS", "Certificate File", "Cannot extract the signing certificate", "", "-");
				er.concat(separate);
				break;
			}

			er.sectionHeading("Validation Signature");

			// DTS 158, Second MIME Part Body
			msgValidator.validateSecondMIMEPartBody(separate, "");

			// DTS 165, AlgorithmIdentifier.algorithm
			msgValidator.validateDigestAlgorithmDirectMessage(separate, digestAlgOID, contentTypeMicalg);

			// DTS 166, SignedData.encapContentInfo
			msgValidator.validateSignedDataEncapContentInfo(separate, new String(cert.getSignature()));

			// DTS 222, tbsCertificate.signature.algorithm
			msgValidator.validateTbsCertificateSA(separate, cert.getSigAlgName());
			// needs signer.getDigestAlgorithmID(); and compare the two (needs to be the same)

			// DTS 225, tbsCertificate.subject
			msgValidator.validateTbsCertificateSubject(separate, cert.getSubjectDN().toString());

			// DTS 240, Extensions.subjectAltName
			// C-4 - cert/subjectAltName must contain either rfc822Name or dNSName extension
			// C-5 cert/subjectAltName/rfc822Name must be an email address - Conditional
			msgValidator.validateExtensionsSubjectAltName(separate, cert.getSubjectAlternativeNames());

			// C-2 - Key size <=2048
			//msgValidator.validateKeySize(er, new String(cert.getPublicKey()));


			// -------how to get other extension fields:
			//-------  cert.getExtensionValue("2.5.29.17")

			// verify that the sig is valid and that it was generated
			// when the certificate was current
			msgValidator.validateSignature(separate, cert, signer, BC);

			// Update summary
			validationSummary.updateSignatureStatus(separate.hasErrors());
			er.concat(separate);

		}
	}

	/**
	 * 
	 * */
	public Part processSMIMEEnvelope(ErrorRecorder er, Part p, InputStream certificate, String password) {
		//er.detail("Processing S/MIME");
		logger.info("Processing SMIME Envelope");
		//
		// Open the key store
		//
		KeyStore ks = null;
		try {
			ks = KeyStore.getInstance("PKCS12", "BC");
			logger.info("Created empty keystore");
		} catch (KeyStoreException e1) {
			er.error("No DTS", "Certificate file", "Error in keystore creation", e1.getMessage(), "-");
			logger.error("Error creating keystore of type PKCS12: " + ExceptionUtil.exception_details(e1));
		} catch (NoSuchProviderException e1) {
			logger.error("Error creating keystore of type PKCS12: " + ExceptionUtil.exception_details(e1));
			er.error("No DTS", "Certificate file", "Error in keystore creation", e1.getMessage(), "-");
		}

		// Message Validator
		MessageValidatorFacade msgValidator = new DirectMimeMessageValidatorFacade();

		try {
			if(password == null) {
				password="";
			} 
			ks.load(certificate, password.toCharArray());
			logger.info("Loaded certificate for decryption");
		} catch (NoSuchAlgorithmException e1) {
			logger.error("Error loading certificate (decryption): " + ExceptionUtil.exception_details(e1));
			er.error("No DTS", "Certificate file", "Error in loading certificate", e1.getMessage(), "-");
		} catch (CertificateException e1) {
			logger.error("Error loading certificate (decryption): " + ExceptionUtil.exception_details(e1));
			er.error("No DTS", "Certificate file", "Error in loading certificate", e1.getMessage(), "-");
		} catch (IOException e1) {
			logger.error("Error loading certificate (decryption): " + ExceptionUtil.exception_details(e1));
			er.error("No DTS", "Certificate file", "Error in loading certificate", e1.getMessage(), "-");
		} catch (Exception e1) {
			logger.error("Error loading certificate (decryption): " + ExceptionUtil.exception_details(e1) + e1.toString());
			er.error("No DTS", "Certificate file", "Error in loading certificate", e1.getMessage(), "-");
		}

		@SuppressWarnings("rawtypes")
		Enumeration e = null;
		try {
			e = ks.aliases();
		} catch (KeyStoreException e1) {
			logger.error("Error loading certificate aliases: " + ExceptionUtil.exception_details(e1));
			er.error("No DTS", "Certificate file", "Error in loading certificate", e1.getMessage(), "-");
		}
		String      keyAlias = null;

		if (e != null) {
			while (e.hasMoreElements())
			{
				String  alias = (String)e.nextElement();

				try {
					if (ks.isKeyEntry(alias))
					{
						keyAlias = alias;
					}
				} catch (KeyStoreException e1) {
					logger.error("Error extracting certificate alias: " + ExceptionUtil.exception_details(e1));
					er.error("No DTS", "Certificate file", "Error in loading certificate", e1.getMessage(), "-");
				}
			}
		}

		if (keyAlias == null)
		{
			logger.error("Can't find a private key in encryption keystore.");
			er.error("No DTS", "Certificate file", "Error in loading certificate", "", "-");

			// DTS 129, Message Body
			msgValidator.validateMessageBody(er, false);

			//			System.exit(0);
			return null;
		} else
			logger.info("Found private key alias: " + keyAlias);

		//
		// find the certificate for the private key and generate a 
		// suitable recipient identifier.
		//
		X509Certificate cert = null;
		try {
			cert = (X509Certificate)ks.getCertificate(keyAlias);
		} catch (KeyStoreException e1) {
			logger.error("Error extracting private key: " + ExceptionUtil.exception_details(e1));
			er.error("No DTS", "Certificate file", "Error extracting private key", e1.getMessage(), "-");
		}
		RecipientId     recId = new JceKeyTransRecipientId(cert);

		SMIMEEnveloped m = null;
		try {
			m = new SMIMEEnveloped((MimeMessage)p);
		} catch (MessagingException e1) {
			logger.error("Error un-enveloping message body: " + ExceptionUtil.exception_details(e1));
			er.error("No DTS", "Certificate file", "Error extracting private key", e1.getMessage(), "-");
		} catch (CMSException e1) {
			logger.error("Error un-enveloping message body: " + ExceptionUtil.exception_details(e1));
			er.error("No DTS", "Certificate file", "Error extracting private key", e1.getMessage(), "-");
		}
		RecipientInformationStore   recipients = m.getRecipientInfos();
		RecipientInformation        recipient = recipients.get(recId);

		MimeBodyPart res = null;
		try {
			PrivateKey pkey = (PrivateKey)ks.getKey(keyAlias, null);
			res = SMIMEUtil.toMimeBodyPart(recipient.getContent(new JceKeyTransEnvelopedRecipient(pkey).setProvider("BC")));
		} catch (UnrecoverableKeyException e1) {
			logger.error("Error extracting MIME body part: " + ExceptionUtil.exception_details(e1));
			er.error("No DTS", "Certificate file", "Error extracting MIME body part", e1.getMessage(), "-");
		} catch (KeyStoreException e1) {
			logger.error("Error extracting MIME body part: " + ExceptionUtil.exception_details(e1));
			er.error("No DTS", "Certificate file", "Error extracting MIME body part", e1.getMessage(), "-");
		} catch (NoSuchAlgorithmException e1) {
			logger.error("Error extracting MIME body part: " + ExceptionUtil.exception_details(e1));
			er.error("No DTS", "Certificate file", "Error extracting MIME body part", e1.getMessage(), "-");
		} catch (SMIMEException e1) {
			logger.error("Error extracting MIME body part: " + ExceptionUtil.exception_details(e1));
			er.error("No DTS", "Certificate file", "Error extracting MIME body part", e1.getMessage(), "-");
		} catch (CMSException e1) {
			logger.error("Error extracting MIME body part: " + ExceptionUtil.exception_details(e1));
			er.error("No DTS", "Certificate file", "Error extracting MIME body part", e1.getMessage(), "-");
		}  catch (Exception e1) {
			er.error("No DTS", "Certificate file", "Error extracting MIME body part", e1.getMessage(), "-");
		}


		if(res==null) {
			// DTS 129, Message Body
			msgValidator.validateMessageBody(er, false);
		} else {
			// DTS 129, Message Body
			msgValidator.validateMessageBody(er, true);
		}

		er.detail("====================Inner decrypted Message==========================");


		// Description: the first MIME part is the content of the message and is referred to by Direct as the
		// "Health Content Container", and the second MIME part is the signature.
		Part mimeEntityBodyPart = (Part) res;
		// Validate Inner decrypted message
		ProcessEnvelope process = new ProcessEnvelope();
		// Separate ErrorRecorder
		ErrorRecorder separate = new GwtErrorRecorder();
		try {
			process.validateDirectMessageInnerDecryptedMessage(separate, mimeEntityBodyPart);
			er.concat(separate);
		} catch (Exception e1) {
			logger.error("Error validating Direct decrypted message: " + ExceptionUtil.exception_details(e1));
		}

		// Update summary
		validationSummary.recordKey("Decrypted Message", Status.PART, true);
		validationSummary.updateInfos("Decrypted Message", separate.hasErrors(), true);

		return res;
	}


	/**
	 * 
	 * */
	//TODO should probably be replaced later by a call to an XDM validator in the toolkit
	public void processZip(ErrorRecorder er, Part p) throws IOException, XDMException, Exception {

		// Update summary
		validationSummary.recordKey(getShiftIndent(shiftNumber) + "Part " + partNumber +": application/zip interpreted as a XDM Content", Status.PART, true);

		// DTS 129, Validate First MIME Part
		er.detail("====================Process Zip Part==========================");
		ProcessEnvelope process = new ProcessEnvelope();

		// Separate ErrorRecorder
		ErrorRecorder separate = new GwtErrorRecorder();
		process.validateMimeEntity(separate, p, validationSummary, shiftNumber+1);
		er.concat(separate);
		MessageValidatorFacade msgValidator = new DirectMimeMessageValidatorFacade();
		msgValidator.validateFirstMIMEPart(er, true);

		// Update summary
		validationSummary.updateInfos(getShiftIndent(shiftNumber) + "Part " + partNumber +": application/zip interpreted as a XDM Content", separate.hasErrors(), true);

		partNumber++;

		InputStream attachmentContents = p.getInputStream();
		byte[] contents = Io.getBytesFromInputStream(attachmentContents);

		// Use the XDMDecoder to make sure it is XMD content before running XDM validator
		//		XdmDecoder decoder = new XdmDecoder(vc, (ErrorRecorderBuilder)er, new ByteArrayInputStream(contents));
		//		decoder.detect(new ByteArrayInputStream(contents));

		// Warning: Mandatory for validation report
		er.detail("Try validation as XDM");

		ValidationContext docVC = new ValidationContext();
		docVC.clone(vc);  // this leaves ccdaType in place since that is what is setting the expectations
		docVC.isDIRECT = false;
		docVC.isCCDA = false;
		docVC.isXDM = true;

		MessageValidatorEngine mve = MessageValidatorFactoryFactory.messageValidatorFactory2I.getValidator((ErrorRecorderBuilder)er, contents, directCertificate, docVC, null);
		mve.run();

		// Warning: Mandatory for validation report
		er.detail("XDM Validation done");

		// Log attachment
		String attachmentFilename = "attachment" + attachmentNumber + ".zip";
		if(p.getFileName() != null) {
			attachmentFilename = p.getFileName();
		} else {
			attachmentNumber++;
		}

		String attachmentLink = logAttachment(p, attachmentFilename);
		er.detail("#####################Zip attachment######################");
		er.detail("attachment=" + attachmentLink + ";filename=" + attachmentFilename);
		er.detail("##########################################################");


	}

	/**
	 * 
	 * */
	public void processOctetStream(ErrorRecorder er, Part p) throws Exception{
		//er.detail("Processing Octet Stream");

		// Update summary
		validationSummary.recordKey(getShiftIndent(shiftNumber) + "Part " + partNumber +": octet/stream", Status.PART, true);

		// DTS 129, Validate First MIME Part
		er.detail("====================Process Octet Stream Part==========================");
		ProcessEnvelope process = new ProcessEnvelope();

		// Separate ErrorRecorder
		ErrorRecorder separate = new GwtErrorRecorder();
		process.validateMimeEntity(separate, p, validationSummary, shiftNumber+1);
		er.concat(separate);

		MessageValidatorFacade msgValidator = new DirectMimeMessageValidatorFacade();
		msgValidator.validateFirstMIMEPart(er, true);

		// Update summary
		validationSummary.updateInfos(getShiftIndent(shiftNumber) + "Part " + partNumber +": octet/stream", separate.hasErrors(), true);
		partNumber++;

		InputStream attachmentContents = p.getInputStream();
		if(qpEncoded) {
			attachmentContents = decodeQPInputStream(attachmentContents);
		}
		byte[] contents = Io.getBytesFromInputStream(attachmentContents);

		if(attachmentContents.markSupported()) {
			attachmentContents.reset();
		}


		// Log attachment
		String attachmentFilename = "attachment" + attachmentNumber + ".xml";
		if(p.getFileName() != null) {
			attachmentFilename = p.getFileName();
		} else {
			attachmentNumber++;
		}

		String attachmentLink = logAttachment(p, attachmentFilename);
		er.detail("#####################XML attachment######################");
		er.detail("attachment=" + attachmentLink + ";filename=" + attachmentFilename);
		er.detail("##########################################################");

		/*
		// If it is a XML file
		if(p.getFileName().contains(".xml")) {
			er.detail("#######################XML File############################");
			if(qpEncoded) {
				er.detail(SafeHtmlUtils.htmlEscape(decodeQPToString(p.getInputStream())));
			} else {
				er.detail(SafeHtmlUtils.htmlEscape(new String(contents)));
			}
			er.detail("##########################################################");
		} else {

			// Use the XDMDecoder to make sure it is XMD content before running XDM validator
			XdmDecoder decoder = new XdmDecoder(vc, (ErrorRecorderBuilder)er, new ByteArrayInputStream(contents));
			try {
				decoder.detect(new ByteArrayInputStream(contents));
			} catch(IOException e) {
				er.detail("The file is not an XDM content");
				return;
			} catch(XDMException e) {
				er.detail("The file is not an XDM content");
				return;
			}
			this.processAttachments(er, p);

		}
		 */

	}


	/**
	 * Saves attachment to file if desired. Sends it to C-CDA validation tool.
	 * @param er
	 * @param p
	 * @throws Exception
	 */
	public void processAttachments(ErrorRecorder er, Part p) throws Exception{
		er.detail("Try validation as XDM");
		ValidationContext docVC = new ValidationContext();
		docVC.clone(vc);  // this leaves ccdaType in place since that is what is setting the expectations
		docVC.isDIRECT = false;
		docVC.isCCDA = false;
		docVC.isXDM = true;

		InputStream attachmentContents = p.getInputStream();
		byte[] contents = Io.getBytesFromInputStream(attachmentContents);

		MessageValidatorEngine mve = MessageValidatorFactoryFactory.messageValidatorFactory2I.getValidator((ErrorRecorderBuilder)er, contents, directCertificate, docVC, null);
		mve.run();


		//			logger.info("Processing attachments, Validation context is " + vc.toString());
		//			
		//			// Send to C-CDA validation tool.
		//			InputStream attachmentContents = p.getInputStream();
		//
		//			byte[] contents = Io.getBytesFromInputStream(attachmentContents);
		//			
		//			er.detail("Forcing validation of attachment as CCDA");
		//			// This should be driven by Part type information  - this will do for now
		//			ValidationContext docVC = new ValidationContext();
		//			docVC.clone(vc);
		//			docVC.isDIRECT = false;
		//			docVC.isCCDA = true;
		//			
		//			MessageValidatorEngine mve = MessageValidatorFactoryFactory.messageValidatorFactory2I.getValidator((ErrorRecorderBuilder)er, contents, directCertificate, docVC, null);
		//			//			MessageValidatorEngine mve = MessageValidatorFactory.getValidator((ErrorRecorderBuilder)er, contents, null, vc, null);
		//			mve.run();
		//er.detail("Attachment not processed because mimeType is multipart");
	}



	/**
	 *  If we're saving attachments, write out anything that
	 * looks like an attachment into an appropriately named
	 * file.  Don't overwrite existing files to prevent
	 * mistakes.
	 * */
	public void saveAttachmentToFile(ErrorRecorder er, Part p) throws Exception{

		if (!p.isMimeType("multipart/*") && !(p instanceof Message)){
			String filename = p.getFileName();			
			String disp = p.getDisposition();

			// many mailers don't include a Content-Disposition
			if (disp == null || disp.equalsIgnoreCase(Part.ATTACHMENT)) {
				if ( filename == null)
					filename = "Attachment" + attnum++;
				er.detail("---------Attachment Processing------------------");
				er.detail("Saving attachment to file " + filename);
				try {
					File f = new File(filename);
					if (f.exists())
						// XXX - could try a series of names
						throw new IOException("file exists");
					((MimeBodyPart)p).saveFile(f);
				} catch (IOException ex) {
					er.detail("Failed to save attachment: " + ex);
				}
				er.detail("---------------------------");
			}
		}
	}


	// Write DIRECT log
	public void logDirectMessage(Part p){

		// Get sender name (username)
		String username = null;
		String _username = null;
		try {
			Address[] addr = ((MimeMessage) p).getFrom();
			_username = (addr[0]).toString();
			username = Utils.rawFromHeader(_username);
		} catch (MessagingException e3) {
			// TODO Auto-generated catch block
			e3.printStackTrace();
		}

		// Get MDN message ID 
		String _messageID = null;
		try {
			_messageID = ((MimeMessage) p).getMessageID();
		} catch (MessagingException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		String messageID = Utils.rawFromHeader(_messageID);

		// Get  reception time - Logging system date instead of SUT sender date contained in headers
		//Date date = new Date();

		// Get label - TODO
		String label = "";

		MessageLogManager.logDirectMessage(username, logDate, "DIRECT_RECEIVE", "DIRECT", messageID, (MimeMessage)p, label);


		System.out.println("Logged direct message.");

	}


	/**
	 * 
	 * */
	@SuppressWarnings({ "rawtypes", "unused" })
	public void info(Part p) throws Exception{
		Enumeration e = p.getAllHeaders();
		while (e.hasMoreElements()){
			Header header = (Header)e.nextElement();
			//er.detail("header: "+header.getName()+" value: "+header.getValue());
		}

		System.out.println("Data handler: "+p.getDataHandler().getClass().getName());
		System.out.println("Line count: "+p.getLineCount());
	}

	public String getShiftIndent(int shiftNumber) {
		String shiftIndent = "";
		for(int k=0;k<shiftNumber;k++) {
			shiftIndent += "-----";					
		}
		return shiftIndent;
	}

	public MimeBodyPart decodeQP(Part p) throws MessagingException, IOException {
		InputStream res = MimeUtility.decode(p.getInputStream(), "quoted-printable");
		MimeBodyPart decodedPart = new MimeBodyPart(res);
		return decodedPart;
	}

	public InputStream decodeQPInputStream(InputStream encodedQP) throws MessagingException {
		InputStream res = MimeUtility.decode(encodedQP, "quoted-printable");
		return res;
	}

	public String decodeQPToString(InputStream encodedQP) throws MessagingException, IOException {
		InputStream res = MimeUtility.decode(encodedQP, "quoted-printable");
		return IOUtils.toString(res, "UTF-8");
	}

	public String decodeQPText(String text) throws UnsupportedEncodingException {
		return MimeUtility.decodeText(text);
	}

	public String logAttachment(Part p, String attachmentFilename) {
		String attachmentLink = "";
		try {
			MessageLogManager.logAttachment(this.username, this.logDate, this.messageId, "DIRECT_RECEIVE", "DIRECT", p.getInputStream(), attachmentFilename);
			attachmentLink = MessageLogManager.getAttachmentLink(this.username, this.logDate, this.messageId, "DIRECT_RECEIVE", "DIRECT", attachmentFilename);
		} catch(Exception e) {
			e.printStackTrace();
		}

		return attachmentLink;
	}


}
