package gov.nist.toolkit.simulators.sim.reg;

import gov.nist.toolkit.actorfactory.PatientIdentityFeedServlet;
import gov.nist.toolkit.configDatatypes.SimulatorProperties;
import gov.nist.toolkit.actorfactory.client.NoSimException;
import gov.nist.toolkit.actorfactory.client.SimId;
import gov.nist.toolkit.actorfactory.client.SimulatorConfig;
import gov.nist.toolkit.actorfactory.client.SimulatorStats;
import gov.nist.toolkit.configDatatypes.client.TransactionType;
import gov.nist.toolkit.simcommon.client.config.SimulatorConfigElement;
import gov.nist.toolkit.simulators.servlet.SimServlet;
import gov.nist.toolkit.simulators.sim.reg.mu.MuSim;
import gov.nist.toolkit.simulators.sim.reg.sq.SqSim;
import gov.nist.toolkit.simulators.sim.reg.store.Committer;
import gov.nist.toolkit.simulators.sim.reg.store.MetadataCollection;
import gov.nist.toolkit.simulators.sim.reg.store.RegIndex;
import gov.nist.toolkit.simulators.support.BaseDsActorSimulator;
import gov.nist.toolkit.simulators.support.DsSimCommon;
import gov.nist.toolkit.simulators.support.SimCommon;
import gov.nist.toolkit.valsupport.client.ValidationContext;
import gov.nist.toolkit.valsupport.engine.MessageValidatorEngine;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RegistryActorSimulator extends BaseDsActorSimulator {
	static Logger logger = Logger.getLogger(RegistryActorSimulator.class);
	boolean updateEnabled;
	private boolean generateResponse = true;

	static List<TransactionType> transactions = new ArrayList<>();

	static {
		transactions.add(TransactionType.REGISTER);
		transactions.add(TransactionType.REGISTER_ODDE);
		transactions.add(TransactionType.STORED_QUERY);
		transactions.add(TransactionType.UPDATE);
	}

	public boolean supports(TransactionType transactionType) {
		return transactions.contains(transactionType);
	}

	public RegistryActorSimulator() {}

	public boolean isPartOfRecipient() {
		SimulatorConfigElement sce = getSimulatorConfig().get(SimulatorProperties.PART_OF_RECIPIENT);
		return sce != null && sce.asBoolean();
	}

	public boolean isValidateCodes() {
		SimulatorConfigElement sce = getSimulatorConfig().get(SimulatorProperties.VALIDATE_CODES);
		return sce != null && sce.asBoolean();
	}

	public boolean validateAgainstPatientIdentityFeed() {
		SimulatorConfigElement sce = getSimulatorConfig().get(SimulatorProperties.VALIDATE_AGAINST_PATIENT_IDENTITY_FEED);
		return sce != null && sce.asBoolean();
	}

	// this constructor must be used when running simulator
	public RegistryActorSimulator(DsSimCommon dsSimCommon, SimulatorConfig simulatorConfig) {
		super(dsSimCommon.simCommon, dsSimCommon);
		this.db = dsSimCommon.simCommon.db;
		this.response = dsSimCommon.simCommon.response;
		this.setSimulatorConfig(simulatorConfig);
		init();
	}

	public void init() {
		SimulatorConfigElement updateConfig = getSimulatorConfig().get(SimulatorProperties.UPDATE_METADATA_OPTION);
		if (updateConfig != null)
			updateEnabled = updateConfig.asBoolean();
	}

	// This constructor can be used to implement calls to onCreate(), onDelete(),
	// onServiceStart(), onServiceStop()
	public RegistryActorSimulator(SimulatorConfig simulatorConfig) {
		setSimulatorConfig(simulatorConfig);
	}

	public boolean run(TransactionType transactionType, MessageValidatorEngine mvc, String validation) throws IOException {
		AdhocQueryResponseGenerator queryResponseGenerator;
		RegistryResponseGeneratorSim registryResponseGenerator;

        logger.info(getSimulatorConfig());
		
		common.getValidationContext().updateEnabled = updateEnabled;
		
		if (transactionType.equals(TransactionType.REGISTER)) {
			common.vc.isR = true;

			common.vc.isPartOfRecipient = isPartOfRecipient();   // part of implementation of Document Recipient
			common.vc.isValidateCodes = isValidateCodes();
			common.vc.validateAgainstPatientIdentityFeed = validateAgainstPatientIdentityFeed();
			common.vc.xds_b = true;
			common.vc.isRequest = true;
			common.vc.hasHttp = true;
			common.vc.hasSoap = true;

            // *********************************************
            // Metadata is validated by the class MetadataMessageValidator
            //
            //  eventually.
            //
            // *********************************************

			if (!dsSimCommon.runInitialValidationsAndFaultIfNecessary())
				return false;  // returns if SOAP Fault was generated
			
			if (mvc.hasErrors()) {
				if (generateResponse)
				dsSimCommon.sendErrorsInRegistryResponse(er);
				return false;
			}

			
			RegRSim rsim = new RegRSim(common, dsSimCommon, getSimulatorConfig());
			mvc.addMessageValidator("Register Transaction", rsim, er);

			registryResponseGenerator = new RegistryResponseGeneratorSim(common, dsSimCommon);
			mvc.addMessageValidator("Attach Errors", registryResponseGenerator, er);

			mvc.run();

			if (generateResponse) {
				// wrap in soap wrapper and http wrapper
				mvc.addMessageValidator("ResponseInSoapWrapper",
						new SoapWrapperRegistryResponseSim(common, dsSimCommon, registryResponseGenerator),
						er);

				// catch up on validators to be run so we can judge whether to commit or not
				mvc.run();
			}

			// commit updates (delta) to registry database
			if (!common.hasErrors())
				commit(mvc, common, rsim.delta);
			
			return !common.hasErrors();

		}
		if (transactionType.equals(TransactionType.REGISTER_ODDE)) { // ITI-61 is an optional implementation of the registry actor
			System.out.flush();

			common.vc.isRODDE = true;

			common.vc.isPartOfRecipient = isPartOfRecipient();   // part of implementation of Document Recipient
			common.vc.isValidateCodes = isValidateCodes();
			common.vc.validateAgainstPatientIdentityFeed = validateAgainstPatientIdentityFeed();
			common.vc.xds_b = true;
			common.vc.isRequest = true;
			common.vc.hasHttp = true;
			common.vc.hasSoap = true;

			// *********************************************
			// Metadata is validated by the class MetadataMessageValidator
			//
			//  eventually.
			//
			// *********************************************

			if (!dsSimCommon.runInitialValidationsAndFaultIfNecessary())
				return false;  // returns if SOAP Fault was generated

			if (mvc.hasErrors()) {
				dsSimCommon.sendErrorsInRegistryResponse(er);
				return false;
			}


			RegRSim rsim = new RegRSim(common, dsSimCommon, getSimulatorConfig());
			mvc.addMessageValidator("Register Transaction", rsim, er);

			registryResponseGenerator = new RegistryResponseGeneratorSim(common, dsSimCommon);
			mvc.addMessageValidator("Attach Errors", registryResponseGenerator, er);

			mvc.run();

			// wrap in soap wrapper and http wrapper
			mvc.addMessageValidator("ResponseInSoapWrapper",
					new SoapWrapperRegistryResponseSim(common, dsSimCommon, registryResponseGenerator),
					er);

			// catch up on validators to be run so we can judge whether to commit or not
			mvc.run();

			// commit updates (delta) to registry database
			if (!common.hasErrors())
				commit(mvc, common, rsim.delta);

			return !common.hasErrors();


		}
		else if (transactionType.equals(TransactionType.STORED_QUERY)) {

			common.vc.isSQ = true;
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


			SqSim sqsim = new SqSim(common, dsSimCommon);
			mvc.addMessageValidator("SqSim", sqsim, er);

			mvc.run();

			// Add in errors
			queryResponseGenerator = new AdhocQueryResponseGenerator(common, dsSimCommon, sqsim);
			mvc.addMessageValidator("Attach Errors", queryResponseGenerator, er);
			mvc.run();

			// wrap in soap wrapper and http wrapper
			mvc.addMessageValidator("ResponseInSoapWrapper", new SoapWrapperRegistryResponseSim(common, dsSimCommon, queryResponseGenerator), er);

			// this will only run the new validators
			mvc.run();
			
			return true; // no updates anyway

		}
		else if (transactionType.equals(TransactionType.UPDATE)) {
			if (!updateEnabled) {
				dsSimCommon.sendFault("Metadata Update not enabled on this actor ", null);
				return false;
			}
			common.vc.isMU = true;
			common.vc.isRequest = true;

			if (!dsSimCommon.runInitialValidationsAndFaultIfNecessary())
				return false;

			if (mvc.hasErrors()) {
				dsSimCommon.sendErrorsInRegistryResponse(er);
				return false;
			}

			MuSim musim = new MuSim(common, dsSimCommon, getSimulatorConfig());
			mvc.addMessageValidator("MuSim", musim, er);
			
			mvc.run();

			MetadataPatternValidator mpv = new MetadataPatternValidator(common, validation);
			mvc.addMessageValidator("MetadataPatternValidator", mpv, er);
			
			mvc.run();
			

			registryResponseGenerator = new RegistryResponseGeneratorSim(common, dsSimCommon);
			mvc.addMessageValidator("Attach Errors", registryResponseGenerator, er);

			mvc.run();

			// wrap in soap wrapper and http wrapper
			mvc.addMessageValidator("ResponseInSoapWrapper", new SoapWrapperRegistryResponseSim(common, dsSimCommon, registryResponseGenerator), er);

			// run all the queued up validators so we can check for errors
			mvc.run();

			if (!common.hasErrors())
				commit(mvc, common, musim.delta);


			return !common.hasErrors();

		}
		else {
			dsSimCommon.sendFault("RegistryActorSimulator - Don't understand transaction " + transactionType, null);
			return false;
		}


	}

	void commit(MessageValidatorEngine mvc,
			SimCommon common, MetadataCollection delta)
	throws IOException {

		logger.info("Committing:");
		// changes to be committed
		logger.info(delta.subSetCollection.toString());
		logger.info(delta.docEntryCollection.toString());
		logger.info(delta.folCollection.toString());
		logger.info(delta.updatedFolCollection.toString() + " updated");
		logger.info(delta.assocCollection.toString());

		delta.mkDirty();

		synchronized(dsSimCommon.regIndex) {
			Committer com = new Committer(common, delta);

			mvc.addMessageValidator("Commit", com, er);

			mvc.run();

			if (!common.hasErrors()) {
				dsSimCommon.regIndex.save();
			}
		}
	}

	static public SimulatorStats getSimulatorStats(SimId simId) throws IOException, NoSimException {
		RegIndex regIndex = SimServlet.getRegIndex(simId);
		return regIndex.getSimulatorStats();
	}

	public void setValidationContext(ValidationContext vc) {
		common.setValidationContext(vc);
	}

	public boolean isGenerateResponse() {
		return generateResponse;
	}

	public void setGenerateResponse(boolean generateResponse) {
		this.generateResponse = generateResponse;
	}

	@Override
	public void onCreate(SimulatorConfig config) {
		// When registry part of implementation of Document Recipient there is no patient feed necessary
		SimulatorConfigElement pifPortConfigured = config.get(SimulatorProperties.PIF_PORT);
		if (pifPortConfigured != null)
			PatientIdentityFeedServlet.generateListener(config);
	}

	@Override
	public void onDelete(SimulatorConfig config) {
		PatientIdentityFeedServlet.deleteListener(config);
	}

	@Override
	public void onServiceStart(SimulatorConfig config) {
		PatientIdentityFeedServlet.generateListener(config);
	}

	@Override
	public void onServiceStop(SimulatorConfig config) {
		PatientIdentityFeedServlet.deleteListener(config);
	}
}
