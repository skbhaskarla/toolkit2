package gov.nist.toolkit.soap; 

import static org.junit.Assert.*;

import gov.nist.toolkit.installation.Installation;
import gov.nist.toolkit.securityCommon.SecurityParams;
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
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import javax.swing.plaf.basic.BasicArrowButton;
import javax.xml.parsers.FactoryConfigurationError;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.tools.ant.util.WeakishReference.HardReference;
import org.junit.Test;

public class SOAPmessageBuilder_FromSoapObject_Test {
	
	
	private final String WAR_HOME= getClass().getClassLoader().getResource(".").getPath();
	//the soap code calls it axis2 repository...  
	private final String HARCODED_PATH_TO_RAMPART="/Users/gerardin/Documents/workspace/iheos_FROM_SOURCEFORGE/xdstools2/war/toolkitx/xdstest";
	
	private final String HARCODED_PATH_TO_ENVIRONMENT="/Users/gerardin/IHE-Testing/xdstools2_environment/environment/NA2013-2";
	
	private final String MESSAGE_NAME="findpatient.xml";

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
	 * - Configuration is an ordeal to set up. This indicates we should review and test the 
	 * configuration mechanism to improve code readability and test automation.
	 * - Soap is trying to send a message after building it.
	 * Separation of concerns principles indicates we should refactor this.
	 * Moreover, this prevents us to write a meaningful unit test since a exception is thrown
	 */
	@Test
	public void BuildFindPatientSOAPEnvelope() throws XdsInternalException, FactoryConfigurationError, IOException, XdsFormatException, LoadKeystoreException, EnvironmentNotSelectedException {
		
		TestConfig testConfig = configureTestWithRampart(HARCODED_PATH_TO_RAMPART);
		SecurityParams secParams = configureSecurityParams(HARCODED_PATH_TO_ENVIRONMENT);
		Soap soap = configureSoap(testConfig);	
		soap.setSecurityParams(secParams);
		
		
		OMElement body = parseXMLInput(MESSAGE_NAME);
		
		
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

	private SecurityParams configureSecurityParams( final String envDir) {
		SecurityParams secParams = new SecurityParams() {
			
			@Override
			public String getKeystorePassword() throws IOException,
					EnvironmentNotSelectedException {
				Properties p = new Properties();
				File f = new File(getKeystoreDir() + File.separator + "keystore.properties");
				if (!f.exists())
					return "";
				FileInputStream fis = new FileInputStream(f);
				p.load(fis);
				return p.getProperty("keyStorePassword");
			}
			
			@Override
			public File getKeystoreDir() throws EnvironmentNotSelectedException {
				File f = new File( envDir + File.separator + "keystore");
				if (f.exists() && f.isDirectory())
					return f;
				throw new EnvironmentNotSelectedException("");
			}
			
			@Override
			public File getKeystore() throws EnvironmentNotSelectedException {
				File kd = getKeystoreDir();
				return new File(kd + File.separator + "keystore");
			}
			
			@Override
			public File getCodesFile() {
					return new File(envDir + File.separator + "codes.xml");
			}
		};
		
		return secParams;
	}

	private OMElement parseXMLInput(String fileName) throws XdsInternalException, FactoryConfigurationError, IOException {
		URL url = getClass().getClassLoader().getResource(fileName);
		assertNotNull(url);
		System.out.println( url.getFile() );
		OMElement body = Util.parse_xml(url.openStream());
		return body;
	}

	private Soap configureSoap(TestConfig testConfig) {
		Soap soap = new Soap();
		soap.setUseSaml(true);
		soap.setRepositoryLocation(testConfig.testmgmt_dir + File.separator + "rampart" + File.separator + "client_repositories" );
		return soap;
	}

	private TestConfig configureTestWithRampart(String path) {
		TestConfig testConfig = new TestConfig();
		testConfig.testmgmt_dir = path;	
		return testConfig;
	}

	

}
