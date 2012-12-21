package gov.nist.toolkit.testengine.transactions;

import gov.nist.direct.messageGenerator.impl.UnwrappedMessageGenerator;
import gov.nist.direct.messageGenerator.impl.WrappedMessageGenerator;
import gov.nist.toolkit.directsupport.SMTPException;
import gov.nist.toolkit.testengine.StepContext;
import gov.nist.toolkit.testengine.smtp.SMTPAddress;
import gov.nist.toolkit.utilities.io.Io;
import gov.nist.toolkit.xdsexception.ExceptionUtil;
import gov.nist.toolkit.xdsexception.MetadataException;
import gov.nist.toolkit.xdsexception.XdsInternalException;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CertStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.InternetHeaders;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.axiom.om.OMElement;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.cms.IssuerAndSerialNumber;
import org.bouncycastle.asn1.smime.SMIMECapabilitiesAttribute;
import org.bouncycastle.asn1.smime.SMIMECapability;
import org.bouncycastle.asn1.smime.SMIMECapabilityVector;
import org.bouncycastle.asn1.smime.SMIMEEncryptionKeyPreferenceAttribute;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x509.X509Extension;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.cms.CMSAlgorithm;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.CMSSignedGenerator;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoGeneratorBuilder;
import org.bouncycastle.cms.jcajce.JceCMSContentEncryptorBuilder;
import org.bouncycastle.cms.jcajce.JceKeyTransRecipientInfoGenerator;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.mail.smime.CMSProcessableBodyPart;
import org.bouncycastle.mail.smime.SMIMEEnvelopedGenerator;
import org.bouncycastle.mail.smime.SMIMESignedGenerator;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.util.Store;

public class DirectTransaction extends BasicTransaction {
	File certFile = null;
	String certFilePassword = null;
	File bodyContentFile = null;
	String bodyContent = null;
	String fromAddress = null;
	String toAddress = null;
	String mdnAddress = null;
	String subject = null;
	File attachmentContentFile = null;
	String mailerHostname = null;
	int mailerPort = 25;
	boolean useSMTPProtocol = true;
	boolean sendWrapped = true;

	static final Logger logger = Logger.getLogger(DirectTransaction.class);


	static { 
		// add BC in case it isn't yet a valid security provider 
		Security.addProvider(new BouncyCastleProvider()); 
	} 

	public DirectTransaction(StepContext s_ctx, OMElement instruction,
			OMElement instruction_output) {
		super(s_ctx, instruction, instruction_output);
		// TODO Auto-generated constructor stub
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void run(OMElement request) throws Exception {

		if (!verifyParameters())
			return;

		if (!s_ctx.getStatus())
			return;

		/*Security.addProvider(new BouncyCastleProvider());

		//
		// set up our certs
		//
		KeyPairGenerator    kpg  = KeyPairGenerator.getInstance("RSA", "BC");

		kpg.initialize(1024, new SecureRandom());

		//List                certList = new ArrayList();

		//
        // Open the key store
        //
        KeyStore    ks = KeyStore.getInstance("PKCS12", "BC");
        
        Map<String, Object> extra2 = planContext.getExtraLinkage2();
        byte[] signingCert = (byte[]) extra2.get("signingCert");
        Object signingCertPwO = planContext.getExtraLinkage().get("signingCertPassword");
        String signingCertPw = (signingCertPwO == null) ? "" : signingCertPwO.toString();

        try {
        	ks.load(Io.bytesToInputStream(signingCert), signingCertPw.toCharArray());
        } catch (Throwable e) {
        	throw new Exception("Signing private key may be in wrong format, PKCS12 expected", e);
        }

        Enumeration e = ks.aliases();
        String      keyAlias = null;

        while (e.hasMoreElements())
        {
            String  alias = (String)e.nextElement();

            if (ks.isKeyEntry(alias))
            {
                keyAlias = alias;
            }
        }

        if (keyAlias == null)
        {
            System.err.println("can't find a private key!");
            System.exit(0);
        }

        Certificate[]   chain = ks.getCertificateChain(keyAlias);
		
        //
		// cert that issued the signing certificate
		//
        String              signDN = ((X509Certificate) chain[0]).getIssuerDN().toString();
		
		//certList.add(chain[0]);
        

		//
		// be careful about setting extra headers here. Some mail clients
		// ignore the To and From fields (for example) in the body part
		// that contains the multipart. The result of this will be that the
		// signature fails to verify... Outlook Express is an example of
		// a client that exhibits this behaviour.
		//

        Collection<X509Certificate> signingCertificates = new ArrayList<X509Certificate>();
        X509CertificateEx signCert = X509CertificateEx.fromX509Certificate((X509Certificate) chain[0], (PrivateKey)ks.getKey(keyAlias, signingCertPw.toCharArray()));
        
        signingCertificates.add(signCert);

		//
		// create a CertStore containing the certificates we want carried
		// in the signature
		//
		Store certs = new JcaCertStore(signingCertificates);

		//
		// create some smime capabilities in case someone wants to respond
		//
		ASN1EncodableVector         signedAttrs = new ASN1EncodableVector();
		SMIMECapabilityVector       caps = new SMIMECapabilityVector();

		caps.addCapability(SMIMECapability.dES_EDE3_CBC);
		caps.addCapability(SMIMECapability.rC2_CBC, 128);
		caps.addCapability(SMIMECapability.dES_CBC);
		caps.addCapability(new ASN1ObjectIdentifier("1.2.840.113549.1.7.1"));
		caps.addCapability(new ASN1ObjectIdentifier("1.2.840.113549.1.9.22.1"));

		signedAttrs.add(new SMIMECapabilitiesAttribute(caps));
		
		logger.debug("Signing Cert is \n = " + signCert.toString());
		//
		// add an encryption key preference for encrypted responses -
		// normally this would be different from the signing certificate...
		//
		IssuerAndSerialNumber   issAndSer = new IssuerAndSerialNumber(
				new X500Name(signDN), signCert.getSerialNumber());

		signedAttrs.add(new SMIMEEncryptionKeyPreferenceAttribute(issAndSer));

		//
		// create the generator for creating an smime/signed message
		//
		SMIMESignedGenerator gen = new SMIMESignedGenerator();

		//
		// add a signer to the generator - this specifies we are using SHA1 and
		// adding the smime attributes above to the signed attributes that
		// will be generated as part of the signature. The encryption algorithm
		// used is taken from the key - in this RSA with PKCS1Padding
		//
		gen.addSignerInfoGenerator(new JcaSimpleSignerInfoGeneratorBuilder().setProvider("BC").setSignedAttributeGenerator(new AttributeTable(signedAttrs)).build("SHA1withRSA", signCert.getPrivateKey(), signCert));

		//
		// add our pool of certs and cerls (if any) to go with the signature
		//
		gen.addCertificates(certs);
		
        //
        // create the base for our message
        //
        MimeBodyPart    msg1 = new MimeBodyPart();
        
        msg1.setText(bodyContent);
        
        // -- Set the attachment --
        MimeBodyPart ccda = new MimeBodyPart();
     
        String attachmentFileName = attachmentContentFile.getName();
        
        if (attachmentContentFile.toString().endsWith(".xml")) {

            byte[] fileContent = FileUtils.readFileToByteArray(attachmentContentFile);
            byte[] content = Base64.encodeBase64(fileContent);

            InternetHeaders partHeaders = new InternetHeaders();
            partHeaders.addHeader("Content-Type", "text/xml; name="+attachmentFileName);
            partHeaders.addHeader("Content-Transfer-Encoding", "base64");
            partHeaders.addHeader("Content-Disposition", "attachment; filename="+attachmentFileName);

            ccda = new MimeBodyPart(partHeaders, content);
        }
        else if (attachmentContentFile.toString().endsWith(".zip")) {
        	byte[] fileContent = FileUtils.readFileToByteArray(attachmentContentFile);
            byte[] content = Base64.encodeBase64(fileContent);


            InternetHeaders partHeaders = new InternetHeaders();
            partHeaders.addHeader("Content-Type", "application/zip; name="+attachmentFileName);
            partHeaders.addHeader("Content-Transfer-Encoding", "base64");
            partHeaders.addHeader("Content-Disposition", "attachment; filename="+attachmentFileName);
            
            ccda = new MimeBodyPart(partHeaders, content);
        }
        else {
        	FileDataSource fds = new FileDataSource(attachmentContentFile);
        	
        	ccda.setDataHandler(new DataHandler(fds));
            ccda.setFileName(fds.getName());
        }
        
        MimeMultipart mp = new MimeMultipart();
        
        mp.addBodyPart(msg1);
        mp.addBodyPart(ccda);
        
        MimeBodyPart m = new MimeBodyPart();
        m.setContent(mp);

		//
		// extract the multipart object from the SMIMESigned object.
		//
		
        MimeMultipart mm = gen.generate(m);

        
        /*OutputStream ostmp = new FileOutputStream(new File("/Users/bill/tmp/direct.send.txt"));
        String ctype = mm.getContentType();
        ostmp.write(ctype.getBytes());
        ostmp.write(new String("\r\n\r\n").getBytes());
        mm.writeTo(ostmp);
		
        
		//
		// Get a Session object and create the mail message
		//
		Properties props = System.getProperties();
		Session session = Session.getDefaultInstance(props, null);

		Address fromUser = new InternetAddress(new SMTPAddress().properEmailAddr(fromAddress));
		Address toUser = new InternetAddress(new SMTPAddress().properEmailAddr(toAddress));


		MimeBodyPart body = new MimeBodyPart();
		ByteArrayOutputStream oStream = new ByteArrayOutputStream();
		try {
			mm.writeTo(oStream);
			oStream.flush();
			InternetHeaders headers = new InternetHeaders();
			headers.addHeader("Content-Type", mm.getContentType());

			body = new MimeBodyPart(headers, oStream.toByteArray());
			IOUtils.closeQuietly(oStream);

		}    

		catch (Exception ex) {
			throw new RuntimeException(ex);
		}

		//
		// Open the key store
		//
		/*
		KeyStore    ks = KeyStore.getInstance("PKCS12", "BC");

		ks.load(new FileInputStream(certFile.toString()), certFilePassword.toCharArray());

		Enumeration e = ks.aliases();
		String      keyAlias = null;

		while (e.hasMoreElements())
		{
			String  alias = (String)e.nextElement();

			if (ks.isKeyEntry(alias))
			{
				keyAlias = alias;
			}
		}

		if (keyAlias == null)
		{
			System.err.println("can't find a private key!");
			System.exit(0);
		}

		Certificate[]   chain = ks.getCertificateChain(keyAlias);
		*/
		
		/*
		
		// Encryption cert
		X509Certificate encCert = null;
        
        ByteArrayInputStream is;
        try {
        	byte[] encryptionCertBA = (byte[]) planContext.getExtraLinkage2().get("encryptionCert");
        	is = new ByteArrayInputStream(encryptionCertBA);
            CertificateFactory x509CertFact = CertificateFactory.getInstance("X.509");
            encCert = (X509Certificate)x509CertFact.generateCertificate(is);
            logger.debug("Encrypton cert = \n" + encCert.toString());
        } catch (Exception e1) {
        	throw new Exception("Error loading X.509 encryption cert - probably wrong format", e1);
        }


		// Create the encrypter
        SMIMEEnvelopedGenerator encrypter = new SMIMEEnvelopedGenerator();
        try {
        	encrypter.addRecipientInfoGenerator(new JceKeyTransRecipientInfoGenerator(encCert).setProvider("BC"));
        } catch (Exception e1) {
        	throw new Exception("Error loading encryption cert - must be in X.509 format", e1);
        }
		// Encrypt the message
		MimeBodyPart encryptedPart = encrypter.generate(body,
				// RC2_CBC
				new JceCMSContentEncryptorBuilder(CMSAlgorithm.AES128_CBC).setProvider("BC").build());

		MimeMessage msg = new MimeMessage(session);
		msg.setFrom(fromUser);
		msg.setRecipient(Message.RecipientType.TO, toUser);
		msg.setSentDate(new Date());
		msg.setSubject(subject);
		msg.setContent(encryptedPart.getContent(), encryptedPart.getContentType());
		msg.setDisposition("attachment");
		msg.setFileName("smime.p7m");
		msg.saveChanges();*/
		
		Address fromUser = new InternetAddress(new SMTPAddress().properEmailAddr(fromAddress));
		Address toUser = new InternetAddress(new SMTPAddress().properEmailAddr(toAddress));
		
		
		Properties props = System.getProperties();
		Session session = Session.getDefaultInstance(props, null);
		MimeMessage msg = new MimeMessage(session);
		
		if (sendWrapped) {
			msg = createWrapedSendMail(toAddress, fromAddress);
		} else {
			msg = createSendMail(toAddress, fromAddress);
		}
		/*InputStream is2 = new FileInputStream(new File("/var/lib/tomcat_ttt/webapps/ttt/pubcert/encrypted3.txt"));
		msg = new MimeMessage(session, is2);*/
		
        //OutputStream ostmp1 = new FileOutputStream(new File("/var/lib/tomcat_ttt/webapps/ttt/pubcert/encrypted_before_sending.txt"));
        //msg.writeTo(ostmp1);

		// For test purpose NEED to be removed
		// TODO        
		//msg.writeTo(new FileOutputStream("encrypted.txt"));

		logger.debug("Opening socket to Direct system on " + mailerHostname + ":" + mailerPort + "...");
		Socket socket = new Socket(mailerHostname, mailerPort);
		logger.debug("\t...Success");
		OutputStream os;

		// org.bouncycastle.cms.CMSException: exception wrapping content key: 
		//      cannot create cipher: No such algorithm: 1.2.840.10040.4.1

		//1.2.840.10040.4.1 is the OID for DSA, but as it says there is no Cipher 
		//for it. This is because DSA can only be used for signing - you cannot 
		//use it to encrypt with. 

		try {
			if (useSMTPProtocol) {
				// fromUser.toString() does the parsing for me so I don't need my code. 
				smtpProtocol(socket, msg, "hit-testing.nist.gov", fromUser.toString(), toUser.toString());
			} else {
				os = socket.getOutputStream();
				msg.writeTo(os);
			}
		} catch (Exception ex) {
			System.out.println("Exception: " + ex.getMessage());
		} finally {
			socket.close();
		}
	}

	static final String CRLF = "\r\n";
	BufferedReader in = null;
	BufferedOutputStream out = null;

	void smtpProtocol(Socket socket, MimeMessage mmsg, String domainname, String from, String to) throws Exception {
		in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		out = new BufferedOutputStream(socket.getOutputStream());

		try {
			from = new SMTPAddress().properEmailAddr(from);
			to = new SMTPAddress().properEmailAddr(to);

			rcv("220");

			send("HELO " + domainname);

			rcv("250"); 

			send("MAIL FROM:" + from);

			rcv("250"); 

			send("RCPT TO:" + to);

			rcv("250");

			send("DATA");

			rcv("354"); 

			send("Subject: " + subject);

			mmsg.writeTo(out);

			send(CRLF + ".");

			rcv("250");

			send("QUIT");

			rcv("221"); 
		} catch (Exception e) {
			logger.error("Protocol error: " + ExceptionUtil.exception_details(e));
			throw new Exception("Protocol error: ", e);
		} finally {
			in.close();
			out.close();
			in = null;
			out = null;
		}

	}

	void send(String cmd) throws IOException {
		logger.debug("SMTP SEND: " + cmd);
		cmd = cmd + CRLF;
		out.write(cmd.getBytes());
		out.flush();
	}

	String rcv(String expect) throws IOException, SMTPException {
		String msg;
		msg = in.readLine();
		logger.debug("SMTP RCV: " + msg);
		if (expect != null && !msg.startsWith(expect))
			throw new SMTPException("Error: expecting " + expect + ", got <" + msg + "> instead");
		return msg;
	}



//	String escapeInternetAddress(String in) {
//		String x = in.trim();
//		if (x.length() > 0 && x.charAt(0) != '"')
//			return "\"" + x + "\"";
//		return in;
//	}

	boolean verifyParameters() {
		boolean ok = true;

		if (certFile == null) {
			s_ctx.set_error("CertFile parameter missing");
			ok = false;
		}
		if (certFilePassword == null) {
			s_ctx.set_error("CertFilePassword parameter missing");
			ok = false;
		}
		if (bodyContent == null) {
			s_ctx.set_error("BodyContentFile and BodyContent parameters missing");
			ok = false;
		}
		if (fromAddress == null) {
			s_ctx.set_error("DirectFromAddress parameter missing");
			ok = false;
		}
		if (toAddress == null) {
			s_ctx.set_error("DirectToAddress parameter missing");
			ok = false;
		}
		if (subject == null) {
			s_ctx.set_error("Subject parameter missing");
			ok = false;
		}
		if (attachmentContentFile == null) {
			s_ctx.set_error("AttachmentContentFile parameter missing");
			ok = false;
		}
		if (mailerHostname == null) {
			s_ctx.set_error("DirectSystemMailerHostname parameter missing");
			ok = false;
		}

		return ok;
	}

	@Override
	protected void parseInstruction(OMElement part)
			throws XdsInternalException, MetadataException {
		String part_name = part.getLocalName();
		if (part_name.equals("CertFile")) {
			if (certFile == null) {
				certFile = new File(testConfig.testplanDir + File.separator + part.getText());
			}
			if (!certFile.exists())
				s_ctx.set_error("CertFile points to file that does not exist, " + certFile.toString());
		} 
		else if (part_name.equals("CertFilePassword")) {
			if (certFilePassword == null)
				certFilePassword = part.getText();
		}
		else if (part_name.equals("Wrapped")) {
			sendWrapped = false;
			String val = part.getText();
			if (val.equalsIgnoreCase("true") || val.equalsIgnoreCase("yes"))
				sendWrapped = true;
		}
		else if (part_name.equals("BodyContent")) {
			bodyContent = part.getText();
		}
		else if (part_name.equals("BodyContentFile")) {
			bodyContentFile = new File(testConfig.testplanDir + File.separator + part.getText());
			if (!bodyContentFile.exists())
				s_ctx.set_error("BodyContentFile points to file that does not exist, " + bodyContentFile.toString());
			try {
				bodyContent = Io.stringFromFile(bodyContentFile);
			} catch (IOException e) {
				throw new XdsInternalException("Cannot load BodyContent", e);
			}
		}
		else if (part_name.equals("DirectFromAddress")) {
			fromAddress = part.getText();
		}
		else if (part_name.equals("DirectToAddress")) {
			toAddress = part.getText();
		}
		else if (part_name.equals("MDNAddress")) {
			mdnAddress = part.getText();
		}
		else if (part_name.equals("DirectSystemMailerPort")) {
			try {
				mailerPort = Integer.parseInt(part.getText());
			} catch (NumberFormatException e) {
				s_ctx.set_error("MailerPort value, " + part.getText() + ", cannot be parsed as an Integer");
			}
		}
		else if (part_name.equals("Subject")) {
			subject = part.getText();
		}
		else if (part_name.equals("AttachmentContentFile")) {
			String text = part.getText();
			if (text.startsWith("/") || text.charAt(1) == ':')
				attachmentContentFile = new File(text);
			else
				attachmentContentFile = new File(testConfig.testplanDir + File.separator + text);
			if (!attachmentContentFile.exists()) {
				File acf2 = new File(text);
				if (!acf2.exists()) {
					s_ctx.set_error("AttachmentContentFile points to file that does not exist, " + attachmentContentFile.toString());
				} else {
					attachmentContentFile = acf2;
				}
			}
		}
		else if (part_name.equals("DirectSystemMailerHostname")) {
			mailerHostname = part.getText();
		}
		else
			parseBasicInstruction(part);
	}

	@Override
	protected String getRequestAction() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected String getBasicTransactionName() {
		return "Direct";
	}
	
	public MimeMessage createSendMail(String toAddress, String fromAddress) {
		// Get the signing certificate
		Map<String, Object> extra2 = planContext.getExtraLinkage2();
		byte[] signingCert = (byte[]) extra2.get("signingCert");
		// Get the signing certificate password
		Object signingCertPwO = planContext.getExtraLinkage().get("signingCertPassword");
		String signingCertPw = (signingCertPwO == null) ? "" : signingCertPwO.toString();        
		
		// Get the encryption certificate
		byte[] encryptionCertBA = (byte[]) planContext.getExtraLinkage2().get("encryptionCert");
		
		String textMessage = "Message test";
		String subject = "Message test";
		
		UnwrappedMessageGenerator gen = new UnwrappedMessageGenerator();
		return gen.generateMessage(signingCert, signingCertPw, textMessage, subject, attachmentContentFile, fromAddress, toAddress, encryptionCertBA);		
	}
	
	public MimeMessage createWrapedSendMail(String toAddress, String fromAddress) {
		// Get the signing certificate
		Map<String, Object> extra2 = planContext.getExtraLinkage2();
		byte[] signingCert = (byte[]) extra2.get("signingCert");
		// Get the signing certificate password
		Object signingCertPwO = planContext.getExtraLinkage().get("signingCertPassword");
		String signingCertPw = (signingCertPwO == null) ? "" : signingCertPwO.toString();        

		// Get the encryption certificate
		byte[] encryptionCertBA = (byte[]) planContext.getExtraLinkage2().get("encryptionCert");

		String textMessage = "Message test";
		String subject = "Message test";

		WrappedMessageGenerator gen = new WrappedMessageGenerator();
		return gen.generateMessage(signingCert, signingCertPw, textMessage, subject, attachmentContentFile, fromAddress, toAddress, encryptionCertBA);
	}

}
