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


package gov.nist.direct.messageParser.impl;

import gov.nist.direct.messageParser.DirectMessageProcessor;
import gov.nist.direct.messageParser.MessageParser;
import gov.nist.direct.validation.MessageValidatorFacade;
import gov.nist.direct.validation.impl.DirectMimeMessageValidatorFacade;
import gov.nist.direct.validation.impl.ProcessEnvelope;
import gov.nist.toolkit.MessageValidatorFactory2.MessageValidatorFactoryFactory;
import gov.nist.toolkit.errorrecording.ErrorRecorder;
import gov.nist.toolkit.errorrecording.factories.ErrorRecorderBuilder;
import gov.nist.toolkit.utilities.io.Io;
import gov.nist.toolkit.valccda.CdaDetector;
import gov.nist.toolkit.valsupport.client.ValidationContext;
import gov.nist.toolkit.valsupport.engine.MessageValidatorEngine;
import gov.nist.toolkit.valsupport.errrec.GwtErrorRecorder;
import gov.nist.toolkit.xdsexception.ExceptionUtil;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.activation.CommandMap;
import javax.activation.MailcapCommandMap;
import javax.mail.Header;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

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

public class DirectMimeMessageProcessor implements DirectMessageProcessor {

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
	ValidationContext vc;
	private LinkedHashMap<String, Integer> summary = new LinkedHashMap<String, Integer>();
	private int partNumber;
	private int shiftNumber;

	public void processAndValidateDirectMessage(ErrorRecorder er, byte[] inputDirectMessage, byte[] _directCertificate, String _password, ValidationContext vc){
		directCertificate = _directCertificate;
		password = _password;
		this.vc = vc;
		
		// Set the part number to 1
		partNumber = 1;
		// Set shift number to 1
		shiftNumber = 1;

		logger.debug("ValidationContext is " + vc.toString());
		MessageParser<MimeMessage> parser = new MimeMessageParser();
		MimeMessage mm = parser.parseMessage(er, inputDirectMessage);
		try {
			processPart(er, mm);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
		er.detail("");
		er.detail("############################Message Content Summary################################");
		er.detail("");
		
		// Put the signature at the end
		if(summary.containsKey("Signature")) {
			int signatureValidation = summary.get("Signature");
			summary.remove("Signature");
			summary.put("Signature", signatureValidation);
		}
		
		String partPattern = "-*Part [0-9][0-9]?:.*";
		
		Set cles = summary.keySet();
		Iterator it = cles.iterator();
		int previousValue = 0;
		while (it.hasNext()) {
			String key = (String) it.next();
			int value = summary.get(key);
			
			// Determine if it is a part
			Pattern pattern = Pattern.compile(partPattern, Pattern.CASE_INSENSITIVE);
			Matcher matcher = pattern.matcher(key);
			
			if(value==previousValue) {
				if(matcher.matches()){
					er.sectionHeading(key);
				} else {
					er.detail(key);					
				}
			} else if(value<previousValue) {
				if(matcher.matches()){
					er.sectionHeading(key);
				} else {
					er.detail(key);
				}
				previousValue=value;
			} else {
				if(matcher.matches()){
					er.sectionHeading(key);
				} else {
					er.err("", key, "", "", "");
				}
				if((value-previousValue)==1) {
					previousValue++;
				}
			}
		}
	}

	/**
	 * 
	 *  Validates a part of the message*/
	public void processPart(ErrorRecorder er, Part p) throws Exception{
		
		if (p == null)
			return;
		//er.detail("Processing Part");
		// If the Part is a Message then first validate the Envelope
		if (p instanceof Message){
			er.detail("Detected an Envelope");
			er.detail("\n====================Outer Enveloped Message==========================\n");
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

		} else if (p.isMimeType("text/xml")) {
			//er.detail("This is plain text xml"+"  Content Name: "+p.getContent().getClass().getName());
			this.processTextXML(er, p);

		} else if (p.isMimeType("message/rfc822")) {
			//er.detail("This is a nested message"+"  Content Name: "+p.getContent().getClass().getName());
			
			// Summary
			summary.put(getShiftIndent(shiftNumber) + "Part " + partNumber +": message/rfc822 interpreted as a message", er.getNbErrors());
			
			p = (Part)p.getContent();
			if (p instanceof Message) {
				er.detail("Detected an Envelope");
				er.detail("\n====================Message RFC 822==========================\n");
				ProcessEnvelope process = new ProcessEnvelope();
				
				// Separate ErrorRecorder
				ErrorRecorder separate = new GwtErrorRecorder();
				
				process.validateMimeEntity(separate, p, summary, shiftNumber+1);
				er.concat(separate);
				
				// Separate ErrorRecorder 2
				ErrorRecorder separate2 = new GwtErrorRecorder();
				
				process.validateMessageHeader(separate2, (Message)p, summary, partNumber);
				er.concat(separate2);

				// DTS 151, Validate First MIME Part Body
				MessageValidatorFacade msgValidator = new DirectMimeMessageValidatorFacade();
				msgValidator.validateFirstMIMEPartBody(er, true);

				// Update summary
				separate.concat(separate2);
				
				summary.put(getShiftIndent(shiftNumber) + "Part " + partNumber +": message/rfc822 interpreted as a message", separate.getNbErrors());
				partNumber++;

				if(p.getContent() instanceof MimeMultipart) {
					shiftNumber++;
					partNumber=1;
					MimeMultipart mp = (MimeMultipart)p.getContent();
					int count = mp.getCount();
					for (int i = 0; i < count; i++){
						this.processPart(er, mp.getBodyPart(i));
					}
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

		} else if (p.isMimeType("application/x-pkcs7-mime")) {
			//er.detail("This is a x s/mime"+"  Content Name: "+p.getContent().getClass().getName());

		} else if (p.isMimeType("application/zip")) {
			//er.detail("This is a zip"+"  Content Name: "+p.getContent().getClass().getName());
			this.processZip(er, p);

		}  else if (p.isMimeType("application/x-zip-compressed")) {
			//er.detail("This is a zip"+"  Content Name: "+p.getContent().getClass().getName());
			this.processZip(er, p);

		} else if (p.isMimeType("application/octet-stream")) {
			//er.detail("This is a binary"+"  Content Name: "+p.getContent().getClass().getName());
			this.processOctetStream(er, p);

		} else if (p.isMimeType("multipart/signed")) {
			//er.detail("This is a signed multipart"+"  Content Name: "+p.getContent().getClass().getName());

			// DTS 129, Validate First MIME Part
			er.detail("\n====================Process Multipart/signed Part==========================\n");
			ProcessEnvelope process = new ProcessEnvelope();
			
			// Separate ErrorRecorder
			ErrorRecorder separate = new GwtErrorRecorder();
			process.validateMimeEntity(separate, p, summary, shiftNumber);
			er.concat(separate);
			
			// Increase shift number
			shiftNumber++;
			
			MessageValidatorFacade msgValidator = new DirectMimeMessageValidatorFacade();
			msgValidator.validateFirstMIMEPart(er, true);


			// DTS 152, Validate Second MIME Part
			msgValidator.validateSecondMIMEPart(er, true);

			// DTS 155, Validate Content-Type
			msgValidator.validateContentType2(er, p.getContentType());

			org.bouncycastle.mail.smime.handlers.multipart_signed dsf;
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
			er.detail("\n====================Process Multipart/mixed Part==========================\n");
			ProcessEnvelope process = new ProcessEnvelope();
			
			// Separate ErrorRecorder
			ErrorRecorder separate = new GwtErrorRecorder();
			process.validateMimeEntity(separate, p, summary, shiftNumber);
			er.concat(separate);
			
			// Increase shift number to display indentation
			shiftNumber++;
			
			MessageValidatorFacade msgValidator = new DirectMimeMessageValidatorFacade();
			msgValidator.validateFirstMIMEPart(er, true);


			MimeMultipart mp = (MimeMultipart)p.getContent();
			int count = mp.getCount();
			for (int i = 0; i < count; i++){
				this.processPart(er, mp.getBodyPart(i));	
			}

		} else {
			er.detail("\n===================Unknown Part==========================\n");
			er.detail("Couldn't figure out the type"+"  Content Name: "+p.getContent().getClass().getName());
			// Summary
			summary.put(getShiftIndent(shiftNumber) + "Part " + partNumber +": Unknown part type", 0);
			partNumber++;

		}

		//Save Attachments
		//		processAttachments(er, p);

	}

	/**
	 * Validates the envelope of the message
	 * */
	public void processEnvelope(ErrorRecorder er, Message m) throws Exception {
		er.detail("Processing Envelope");
		ProcessEnvelope process = new ProcessEnvelope();
		MessageValidatorFacade msgValidator = new DirectMimeMessageValidatorFacade();

		// Verifying Outer message checks
		
		// Update the summary
		summary.put("Encrypted Message", er.getNbErrors());

		
		// Separate ErrorRecorder
		ErrorRecorder separate2 = new GwtErrorRecorder();
		process.validateMessageHeader(separate2, m, summary, 0);
		er.concat(separate2);
		
		// MIME Entity Validation

		// Separate ErrorRecorder
		ErrorRecorder separate = new GwtErrorRecorder();
		process.validateMimeEntity(separate, m, summary, shiftNumber);
		er.concat(separate);
		
		// DTS 133a, Content-Type
		msgValidator.validateMessageContentTypeA(separate, m.getContentType());
		summary.put(getShiftIndent(shiftNumber) + "Content-type: "+m.getContentType(), separate.getNbErrors());
		
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
		

		// Update the summary
		summary.put("Encrypted Message", er.getNbErrors());
	}


	/**
	 * 
	 * */
	public void processText(ErrorRecorder er, Part p) throws Exception{
		//er.detail("Processing Text");
		System.out.println(p.getContent());
		er.detail("\n====================Process Text/plain Part==========================\n");
		ProcessEnvelope process = new ProcessEnvelope();
		
		// Summary
		summary.put(getShiftIndent(shiftNumber) + "Part " + partNumber +": plain/text interpreted as a text content", er.getNbErrors());
		
		// Separate ErrorRecorder
		ErrorRecorder separate = new GwtErrorRecorder();
		process.validateMimeEntity(separate, p, summary, shiftNumber+1);
		er.concat(separate);
		
		MessageValidatorFacade msgValidator = new DirectMimeMessageValidatorFacade();
		msgValidator.validateFirstMIMEPart(er, true);
		msgValidator.validateBody(er, p, (String)p.getContent());
		//this.processAttachments(er, p);
		
		// Update the summary
		summary.put(getShiftIndent(shiftNumber) + "Part " + partNumber +": plain/text interpreted as a text content", separate.getNbErrors());
		partNumber++;
	}

	/**
	 * 
	 * */
	public void processTextXML(ErrorRecorder er, Part p) throws Exception{
		er.detail("\n====================Processing Text XML==========================\n");
		logger.info("Processing attachments, Validation context is " + vc.toString());

		// Send to C-CDA validation tool.
		InputStream attachmentContents = p.getInputStream();

		byte[] contents = Io.getBytesFromInputStream(attachmentContents);

		if (new CdaDetector().isCDA(contents)) {
			er.detail("Input is CDA R2, try validation as CCDA");
			ValidationContext docVC = new ValidationContext();
			docVC.clone(vc);  // this leaves ccdaType in place since that is what is setting the expectations
			docVC.isDIRECT = false;
			docVC.isCCDA = true;

			MessageValidatorEngine mve = MessageValidatorFactoryFactory.messageValidatorFactory2I.getValidator((ErrorRecorderBuilder)er, contents, directCertificate, docVC, null);
			mve.run();
		} else {
			er.detail("Is not a CDA R2 so no validation attempted");
		}
		
		// Update the summary
		summary.put(getShiftIndent(shiftNumber) + "Part " + partNumber +": text/xml interpreted as a CCDA content", er.getNbErrors());
		partNumber++;
	}

	/**
	 * verify the signature (assuming the cert is contained in the message)
	 */
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


		// DTS 167, SignedData.certificates must contain at least one certificate
		msgValidator.validateSignedDataAtLeastOneCertificate(separate, c);

		//
		// check each signer
		//
		while (it.hasNext())
		{
			SignerInformation   signer = (SignerInformation)it.next();
			Collection certCollection = certs.getMatches(signer.getSID());

			Iterator certIt = certCollection.iterator();
			X509Certificate cert = null;
			try {
				cert = new JcaX509CertificateConverter().setProvider(BC).getCertificate((X509CertificateHolder)certIt.next());
			} catch (Exception e) {
				separate.err("", "Cannot extract the signing certificate", "", "", "");
				summary.put("Signature", separate.getNbErrors());
				er.concat(separate);
				break;
			}


			//System.out.println(cert);

			er.sectionHeading("Validation Signature");
			
			// DTS 158, Second MIME Part Body
			msgValidator.validateSecondMIMEPartBody(separate, "");

			// DTS 165, AlgorithmIdentifier.algorithm
			msgValidator.validateDigestAlgorithmDirectMessage(separate, cert.getSigAlgName().toLowerCase(), contentTypeMicalg);

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
			summary.put("Signature", separate.getNbErrors());
			er.concat(separate);

		}
	}

	/**
	 * 
	 * */
	public Part processSMIMEEnvelope(ErrorRecorder er, Part p, InputStream certificate, String password) {
		er.detail("Processing S/MIME");
		logger.info("Processing SMIME Envelope");
		//
		// Open the key store
		//
		KeyStore ks = null;
		try {
			ks = KeyStore.getInstance("PKCS12", "BC");
			logger.info("Created empty keystore");
		} catch (KeyStoreException e1) {
			er.err("0", "Error in keystore creation", "", "", "Certificate file");
			logger.error("Error creating keystore of type PKCS12: " + ExceptionUtil.exception_details(e1));
		} catch (NoSuchProviderException e1) {
			logger.error("Error creating keystore of type PKCS12: " + ExceptionUtil.exception_details(e1));
			er.err("0", "Error in keystore creation", "", "", "Certificate file");
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
			er.err("0", "Error in loading certificate", "", "", "Certificate file");
		} catch (CertificateException e1) {
			logger.error("Error loading certificate (decryption): " + ExceptionUtil.exception_details(e1));
			er.err("0", "Error in loading certificate", "", "", "Certificate file");
		} catch (IOException e1) {
			logger.error("Error loading certificate (decryption): " + ExceptionUtil.exception_details(e1));
			er.err("0", "Error in loading certificate (decryption)", "", "", "Certificate file");
		}

		@SuppressWarnings("rawtypes")
		Enumeration e = null;
		try {
			e = ks.aliases();
		} catch (KeyStoreException e1) {
			logger.error("Error loading certificate aliases: " + ExceptionUtil.exception_details(e1));
			er.err("0", "Error in loading certificate", "", "", "Certificate file");
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
					er.err("0", "Error in loading certificate", "", "", "Certificate file");
				}
			}
		}

		if (keyAlias == null)
		{
			logger.error("Can't find a private key in encryption keystore.");
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
		}
		RecipientId     recId = new JceKeyTransRecipientId(cert);

		SMIMEEnveloped m = null;
		try {
			m = new SMIMEEnveloped((MimeMessage)p);
		} catch (MessagingException e1) {
			logger.error("Error un-enveloping message body: " + ExceptionUtil.exception_details(e1));
		} catch (CMSException e1) {
			logger.error("Error un-enveloping message body: " + ExceptionUtil.exception_details(e1));
		}
		RecipientInformationStore   recipients = m.getRecipientInfos();
		RecipientInformation        recipient = recipients.get(recId);

		MimeBodyPart res = null;
		try {
			PrivateKey pkey = (PrivateKey)ks.getKey(keyAlias, null);
			res = SMIMEUtil.toMimeBodyPart(recipient.getContent(new JceKeyTransEnvelopedRecipient(pkey).setProvider("BC")));
		} catch (UnrecoverableKeyException e1) {
			logger.error("Error extracting MIME body part: " + ExceptionUtil.exception_details(e1));
		} catch (KeyStoreException e1) {
			logger.error("Error extracting MIME body part: " + ExceptionUtil.exception_details(e1));
		} catch (NoSuchAlgorithmException e1) {
			logger.error("Error extracting MIME body part: " + ExceptionUtil.exception_details(e1));
		} catch (SMIMEException e1) {
			logger.error("Error extracting MIME body part: " + ExceptionUtil.exception_details(e1));
		} catch (CMSException e1) {
			logger.error("Error extracting MIME body part: " + ExceptionUtil.exception_details(e1));
		}

		if(res==null) {
			// DTS 129, Message Body
			msgValidator.validateMessageBody(er, false);
		} else {
			// DTS 129, Message Body
			msgValidator.validateMessageBody(er, true);
		}

		er.detail("\n====================Inner decrypted Message==========================\n");


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
		summary.put("", 0);
		summary.put("Decrypted Message", separate.getNbErrors());

		return res;
	}


	/**
	 * 
	 * */
	//TODO should probably be replaced later by a call to an XDM validator in the toolkit
	public void processZip(ErrorRecorder er, Part p) throws Exception{

		// Update summary
		summary.put(getShiftIndent(shiftNumber) + "Part " + partNumber +": application/zip interpreted as a XDM Content", er.getNbErrors());
		
		// DTS 129, Validate First MIME Part
		er.detail("\n====================Process Zip Part==========================\n");
		ProcessEnvelope process = new ProcessEnvelope();
		
		// Separate ErrorRecorder
		ErrorRecorder separate = new GwtErrorRecorder();
		process.validateMimeEntity(separate, p, summary, shiftNumber+1);
		er.concat(separate);
		MessageValidatorFacade msgValidator = new DirectMimeMessageValidatorFacade();
		msgValidator.validateFirstMIMEPart(er, true);
		
		// Update summary
		summary.put(getShiftIndent(shiftNumber) + "Part " + partNumber +": application/zip interpreted as a XDM Content", separate.getNbErrors());
		
		partNumber++;
		
		InputStream attachmentContents = p.getInputStream();
		byte[] contents = Io.getBytesFromInputStream(attachmentContents);

		er.detail("Try validation as XDM");
		ValidationContext docVC = new ValidationContext();
		docVC.clone(vc);  // this leaves ccdaType in place since that is what is setting the expectations
		docVC.isDIRECT = false;
		docVC.isCCDA = false;
		docVC.isXDM = true;

		MessageValidatorEngine mve = MessageValidatorFactoryFactory.messageValidatorFactory2I.getValidator((ErrorRecorderBuilder)er, contents, directCertificate, docVC, null);
		mve.run();
	}

	/**
	 * 
	 * */
	public void processOctetStream(ErrorRecorder er, Part p) throws Exception{
		//er.detail("Processing Octet Stream");

		// Update summary
		summary.put(getShiftIndent(shiftNumber) + "Part " + partNumber +": octet/stream", er.getNbErrors());
		
		// DTS 129, Validate First MIME Part
		er.detail("\n====================Process Octet Stream Part==========================\n");
		ProcessEnvelope process = new ProcessEnvelope();
		
		// Separate ErrorRecorder
		ErrorRecorder separate = new GwtErrorRecorder();
		process.validateMimeEntity(separate, p, summary, shiftNumber+1);
		er.concat(separate);
		
		MessageValidatorFacade msgValidator = new DirectMimeMessageValidatorFacade();
		msgValidator.validateFirstMIMEPart(er, true);
		
		// Update summary
		summary.put(getShiftIndent(shiftNumber) + "Part " + partNumber +": octet/stream", separate.getNbErrors());
		partNumber++;

		this.processAttachments(er, p);
	}


	/**
	 * Saves attachment to file if desired. Sends it to C-CDA validation tool.
	 * @param er
	 * @param p
	 * @throws Exception
	 */
	public void processAttachments(ErrorRecorder er, Part p) throws Exception{
		if (!p.isMimeType("multipart/*") && !(p instanceof Message)){

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
		}
		er.detail("Attachment not processed because mimeType is multipart");
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


	/**
	 * 
	 * */
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
}
