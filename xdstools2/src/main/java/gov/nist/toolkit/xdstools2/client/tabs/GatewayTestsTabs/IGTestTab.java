package gov.nist.toolkit.xdstools2.client.tabs.GatewayTestsTabs;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.ui.Panel;
import gov.nist.toolkit.configDatatypes.SimulatorProperties;
import gov.nist.toolkit.actorfactory.client.SimulatorConfig;
import gov.nist.toolkit.actortransaction.client.ActorType;
import gov.nist.toolkit.configDatatypes.client.TransactionType;
import gov.nist.toolkit.results.client.Result;
import gov.nist.toolkit.results.client.SiteSpec;
import gov.nist.toolkit.results.client.TestInstance;
import gov.nist.toolkit.xdstools2.client.*;
import gov.nist.toolkit.xdstools2.client.inspector.MetadataInspectorTab;
import gov.nist.toolkit.xdstools2.client.siteActorManagers.GetDocumentsSiteActorManager;
import gov.nist.toolkit.xdstools2.client.tabs.genericQueryTab.GenericQueryTab;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
Test cases
Query response content from one RG relayed
    no data for patient yields empty set or XDSUnknownPatientId
Query response content from two RGs relayed and merged.
Secondary request (query/retrieve)
    routed only to correct RG (check for shall language)
    contains correct homeCommunityId
OnDemand out of scope
pass all entries received in XC query

Negative tests
    -generate XDSMissingHomeCommunityId error
    -All XDSMissingHomeCommunityId errors generated by the Initiating Gateway shall include, in the context of the
    message, identification of the Responding Gateway that returned the invalid response and the element or elements that were in error.
    - XDSUnknownPatientId error not propagated (per spec)
 */
public class IGTestTab extends GenericQueryTab implements GatewayTool {
    final protected ToolkitServiceAsync toolkitService = GWT
            .create(ToolkitService.class);
    String selectedActor = ActorType.INITIATING_GATEWAY.getShortName();
    List<SimulatorConfig> rgConfigs;
    GenericQueryTab genericQueryTab;
    static final String COLLECTION_NAME =  "igtool1rg";
    final TestSelectionManager testSelectionManager;

    public IGTestTab() {
        super(new GetDocumentsSiteActorManager());
        testSelectionManager = new TestSelectionManager(this);
    }

    @Override
    public ToolkitServiceAsync getToolkitService() { return toolkitService; }

    @Override
    public TabContainer getToolContainer() { return myContainer; }

    public void onTabLoad(TabContainer container, boolean select) {
    }

    public void onTabLoad(TabContainer container, boolean select, String eventName) {
        myContainer = container;
        topPanel = new VerticalPanel();
        genericQueryTab = this;

        container.addTab(topPanel, eventName, select);
        addCloseButton(container,topPanel, null);
        tlsOptionEnabled = false;

        genericQueryTab.reloadTransactionOfferings();

        // customization of GenericQueryTab
        autoAddRunnerButtons = false;  // want them in a different place
        genericQueryTitle = "Select System Under Test";
        genericQueryInstructions = new HTML(
                "<p>When the test is run a Stored Query or Retrieve transaction will be sent to the " +
                        "Initiating Gateway " +
                        "selected below. This will start the test. Before running a test, make sure your " +
                        "Initiating Gateway is configured to send to the Responding Gateways above.  This " +
                        "test only uses non-TLS endpoints (for now). TLS selection is disabled.</p>"
        );
        addResultsPanel = false;  // manually done below




        ////////////////////////////////////////////////////////////////////////////////////////////////
        topPanel.add(new HTML("<h1>Initiating Gateway Test Tool</h1>"));

        topPanel.add(new HTML("<p>" +
                "This tool tests an Initiating Gateway with Affinity Domain option.  The tests are driven by " +
                "a Document Consumer as defined by the Affinity Domain option. The Initiating Gateway " +
                "(System Under Test) will " +
                "be configured to relay requests to two Responding Gateways. This tool supplies the " +
                "Responding Gateways. " +
                "Each of the Responding Gateways is backed by a Registry and a Repository which will be loaded " +
                "with supporting test data. The test data patterns needed for testing are assigned to different " +
                "Patient IDs so that once the Initiating Gateway SUT is configured, different test environments " +
                "can be simulated by targeting different Patient IDs." +
                "</p>" +

                "<h2>Create supporting test session</h2>" +
                "These simulators and " +
                "their logs will be maintained in a test session you create for this test. At the top of the window, " +
                "create a new test session and select it. " +
                "<br /><br />WARNING - All logs and simulators in the selected test session will be deleted in the next step below.  " +
                "</p>"
        ));

        ////////////////////////////////////////////////////////////////////////////////////////////////
        topPanel.add(new HTML(
                "<hr />" +
                "<h2>Build Test Environment</h2>" +
                "<p>" +
                "This will delete the contents of the selected test session and re-initialize it. " +
                "The Build Test Environment button will create the necessary simulators to test your Initiating Gateway:  " +
                        "a Document Consumer to drive the test and two Responding Gateways to service requests from " +
                        "your Initiating Gateway. " +

                        "The generated test configuration will be displayed below. " +
                        "Once the test environment is built, configure your Initiating Gateway to forward requests " +
                        "to the two generated Responding Gateways. "

        ));

        HorizontalPanel testEnvironmentsPanel = new HorizontalPanel();
        topPanel.add(testEnvironmentsPanel);

        new BuildIGTestOrchestrationButton(this, testEnvironmentsPanel, "Build Test Environment", false);

//        new BuildIGTestOrchestrationButton(this, testEnvironmentsPanel, "Build Demonstration Environment", true);

        ////////////////////////////////////////////////////////////////////////////////////////////////
        // Query boilerplate
        ActorType act = ActorType.findActor(selectedActor);

        List<TransactionType> tt = act.getTransactions();

        // has to be before addQueryBoilerplate() which
        // references mainGrid
        mainGrid = new FlexTable();

        queryBoilerplate = addQueryBoilerplate(
                new Runner(),
                tt,
                new CoupledTransactions(),
                false  /* display patient id param */);

        topPanel.add(testSelectionManager.buildTestSelector());

        topPanel.add(testSelectionManager.buildSectionSelector());

        topPanel.add(mainGrid);

        testSelectionManager.loadTestsFromCollection(COLLECTION_NAME);


        ////////////////////////////////////////////////////////////////////////////////////////////////
        topPanel.add(new HTML(
                "<hr />" +
                        "<h2>Run Test</h2>" +
                        "<p>" +
                        "Initiate the test from the Toolkit Document Consumer. After the test is run " +
                        "the Document Consumer's logs can be displayed with Inspect Results." +
                        "</p>"
        ));

        addRunnerButtons(topPanel);

        topPanel.add(resultPanel);
    }

    class Runner implements ClickHandler {

        public void onClick(ClickEvent event) {
            resultPanel.clear();

			if (getCurrentTestSession().isEmpty()) {
				new PopupMessage("Test Session must be selected");
				return;
			}

            if (!verifySiteProvided()) return;

			addStatusBox();
			getGoButton().setEnabled(false);
			getInspectButton().setEnabled(false);

            Map<String, String> parms = new HashMap<>();
            parms.put("$testdata_home$", rgConfigs.get(0).get(SimulatorProperties.homeCommunityId).asString());

            Panel logLaunchButtonPanel = rigForRunning();
            logLaunchButtonPanel.clear();
            logLaunchButtonPanel.add(testSelectionManager.buildLogLauncher(rgConfigs));
            String testToRun = selectedTest;
            if (TestSelectionManager.ALL.equals(testToRun)) {
                testToRun = "tc:" + COLLECTION_NAME;
            }

            TestInstance testInstance = new TestInstance(testToRun);
            testInstance.setUser(getCurrentTestSession());
            toolkitService.runMesaTest(getCurrentTestSession(), getSiteSelection(), new TestInstance(testToRun), testSelectionManager.getSelectedSections(), parms, true, queryCallback);
        }

    }

    Button addTestEnvironmentInspectorButton(final String siteName) {
        return addTestEnvironmentInspectorButton(siteName, "Inspect Test Data - " + siteName);
    }

    Button addTestEnvironmentInspectorButton(final String siteName, String label) {
        Button button = new Button(label);
        button.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                List<TestInstance> tests = new ArrayList<TestInstance>();
                tests.add(new TestInstance("15807"));
                toolkitService.getTestResults(tests, getCurrentTestSession(), new AsyncCallback<Map<String, Result>>() {
                    @Override
                    public void onFailure(Throwable throwable) {
                        new PopupMessage(throwable.getMessage());
                    }

                    @Override
                    public void onSuccess(Map<String, Result> stringResultMap) {
                        Result result = stringResultMap.get("15807");
                        if (result == null) {
                            new PopupMessage("Results not available");
                            return;
                        }
                        SiteSpec siteSpec = new SiteSpec(siteName, ActorType.RESPONDING_GATEWAY, null);

                        MetadataInspectorTab itab = new MetadataInspectorTab();
                        List<Result> results = new ArrayList<Result>();
                        results.add(result);
                        itab.setResults(results);
                        itab.setSiteSpec(siteSpec);
                        itab.setToolkitService(toolkitService);
                        itab.onTabLoad(myContainer, true, null);
                    }
                });
            }
        });
        return button;
    }

    public String getWindowShortName() {
        return "igtests";
    }

}
