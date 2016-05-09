package gov.nist.toolkit.actorfactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import gov.nist.toolkit.actorfactory.client.SimId;
import gov.nist.toolkit.actorfactory.client.Simulator;
import gov.nist.toolkit.actorfactory.client.SimulatorConfig;
import gov.nist.toolkit.actortransaction.client.ActorType;
import gov.nist.toolkit.actortransaction.client.ParamType;
import gov.nist.toolkit.configDatatypes.SimulatorProperties;
import gov.nist.toolkit.configDatatypes.client.PatientErrorMap;
import gov.nist.toolkit.configDatatypes.client.TransactionType;
import gov.nist.toolkit.envSetting.EnvSetting;
import gov.nist.toolkit.sitemanagement.client.Site;
import gov.nist.toolkit.sitemanagement.client.TransactionBean;
import gov.nist.toolkit.sitemanagement.client.TransactionBean.RepositoryType;
import gov.nist.toolkit.xdsexception.EnvironmentNotSelectedException;
import gov.nist.toolkit.xdsexception.NoSessionException;
import gov.nist.toolkit.xdsexception.NoSimulatorException;

public class RGActorFactory extends AbstractActorFactory {
   SimId newID = null;

   static final String homeCommunityIdBase = "urn:oid:1.1.4567334.1.";
   static int homeCommunityIdIncr = 1;

   static String getNewHomeCommunityId() {
      return homeCommunityIdBase + homeCommunityIdIncr++ ;
   }

   static final List <TransactionType> incomingTransactions =
      Arrays.asList(TransactionType.XC_QUERY, TransactionType.XC_RETRIEVE);

   @Override
   protected Simulator buildNew(SimManager simm, SimId newID,
      boolean configureBase)
         throws EnvironmentNotSelectedException, NoSessionException {
      this.newID = newID;
      ActorType actorType = ActorType.RESPONDING_GATEWAY;
      SimulatorConfig sc;
      if (configureBase) sc = configureBaseElements(actorType, newID);
      else sc = new SimulatorConfig();

      SimId simId = sc.getId();

      File codesFile = EnvSetting.getEnvSetting(simm.sessionId).getCodesFile();
      addEditableConfig(sc, SimulatorProperties.codesEnvironment,
         ParamType.SELECTION, codesFile.toString());
      addEditableConfig(sc, SimulatorProperties.homeCommunityId, ParamType.TEXT,
         getNewHomeCommunityId());

      addFixedEndpoint(sc, SimulatorProperties.xcqEndpoint, actorType,
         TransactionType.XC_QUERY, false);
      addFixedEndpoint(sc, SimulatorProperties.xcqTlsEndpoint, actorType,
         TransactionType.XC_QUERY, true);
      addFixedEndpoint(sc, SimulatorProperties.xcrEndpoint, actorType,
         TransactionType.XC_RETRIEVE, false);
      addFixedEndpoint(sc, SimulatorProperties.xcrTlsEndpoint, actorType,
         TransactionType.XC_RETRIEVE, true);
      addFixedEndpoint(sc, SimulatorProperties.xcirEndpoint, actorType,
         TransactionType.XC_RET_IMG_DOC_SET, false);
      addFixedEndpoint(sc, SimulatorProperties.xcirTlsEndpoint, actorType,
         TransactionType.XC_RET_IMG_DOC_SET, true);
      addEditableConfig(sc, SimulatorProperties.errors, ParamType.SELECTION,
         new ArrayList <String>(), false);
      addEditableConfig(sc, SimulatorProperties.errorForPatient,
         ParamType.SELECTION, new PatientErrorMap());

      // This needs to be grouped with a Document Registry
      SimulatorConfig registryConfig =
         new RegistryActorFactory().buildNew(simm, simId, true).getConfig(0); // was
                                                                              // false

      // This needs to be grouped with a Document Repository also
      SimulatorConfig repositoryConfig =
         new RepositoryActorFactory().buildNew(simm, simId, true).getConfig(0); // was
                                                                                // false
      
      SimulatorConfig idsConfig = 
         new ImagingDocSourceActorFactory().buildNew(simm, simId, true).getConfig(0);

      sc.add(registryConfig); // this adds the individual
                              // SimulatorConfigElements to the RG
                              // SimulatorConfig
      // their identity as belonging to the Registry or Repository is lost
      // which means the SimServlet cannot find them when a message comes in
      sc.add(repositoryConfig);
      
      sc.add(idsConfig);

      return new Simulator(sc);
   }

   @Override
   protected void verifyActorConfigurationOptions(SimulatorConfig sc) {

   }

   @Override
   public Site getActorSite(SimulatorConfig sc, Site site)
      throws NoSimulatorException {

      if (sc == null || sc.isExpired())
         throw new NoSimulatorException("Expired");

      try {
         String siteName = sc.getDefaultName();

         if (site == null) site = new Site(siteName);
         site.user = sc.getId().user; // labels this site as coming from a sim

         boolean isAsync = false;

         site.addTransaction(new TransactionBean(
            TransactionType.XC_QUERY.getCode(), RepositoryType.NONE,
            sc.get(SimulatorProperties.xcqEndpoint).asString(), false,
            isAsync));
         site.addTransaction(new TransactionBean(
            TransactionType.XC_QUERY.getCode(), RepositoryType.NONE,
            sc.get(SimulatorProperties.xcqTlsEndpoint).asString(), true,
            isAsync));

         site.addTransaction(new TransactionBean(
            TransactionType.XC_RETRIEVE.getCode(), RepositoryType.NONE,
            sc.get(SimulatorProperties.xcrEndpoint).asString(), false,
            isAsync));
         site.addTransaction(new TransactionBean(
            TransactionType.XC_RETRIEVE.getCode(), RepositoryType.NONE,
            sc.get(SimulatorProperties.xcrTlsEndpoint).asString(), true,
            isAsync));

         site.setHome(sc.get(SimulatorProperties.homeCommunityId).asString());

         new RegistryActorFactory().getActorSite(sc, site);
         new RepositoryActorFactory().getActorSite(sc, site);

         return site;
      } catch (Throwable t) {
         sc.isExpired(true);
         throw new NoSimulatorException("Not Defined", t);
      }
   }

   @Override
   public List <TransactionType> getIncomingTransactions() {
      List <TransactionType> tt = new ArrayList <TransactionType>();
      tt.addAll(incomingTransactions);
      tt.addAll(new RegistryActorFactory().getIncomingTransactions());
      tt.addAll(new RepositoryActorFactory().getIncomingTransactions());
      return tt;
   }

}
