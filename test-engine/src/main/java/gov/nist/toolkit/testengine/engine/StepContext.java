package gov.nist.toolkit.testengine.engine;

import gov.nist.toolkit.configDatatypes.client.Pid;
import gov.nist.toolkit.testengine.transactions.BasicTransaction;
import gov.nist.toolkit.testengine.transactions.DsubPublishTransaction;
import gov.nist.toolkit.testengine.transactions.DsubSubscribeTransaction;
import gov.nist.toolkit.testengine.transactions.EchoV2Transaction;
import gov.nist.toolkit.testengine.transactions.EchoV3Transaction;
import gov.nist.toolkit.testengine.transactions.EpsosTransaction;
import gov.nist.toolkit.testengine.transactions.GenericSoap11Transaction;
import gov.nist.toolkit.testengine.transactions.IGQTransaction;
import gov.nist.toolkit.testengine.transactions.ImagingDocSetRetrieveTransaction;
import gov.nist.toolkit.testengine.transactions.MPQTransaction;
import gov.nist.toolkit.testengine.transactions.MockTransaction;
import gov.nist.toolkit.testengine.transactions.MuTransaction;
import gov.nist.toolkit.testengine.transactions.NullTransaction;
import gov.nist.toolkit.testengine.transactions.PatientIdentityFeedTransaction;
import gov.nist.toolkit.testengine.transactions.ProvideAndRegisterTransaction;
import gov.nist.toolkit.testengine.transactions.RegisterODDETransaction;
import gov.nist.toolkit.testengine.transactions.RegisterTransaction;
import gov.nist.toolkit.testengine.transactions.RetrieveTransaction;
import gov.nist.toolkit.testengine.transactions.SimpleTransaction;
import gov.nist.toolkit.testengine.transactions.SocketTransaction;
import gov.nist.toolkit.testengine.transactions.SqlQueryTransaction;
import gov.nist.toolkit.testengine.transactions.StoredQueryTransaction;
import gov.nist.toolkit.testengine.transactions.XCQTransaction;
import gov.nist.toolkit.testengine.transactions.XDRProvideAndRegisterTransaction;
import gov.nist.toolkit.testengine.transactions.XcpdTransaction;
import gov.nist.toolkit.xdsexception.XdsInternalException;
import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;

import javax.xml.namespace.QName;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class StepContext extends BasicContext implements ErrorReportingInterface {
	OMElement output = null;
	OMElement test_step_output = null;
//	boolean expectedstatus = true;
//	TransactionStatus expectedStatus = new TransactionStatus();
	List<TransactionStatus> expectedStatus = new ArrayList<>();
	String expectedErrorMessage = "";
	String expected_error_code = "";
	String stepId;
	boolean useAltPatientId = false;
	TestConfig testConfig;
	TransactionSettings transactionSettings = null;
	
	public void setTransationSettings(TransactionSettings ts) {
		this.transactionSettings = ts;
	}
	
	public TransactionSettings getTransactionSettings() {
		return transactionSettings;
	}
	
	public void setTestConfig(TestConfig config) {
		testConfig = config;
	}
	
	public boolean useAltPatientId() {
		return useAltPatientId;
	}
	
	public String getId() {
		return stepId;
	}
	
	boolean expectFault = false;
	String expectedFaultCode = null;
	
	public boolean expectFault() {
		return expectFault;
	}
	
	public String getExpectedFaultCode() {
		return expectedFaultCode;
	}
	
	boolean status = true;

	@Override
   public String toString() {
		StringBuffer buf = new StringBuffer();
		
		buf
//		.append("XDS Version = ").append(xdsVersionName()).append("\n")
		.append("Expected Status = ").append(Arrays.toString(expectedStatus.toArray())).append("\n")
		.append("Expected Error Message = ").append(expectedErrorMessage).append("\n");

		return buf.toString();
	}

	public void setExpectedStatus(List<TransactionStatus> transactionStatus) {
		this.expectedStatus = transactionStatus;
	}

	public void addExpectedStatus(TransactionStatus transactionStatus) {
		this.expectedStatus.add(transactionStatus);
	}

	public List<TransactionStatus> getExpectedStatus() {
		return expectedStatus;
	}
	
	public StepContext(PlanContext plan) {
		super(plan);
	}
	public void setId(String id) {
		set("step_id", id);
		stepId = id;
	}

	void setStatus(boolean status) {
		this.status = status;
	}

	public void setStatusInOutput(boolean status) {
		this.status = status;
		setStatusInOutput();
	}

	void setStatusInOutput() {
		test_step_output.addAttribute("status", (status) ? "Pass" : "Fail", null);
	}

	void resetStatus() {
		status = true;
	}

	public boolean getStatus()  {
		return status;
	}

    public void addDetail(String name, String value) {
        addDetail(test_step_output, name, value);
    }

	public  void set_error(String msg) throws XdsInternalException {
		setStatus(false);
		error(test_step_output, msg);
	}
	
	public void set_fault(String code, String msg) throws XdsInternalException {
		setStatus(false);
		fault(test_step_output, code, msg);
	}
	
	public void set_fault(AxisFault e) throws XdsInternalException {
//		String code = "";
		String detail = "";
		try {
			//code = e.getFaultCode().getLocalPart();
			detail = e.getCause().toString();
		} catch (Exception ex) {
			
		}
		detail = detail + " : " + e.getMessage();
		setStatus(false);
		fault(test_step_output, detail, detail);
	}

	public void fail(String message) throws XdsInternalException {
		set_error(message);
	}

	public void setInContext(String title, Object value) {
		set(title, value);
	}
	
	public String getExpectedErrorCode() {
		return expected_error_code;
	}
	
	void run(OMElement step, PlanContext plan_context) throws Exception, FileNotFoundException {
		String step_id = null;
		step_id = null;
		String expected_status = null;
		String expected_error_message = null;

		OMAttribute id = step.getAttribute(new QName("id"));
		if (id == null)
			throw new XdsInternalException("Found TestStep without an id attribute");
		step_id = id.getAttributeValue();
		
		testConfig.currentStep = step_id;
		
		setId(step_id);
		System.out.println("\tStep: " + step_id);


		test_step_output = testLog.add_simple_element_with_id(
				plan_context.getResultsDocument(), 
				"TestStep", 
				step_id);

		Iterator elements = step.getChildElements();
		while (elements.hasNext()) {
			OMElement instruction = (OMElement) elements.next();
			String instruction_name = instruction.getLocalName();
			InstructionContext ins_context = new InstructionContext(this);
			//System.out.println("******* " + instruction_name + " ***"); 

			if (instruction_name.equals("ExpectedStatus")) 
			{
				expected_status = instruction.getText();
				testLog.add_name_value(test_step_output, instruction_name, expected_status);

				if (expected_status!=null && !"".equals(expected_status)) {
					String[] statuses = expected_status.split(",");
					for (String status : statuses) {
						addExpectedStatus(new TransactionStatus(status.trim()));
					}
				}

			}
			/*
			Save this code for later when an intrustuction with mulitple values is needed.

			else if (instruction_name.equals("AcceptableStatus"))
			{
				String acceptableStatus;
				Iterator statusElements = instruction.getChildElements();
				OMElement acceptableStatusEle = MetadataSupport.om_factory.createOMElement(new QName("AcceptableStatus"));
				while (statusElements.hasNext()) {
					OMElement statusElement = (OMElement) statusElements.next();
					String localName = "Status";
					if (localName.equals(statusElement.getLocalName())) {
						acceptableStatus = statusElement.getText();

						OMElement status = MetadataSupport.om_factory.createOMElement(localName, null);
						status.setText(acceptableStatus);
						acceptableStatusEle.addChild(status);


						addExpectedStatus(new TransactionStatus(acceptableStatus));
					}
				}
				testLog.add_name_value(test_step_output, instruction_name, acceptableStatusEle);
			} */
			else if (instruction_name.equals("Rule")) 
			{
			} 
			else if (instruction_name.equals("Goal")) 
			{
				String goal = instruction.getText();
				testLog.add_name_value(test_step_output, instruction_name, goal);
			} 
			else if (instruction_name.equals("RegistryEndpoint")) 
			{
				plan_context.defaultRegistryEndpoint = instruction.getText();
				testLog.add_name_value(test_step_output, instruction); 
				plan_context.setRegistryEndpoint(plan_context.defaultRegistryEndpoint);
			} 
			else if (instruction_name.equals("NewPatientId"))  
			{
				Pid pid = PatientIdAllocator.getNew(transactionSettings.patientIdAssigningAuthorityOid);
				testLog.add_name_value(test_step_output, "NewPatientId", pid.toString());
				transactionSettings.patientId = pid.toString();
			}
			else if (instruction_name.equals("AltPatientId"))  
			{
				useAltPatientId = true;
				Pid pid = PatientIdAllocator.getNew(transactionSettings.patientIdAssigningAuthorityOid);
				testLog.add_name_value(test_step_output, "AltPatientId", pid.toString());
				transactionSettings.altPatientId = pid.toString();
			}
			else if (instruction_name.equals("ExpectedErrorMessage")) 
			{
				expected_error_message = instruction.getText();
				testLog.add_name_value(test_step_output, instruction_name, expected_error_message);
				setExpectedErrorMessage(expected_error_message);
			} 
			else if (instruction_name.equals("ExpectedErrorCode")) 
			{
				expected_error_code = instruction.getText();
				testLog.add_name_value(test_step_output, instruction); 
			} 
			else {
				resetStatus();
				OMElement instruction_output = null;
				BasicTransaction transaction = null;
				
				instruction_output = testLog.add_simple_element(test_step_output, instruction_name);
				instruction_output.addAttribute("step", step_id, null);
				
				if (instruction_name.equals("SqlQueryTransaction")) 
				{
					transaction = new SqlQueryTransaction(this, instruction, instruction_output);
				} 
				else if (instruction_name.equals("StoredQueryTransaction")) 
				{
					transaction = new StoredQueryTransaction(this, instruction, instruction_output);
				} 
				else if (instruction_name.equals("GenericSoap11Transaction")) 
				{
					transaction = new GenericSoap11Transaction(this, instruction, instruction_output);
				} 
				else if (instruction_name.equals("DsubSubscribeTransaction")) 
				{
					transaction = new DsubSubscribeTransaction(this, instruction, instruction_output);
				}
				else if (instruction_name.equals("PatientIdentityFeedTransaction"))
				{
					transaction = new PatientIdentityFeedTransaction(this, instruction, instruction_output);
				}
				else if (instruction_name.equals("IGQTransaction"))
				{
					transaction = new IGQTransaction(this, instruction, instruction_output);
				} 
				else if (instruction_name.equals("XCQTransaction")) 
				{
					transaction = new XCQTransaction(this, instruction, instruction_output);
				} 
				else if (instruction_name.equals("EpsosTransaction")) 
				{
					transaction = new EpsosTransaction(this, instruction, instruction_output);
				} 
				else if (instruction_name.equals("MPQTransaction")) 
				{
					transaction = new MPQTransaction(this, instruction, instruction_output);
				} 
				else if (instruction_name.equals("SimpleTransaction")) 
				{
					transaction = new SimpleTransaction(this, instruction, instruction_output);
				} 
				else if (instruction_name.equals("RetrieveTransaction")) 
				{
					transaction = new RetrieveTransaction(this, instruction, instruction_output);
				} 
				else if (instruction_name.equals("NullTransaction")) 
				{
					transaction = new NullTransaction(this, instruction, instruction_output);
				} 
				else if (instruction_name.equals("XCRTransaction"))
				{
					transaction = new RetrieveTransaction(this, instruction, instruction_output);
					((RetrieveTransaction)transaction).setIsXca(true);
				} 
				else if (instruction_name.equals("IGRTransaction"))
				{
					transaction = new RetrieveTransaction(this, instruction, instruction_output);
					((RetrieveTransaction)transaction).setIsXca(true);
					((RetrieveTransaction)transaction).setUseIG(true);
				} 
				else if (instruction_name.equals("RegisterTransaction")) 
				{
					transaction = new RegisterTransaction(this, instruction, instruction_output);
				}
				else if (instruction_name.equals("RegisterODDETransaction"))
				{
					transaction = new RegisterODDETransaction(this, instruction, instruction_output);
				}
				else if (instruction_name.equals("MuTransaction")) 
				{
					transaction = new MuTransaction(this, instruction, instruction_output);
				} 
				else if (instruction_name.equals("PublishTransaction")) 
				{
					transaction = new DsubPublishTransaction(this, instruction, instruction_output);
				} 
				else if (instruction_name.equals("MockTransaction")) 
				{
					transaction = new MockTransaction(this, instruction, instruction_output);
				} 
				else if (instruction_name.equals("ProvideAndRegisterTransaction")) 
				{
					transaction = new ProvideAndRegisterTransaction(this, instruction, instruction_output);
				} 
				else if (instruction_name.equals("XDRProvideAndRegisterTransaction")) 
				{
					transaction = new XDRProvideAndRegisterTransaction(this, instruction, instruction_output);
				} 
				else if (instruction_name.equals("EchoV2Transaction")) 
				{
					transaction = new EchoV2Transaction(this, instruction, instruction_output);
				} 
				else if (instruction_name.equals("EchoV3Transaction")) 
				{
					transaction = new EchoV3Transaction(this, instruction, instruction_output);
				} 
				else if (instruction_name.equals("XcpdTransaction")) 
				{
					transaction = new XcpdTransaction(this, instruction, instruction_output);
				} 
				else if (instruction_name.equals("SocketTransaction")) 
				{
					transaction = new SocketTransaction(this, instruction, instruction_output);
				} 
				else if (instruction_name.equals("RetrieveImagingDocSetTransaction")) 
				{
					transaction = new ImagingDocSetRetrieveTransaction(this, instruction, instruction_output);
				} 
				else 
				{
					dumpContextIntoOutput(test_step_output);
					throw new XdsInternalException(ins_context.error("StepContext: Don't understand instruction named " + instruction_name));
				}

				setTransaction(transaction);
				transaction.setPlanContext(plan_context);
				transaction.setTestConfig(testConfig);
				transaction.setTransactionSettings(transactionSettings);
				if (transactionSettings.transactionTransport != null)
					transactionSettings.transactionTransport.attach(transaction);
				transaction.doRun();				
				
				if (transaction != null && getStatus() /*== false*/) {
					OMElement assertion_output = testLog.add_simple_element(
							test_step_output, 
							"Assertions");
					transaction.runAssertionEngine(instruction_output, this, assertion_output);
				}

				//dumpContextIntoOutput(test_step_output);

				//System.out.println("xdstest2 step status : " + ((this.getStatus()) ? "Pass" : "Fail"));
				System.out.flush();
				setStatusInOutput();
				
				PatientIdAllocator.reset();

			}
		}

	}
	public String getExpectedErrorMessage() {
		String exp = get("ExpectedErrorMessage");
		if (exp == null)
			exp = "";
		return exp;
	}
	public void setExpectedErrorMessage(String expectedErrorMessage) {
		this.expectedErrorMessage = expectedErrorMessage;
		this.set("ExpectedErrorMessage", expectedErrorMessage);
	}
	public BasicTransaction getTransaction() {
		return (BasicTransaction) getObj("transaction");
	}

	public void setTransaction(BasicTransaction transaction) {
		parent_context.set("transaction", transaction);
	}

	public String getRegistryEndpoint() {
		return getRecursive("RegistryEndpoint");
	}


}
