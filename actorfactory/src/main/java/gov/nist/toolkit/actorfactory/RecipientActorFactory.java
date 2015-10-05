package gov.nist.toolkit.actorfactory;

import gov.nist.toolkit.actorfactory.client.SimId;
import gov.nist.toolkit.actorfactory.client.Simulator;
import gov.nist.toolkit.actorfactory.client.SimulatorConfig;
import gov.nist.toolkit.actortransaction.client.ActorType;
import gov.nist.toolkit.actortransaction.client.TransactionType;
import gov.nist.toolkit.sitemanagement.client.Site;
import gov.nist.toolkit.sitemanagement.client.TransactionBean;
import gov.nist.toolkit.sitemanagement.client.TransactionBean.RepositoryType;
import gov.nist.toolkit.xdsexception.EnvironmentNotSelectedException;
import gov.nist.toolkit.xdsexception.NoSessionException;

import java.util.Arrays;
import java.util.List;

public class RecipientActorFactory  extends AbstractActorFactory {

	static final List<TransactionType> incomingTransactions = 
		Arrays.asList(TransactionType.XDR_PROVIDE_AND_REGISTER);


	protected Simulator buildNew(SimManager simm, SimId newID, boolean configureBase) throws EnvironmentNotSelectedException, NoSessionException {
		RegistryActorFactory registryActorFactory;
		RepositoryActorFactory repositoryActorFactory;

		ActorType actorType = ActorType.DOCUMENT_RECIPIENT;
		SimulatorConfig sc;
		if (configureBase)
			sc = configureBaseElements(actorType, newID);
		else 
			sc = new SimulatorConfig();

		SimId simId = sc.getId();
		// This needs to be grouped with a Document Registry
		registryActorFactory = new RegistryActorFactory();
		registryActorFactory.asRecipient();
		SimulatorConfig registryConfig = registryActorFactory.buildNew(simm, simId, true).getConfig(0);

		// This needs to be grouped with a Document Repository also
		repositoryActorFactory = new RepositoryActorFactory();
		repositoryActorFactory.asRecipient();  // behave like Document Recipient
		SimulatorConfig repositoryConfig = repositoryActorFactory.buildNew(simm, simId, true).getConfig(0);

		// two combined simulators do not have separate lives
		sc.add(registryConfig);
		sc.add(repositoryConfig);

		return new Simulator(sc);
	}

	protected void verifyActorConfigurationOptions(SimulatorConfig sc) {

	}

	public Site getActorSite(SimulatorConfig sc, Site site) {
		String siteName = sc.getDefaultName();

		if (site == null)
			site = new Site(siteName);

		site.user = sc.getId().user;  // labels this site as coming from a sim

		boolean isAsync = false;

		site.addTransaction(new TransactionBean(
				TransactionType.XDR_PROVIDE_AND_REGISTER.getCode(),
				RepositoryType.NONE,
				sc.get(pnrEndpoint).asString(), 
				false, 
				isAsync));
		site.addTransaction(new TransactionBean(
				TransactionType.XDR_PROVIDE_AND_REGISTER.getCode(),
				RepositoryType.NONE,
				sc.get(pnrTlsEndpoint).asString(), 
				true, 
				isAsync));

		return site;
	}

	public List<TransactionType> getIncomingTransactions() {
		return incomingTransactions;
	}


}
