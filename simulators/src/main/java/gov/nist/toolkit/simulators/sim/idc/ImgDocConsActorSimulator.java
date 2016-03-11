/**
 * 
 */
package gov.nist.toolkit.simulators.sim.idc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.FactoryConfigurationError;

import org.apache.axiom.om.OMElement;
import org.apache.log4j.Logger;

import gov.nist.toolkit.actorfactory.SiteServiceManager;
import gov.nist.toolkit.actortransaction.client.TransactionType;
import gov.nist.toolkit.registrymsg.repository.RetrieveRequestModel;
import gov.nist.toolkit.registrymsg.repository.RetrievedDocumentModel;
import gov.nist.toolkit.registrymsg.repository.RetrievedDocumentsModel;
import gov.nist.toolkit.simulators.support.BaseDsActorSimulator;
import gov.nist.toolkit.sitemanagement.client.Site;
import gov.nist.toolkit.soap.axis2.Soap;
import gov.nist.toolkit.testengine.engine.RetrieveB;
import gov.nist.toolkit.utilities.xml.Util;
import gov.nist.toolkit.valsupport.engine.MessageValidatorEngine;
import gov.nist.toolkit.xdsexception.XdsInternalException;

/**
 * Image Document Consumer Actor Simulator.  PRELIMINARY
 * 
 * @author Ralph Moulton / MIR WUSTL IHE Development Project <a
 * href="mailto:moultonr@mir.wustl.edu">moultonr@mir.wustl.edu</a>
 *
 */
public class ImgDocConsActorSimulator extends BaseDsActorSimulator {
   

   private static final TransactionType type = TransactionType.RET_IMG_DOC_SET;
   
   static private final Logger logger = Logger.getLogger(ImgDocConsActorSimulator.class);
   private List<OMElement> extraSoapHeaderElements = new ArrayList<>();
   
   /** boolean, is transaction to use TLS connection */
   private boolean tls = false;

   /**
    * @return the {@link #tls} value.
    */
   public boolean isTls() {
      return tls;
   }

   /**
    * @param tls the {@link #tls} to set
    */
   public void setTls(boolean tls) {
      this.tls = tls;
   }


    /**
     * This would be used if this were a server sim.  It is useless as a client sim
     * @param transactionType transaction code
     * @param mvc MessageValidatorEngine - execution engine for validators and simulators
     * @param validation name of special validation to be run. Allows simulators to be extended
     * to perform test motivated validations
     * @return
     * @throws IOException
     */
   @Override
   public boolean run(TransactionType transactionType, MessageValidatorEngine mvc, String validation)
      throws IOException {
      return false;
   }
  
   @Override
   public void init() { }
   
   public ImgDocConsActorSimulator() {}
   
   private String retrieveTemplate = 
      "<RetrieveImagingDocumentSetRequest " + 
      "xmlns:iherad=\"urn:ihe:rad:xdsi-b:2009\" " + 
      "xmlns:ihe=\"urn:ihe:iti:xds-b:2007\">";
   
   public RetrievedDocumentsModel retrieve(Site site,
                                           RetrieveRequestModel iModel) throws Exception {

       String endpoint = site.getEndpoint(type, isTls(), false);

      OMElement retrieveRequest = buildRetrieve(iModel);
      
      Soap soap = new Soap();
      for (OMElement ele : extraSoapHeaderElements) {
          soap.addHeader(ele);
      }
      
      OMElement result =  soap.soapCall(retrieveRequest,
         endpoint,
         true, // mtom
         true, // addressing
         true, // SOAP1.2
         type.getRequestAction(),
         type.getResponseAction());
      
      logger.info(result.toStringWithConsume());
      
      return parseResponse(result);
      
   } // EO retrieve method
   
   private String getEndpoint(String id, String user) throws Exception {
      Site site = null;
      if (id == null) throw new Exception("null actor id");
      if (user == null) throw new Exception("null user");
      SiteServiceManager ssm = SiteServiceManager.getSiteServiceManager();
      List<Site> sites = ssm.getAllSites("xdsi01");
      for (Site s : sites) { 
         if (s.getName().equals(id)) {
            site = s;
            break;
         }
      }
      if (site == null) throw new Exception("No such actor, id=" + id + " session=" + user);
      return site.getEndpoint(type, tls, false);
   }
   
   
   
   private OMElement buildRetrieve (RetrieveRequestModel iModel) 
      throws XdsInternalException, FactoryConfigurationError {
      // TODO Build a real one
      return Util.parse_xml(
         
         "<iherad:RetrieveImagingDocumentSetRequest xmlns:iherad=\"urn:ihe:rad:xdsi-b:2009\" xmlns:ihe=\"urn:ihe:iti:xds-b:2007\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" +
         "<iherad:StudyRequest studyInstanceUID=\"1.3.6.1.4.1.21367.201599.1.201602281031046\">" +
            "<iherad:SeriesRequest seriesInstanceUID=\"1.3.6.1.4.1.21367.201599.2.201602281031046\">" +
               "<ihe:DocumentRequest>" +
                  "<ihe:RepositoryUniqueId>1.1.4567332.10.99</ihe:RepositoryUniqueId>" +
                  "<ihe:DocumentUniqueId>1.3.6.1.4.1.21367.201599.3.201602281031046.1</ihe:DocumentUniqueId>" +
               "</ihe:DocumentRequest>" +
            "</iherad:SeriesRequest>" +
         "</iherad:StudyRequest>" +
         "<iherad:TransferSyntaxUIDList>" +
            "<iherad:TransferSyntaxUID>1.2.840.10008.1.2.1</iherad:TransferSyntaxUID>" +
         "</iherad:TransferSyntaxUIDList>" +
      "</iherad:RetrieveImagingDocumentSetRequest>");
         

   }
   
   private RetrievedDocumentsModel parseResponse(OMElement result) throws Exception {
      RetrieveB retb = new RetrieveB(null);
      Map<String, RetrievedDocumentModel> map = retb.parse_rep_response(result).getMap();
      return new RetrievedDocumentsModel(map);
   }

} // EO ImgDocConsActorSimulator class
