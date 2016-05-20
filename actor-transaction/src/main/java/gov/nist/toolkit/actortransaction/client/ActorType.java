package gov.nist.toolkit.actortransaction.client;

import com.google.gwt.user.client.rpc.IsSerializable;
import gov.nist.toolkit.configDatatypes.client.TransactionType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// This file must be kept up to date with SimulatorActorTypes.java

/**
 * Actor types defined by test engine.  A subset of these are available as simulators.
 */
public enum ActorType implements IsSerializable, Serializable {
    XDR_DOC_SRC(
            "XDR Document Source",
            Arrays.asList("XDR_Source"),
            "xdrsrc",
            "gov.nist.toolkit.simulators.sim.src.XdrDocSrcActorSimulator",
            Arrays.asList(TransactionType.XDR_PROVIDE_AND_REGISTER),
            false,
            null
    ),
    REGISTRY(
            "Document Registry",
            Arrays.asList("DOC_REGISTRY", "registryb", "initialize_for_stored_query"),
            "reg",
            "gov.nist.toolkit.simulators.sim.reg.RegistryActorSimulator",
            Arrays.asList(TransactionType.REGISTER, TransactionType.REGISTER_ODDE, TransactionType.STORED_QUERY, TransactionType.UPDATE, TransactionType.MPQ),
            true,
            null
    ),
    // Update option on Document Registry
    // this should be removed once implications are re-discovered
//		UPDATE (
//				"Update Option",
//				new ArrayList<String>(),
//				"update",
//				new ArrayList<TransactionType>(),
//				false,
//				null
//				),
    REPOSITORY(
            "Document Repository",
            Arrays.asList("DOC_REPOSITORY", "repositoryb"),
            "rep",
            "gov.nist.toolkit.simulators.sim.rep.RepositoryActorSimulator",
            Arrays.asList(TransactionType.PROVIDE_AND_REGISTER, TransactionType.RETRIEVE),
            true,
            "repository"
    ),
    ONDEMAND_DOCUMENT_SOURCE(
            "On-Demand Document Source",
            Arrays.asList("ODDS", "ON_DEMAND_DOC_SOURCE"),
            "odds",
            "gov.nist.toolkit.simulators.sim.rep.od.OddsActorSimulator",
//            Arrays.asList(TransactionType.RETRIEVE),
            Arrays.asList(TransactionType.ODDS_RETRIEVE),
            true,
            "odds"
    ),
    ISR(
            "Integrated Source/Repository",
            Arrays.asList("EMBED_REPOS"),
            "isr",
            null,
            Arrays.asList(TransactionType.ISR_RETRIEVE),
            true,
            "isr"
    ),
    REPOSITORY_REGISTRY(
            "Document Repository/Registry",
            new ArrayList<String>(),
            "rr",
            "gov.nist.toolkit.simulators.sim.RegRepActorSimulator",
            Arrays.asList(TransactionType.REGISTER, TransactionType.STORED_QUERY, TransactionType.UPDATE, TransactionType.MPQ, TransactionType.PROVIDE_AND_REGISTER, TransactionType.RETRIEVE),
            false,
            null
    ),
    DOCUMENT_RECIPIENT(
            "Document Recipient",
            Arrays.asList("DOC_RECIPIENT"),
            "rec",
            "gov.nist.toolkit.simulators.sim.RegRepActorSimulator",
            Arrays.asList(TransactionType.XDR_PROVIDE_AND_REGISTER),
            true,
            null
    ),
    RESPONDING_GATEWAY(
            "Responding Gateway",
            Arrays.asList("RESP_GATEWAY"),
            "rg",
            "gov.nist.toolkit.simulators.sim.rg.RGADActorSimulator",
            Arrays.asList(TransactionType.XC_QUERY, TransactionType.XC_RETRIEVE,
               TransactionType.RET_IMG_DOC_SET),
            true,
            null
    ),
    INITIATING_GATEWAY(
            "Initiating Gateway",
            Arrays.asList("INIT_GATEWAY"),
            "ig",
            "gov.nist.toolkit.simulators.sim.ig.IgActorSimulator",
            Arrays.asList(TransactionType.IG_QUERY, TransactionType.IG_RETRIEVE,
               TransactionType.RET_IMG_DOC_SET),
            true,
            null
    ),
    INITIALIZE_FOR_STORED_QUERY (  // this is an artificial type used by test indexer
            "Initialize for Stored Query",
            new ArrayList<String>(),
            "initialize_for_stored_query",
            null,
            new ArrayList<TransactionType>(),
            false,
            null
    ),

    // Issue 78 (ODDS Issue 98)
    // TODO - actorType lookup problem
    // This at the end of the list on purpose.  From the UI actor types are selected by the transactions they support.
    // A problem came up in TransactionSelectionManager#generateSiteSpec() where this gets chosen instead of
    // REGISTRY when searching on STORED_QUERY.  getSiteSpec() (and the stuff around it) needs to make decisions
    // on more than just what transactions are offered.  Probably needs to maintain specific actor type so
    // the lookup by transaction is not necessary
    DOC_CONSUMER(
            "XDS Document Consumer",
            Arrays.asList("XDS_Consumer", "doccons"),
            "cons",
            "gov.nist.toolkit.simulators.sim.cons.DocConsActorSimulator",
            Arrays.asList(TransactionType.STORED_QUERY, TransactionType.RETRIEVE),
            false,
            null
    ),
    IMAGING_DOC_SOURCE(
            "Imaging Document Source",
            Arrays.asList("IMAGING_DOC_SOURCE"),
            "ids",
            "gov.nist.toolkit.simulators.sim.ids.IdsActorSimulator",
            Arrays.asList(TransactionType.RET_IMG_DOC_SET),
            true,
            null
        ),
    IMAGING_DOC_CONSUMER(
            "Imaging Document Consumer",
            Arrays.asList("IMAGING_DOC_CONSUMER", "XDSI_Consumer"),
            "idc",
            "gov.nist.toolkit.simulators.sim.idc.ImgDocConsActorSimulator",
            Arrays.asList(TransactionType.RET_IMG_DOC_SET),
            false,
            null
    )
    ;

    private static final long serialVersionUID = 1L;
    String name;
    List<String> altNames;
    String shortName;
    List<TransactionType> transactionTypes; // TransactionTypes this actor can receive
    boolean showInConfig;
    String actorsFileLabel;
    String simulatorClassName;

    ActorType() {
    } // for GWT

    ActorType(String name, List<String> altNames, String shortName, String simulatorClassName, List<TransactionType> tt, boolean showInConfig, String actorsFileLabel) {
        this.name = name;
        this.altNames = altNames;
        this.shortName = shortName;
        this.simulatorClassName = simulatorClassName;
        this.transactionTypes = tt;   // This actor receives
        this.showInConfig = showInConfig;
        this.actorsFileLabel = actorsFileLabel;
    }

    public boolean showInConfig() {
        return showInConfig;
    }

    public boolean isRepositoryActor() {
        return this.equals(REPOSITORY);
    }

    public boolean isRegistryActor() {
        return this.equals(REGISTRY);
    }

    public boolean isRGActor() {
        return this.equals(RESPONDING_GATEWAY);
    }

    public boolean isIGActor() {
        return this.equals(INITIATING_GATEWAY);
    }

    public boolean isGW() {
        return isRGActor() || isIGActor();
    }

    public boolean isImagingDocumentSourceActor() {
        return this.equals(IMAGING_DOC_SOURCE);
    }

    public String getSimulatorClassName() { return simulatorClassName; }

    public String getActorsFileLabel() {
        return actorsFileLabel;
    }

    static public List<String> getActorNames() {
        List<String> names = new ArrayList<String>();

        for (ActorType a : values())
            names.add(a.name);

        return names;
    }

    /**
     * Within toolkit, each TransactionType maps to a unique ActorType
     * (as receiver of the transaction). To make this work, transaction
     * names are customized to make this mapping unique.  This goes
     * beyond the definition in the TF.
     *
     * @param tt
     * @return
     */
    static public ActorType getActorType(TransactionType tt) {
        if (tt == null)
            return null;
        for (ActorType at : values()) {
            if (at.hasTransaction(tt))
                return at;
        }
        return null;
    }

    static public Set<ActorType> getActorTypes(TransactionType tt) {
        Set<ActorType> types = new HashSet<>();
        if (tt == null)
            return types;
        for (ActorType at : values()) {
            if (at.hasTransaction(tt))
                types.add(at);
        }
        return types;
    }

    static public ActorType findActor(String name) {
        if (name == null)
            return null;

        for (ActorType actor : values()) {
            if (actor.name.equals(name)) return actor;
            if (actor.shortName.equals(name)) return actor;
            if (actor.altNames.contains(name)) return actor;
        }
        return null;
    }

    static public TransactionType find(String receivingActorStr, String transString) {
        if (receivingActorStr == null || transString == null) return null;

        ActorType a = findActor(receivingActorStr);
        return a.getTransaction(transString);
    }

    public TransactionType getTransaction(String name) {
        for (TransactionType tt : transactionTypes) {
            if (tt.getShortName().equals(name)) return tt;
            if (tt.getName().equals(name)) return tt;
            if (tt.getId().equals(name)) return tt;
        }
        return null;
    }


    public String toString() {
        StringBuffer buf = new StringBuffer();

        buf.append(name).append(" [");
        for (TransactionType tt : transactionTypes)
            buf.append(tt).append(",");
        buf.append("]");

        return buf.toString();
    }

    public String getName() {
        return name;
    }

    public String getShortName() {
        return shortName;
    }

    public List<TransactionType> getTransactions() {
        return transactionTypes;
    }

    public boolean hasTransaction(TransactionType transType) {
        for (TransactionType transType2 : transactionTypes) {
            if (transType2.equals(transType))
                return true;
        }
        return false;
    }


    public boolean equals(ActorType at) {
        try {
            return name.equals(at.name);
        } catch (Exception e) {
        }
        return false;
    }
}
