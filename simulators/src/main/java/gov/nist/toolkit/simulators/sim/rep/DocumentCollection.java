package gov.nist.toolkit.simulators.sim.rep;


import gov.nist.toolkit.simulators.support.StoredDocument;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class DocumentCollection implements Serializable {

	private static final long serialVersionUID = 1L;
	List<StoredDocument> documents;

	transient RepIndex repIndex;
	transient boolean dirty;

	public void add(StoredDocument d) {
		synchronized(repIndex) {
			d.setId(documents.size());
			documents.add(d);
			dirty = true;
		}
	}

	public void update(StoredDocument d) {
		synchronized(repIndex) {
			documents.remove(d.getId());
			documents.add(d.getId(),d);
			dirty = true;
		}
	}
	
	public void delete(String uid) {
		StoredDocument toDelete = null;
		for (StoredDocument sd : documents) {
			if (sd.getUid().equals(uid)) {
				toDelete = sd;
				break;
			}
		}
		if (toDelete != null)
			synchronized(repIndex) {
				documents.remove(toDelete);
			}
	}
	
	public StoredDocument getStoredDocument(String uid) {
		if (uid == null)
			return null;
		for (StoredDocument sd : documents) {
			if (uid.equals(sd.getUid()))
				return sd;
		}
		return null;
	}
	
	public void init() {
		documents = new ArrayList<>();
	}

	public int size() { return documents.size(); }

	public List<StoredDocument> getDocuments() {
		return documents;
	}
}
