package gov.nist.toolkit.testengine.transactions;

import gov.nist.toolkit.commondatatypes.client.MetadataTypes;
import gov.nist.toolkit.registrymetadata.Metadata;
import gov.nist.toolkit.registrymetadata.MetadataParser;
import gov.nist.toolkit.testengine.StepContext;
import gov.nist.toolkit.utilities.xml.Util;
import gov.nist.toolkit.xdsexception.ExceptionUtil;
import gov.nist.toolkit.xdsexception.XdsException;
import gov.nist.toolkit.xdsexception.XdsInternalException;

import java.io.File;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNamespace;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;

public class SqlQueryTransaction extends QueryTransaction {
	OMElement expected_contents = null;
	OMElement metadata = null;

	public SqlQueryTransaction(StepContext s_ctx, OMElement instruction, OMElement instruction_output) {
		super(s_ctx, instruction, instruction_output);
	}

	protected void parseInstruction(OMElement part) throws XdsInternalException {
		String part_name = part.getLocalName();
		if (part_name.equals("MetadataFile")) {
			metadata_filename = testConfig.testplanDir + part.getText();
			testLog.add_name_value(instruction_output, "MetadataFile", metadata_filename);
		} else if (part_name.equals("Metadata")) { 
			metadata_filename = "";
			metadata = part.getFirstElement();
		} else if (part_name.equals("ExpectedContents")) {
			expected_contents = part;
			testLog.add_name_value(instruction_output, "ExpectedContents", part);
		} else if (part_name.equals("UseId")) {
			use_id.add(part);
			testLog.add_name_value(instruction_output, "UseId", part);
		} else
			parseBasicInstruction(part);

	}
	
	public void run(OMElement ignore) throws XdsException {
		String metadata_filename = null;

		xds_version = BasicTransaction.xds_a;

		if (metadata_filename == null && metadata == null)
			throw new XdsInternalException("No MetadataFile element or Metadata element found for QueryTransaction instruction within step " + s_ctx.get("step_id"));


		// input file is read twice. Adding to log and then sending through Axis2 results in
		// no output in the log. Axiom does not seem to have a recursive cloner available.
		// so this is easier for now.
		OMElement metadata_ele = null;
		if (metadata != null)
			metadata_ele = metadata;
		else
			metadata_ele = Util.parse_xml(new File(metadata_filename));

		Metadata m = MetadataParser.noParse(metadata_ele);

		// compile in results of previous steps
		if (use_id.size() > 0)
			compileUseIdLinkage(m, use_id);

		testLog.add_name_value(instruction_output, "InputMetadata", Util.deep_copy(metadata_ele));

		OMNamespace ns = metadata_ele.getNamespace();
		String ns_uri = ns.getNamespaceURI();
		int metadata_type = 0;

		if (ns_uri.equals("urn:oasis:names:tc:ebxml-regrep:xsd:query:3.0")) {
			metadata_type = MetadataTypes.METADATA_TYPE_SQ;
		} else if (ns_uri.equals("urn:oasis:names:tc:ebxml-regrep:query:xsd:2.1")) {
			metadata_type = MetadataTypes.METADATA_TYPE_Q;
		} else {
			throw new XdsInternalException("Don't understand version of metadata (namespace on root element): " + ns_uri);
		}


		// verify input is correct top-level request
		if (! metadata_ele.getLocalName().equals("AdhocQueryRequest")) 
			throw new XdsInternalException("Query Transaction (as coded in testplan step '" + s_ctx.get("step_id") + 
			"') must reference a file containing an AdhocQueryRequest");


		Options options = new Options();
		options.setTo(new EndpointReference(endpoint)); // this sets the location of MyService service
		try {
			ServiceClient serviceClient = new ServiceClient();
			serviceClient.setOptions(options);

			if (System.getenv("XDSHTTP10") != null) {
				System.out.println("Generating HTTP 1.0");

				serviceClient.getOptions().setProperty
				(org.apache.axis2.transport.http.HTTPConstants.HTTP_PROTOCOL_VERSION,
						org.apache.axis2.transport.http.HTTPConstants.HEADER_PROTOCOL_10);

				serviceClient.getOptions().setProperty
				(org.apache.axis2.transport.http.HTTPConstants.CHUNKED,
						Boolean.FALSE);

			}


			OMElement result = null;
			result = serviceClient.sendReceive(metadata_ele);

			testLog.add_name_value(instruction_output, "Result", result);

			validate_registry_response_no_set_status(result, metadata_type);

			if (expected_contents != null ) {
				String errors = validate_assertions(result, metadata_type, expected_contents);

				if (errors.length() > 0) {
					//s_ctx.error(instruction_output, errors);
					s_ctx.set_error(errors);
					failed();
				}
			}

			add_step_status_to_output();


		} catch (AxisFault e) {
			throw new XdsInternalException(ExceptionUtil.exception_details(e));
		}
	}

	@Override
	protected String getRequestAction() {
		// TODO Auto-generated method stub
		return null;
	}

	protected String getBasicTransactionName() {
		return "sql";
	}

}
