package gov.nist.toolkit.xdstools2.server.gazelle.actorConfig;

public class OidConfigs extends CSVTable {

	static final int Systm = 1;
	static final int Type = 2;
	static final int Value = 3;
	
	public String getSystem(int entry) { return ((OidEntry)get(entry)).getSystem(); }
	public boolean isSourceId(int entry) { return get(entry, Type).startsWith("sourceID"); }
	public boolean isRepositoryUniqueId(int entry) { return get(entry, Type).startsWith("repositoryUniqueID OID");  } 
	public boolean isODDSRepositoryUniqueId(int entry) { return get(entry, Type).startsWith("repositoryUniqueID-OnDemandDocSrc");  } 
	public boolean isHomeCommunityId(int entry) { return get(entry, Type).startsWith("homeCommunityID"); }
	public String getValue(int entry) { return get(entry, Value); }
	
	public OidEntry get(int i) {
		return (OidEntry) super.get(i);
	}
	
	public void printAll() {
		System.out.println("OidConfigs:");
		for (int i=0; i<size(); i++) {
			System.out.println("OID: " +
								getSystem(i) + " " +
								getType(i) + " " + getValue(i));
		}
	}
	
	String getType(int i) {
		if (isSourceId(i)) return "SourceID";
		if (isRepositoryUniqueId(i)) return "RepositoryUniqueID";
		if (isHomeCommunityId(i)) return "HomeCommunityID";
		return "Unknown";
	}
	

	
	public String getRepUid(String system) {
		for (int i=0; i<this.size(); i++) {
			if (system.equals(getSystem(i)) && isRepositoryUniqueId(i)) {
				return getValue(i);
			}
		}
		return "";
	}

	public String getODDSRepUid(String system) {
		for (int i=0; i<this.size(); i++) {
			if (system.equals(getSystem(i)) && isODDSRepositoryUniqueId(i)) {
				return getValue(i);
			}
		}
		return "";
	}

	public String getHome(String system) {
		for (int i=0; i<this.size(); i++) {
			if (system.equals(getSystem(i)) && isHomeCommunityId(i)) {
				return getValue(i);
			}
		}
		return "";
	}

}
