package gov.nist.toolkit.actorfactory;

import gov.nist.toolkit.actorfactory.client.NoSimException;
import gov.nist.toolkit.actorfactory.client.SimId;
import gov.nist.toolkit.actorfactory.client.Simulator;
import gov.nist.toolkit.actorfactory.client.SimulatorConfig;
import gov.nist.toolkit.actortransaction.client.ActorType;
import gov.nist.toolkit.actortransaction.client.ParamType;
import gov.nist.toolkit.actortransaction.client.TransactionInstance;
import gov.nist.toolkit.actortransaction.client.TransactionType;
import gov.nist.toolkit.installation.Installation;
import gov.nist.toolkit.installation.PropertyServiceManager;
import gov.nist.toolkit.registrymetadata.UuidAllocator;
import gov.nist.toolkit.simcommon.client.config.SimulatorConfigElement;
import gov.nist.toolkit.sitemanagement.client.Site;
import gov.nist.toolkit.xdsexception.ExceptionUtil;
import gov.nist.toolkit.xdsexception.NoSimulatorException;
import gov.nist.toolkit.xdsexception.ToolkitRuntimeException;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * Factory class for simulators.  Technically ActorFactry is no longer accurate
 * since simulators are being built for things that are not IHE actors.
 * Oh well!
 * @author bill
 *
 */
public abstract class AbstractActorFactory {
	static Logger logger = Logger.getLogger(AbstractActorFactory.class);

	protected abstract Simulator buildNew(SimManager simm, SimId simId, boolean configureBase) throws Exception;
	//	protected abstract List<SimulatorConfig> buildNew(Session session, SimulatorConfig asc) throws Exception;
	protected abstract void verifyActorConfigurationOptions(SimulatorConfig config);
	public abstract Site getActorSite(SimulatorConfig asc, Site site) throws NoSimulatorException;
	public abstract List<TransactionType> getIncomingTransactions();
	//	protected abstract void addConfigElements(SimulatorConfig asc);

	static final Map<String /* ActorType.name */, AbstractActorFactory> factories = new HashMap<String, AbstractActorFactory>();
	static {
		factories.put(ActorType.REGISTRY.getName(),           		new RegistryActorFactory());
		factories.put(ActorType.REPOSITORY.getName(),         		new RepositoryActorFactory());
		factories.put(ActorType.ONDEMAND_DOCUMENT_SOURCE.getName(),	new OnDemandDocumentSourceActorFactory());
		factories.put(ActorType.DOCUMENT_RECIPIENT.getName(),  		new RecipientActorFactory());
		factories.put(ActorType.REPOSITORY_REGISTRY.getName(), 		new RepositoryRegistryActorFactory());
		factories.put(ActorType.INITIATING_GATEWAY.getName(),  		new IGActorFactory());
		factories.put(ActorType.RESPONDING_GATEWAY.getName(),  		new RGActorFactory());
        factories.put(ActorType.XDR_DOC_SRC.getName(), 				new XdrDocSrcActorFactory());
	}

	static public AbstractActorFactory getActorFactory(ActorType at) {
		return factories.get(at.getName());
	}

    public static final String                        xcpdEndpoint = "XCPD_endpoint";
	public static final String                     xcpdTlsEndpoint = "XCPD_TLS_endpoint";


    static final String name = "Name";
	static final String isTls = "UseTLS";
	static final String owner = "Owner";
	static final String description = "Description";

	PropertyServiceManager propertyServiceMgr = null;
	//	protected Session session;
	SimManager simManager;

	static public ActorType getActorTypeFromName(String name) {
		return ActorType.findActor(name);
	}

	protected SimulatorConfig configureBaseElements(ActorType simType) {
		return configureBaseElements(simType, null);
	}

	protected SimulatorConfig configureBaseElements(ActorType simType, SimId newId) {
		if (newId == null)
			newId = getNewId();
		SimulatorConfig sc = new SimulatorConfig(newId, simType.getShortName(), SimDb.getNewExpiration(SimulatorConfig.class));

		return configureBaseElements(sc);
	}	

	SimulatorConfig configureBaseElements(SimulatorConfig sc) {
		SimulatorConfigElement ele;

		ele = new SimulatorConfigElement();
		ele.name = SimulatorProperties.creationTime;
		ele.type = ParamType.TIME;
		ele.setValue(new Date().toString());
		addFixed(sc, ele);

		ele = new SimulatorConfigElement();
		ele.name = name;
		ele.type = ParamType.TEXT;
		ele.setValue(sc.getId().toString());
		addUser(sc, ele);

		return sc;
	}

	protected AbstractActorFactory() {}

	protected void setSimManager(SimManager simManager) {
		this.simManager = simManager;
	}

	public AbstractActorFactory(SimManager simManager) {
		this.simManager = simManager;
	}

	// Returns list since multiple simulators could be built as a grouping/cluster
	// only used by SimulatorFactory to offer a generic API for building sims
	public Simulator buildNewSimulator(SimManager simm, String simtype, SimId simID, boolean save) throws Exception {

		ActorType at = ActorType.findActor(simtype);

		if (at == null)
			throw new NoSimException("Simulator type [" + simtype + "] does not exist");

		return buildNewSimulator(simm, at, simID, save);

	}

	public Simulator buildNewSimulator(SimManager simm, ActorType at, SimId simID, boolean save) throws Exception {
		logger.info("Build new Simulator of type " + getClass().getSimpleName() + " simID: " + simID);

		// This is the simulator-specific factory
		AbstractActorFactory af = factories.get(at.getName());

		if (af == null)
			throw new ToolkitRuntimeException(String.format("Cannot build simulator of type %s - cannot find ActorType", at.getName()));

		af.setSimManager(simm);

		Simulator simulator = af.buildNew(simm, simID, true);

		// This is out here instead of being attached to a simulator-specific factory - why?
		if (save) {
			for (SimulatorConfig conf : simulator.getConfigs()) {
				AbstractActorFactory actorFactory = getActorFactory(conf);
				saveConfiguration(conf);

				BaseActorSimulator sim = RuntimeManager.getSimulatorRuntime(conf.getId());
				logger.info("calling onCreate:" + conf.getId().toString());
				sim.onCreate(conf);
			}
		}

		return simulator;
	}


	//
	// End of hooks


	// A couple of utility classes that get around a client class calling a server class - awkward
	static public ActorType getActorType(SimulatorConfig config) {
		return ActorType.findActor(config.getActorType());
	}

	static public AbstractActorFactory getActorFactory(SimulatorConfig config) {
		ActorType actorType = getActorType(config);
		String actorTypeName = actorType.getName();
		AbstractActorFactory actorFactory = factories.get(actorTypeName);
		return actorFactory;
	}

	public List<SimulatorConfig> checkExpiration(List<SimulatorConfig> configs) {
		List<SimulatorConfig> remove = new ArrayList<SimulatorConfig>();

		for (SimulatorConfig sc : configs) {
			if (sc.checkExpiration())
				remove.add(sc);
		}
		configs.removeAll(remove);
		return configs;
	}


	public SimId getNewId() {
		String id = UuidAllocator.allocate();
		String[] parts = id.split(":");
		id = parts[2];
		//		id = id.replaceAll("-", "_");

		return new SimId(id);
	}

	String mkEndpoint(SimulatorConfig asc, SimulatorConfigElement ele, boolean isTLS) {
		return mkEndpoint(asc, ele, asc.getActorType().toLowerCase(), isTLS);
	}

	protected String mkEndpoint(SimulatorConfig asc, SimulatorConfigElement ele, String actor, boolean isTLS) {
		String transtype = SimDb.getTransactionDirName(ele.transType);

		String contextName = Installation.installation().tkProps.get("toolkit.servlet.context", "xdstools2");

		return "http"
		+ ((isTLS) ? "s" : "")
		+ "://" 
		+ Installation.installation().propertyServiceManager().getToolkitHost() 
		+ ":" 
		+ ((isTLS) ? Installation.installation().propertyServiceManager().getToolkitTlsPort() : Installation.installation().propertyServiceManager().getToolkitPort()) 
		+ "/"  
		+ contextName  
		+ "/sim/" 
		+ asc.getId() 
		+ "/" +
		actor           //asc.getActorType().toLowerCase()
		+ "/" 
		+ transtype;
	}

	public void saveConfiguration(SimulatorConfig config) throws Exception {
		verifyActorConfigurationOptions(config);

		//
		// This statically links the IG to the CURRENT list of remote sites that it could possibly be a
		// gateway to in the future.  BAD IDEA.  This list needs to be generated on the fly so it is current.
		//
//		if (config.getActorType().equals(ActorType.INITIATING_GATEWAY.getName())) {
//			// must load up XCQ and XCR endpoints for simulator to use
//			config.remoteSites = new ArrayList<>();
//
//			Sites sites = simManager.getAllSites();
//			for (String remote : config.rgSiteNames) {
//				Site site = sites.getSite(remote);
//				config.remoteSites.add(site);
//			}
//		}
		//
		//

		SimDb simdb = SimDb.mkSim(Installation.installation().simDbFile(), config.getId(), config.getActorType());
		File simCntlFile = simdb.getSimulatorControlFile();
		new SimulatorConfigIo().save(config, simCntlFile.toString());   //config.save(simCntlFile.toString());
	}

	static public void delete(SimulatorConfig config) throws IOException {
        delete(config.getId());
    }

    static public void delete(SimId simId) throws IOException {
        logger.info("delete simulator" + simId);
		SimDb simdb;
		try {
			BaseActorSimulator sim = RuntimeManager.getSimulatorRuntime(simId);
            SimulatorConfig config = loadSimulator(simId, true);
            if (config != null)
			    sim.onDelete(config);

			simdb = new SimDb(simId);
			File simDir = simdb.getSimDir();
			simdb.delete(simDir);

        } catch (NoSimException e) {
			return;		
		} catch (ClassNotFoundException e) {
			logger.error(ExceptionUtil.exception_details(e));
		} catch (InvocationTargetException e) {
			logger.error(ExceptionUtil.exception_details(e));
		} catch (InstantiationException e) {
			logger.error(ExceptionUtil.exception_details(e));
		} catch (IllegalAccessException e) {
			logger.error(ExceptionUtil.exception_details(e));
		}
//		AbstractActorFactory actorFactory = getActorFactory(config);
	}

	static public boolean simExists(SimulatorConfig config) throws IOException {
		SimDb simdb;
		try {
			simdb = new SimDb(config.getId());
		} catch (NoSimException e) {
			return false;
		}
		File simDir = simdb.getSimDir();
		return simDir.exists();
	}

	static public List<TransactionInstance> getTransInstances(SimId simid, String xactor, String trans) throws NoSimException
	{
		SimDb simdb;
		try {
			simdb = new SimDb(simid);
		} catch (IOException e) {
			throw new ToolkitRuntimeException("Error loading sim " + simid + " of actor " + xactor,e);
		}
		ActorType actor = simdb.getSimulatorActorType();
		return simdb.getTransInstances(actor.toString(), trans);
	}

	static public void renameSimFile(String simFileSpec, String newSimFileSpec)
			throws Exception {
		new SimDb().rename(simFileSpec, newSimFileSpec);
	}

	static public List<SimulatorConfig> getSimConfigs(ActorType actorType) {
		return getSimConfigs(actorType.getName());
	}

	static public List<SimulatorConfig> getSimConfigs(String actorTypeName) {
		SimDb db = new SimDb();

		List<SimId> allSimIds = db.getAllSimIds();
		List<SimulatorConfig> simConfigs = new ArrayList<>();

		try {
			for (SimulatorConfig simConfig : loadSimulators(allSimIds)) {
				if (actorTypeName.equals(simConfig.getActorType()))
					simConfigs.add(simConfig);
			}
		} catch (Exception e) {
			throw new ToolkitRuntimeException("Error loading simulators of type " + actorTypeName + ".", e);
		}

		return simConfigs;
	}


	/**
	 * Load simulators - IOException if sim not found
	 * @param ids
	 * @return
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws NoSimException 
	 */
	static public List<SimulatorConfig> loadSimulators(List<SimId> ids) throws IOException, ClassNotFoundException, NoSimException {
		List<SimulatorConfig> configs = new ArrayList<SimulatorConfig>();

		for (SimId id : ids) {
			SimDb simdb = new SimDb(id);
			File simCntlFile = simdb.getSimulatorControlFile();
			SimulatorConfig config = restoreSimulator(simCntlFile.toString());
			configs.add(config);
		}

		return configs;
	}

//    public SimulatorConfig loadSimulator(SimId simId) throws IOException, ClassNotFoundException {
//        List<SimId> ids = new ArrayList<>();
//        ids.add(simId);
//        List<SimulatorConfig> configs = loadAvailableSimulators(ids);
//        if (configs.size() == 0) return null;
//        return configs.get(0);
//    }

	/**
	 * Load simulators - ignore sims not found (length(simlist) &lt; length(idlist))
	 * @param ids
	 * @return
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public List<SimulatorConfig> loadAvailableSimulators(List<SimId> ids) throws IOException, ClassNotFoundException {
		List<SimulatorConfig> configs = new ArrayList<SimulatorConfig>();

		for (SimId id : ids) {
			try {
				SimDb simdb = new SimDb(id);
				File simCntlFile = simdb.getSimulatorControlFile();
				SimulatorConfig config = restoreSimulator(simCntlFile.toString());
				configs.add(config);
			} catch (NoSimException e) {
				continue;
			}
		}

		return configs;
	}

	static SimulatorConfig restoreSimulator(String filename) throws IOException, ClassNotFoundException {
		FileInputStream fis = null;
		ObjectInputStream in = null;
		SimulatorConfig config;
		fis = new FileInputStream(filename);
		in = new ObjectInputStream(fis);
		config = (SimulatorConfig)in.readObject();
		in.close();

		return config;
	}

	static public SimulatorConfig loadSimulator(SimId simid, boolean okifNotExist) throws IOException, ClassNotFoundException, NoSimException {
		SimDb simdb;
		try {
			simdb = new SimDb(simid);
		} catch (NoSimException e) {
			if (okifNotExist) return null;
			throw e;
		}
		File simCntlFile = simdb.getSimulatorControlFile();
		SimulatorConfig config = restoreSimulator(simCntlFile.toString());
		return config;
	}	

	static public SimulatorConfig getSimConfig(File simDbFile, SimId simulatorId) throws IOException, ClassNotFoundException, NoSimException {
		SimDb simdb = new SimDb(simDbFile, simulatorId, null, null);
		File simCntlFile = simdb.getSimulatorControlFile();
		SimulatorConfig config = restoreSimulator(simCntlFile.toString());
		return config;
	}

	protected boolean isEndpointSecure(String endpoint) {
		return endpoint.startsWith("https");
	}

	protected List<SimulatorConfig> asList(SimulatorConfig asc) {
		List<SimulatorConfig> ascs = new ArrayList<SimulatorConfig>();
		ascs.add(asc);
		return ascs;
	}

	protected void addFixed(SimulatorConfig sc, SimulatorConfigElement ele) {
		ele.setEditable(false);
		sc.elements().add(ele);
	}

	void addUser(SimulatorConfig sc, SimulatorConfigElement ele) {
		ele.setEditable(true);
		sc.elements().add(ele);
	}

	public void addEditableConfig(SimulatorConfig sc, String name, ParamType type, Boolean value) {
		addUser(sc, new SimulatorConfigElement(name, type, value));
	}

	public void addEditableConfig(SimulatorConfig sc, String name, ParamType type, String value) {
		addUser(sc, new SimulatorConfigElement(name, type, value));
	}

    public void addEditableConfig(SimulatorConfig sc, String name, ParamType type, List<String> values, boolean isMultiSelect) {
        addUser(sc, new SimulatorConfigElement(name, type, values, isMultiSelect));
    }

    public void addFixedConfig(SimulatorConfig sc, String name, ParamType type, Boolean value) {
		addFixed(sc, new SimulatorConfigElement(name, type, value));
	}

	public void addFixedConfig(SimulatorConfig sc, String name, ParamType type, String value) {
		addFixed(sc, new SimulatorConfigElement(name, type, value));
	}

	public void setConfig(SimulatorConfig sc, String name, String value) {
		SimulatorConfigElement ele = sc.getUserByName(name);
		if (ele == null) throw new ToolkitRuntimeException("Simulator " + sc.getId() + " does not have a parameter named " + name + " or cannot set its value");
		ele.setValue(value);
	}

	public void setConfig(SimulatorConfig sc, String name, Boolean value) {
		SimulatorConfigElement ele = sc.getUserByName(name);
		if (ele == null) throw new ToolkitRuntimeException("Simulator " + sc.getId() + " does not have a parameter named " + name + " or cannot set its value");
		ele.setValue(value);
	}

	public void addEditableEndpoint(SimulatorConfig sc, String endpointName, ActorType actorType, TransactionType transactionType, boolean tls) {
		SimulatorConfigElement ele = new SimulatorConfigElement();
		ele.name = endpointName;
		ele.type = ParamType.ENDPOINT;
		ele.transType = transactionType;
		ele.setValue(mkEndpoint(sc, ele, actorType.getShortName(), tls));
		addUser(sc, ele);
	}

	public void addFixedEndpoint(SimulatorConfig sc, String endpointName, ActorType actorType, TransactionType transactionType, boolean tls) {
		SimulatorConfigElement ele = new SimulatorConfigElement();
		ele.name = endpointName;
		ele.type = ParamType.ENDPOINT;
		ele.transType = transactionType;
		ele.setValue(mkEndpoint(sc, ele, actorType.getShortName(), tls));
		addFixed(sc, ele);
	}


}
