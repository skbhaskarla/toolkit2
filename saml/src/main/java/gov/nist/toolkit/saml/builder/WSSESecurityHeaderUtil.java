package gov.nist.toolkit.saml.builder;

import gov.nist.toolkit.dsig.KeyStoreAccessObject;
import gov.nist.toolkit.securityCommon.SecurityParams;
import gov.nist.toolkit.xdsexception.LoadKeystoreException;

import java.io.FileInputStream;
import java.io.StringReader;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import javax.xml.crypto.XMLStructure;
import javax.xml.crypto.dom.DOMStructure;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.KeyValue;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.ExcC14NParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.util.XMLUtils;
import org.apache.xml.security.utils.IdResolver;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;

/**
 * @author vbeera
 *
 */
public class WSSESecurityHeaderUtil {

	//Constant variables	
	public static final String S_TAG = "soapenv";
	public static final String HEADER = "Header";
	public static final String WSSE_SECURITY = "Security";
	public static final String WSSE_TAG = "wsse";
	public static final String NS17_NS_TAG = "xmlns:ns17";
	public static final String NS16_NS_TAG = "xmlns:ns16";
	public static final String WSU_TAG = "wsu";
	public static final String WSU_ID = "wsu:Id";
	public static final String TIME_STAMP = "Timestamp";
	public static final String MUST_UNDERSTAND = "mustUnderstand";
	public static final String CREATED = "Created";
	public static final String EXPIRES = "Expires";
	public static final long INTERVAL = 300;	
	public static final String SAML2_TAG = "saml";
	public static final String ASSERTION = "Assertion";
	public static final String DS_PREFIX = "ds";
	public static final String C14N_PREFIX = "exc14n";
	public static final String XML_NS =  "http://www.w3.org/2000/xmlns/";
	public static final String S_NS =  "http://www.w3.org/2003/05/soap-envelope";
	public static final String WSSE_NS = "http://docs.oasis-open.org/wss" +
	"/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";	
	public static final String WSU_NS = "http://docs.oasis-open.org/wss" +
	"/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd";	
	public static final String WSSE11_NS = 
		"http://docs.oasis-open.org/wss/oasis-wss-wssecurity-secext-1.1.xsd"; 	
	public static final String NS17_NS = 
		"http://docs.oasis-open.org/ws-sx/ws-secureconversation/200512"; 	
	public static final String NS16_NS = 
		"http://schemas.xmlsoap.org/soap/envelope/";
	public static final String TAG_SECURITYTOKEN_REFERENCE = "SecurityTokenReference";
	public static final String TAG_KEYIDENTIFIER = "KeyIdentifier";
	public static final String TOKEN_TYPE = "wsse11:TokenType";
	public static final String TAG_VALUETYPE = "ValueType";
	public static final String SAML2_TOKEN_TYPE = 
		"http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLV2.0";
	public static final String SAML2_ASSERTION_VALUE_TYPE = 
		"http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLID";


	// This is where the actual work for generating WS/SAML headers is done -ANTOINE
	/**
	 * @param securityParams 
	 * @param args
	 * @throws Exception 
	 */
	public static OMElement getWSSecOMElement(SecurityParams securityParams) throws LoadKeystoreException {

		OMElement wsSecOMelemnt = null;
		try {
			//This might break other parts of the code that relies on the
			//erroneous assumption of a global security context! -Antoine 
			KeyStoreAccessObject ksAccessObj = KeyStoreAccessObject.getInstance(securityParams);
			
			
			PrivateKey pvtKey = ksAccessObj.getPrivateKey();
			PublicKey pubKey = ksAccessObj.getPublicKey();
		//Get WS Security OM Elemnt
		wsSecOMelemnt = WSSESecurityHeaderUtil.getWSSecurityHeaders(pvtKey, pubKey);
		} catch (Exception e) {
			throw new LoadKeystoreException(e.getMessage(), null, e);
		}		
		return wsSecOMelemnt;
	}
		
	public static OMElement getWSSecurityHeaders(PrivateKey pvtKey, PublicKey pubKey) throws Exception {
		//Step 1:
		//get the saml Assertion element
		Element assertionDoc = getSamlAssertion();
		
		//Step 2:
		//Create WSSE security element with TimeStamp and embed signed Assertion element
		Document wsSecElementWithUnsignedTS = getWSSecurityWithTSAndSignedAssertion(assertionDoc);

		//Step 3:
		//Embed the created WS Security element into non-Security Soap Envelope
		Document orignalSoapEnvelope = getConstructingSoapEnvelopAsDoc();
		NodeList headerNodes = orignalSoapEnvelope.getElementsByTagName(S_TAG+":"+HEADER);
		Element headerElem = (Element)headerNodes.item(0);

		Element wsSecurityElement = wsSecElementWithUnsignedTS.getDocumentElement();		
		Node assrtNode = orignalSoapEnvelope.importNode(wsSecurityElement, true);
		headerElem.appendChild(assrtNode);		
		
		//Step 4:
		//Create Signature for Assertion & Timestamp
		orignalSoapEnvelope = createAssertionTimestampSignature(orignalSoapEnvelope, pvtKey, pubKey);
		
		//Convert WS Security DOM element into OMElement	
		OMElement wsSecOMelemnt = convertDOMtoOM(orignalSoapEnvelope.getDocumentElement());
		return wsSecOMelemnt;
	}

	/**
	 * @return Document
	 * @throws Exception
	 */
	public static Element getSamlAssertion() throws Exception	
	{	
		return SamlAssertionData.createAssertionRequest();
		
	}

	/**
	 * @return Document
	 * @throws Exception
	 */
	public static Document getConstructingSoapEnvelopAsDoc() throws Exception	
	{	
		String soapStr = "<soapenv:Envelope xmlns:soapenv=\"http://www.w3.org/2003/05/soap-envelope\"" +
				" xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\" xmlns:exc14n=\"http://www.w3.org/2001/10/xml-exc-c14n#\" " +
				"xmlns:saml=\"urn:oasis:names:tc:SAML:1.0:assertion\" xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\"" +
				" xmlns:wsse11=\"http://docs.oasis-open.org/wss/oasis-wss-wssecurity-secext-1.1.xsd\" " +
				"xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\"" +
				" xmlns:ttt=\"http://www.w3.org/2001/XMLSchema\" "+
				" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">"+
						  "<soapenv:Header></soapenv:Header>"+
						  "<soapenv:Body/>"+
						  "</soapenv:Envelope>";
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);		
		Document sampleSoapEnv = dbf.newDocumentBuilder().parse(new InputSource(new StringReader(soapStr)));
		return sampleSoapEnv;		
	}

	/** Create WSSE security element with TimeStamp and embed signed Assertion element
	 * @param assertionDoc
	 * @return document
	 * @throws Exception
	 */
	public static Document getWSSecurityWithTSAndSignedAssertion(Element assertionElement) throws Exception	
	{	
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		DocumentBuilder documentBuilder = dbf.newDocumentBuilder();
		Document document = documentBuilder.newDocument();
		Element wsseHeader = getWSSecurityElement(document);
		document.appendChild(wsseHeader);
	
		Node assrtNode = document.importNode(assertionElement, true);
		wsseHeader.appendChild(assrtNode);

		return document;
	}

	/** Create WSSE security element with TimeStamp
	 * @param document
	 * @return Element
	 * @throws Exception
	 */
	public static Element getWSSecurityElement(Document document) throws Exception
	{		
		//Element wsseHeader = document.createElement(WSSE_TAG + ":" + WSSE_SECURITY);
		Element wsseHeader = document.createElementNS(WSSE_NS, WSSE_TAG + ":" + WSSE_SECURITY);
		
		wsseHeader.setAttribute(S_TAG + ":" + MUST_UNDERSTAND, "true");
		//wsseHeader.setAttribute(S_TAG + ":" + MUST_UNDERSTAND, "1");

		//Add time stamp
		/*Element timeStamp = document.createElement(
				WSU_TAG + ":" +
				TIME_STAMP);*/	
		Element timeStamp = document.createElementNS(WSU_NS,
				WSU_TAG + ":" +
				TIME_STAMP);
		//timeStamp.setAttribute(NS17_NS_TAG, NS17_NS);
		//timeStamp.setAttribute(NS16_NS_TAG, NS16_NS);
		timeStamp.setAttributeNS(XML_NS, NS17_NS_TAG, NS17_NS);
		timeStamp.setAttributeNS(XML_NS, NS16_NS_TAG, NS16_NS);
		
		String tsId = "_1";
		timeStamp.setAttribute(WSU_ID, tsId);
		wsseHeader.appendChild(timeStamp);
		Element created = document.createElement(
				WSU_TAG + ":" + CREATED);
		Date createTime = new Date();
		Date expireTime = new Date(); 
		expireTime.setTime(createTime.getTime() + 
				INTERVAL * 1000);
		created.appendChild(document.createTextNode(toUTCDateFormat(createTime))); 
		timeStamp.appendChild(created);
		Element expires = document.createElement(
				WSU_TAG + ":" + EXPIRES);
		expires.appendChild(document.createTextNode(toUTCDateFormat(expireTime))); 
		timeStamp.appendChild(expires);

		return wsseHeader;
	}

	/**
	 * Returns UTC String representation of a date. For instance,
	 * 2004-03-20T05:53:32Z.
	 * 
	 * @param date Date object.
	 */
	public static String toUTCDateFormat(Date date) {
		final String UTC_DATE_Z_FORMAT = "{0}-{1}-{2}T{3}:{4}:{5}Z";	 
		return dateToString(date, UTC_DATE_Z_FORMAT);
	}

	private static String dateToString(Date date, String format) {	     
		final TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone("UTC");
		GregorianCalendar cal = new GregorianCalendar(UTC_TIME_ZONE);
		cal.setTime(date);
		String[] params = new String[6];

		params[0] = formatInteger(cal.get(Calendar.YEAR), 4);
		params[1] = formatInteger(cal.get(Calendar.MONTH) + 1, 2);
		params[2] = formatInteger(cal.get(Calendar.DAY_OF_MONTH), 2);
		params[3] = formatInteger(cal.get(Calendar.HOUR_OF_DAY), 2);
		params[4] = formatInteger(cal.get(Calendar.MINUTE), 2);
		params[5] = formatInteger(cal.get(Calendar.SECOND), 2);
		
		return MessageFormat.format(format, (Object[])params);
	}

	private static String formatInteger(int value, int length) {
		String val = Integer.toString(value);
		int diff = length - val.length();

		for (int i = 0; i < diff; i++) {
			val = "0" + val;
		}

		return val;
	}

	
	/** Retrieve Private Key from KeyStore
	 * @param ksPath
	 * @param alias
	 * @param ksPwd
	 * @return PrivateKey
	 * @throws Exception
	 */
	public static PrivateKey getPrivateKeyFromKS(String ksPath, String alias, String ksPwd) throws Exception
	{
		KeyStore ks = KeyStore.getInstance("JKS");
		ks.load(new FileInputStream(ksPath), ksPwd.toCharArray());;
		KeyStore.PrivateKeyEntry keyEntry = (KeyStore.PrivateKeyEntry) ks.getEntry
		(alias, new KeyStore.PasswordProtection(ksPwd.toCharArray()));		
		PrivateKey pvtKey = keyEntry.getPrivateKey();
		return pvtKey;
	}
	
	/** Retrieve Public Key from KeyStore
	 * @param ksPath
	 * @param alias
	 * @param ksPwd
	 * @return PublicKey
	 * @throws Exception
	 */
	public static PublicKey getPublicKeyFromKS(String ksPath, String alias, String ksPwd) throws Exception
	{
		KeyStore ks = KeyStore.getInstance("JKS");
		ks.load(new FileInputStream(ksPath), ksPwd.toCharArray());;
		KeyStore.PrivateKeyEntry keyEntry = (KeyStore.PrivateKeyEntry) ks.getEntry
		(alias, new KeyStore.PasswordProtection(ksPwd.toCharArray()));		
		X509Certificate cert = (X509Certificate) keyEntry.getCertificate();		
		PublicKey pubKey  = cert.getPublicKey();
		return pubKey;
	}

	/** Sign the Assertion & Timestamp of the WS Security Element
	 * @param Document
	 * @param pvtKey
	 * @param publicKey
	 * @return Document
	 * @throws Exception
	 */
	public static Document createAssertionTimestampSignature(Document doc2, PrivateKey pvtKey, PublicKey publicKey) throws KeyStoreException, Exception{
		KeyStoreAccessObject ksAccessObj = KeyStoreAccessObject.getInstance(null);
		pvtKey = ksAccessObj.getPrivateKey(); 
		publicKey = ksAccessObj.getPublicKey();	
		
		XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM", new org.jcp.xml.dsig.internal.dom.XMLDSigRI());		
		List<Transform> transformList = new ArrayList<Transform>();
		transformList.add(fac.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null));
		transformList.add(fac.newTransform(CanonicalizationMethod.EXCLUSIVE, (C14NMethodParameterSpec) null));
		
		NodeList assrtNodes = doc2.getElementsByTagName(SAML2_TAG+":"+ASSERTION);
		if(assrtNodes == null)
			throw new Exception("NO SAML Assertions found...");
		Element assrtElement = (Element)assrtNodes.item(0);			
		String samlId = assrtElement.getAttributeNode("ID").getNodeValue();
		if(samlId == null)
			throw new Exception("SAML ID cannot be Null...");
		
		String referenceURI = "";
		if (assrtElement!=null) {
			referenceURI = "#"+samlId;
			Attr id = assrtElement.getAttributeNode("ID");
			//IdResolver.registerElementById(assrtElement, id);
			IdResolver.registerElementById(assrtElement, samlId);
		}	
		//String refSamlId = "#"+samlId;
		Reference ref = fac.newReference(referenceURI, fac.newDigestMethod(DigestMethod.SHA1, null),
										 transformList, null, null);
										 //envelopedTransform, null, null);

		// Create the SignedInfo.
		SignedInfo signInfo = fac.newSignedInfo(fac.newCanonicalizationMethod(CanonicalizationMethod.EXCLUSIVE, (C14NMethodParameterSpec) null),
										  fac.newSignatureMethod(SignatureMethod.RSA_SHA1, null), Collections.singletonList(ref));
		
		
	    KeyInfoFactory keyInfoFac = fac.getKeyInfoFactory();	   
	    KeyValue keyVal = keyInfoFac.newKeyValue(publicKey);
	    KeyInfo keyInf = keyInfoFac.newKeyInfo(Collections.singletonList(keyVal));	      


		// Create a DOMSignContext and specify the RSA PrivateKey and
		// location of the resulting XMLSignature's parent element.
	    
	    //(MA1024) fix. assertion signature has to be placed after the issuer. -Antoine
	    Node nextSibling = assrtElement.getElementsByTagName("saml:Subject").item(0);
	    
		DOMSignContext domSignCtx = new DOMSignContext(pvtKey, assrtElement,nextSibling);
		//domSignCtx.setDefaultNamespacePrefix(DS_PREFIX);
		domSignCtx.putNamespacePrefix(javax.xml.crypto.dsig.XMLSignature.XMLNS, DS_PREFIX);
		domSignCtx.setIdAttributeNS(assrtElement, null, "ID");//added for GWT error		
		// Create the XMLSignature
		XMLSignature signature = fac.newXMLSignature(signInfo, keyInf);
		signature.sign(domSignCtx);			
		//create the detached signature for Timestamp
		return signWSSETimestampAsDetached(doc2, pvtKey);
	}
	
	/** Sign the Timestamp of the WS Security Element
	 * @param doc1
	 * @param pvtKey
	 * @return
	 * @throws Exception
	 */
	public static Document signWSSETimestampAsDetached(Document doc1, PrivateKey pvtKey) throws Exception {

		String providerName = System.getProperty("jsr105Provider", "org.jcp.xml.dsig.internal.dom.XMLDSigRI");
		XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM",(Provider) Class.forName(providerName).newInstance());						
		//Retrieve WS Security Element
		NodeList wsSecNodes = doc1.getElementsByTagName(WSSE_TAG+":"+WSSE_SECURITY);
		Element wsSecElement = (Element)wsSecNodes.item(0);			
		//Retrieve Timestamp from WS Security Element
		//NodeList tsNodes = doc1.getElementsByTagNameNS(WSU_NS, TIME_STAMP);
		NodeList tsNodes = wsSecElement.getElementsByTagName(WSU_TAG+":"+TIME_STAMP);
		Element tsElem = (Element)tsNodes.item(0);

		// Create a Reference to the enveloped document and specify digest algorithm & Transform.
		String referenceURI = "";
		if (tsElem!=null) {
			Attr ts_idAttr = tsElem.getAttributeNode(WSU_ID);
			String ts_id = null;
			if(ts_idAttr != null)
				ts_id = ts_idAttr.getNodeValue();
			IdResolver.registerElementById(tsElem, ts_id);
			referenceURI = "#" + ts_id;
		}
		else
			throw new Exception("Timestamp Element is Null...");
		
		
		C14NMethodParameterSpec c14nSpec = null;
        
            List<String> prefixes = new ArrayList<String>();
            prefixes.add(WSU_TAG);
            prefixes.add(WSSE_TAG);
            prefixes.add(S_TAG); 
            c14nSpec = new ExcC14NParameterSpec(prefixes);
            
            List<Transform> transformList = new ArrayList<Transform>();
    		transformList.add(fac.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null));
    		transformList.add(fac.newTransform(CanonicalizationMethod.EXCLUSIVE, (C14NMethodParameterSpec) null));    
		
    		//(MA115) -fix . Add transformations -Antoine
		Reference ref = fac.newReference
		(referenceURI, fac.newDigestMethod(DigestMethod.SHA1, null),transformList,null,null);

		prefixes.remove(0);
		 c14nSpec = new ExcC14NParameterSpec(prefixes);
		 
		 CanonicalizationMethod canonMethod = fac.newCanonicalizationMethod
			(CanonicalizationMethod.EXCLUSIVE, 
					(C14NMethodParameterSpec) c14nSpec);
		// Create the SignedInfo
		SignedInfo si = fac.newSignedInfo
						(canonMethod, 
						fac.newSignatureMethod(SignatureMethod.RSA_SHA1, null),
						Collections.singletonList(ref));	

		
		// Create a KeyValue pointing to the KeyIdentifier that contains the RSA PublicKey		
		NodeList assrtNodes = doc1.getElementsByTagName(SAML2_TAG+":"+ASSERTION);
		if(assrtNodes == null)
			throw new Exception("NO SAML Assertions found...");
		Element assrtElement = (Element)assrtNodes.item(0);			
		String samlId = assrtElement.getAttributeNode("ID").getNodeValue();
		if(samlId == null)
			throw new Exception("SAML ID cannot be Null...");
		XMLStructure structure = new DOMStructure(createSecurityTokenReference(doc1, samlId));
		
		KeyInfoFactory kif = fac.getKeyInfoFactory();
		KeyInfo ki = kif.newKeyInfo(java.util.Collections.singletonList(structure), null);

		// Provide RSA PrivateKey to sign and location of the resulting XMLSignature's parent element
		DOMSignContext dsc = new DOMSignContext(pvtKey, wsSecElement);		
		dsc.putNamespacePrefix(javax.xml.crypto.dsig.XMLSignature.XMLNS, DS_PREFIX);
		dsc.setDefaultNamespacePrefix(C14N_PREFIX);
		dsc.setIdAttributeNS(tsElem, null, WSU_ID);//added for GWT error		
		XMLSignature signature = fac.newXMLSignature(si, ki, null, "_2", null);
		signature.sign(dsc);
		System.out.println("*** DigitalSignatureUtil - signWSSETimestampAsDetached() --- End ---");
		
		return doc1;
	}


	/**
	 * Creates the Security Token reference
	 * @param doc
	 * @param samlId
	 * @return
	 */
	private static Element createSecurityTokenReference(Document doc, 
			String samlId) throws Exception{

		Element secTokenRef = doc.createElement(WSSE_TAG + ":" + TAG_SECURITYTOKEN_REFERENCE);	    	
		//secTokenRef.setAttribute(TOKEN_TYPE, SAML2_TOKEN_TYPE);
		secTokenRef.setAttributeNS(WSSE11_NS, TOKEN_TYPE, SAML2_TOKEN_TYPE);

		Element keyIdentifier = doc.createElement(WSSE_TAG + ":" + TAG_KEYIDENTIFIER);	    	
		keyIdentifier.setAttribute(TAG_VALUETYPE, SAML2_ASSERTION_VALUE_TYPE);
		Text value = doc.createTextNode(samlId);
		keyIdentifier.appendChild(value);	

		secTokenRef.appendChild(keyIdentifier);	        
		return secTokenRef;
	}	 
	
	

	public static OMElement convertDOMtoOM(Element domElmnt) throws Exception{
		System.out.println("----------------- convertDOMtoOM --- START -------");
		//Convert complete Document Object to OM Element
		OMElement wsSecDocumentAsOM = XMLUtils.toOM(domElmnt);
		//Retrieve wsse Security element from the complete doc
		OMElement wsSecElemnt = wsSecDocumentAsOM.getFirstElement().getFirstElement();
		System.out.println(wsSecElemnt.toString());	
		
		System.out.println("----------------- convertDOMtoOM --- END -------");
		return wsSecElemnt;

	}

}
