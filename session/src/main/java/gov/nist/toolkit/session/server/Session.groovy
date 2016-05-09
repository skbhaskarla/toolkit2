package gov.nist.toolkit.session.server
import gov.nist.toolkit.actorfactory.SimCache
import gov.nist.toolkit.configDatatypes.client.Pid
import gov.nist.toolkit.actorfactory.client.SimId
import gov.nist.toolkit.envSetting.EnvSetting
import gov.nist.toolkit.installation.ExternalCacheManager
import gov.nist.toolkit.installation.Installation
import gov.nist.toolkit.installation.PropertyServiceManager
import gov.nist.toolkit.registrymetadata.Metadata
import gov.nist.toolkit.results.client.AssertionResults
import gov.nist.toolkit.results.client.CodesConfiguration
import gov.nist.toolkit.results.client.SiteSpec
import gov.nist.toolkit.securityCommon.SecurityParams
import gov.nist.toolkit.session.server.serviceManager.QueryServiceManager
import gov.nist.toolkit.session.server.serviceManager.XdsTestServiceManager
import gov.nist.toolkit.simcommon.server.ExtendedPropertyManager
import gov.nist.toolkit.sitemanagement.Sites
import gov.nist.toolkit.sitemanagement.client.Site
import gov.nist.toolkit.testengine.engine.PatientIdAllocator
import gov.nist.toolkit.testengine.engine.TransactionSettings
import gov.nist.toolkit.testengine.engine.Xdstest2
import gov.nist.toolkit.tk.TkLoader
import gov.nist.toolkit.tk.client.TkProps
import gov.nist.toolkit.xdsexception.EnvironmentNotSelectedException
import gov.nist.toolkit.xdsexception.ToolkitRuntimeException
import gov.nist.toolkit.xdsexception.XdsInternalException
import groovy.transform.TypeChecked
import org.apache.log4j.Logger
/**
 * The session object is used in one of four ways depending on the context:
 * 
 * 1) GUI - each GUI session is represented by a session object. It is managed
 * through the Servlet session mechanism. It is managed by the class
 * ToolkitServiceImpl. This is a single threaded use of the session instance.
 * 
 * 2) Simulators - on session object is used and shared by all simulator
 * instances.  It is managed by the class SimServlet. This is a 
 * multi-threaded use of the session instance.
 * 
 * 3) ValServlet - RESTful service for access to validators. Just like
 * simulators above.
 * 
 * 4) Dashboard - coming soon.
 * @author bill
 *
 */
@TypeChecked
public class Session implements SecurityParams {
	
	public Xdstest2 xt;
	public SiteSpec siteSpec = new SiteSpec();
	public String repUid;
	public AssertionResults assertionResults;
	public TransactionSettings transactionSettings = new TransactionSettings();
	public boolean isAdmin = false;
	public boolean isSoap = true;
	
	byte[] lastUpload = null;
	byte[] input2 = null;
	String lastUploadFilename = null;
	String input2Filename = null;
	byte[] lastUpload2 = null;
	String lastUploadFilename2 = null;
	String password1 = null;
	String password2 = null;
	
	File mesaSessionCache = null;     // changes each time new mesaSessionName changes
	
	Metadata lastMetadata = null;
	public String ipAddr = null;  // also used as default sim db id
	String serverIP = null;
	String serverPort = null;
	SimCache simCache = new SimCache();
	String sessionId = Installation.installation().defaultSessionName();
	
	File toolkit = null;
	
	String currentEnvName = null;
	
	String mesaSessionName = null;
	SessionPropertyManager sessionProperties = null;
	PropertyServiceManager propertyServiceMgr = null;
	XdsTestServiceManager xdsTestServiceManager = null;
	QueryServiceManager queryServiceMgr = null;
	static Map<String, Session> sessionMap = new HashMap<String, Session>();
	// environment name ==> codes configuration
	static Map<String, CodesConfiguration> codesConfigurations = new Hashtable<>();

	static final Logger logger = Logger.getLogger(Session.class);
	
	public boolean isTls() {
		return siteSpec.isTls;
	}
	
	public boolean isSaml() {
		return siteSpec.isSaml;
	}
	
	public boolean isAsync() {
		return siteSpec.isAsync;
	}
	
	public void setTls(boolean tls) {
		siteSpec.isTls = tls;
	}
	
	public void setSaml(boolean saml) {
		siteSpec.isSaml = saml;
	}
	
	public void setAsync(boolean async) {
		siteSpec.isAsync = async;
	}
	
	public void verifyCurrentEnvironment() throws EnvironmentNotSelectedException {
		EnvSetting.getEnvSetting(sessionId);
	}
	
	public void addSession() {
		sessionMap.put(sessionId, this);
	}
	
	static public Session getSession(String sessionId) {
		return sessionMap.get(sessionId);
	}
	
	public void setSessionId(String id) {
		sessionId = id;
	}

	public void setSiteSpec(SiteSpec siteSpec) {
		this.siteSpec = siteSpec;
		transactionSettings = new TransactionSettings();
		transactionSettings.siteSpec = siteSpec;
		
		if (repUid == null || repUid.equals("")) {
			// this will not always work and is not always relevant - just try
			//    WHY?
			try {
				Sites sites = new SimCache().getSimManagerForSession(id()).getAllSites();
				Site st = sites.getSite(siteSpec.name);
                logger.info("site is " + st);
                logger.info(st.describe());
				repUid = st.getRepositoryUniqueId();
			} catch (Exception e) {
			}
		}

	}

	public String getServerIP() {
		return serverIP;
	}
	
	public String getServerPort() {
		return serverPort;
	}
	
	public Session(File warHome, String sessionId) {
		this(warHome);
		this.sessionId = sessionId;
	}
	
	public Session(File warHome) {
		Installation.installation().warHome(warHome);
		ExtendedPropertyManager.load(warHome);
		System.out.print("warHome[Session]: " + warHome + "\n");

        ExternalCacheManager.initialize();
//		File externalCache = new File(Installation.installation().propertyServiceManager().getPropertyManager().getExternalCache());
//        System.out.println("External Cache from WAR set to " + externalCache.toString());
//		Installation.installation().externalCache(externalCache);
//		if (externalCache == null || !externalCache.exists() || !externalCache.isDirectory())
//			externalCache = null;
//		Installation.installation().externalCache(externalCache);
	}

	public Session(File warHome, File externalCache) {
		Installation.installation().warHome(warHome);
		ExtendedPropertyManager.load(warHome);
		System.out.print("warHome[Session]: " + warHome + "\n");

//		System.out.println("External Cache set to " + externalCache.toString());
//		Installation.installation().externalCache(externalCache);
        ExternalCacheManager.initialize(externalCache);
	}

	public QueryServiceManager queryServiceManager() {
		if (queryServiceMgr == null)
			queryServiceMgr = new QueryServiceManager(this);
		return queryServiceMgr;
	}
	
	public XdsTestServiceManager xdsTestServiceManager() {
		if (xdsTestServiceManager == null)
			xdsTestServiceManager = new XdsTestServiceManager(this);
		return xdsTestServiceManager;
	}
	
	public void setMesaSessionName(String name) {
		mesaSessionName = name;

		File testLogCache;
		try {
			testLogCache = Installation.installation().propertyServiceManager().getTestLogCache();
		} catch (Exception e) {
			return;
		}
		
		mesaSessionCache = new File(testLogCache, mesaSessionName);
		mesaSessionCache.mkdirs();
	}

	public String getMesaSessionName() { return mesaSessionName; }

	public void setSessionProperties(Map<String, String> m) {
		SessionPropertyManager props = getSessionProperties();
		if (props == null)
			return;
		props.clear();
		props.add(m);
		props.save();
	}

	/**
	 * Get id of current Session
	 * @return
	 */
	public String getId() {
		return sessionId;
	}
	
	public String id() {
		return sessionId;
	}

	static public String getServletContextName() {
		return servletContextName;
	}
	
	public String getSimBaseEndpoint() {
		// the last piece must agree with simulatorServlet in web.xml
		return "http://" + serverIP + ":" + serverPort + servletContextName + "/simulator"
	}
	
	public void setServerSpec(String ip, String port) {
		serverIP = translateIPAddr(ip);
		serverPort = port;
		
	}
	
	String translateIPAddr(String ip) {
		if ("0:0:0:0:0:0:0:1%0".equals(ip)) {
			// value returned when in GWT hosted mode
			return "127.0.0.1";
		}
		return ip;
		
	}
	
	public void setIpAddr(String ip) {
		ipAddr = translateIPAddr(ip);
	}
	
	public String getIpAddr() {
		return ipAddr;
	}
	
	public SimId getDefaultSimId() {
		return new SimId(ipAddr);
	}
	
	public void setLastUpload(String filename, byte[] last, String filename2, byte[] last2) {
		lastUploadFilename = filename;
		lastUpload = last;
		
		lastUploadFilename2 = filename2;
		lastUpload2 = last2;
	}
	
	public void setLastUpload(String filename, byte[] last, String pass1, String filename2, byte[] last2, String pass2) {
		lastUploadFilename = filename;
		lastUpload = last;
		
		lastUploadFilename2 = filename2;
		lastUpload2 = last2;
		
		password1 = pass1;
		password2 = pass2;
		logger.info("lastUpload size=" + ((lastUpload == null) ? "null" : Integer.toString(lastUpload.length)));
		logger.info("lastUpload2 size=" + ((lastUpload2 == null) ? "null" : Integer.toString(lastUpload2.length)));
	}
	
	public byte[] getlastUpload() {
		return lastUpload;
	}
	
	public String getPassword1() {
		return password1;
	}
	
	public String getPassword2() {
		return password2;
	}
	
	public byte[] getInput2() {
		return input2;
	}

	public byte[] getlastUpload2() {
		return lastUpload2;
	}
	
	public String getlastUploadFilename() {
		return lastUploadFilename;
	}
	
	public String getInput2Filename() {
		return input2Filename;
	}

	public String getlastUploadFilename2() {
		return lastUploadFilename2;
	}
		
//	public File getTestkitFile() {
//		return new File(Installation.installation().warHome() + File.separator + "toolkitx" + File.separator + "testkit");
//	}
			
	/**
	 * Reset linkage to XdsTest
	 */
	public void clear() {
		xt = null;
		assertionResults = null;
	}

	public Metadata getLastMetadata() {
		return lastMetadata;
	}

	public void setLastMetadata(Metadata lastMetadata) {
		this.lastMetadata = lastMetadata;
	}
	
	/*
	 * Manage the environment, choice of keystore and codes.xml
	 * 
	 */

	public File getEnvironmentDir() throws EnvironmentNotSelectedException {
		try {
			return EnvSetting.getEnvSetting(sessionId).getEnvDir();
		} catch (Exception e) {
			throw new EnvironmentNotSelectedException("", e);
		}
	}
	
	public File getEnvironment() throws EnvironmentNotSelectedException { return getEnvironmentDir(); }
	
	public File getCodesFile() throws EnvironmentNotSelectedException {
		if (getEnvironmentDir() == null) 
			return null; // new File(Installation.installation().warHome() + File.separator + "toolkitx" + File.separator + "codes" + File.separator + "codes.xml");
		File f = new File(getEnvironmentDir(), "codes.xml");
		if (f.exists())
			return f;
		return null;
	}
	
	public File getKeystoreDir() throws EnvironmentNotSelectedException {
		File f = new File(getEnvironmentDir(), "keystore");
		if (f.exists() && f.isDirectory())
			return f;
		throw new EnvironmentNotSelectedException("");
	}
	
	public File getKeystore() throws EnvironmentNotSelectedException {
		File kd = getKeystoreDir();
		return new File(kd, "keystore");
	}
	
	public String getKeystorePassword() throws IOException, EnvironmentNotSelectedException {
		Properties p = new Properties();
		File f = new File(getKeystoreDir(), "keystore.properties");
		if (!f.exists())
			return "";
		FileInputStream fis = new FileInputStream(f);
		p.load(fis);
		return p.getProperty("keyStorePassword");
	}
	
	static public List<String> getEnvironmentNames() {
		logger.debug( ": " + "getEnvironmentNames");
		List<String> names = new ArrayList<String>();
		
		File k = Installation.installation().environmentFile();     //propertyServiceManager().getPropertyManager().getExternalCache() + File.separator + "environment");
		if (!k.exists() || !k.isDirectory())
			return names;
		File[] files = k.listFiles();
		for (File file : files)
			if (file.isDirectory() && !(file.getName().equals("TestLogCache"))) {
				names.add(file.getName());
			}
		return names;
	}

    static public boolean environmentExists(String environmentName) {
        return getEnvironmentNames().contains(environmentName);
    }
	
	public TkProps tkProps() {
		try {
			return TkLoader.tkProps(Installation.installation().getTkPropsFile());
		} catch (Throwable t) {
			return new TkProps();
		}
	}
	
	/**
	 * Sets name of current environment (for this session)
	 * @throws 
	 */
	public void setEnvironment(String name) {
		if (name == null || name.equals("")) {
            logger.info("Session set environment - null ignored")
            return;
        }
		logger.info("Session: " + getId() + ": " + " Environment set to " + name);
		setEnvironment(name, Installation.installation().propertyServiceManager().getPropertyManager().getExternalCache());
	}
	
	public void setEnvironment(String name, String externalCache) {
		File k = Installation.installation().environmentFile(name);
		if (!k.exists() || !k.isDirectory())
			throw new ToolkitRuntimeException("Environment " + name + " does not exist");
		currentEnvName = name;
		System.setProperty("XDSCodesFile", k.toString() + File.separator + "codes.xml");
		new EnvSetting(sessionId, name, k);
		logger.debug(getId() + ": " + "Environment set to " + k);
	}
	
	public String getCurrentEnvironment() {
        if (!currentEnvName)
            currentEnvName = Installation.installation().defaultEnvironmentName()
		return currentEnvName;
	}
	
	public SessionPropertyManager getSessionProperties() {
		if (mesaSessionName == null)
			return null;

		if (sessionProperties == null) {
			File testLogCache;
			try {
				testLogCache = Installation.installation().propertyServiceManager().getTestLogCache();
			} catch (Exception e) {
				return null;
			}
			
			sessionProperties = new SessionPropertyManager(testLogCache.toString());
		}
		
		return sessionProperties;
	}
	
	public Map<String, String> getSessionPropertiesAsMap() {
		logger.debug(": " + "getSessionProperties()");
		return getSessionProperties().asMap();
	}

	public File getToolkitFile() {
		if (toolkit == null)
			toolkit = Installation.installation().toolkitxFile();
		return toolkit;
	}

	public Pid allocateNewPid(String assigningAuthority) {
		return PatientIdAllocator.getNew(assigningAuthority);
	}

	public Pid allocateNewPid() throws Exception {
		return PatientIdAllocator.getNew(getAssigningAuthority());
	}

	public CodesConfiguration getCodesConfiguration(String environmentName) throws XdsInternalException {
        assert environmentName

		CodesConfiguration config = codesConfigurations.get(environmentName);
		if (config != null) return config;

		File codesFile = getCodesFile();
		if (!codesFile.exists()) throw new XdsInternalException("No code configuration defined for Environment " + environmentName +
		" or that Environment does not exist");
		CodesConfigurationBuilder builder = new CodesConfigurationBuilder(codesFile);
		config = builder.get();
		codesConfigurations.put(environmentName, config);

        assert config
		return config;
	}

	public CodesConfiguration currentCodesConfiguration() throws XdsInternalException {
		return getCodesConfiguration(getCurrentEnvironment());
	}

	public String getAssigningAuthority() throws Exception {
		CodesConfiguration config = null;
		try {
			config = currentCodesConfiguration();
		} catch (XdsInternalException e) {
			throw new Exception("Error loading current Assigning Authority", e);
		}
		return config.getAssigningAuthorityOid();
	}

	public List<String> getAssigningAuthorities() throws Exception {
		CodesConfiguration config = null;
		try {
			config = currentCodesConfiguration();
		} catch (XdsInternalException e) {
			throw new Exception("Error loading current Assigning Authority", e);
		}
		return config.getAssigningAuthorityOids();
	}

}
