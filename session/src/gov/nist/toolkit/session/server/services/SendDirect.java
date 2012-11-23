package gov.nist.toolkit.session.server.services;

import gov.nist.toolkit.actorfactory.CommonServiceManager;
import gov.nist.toolkit.installation.Installation;
import gov.nist.toolkit.results.client.Result;
import gov.nist.toolkit.session.server.DirectConfigManager;
import gov.nist.toolkit.session.server.Session;
import gov.nist.toolkit.utilities.io.Io;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

public class SendDirect extends CommonServiceManager {
	Session session;
	Map<String, String> params;
	byte[] signingCert;   // need password
	byte[] encryptionCert;
	String signingPassword;
	String password2;
	
	static final Logger logger = Logger.getLogger(SendDirect.class);

	
	public SendDirect(Session session, Map<String, String> params, byte[] signingCert, byte[] encryptionCert, String signingCertPassword, String unused)  {
		this.session = session;
		this.params = params;
		this.signingCert = signingCert;
		this.encryptionCert = encryptionCert;
		this.signingPassword = signingCertPassword;
		this.password2 = unused;
	}

	public List<Result> run() {
		try {
//			session.transactionSettings.assignPatientId = false;
			List<String> sections = null;

			String[] areas = new String[1];
			areas[0] = "utilities";
			
			Map<String, Object> params2 = new HashMap<String, Object>();
			
			// Use configured values instead of GUI
			DirectConfigManager directConfig = new DirectConfigManager(Installation.installation().externalCache());
			signingCert = directConfig.getSigningCert();
			signingPassword = directConfig.getSigningCertPassword();
			params2.put("signingCert", signingCert);
			params.put("signingCertPassword", signingPassword);

			if (encryptionCert == null) {
				// not uploaded - pre-installed for a known domain - go find it
				// it is required to be in .der format 
				String targetDomain = params.get("$direct_to_domain$");
				logger.debug("Target domain is " + targetDomain);
				File certFile = directConfig.getEncryptionCertFile(targetDomain);
				if (certFile == null)
					throw new Exception("Cannot load pre-installed cert for domain " + targetDomain);
				encryptionCert = Io.bytesFromFile(certFile);
			}
			params2.put("encryptionCert", encryptionCert);
			

			escapeWindowsBackslashes(params);
			
			session.isSoap = false;
			
			Result r = session.xdsTestServiceManager().xdstest("DirectSendTemplate", sections, params, params2, areas, true);
			return asList(r);
		} catch (Throwable e) {
			return buildExtendedResultList(e);
		} finally {
			session.clear();
		}
	}
	
	void escapeWindowsBackslashes(Map<String, String> parms) {
		for (String key : parms.keySet()) {
			String value = parms.get(key);
			if (value != null) {
				value = value.replaceAll("\\\\", "\\\\\\\\");
				parms.put(key, value);
			}
		}
	}
	
	static public void main(String[] args) {
		String value = "ab\\cd";
		String value2 = value.replaceAll("\\\\", "\\\\\\\\");
		System.out.println("new value is " + value2);
	}

}
