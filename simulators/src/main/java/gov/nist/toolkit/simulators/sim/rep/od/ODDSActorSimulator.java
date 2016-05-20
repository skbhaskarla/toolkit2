package gov.nist.toolkit.simulators.sim.rep.od;

import gov.nist.toolkit.actorfactory.SimDb;
import gov.nist.toolkit.actorfactory.client.NoSimException;
import gov.nist.toolkit.actorfactory.client.SimId;
import gov.nist.toolkit.actorfactory.client.SimulatorConfig;
import gov.nist.toolkit.actorfactory.client.SimulatorStats;
import gov.nist.toolkit.configDatatypes.client.TransactionType;
import gov.nist.toolkit.errorrecording.GwtErrorRecorderBuilder;
import gov.nist.toolkit.errorrecording.client.XdsErrorCode.Code;
import gov.nist.toolkit.registrymsg.registry.RegistryErrorListGenerator;
import gov.nist.toolkit.registrymsg.registry.Response;
import gov.nist.toolkit.simcommon.client.config.SimulatorConfigElement;
import gov.nist.toolkit.simulators.servlet.SimServlet;
import gov.nist.toolkit.simulators.sim.reg.RegistryResponseGeneratingSim;
import gov.nist.toolkit.simulators.sim.reg.SoapWrapperRegistryResponseSim;
import gov.nist.toolkit.simulators.sim.rep.RepIndex;
import gov.nist.toolkit.simulators.support.BaseDsActorSimulator;
import gov.nist.toolkit.simulators.support.DsSimCommon;
import gov.nist.toolkit.simulators.support.SimCommon;
import gov.nist.toolkit.utilities.xml.XmlUtil;
import gov.nist.toolkit.valregmsg.message.SoapMessageValidator;
import gov.nist.toolkit.valsupport.engine.MessageValidatorEngine;
import gov.nist.toolkit.valsupport.message.AbstractMessageValidator;
import org.apache.axiom.om.OMElement;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class OddsActorSimulator extends BaseDsActorSimulator {
	static final Logger logger = Logger.getLogger(OddsActorSimulator.class);

	RepIndex repIndex;
	String repositoryUniqueId;

	static List<TransactionType> transactions = new ArrayList<>();

	static {
// skb		transactions.add(TransactionType.PROVIDE_AND_REGISTER);
		transactions.add(TransactionType.RETRIEVE);
	}

	public boolean supports(TransactionType transactionType) {
		return transactions.contains(transactionType);
	}

	public OddsActorSimulator(RepIndex repIndex, SimCommon common, DsSimCommon dsSimCommon, SimDb db, SimulatorConfig simulatorConfig, HttpServletResponse response, String repositoryUniqueId) {
		super(common, dsSimCommon);
		this.repIndex = repIndex;
		this.db = db;
		this.response = response;
		this.repositoryUniqueId = repositoryUniqueId;
		setSimulatorConfig(simulatorConfig);
	}

	public OddsActorSimulator(DsSimCommon dsSimCommon, SimulatorConfig simulatorConfig) {
		super(dsSimCommon.simCommon, dsSimCommon);
		this.repIndex = dsSimCommon.repIndex;
		this.db = dsSimCommon.simCommon.db;
		this.response = dsSimCommon.simCommon.response;
        setSimulatorConfig(simulatorConfig);
		init();
	}

	public OddsActorSimulator() {}

	public void init() {
		SimulatorConfigElement configEle = getSimulatorConfig().get("repositoryUniqueId");
		if (configEle != null)   // happens when used to implement a Document Recipient
			this.repositoryUniqueId = configEle.asString();
	}

    @Override
	public boolean run(TransactionType transactionType, MessageValidatorEngine mvc, String validation) throws IOException {
		GwtErrorRecorderBuilder gerb = new GwtErrorRecorderBuilder();

		logger.debug("ODDS beginning to process: " + transactionType);

		if (transactionType.equals(TransactionType.ODDS_RETRIEVE)) {

			common.vc.isRet = true;
			common.vc.xds_b = true;
			common.vc.isRequest = true;
			common.vc.hasHttp = true;
			common.vc.hasSoap = true;

			if (!dsSimCommon.runInitialValidationsAndFaultIfNecessary())
				return false;

			if (mvc.hasErrors()) {
				dsSimCommon.sendErrorsInRegistryResponse(er);
				return false;
			}

			SoapMessageValidator smv = (SoapMessageValidator) common.getMessageValidatorIfAvailable(SoapMessageValidator.class);
			if (smv == null) {
				er.err(Code.XDSRepositoryError, "Internal Error: cannot find SoapMessageValidator.class", "od.rep.RepositoryActorSimulator.java", null);
				return false;
			}
			OMElement retrieveRequest = smv.getMessageBody();

			List<String> docUids = new ArrayList<String>();
			for (OMElement uidEle : XmlUtil.decendentsWithLocalName(retrieveRequest, "DocumentUniqueId")) {
				String uid = uidEle.getText();
				docUids.add(uid);
			}


			RegistryResponseGeneratingSim dms = null;

//			if (persistenceOptn) {
				dms = new RetrieveOnDemandDocumentResponseSim(common.vc, docUids, common, dsSimCommon, repositoryUniqueId, getSimulatorConfig());
//			} else {
//				dms = new RetrieveDocumentResponseSim(common.vc, docUids, common, dsSimCommon, repositoryUniqueId);
//			}

			mvc.addMessageValidator("Generate DocumentResponse", (AbstractMessageValidator)dms, gerb.buildNewErrorRecorder());

			mvc.run();

			// generate special retrieve response message
			Response resp = dms.getResponse();
			// add in any errors collected
			try {
				RegistryErrorListGenerator relg = dsSimCommon.getRegistryErrorList();
				resp.add(relg, null);
			} catch (Exception e) {}

			// wrap in soap wrapper and http wrapper
			// auto-detects need for multipart/MTOM
			mvc.addMessageValidator("ResponseInSoapWrapper", new SoapWrapperRegistryResponseSim(common, dsSimCommon, dms), gerb.buildNewErrorRecorder());

			mvc.run();



			return true;
		}
		else {
			dsSimCommon.sendFault("Od-RepositoryActorSimulator: Don't understand transaction " + transactionType, null);
			return false;
		}
	}

	static public SimulatorStats getSimulatorStats(SimId simId) throws IOException, NoSimException {
		RepIndex repIndex = SimServlet.getRepIndex(simId);
		return repIndex.getSimulatorStats();
	}


}
