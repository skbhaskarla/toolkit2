package gov.nist.toolkit.soap;

import static org.junit.Assert.*;

import gov.nist.toolkit.soap.axis2.Soap;
import gov.nist.toolkit.testengine.TestConfig;
import gov.nist.toolkit.testengine.transactions.BasicTransaction;
import gov.nist.toolkit.testengine.transactions.StoredQueryTransaction;
import gov.nist.toolkit.utilities.xml.Util;
import gov.nist.toolkit.xdsexception.EnvironmentNotSelectedException;
import gov.nist.toolkit.xdsexception.LoadKeystoreException;
import gov.nist.toolkit.xdsexception.XdsFormatException;
import gov.nist.toolkit.xdsexception.XdsInternalException;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.swing.plaf.basic.BasicArrowButton;
import javax.xml.parsers.FactoryConfigurationError;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.junit.Test;

public class SOAPmessageBuilder_FromTransactionObject_Test {

	private final String NO_ENDPOINT="no_endpoint";
	private final String DUMMY_ENDPOINT="http://localhost:8080";
	private final String NO_ACTION="no_action";
	private final String NO_EXPECTED_RETURN_ACTION="no_expected_return_action";
	private final boolean NO_MTOM = false;
	private final boolean NO_ADDRESSING = false;
	private final boolean NO_SOAP12 = false;
	
	/*
	 * $Antoine -
	 * 
	 * NOTES:
	 * - Configuration is an ordeal to set up. This indicates we should review and test the configuration mechanism.
	 * - Soap is trying to send a message after building it. Separation of concerns principles indicates we should refactor this.
	 */
	@Test
	public void BuildFindPatientEnvelope() throws XdsInternalException, FactoryConfigurationError, IOException, XdsFormatException, LoadKeystoreException, EnvironmentNotSelectedException {
		// path configuration to rampart 
		String testmgmt_dir= "/Users/gerardin/Documents/workspace/toolkit/xdstools2/war/toolkitx/xdstest";
		System.out.println(testmgmt_dir);
		TestConfig testConfig = new TestConfig();
		testConfig.testmgmt_dir = testmgmt_dir;	
		System.out.println("\tAxis2 client Repository: " + testConfig.testmgmt_dir + File.separator + "rampart" + File.separator + "client_repositories");
		
		Soap soap = new Soap();
		soap.setUseSaml(true);
		soap.setRepositoryLocation(testConfig.testmgmt_dir + File.separator + "rampart" + File.separator + "client_repositories" );
		
		// path configuration to the environment
		System.setProperty("External_Cache","/Users/gerardin/IHE-Testing/xdstools2_environment");
		System.setProperty("Environment_Name", "EURO2012");
		
		//parse the input
		URL url = getClass().getClassLoader().getResource("findpatient.xml");
		assertNotNull(url);
		System.out.println( url.getFile() );
		OMElement body = Util.parse_xml(url.openStream());
		
		//build the soap message
		OMElement output = null;
		try {
			output = soap.soapCall(body, DUMMY_ENDPOINT, NO_MTOM , NO_ADDRESSING , NO_SOAP12 , NO_ACTION , NO_EXPECTED_RETURN_ACTION );
		} catch (AxisFault e) {
			e.printStackTrace();
			System.out.println("Axis complains because endpoint is not available");
		}
		
		// $Antoine - Have introduced public method to bypass soap axis exception
		System.out.println(soap.getSoapHeader());
		
	}
	
	@Test
	public void BuildFindPatientEnvelopeViaTransaction() throws XdsInternalException, FactoryConfigurationError, IOException {
		URL url = getClass().getClassLoader().getResource("findpatient.xml");
		assertNotNull(url);
		System.out.println( url.getFile() );
		
		BasicTransaction transaction = new StoredQueryTransaction(null, null, null);
		Soap soap = new Soap();
		soap.setUseSaml(true);
		OMElement body = Util.parse_xml(url.openStream());
		
		try {
			soap.soapCall(body, NO_ENDPOINT, NO_MTOM , NO_ADDRESSING , NO_SOAP12 , NO_ACTION , NO_EXPECTED_RETURN_ACTION );
		} catch (XdsFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (LoadKeystoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (EnvironmentNotSelectedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println(soap.getOutHeader());
	}

}
