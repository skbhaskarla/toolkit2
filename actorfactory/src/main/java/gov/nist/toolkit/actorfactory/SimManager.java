package gov.nist.toolkit.actorfactory;

import gov.nist.toolkit.actorfactory.client.NoSimException;
import gov.nist.toolkit.actorfactory.client.SimId;
import gov.nist.toolkit.actorfactory.client.Simulator;
import gov.nist.toolkit.actorfactory.client.SimulatorConfig;
import gov.nist.toolkit.actortransaction.client.ActorType;
import gov.nist.toolkit.sitemanagement.Sites;
import gov.nist.toolkit.sitemanagement.client.Site;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Maintains the list of loaded SimulatorConfig objects for a
 * single session.
 * @author bill
 *
 */

public class SimManager {
	List<SimulatorConfig> simConfigs = new ArrayList<SimulatorConfig>();  // for this session
	String sessionId;

	public SimManager(String sessionId) {
		this.sessionId = sessionId;
	}
	
//*****************************************
//  These methods would normally belong in class SimulatorConfig but that
//  class is compiled for the client and some of these classes (ActorFactory)
//	do not belong on the client side.
//*****************************************
	static public Site getSite(SimulatorConfig config) throws Exception {
		AbstractActorFactory af = getActorFactory(config);
		return af.getActorSite(config, null);
	}

	static public AbstractActorFactory getActorFactory(SimulatorConfig config) throws Exception {
		String simtype = config.getType();
		ActorType at = ActorType.findActor(simtype);
		AbstractActorFactory af = AbstractActorFactory.getActorFactory(at);
		return af;
	}
//*****************************************


	
	public String sessionId() {
		return sessionId;
	}
	
	public void purge() throws IOException {
		List<SimulatorConfig> deletions = new ArrayList<>();
		for (SimulatorConfig sc : simConfigs) {
			try {
				new SimDb(sc.getId());
			} catch (NoSimException e) {
				deletions.add(sc);
			}
		}
		simConfigs.removeAll(deletions);
	}
	
	public void addSimConfigs(Simulator s) {
		simConfigs.addAll(s.getConfigs());
	}
	
	public void addSimConfig(SimulatorConfig config) {
		simConfigs.add(config);
	}
	
	public void setSimConfigs(List<SimulatorConfig> configs) {
		simConfigs = configs;
	}
	
	public SimulatorConfig getSimulatorConfig(SimId simId) {
		for (SimulatorConfig config : simConfigs) {
			if (simId.equals(config.getId()) && !config.isExpired())
				return config;
		}
		return null;
	}
	
	/**
	 * Get common sites and sim sites defined for this session.
	 * @return
	 * @throws Exception
	 */
	public Sites getAllSites() throws Exception {
		return getAllSites(SiteServiceManager.getSiteServiceManager().getCommonSites());
	}
	
	public Sites getAllSites(Sites commonSites)  throws Exception{
		Sites sites;
		
		if (commonSites == null)
			sites = new Sites();
		else
			sites = commonSites.clone();
		
		for (SimulatorConfig asc : simConfigs) {
			if (!asc.isExpired())
				sites.putSite(getSite(asc));
		}
		
		sites.buildRepositoriesSite();
		
		return sites;
	}

	/**
	 * Return map from simName => simId
	 * @return
	 */
	public Map<String, SimId> getNameMap() {
		Map<String, SimId> nameMap = new HashMap<>();
		
		for (SimulatorConfig sc : simConfigs) {
			String name = sc.getDefaultName();
			SimId id = sc.getId();
			nameMap.put(name, id);
		}
		
		return nameMap;
	}

	/**
	 * Remove simulator config.  Managed as a list for convienence
	 * not because there can be multiple (there can't)
	 * @param simId
	 */
	public void removeSimulatorConfig(SimId simId) {
		List<SimulatorConfig> delete = new ArrayList<SimulatorConfig>();
		for (SimulatorConfig sc : simConfigs) {
			if (sc.getId().equals(simId))
				delete.add(sc);
		}
		simConfigs.removeAll(delete);
	}

}
