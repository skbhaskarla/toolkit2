package gov.nist.toolkit.xdstools2.client.tabs.GatewayTestsTabs;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.ui.Panel;
import gov.nist.toolkit.actorfactory.SimulatorProperties;
import gov.nist.toolkit.actorfactory.client.SimulatorConfig;
import gov.nist.toolkit.actortransaction.client.ActorType;
import gov.nist.toolkit.actortransaction.client.TransactionType;
import gov.nist.toolkit.results.client.Result;
import gov.nist.toolkit.results.client.SiteSpec;
import gov.nist.toolkit.results.client.TestInstance;
import gov.nist.toolkit.sitemanagement.client.TransactionOfferings;
import gov.nist.toolkit.xdstools2.client.*;
import gov.nist.toolkit.xdstools2.client.inspector.MetadataInspectorTab;
import gov.nist.toolkit.xdstools2.client.siteActorManagers.GetDocumentsSiteActorManager;
import gov.nist.toolkit.xdstools2.client.tabs.genericQueryTab.GenericQueryTab;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RGTestTab extends GenericQueryTab implements GatewayTool {
    final protected ToolkitServiceAsync toolkitService = GWT
            .create(ToolkitService.class);

    static CoupledTransactions couplings = new CoupledTransactions();

    //    TextBox patientIdBox = new TextBox();
    String selectedActor = ActorType.RESPONDING_GATEWAY.getShortName();
    List<SimulatorConfig> rgConfigs;
    GenericQueryTab genericQueryTab;
    static final String COLLECTION_NAME =  "igtool1rg";
    TestSelectionManager testSelectionManager;
    Panel siteSelectionPanel = new VerticalPanel();

    String systemTypeGroup = "System Type Group";
    RadioButton exposed = new RadioButton(systemTypeGroup, "Exposed Registry/Repository");
    RadioButton external = new RadioButton(systemTypeGroup, "External Registry/Repository");
    boolean usingExposedRR() { return exposed.getValue(); }

    public RGTestTab() {
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

        // customization of GenericQueryTab
        autoAddRunnerButtons = false;  // want them in a different place
        genericQueryTitle = "Select System Under Test";
        genericQueryInstructions = new HTML(
                "<p>When the test is run a Cross Gateway Query or Retrieve transaction will be sent to the " +
                        "Responding Gateway " +
                        "selected below. This will start the test. Before running a test, make sure your " +
                        "Responding Gateway is configured to forward requests to the Document Repository and Document Registry above.  This " +
                        "test only uses non-TLS endpoints (for now). TLS selection is disabled.</p>"
        );
        addResultsPanel = false;  // manually done below




        ////////////////////////////////////////////////////////////////////////////////////////////////
        topPanel.add(new HTML("<h1>Responding Gateway Test Tool</h1>"));

        topPanel.add(new HTML(
                "<h2>Create supporting test session</h2>" +
                        "These simulators and " +
                        "their logs will be maintained in a test session you create for this test. At the top of the window, " +
                        "create a new test session and select it. " +
                        "All context for this test is kept within this test session - if multiple test sessions are " +
                        "created they do not interact." +
                        "Don't use a double underscore (__) in the name (bug)." +
                        "WARNING - This tool deletes all logs and simulators in the selected test session when the test environment is built.  " +
                        "</p>"
        ));

        topPanel.add(new HTML("<hr /><h2>System under test</h2>" +
                "<p>" +
                "The system under test is a Responding Gateway. To be testable by this tool one of the following " +
                "configurations must be supported by your implementation. " +
                "<ul>" +
                "<li>Exposed Registry/Repository endpoints - your implementation includes Registry/Repository " +
                "functionality and you expose the required endpoints for these actors." +
                "A single site (system configuration in toolkit) must contain the Responding Gateway " +
                "(system under test), and the related Registry and Repository configurations." +
                "<li>External Registry/Repository - your implementation can be configured to work with an " +
                "external Registry and Repository which will be selected below." +
                "</ul>"));

        Panel systemTypePanel = new HorizontalPanel();
        systemTypePanel.add(exposed);
        systemTypePanel.add(external);
        Panel editExposedSystemConfigPanel = new HorizontalPanel();
        systemTypePanel.add(editExposedSystemConfigPanel);
        final Button editExposedSiteButton = new Button("Edit Site Configuration");
        editExposedSystemConfigPanel.add(editExposedSiteButton);
        editExposedSiteButton.setVisible(false);
        editExposedSiteButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {

            }
        });

//        final Panel externalSystemSelectionPanel = new VerticalPanel();
//        externalSystemSelectionPanel.setVisible(false);  // until needed
//        topPanel.add(externalSystemSelectionPanel);
//
//        new TransactionOfferingsLoader(toolkitService).run(new ServiceCallCompletionHandler<TransactionOfferings>() {
//            @Override
//            public void onCompletion(TransactionOfferings to) {
//                externalSystemSelectionPanel.add(new HTML("<h3>Registry/Repository Selection</h3>"));
//                final List<TransactionType> transactionTypes = new ArrayList<TransactionType>();
//                transactionTypes.add(TransactionType.PROVIDE_AND_REGISTER);
//                transactionTypes.add(TransactionType.STORED_QUERY);
//                transactionTypes.add(TransactionType.RETRIEVE);
//                externalSystemSelectionPanel.add(new SiteSelectionWidget(to, transactionTypes, new CoupledTransactions(), getCurrentTestSession()).build(null, ""));
//            }
//        });


        // exposed means using reg/rep from same site as RG
        exposed.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
            @Override
            public void onValueChange(ValueChangeEvent<Boolean> valueChangeEvent) {
                editExposedSiteButton.setVisible(true);
            }
        });

        // external means using reg/rep from different site
        external.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
            @Override
            public void onValueChange(ValueChangeEvent<Boolean> valueChangeEvent) {
                editExposedSiteButton.setVisible(false);
            }
        });

        topPanel.add(systemTypePanel);

        ////////////////////////////////////////////////////////////////////////////////////////////////
        topPanel.add(new HTML(
                "<hr />" +
                        "<h2>Build Test Environment</h2>" +
                        "<p>" +
                        "This will delete the contents of the selected test session and initialize it. " +
                        "The Build Test Environment button will create the necessary simulators to test your Responding Gateway.  " +
                        "The Build Demonstration Environment button will do the same and also build a Responding Gateway for " +
                        "demonstration and training purposes. Only one can be used." +
                        "To test your Responding Gateway you should use Build Test Environment." +
                        "The generated test environment will be displayed below. " +
                        "Once the test environment is built, configure your Responding Gateway to forward requests " +
                        "to the two generated Responding Gateway simulators. The Demonstration Environment builds this " +
                        "configuration automatically." +
                        "</p>"
        ));

        HorizontalPanel testEnvironmentsPanel = new HorizontalPanel();
        topPanel.add(testEnvironmentsPanel);

//        new BuildTestOrchestrationButton(testEnvironmentsPanel, "Build Test Environment", false);
//
//        new BuildTestOrchestrationButton(testEnvironmentsPanel, "Build Demonstration Environment", true);

        ////////////////////////////////////////////////////////////////////////////////////////////////
        topPanel.add(new HTML("<hr />"));

        topPanel.add(siteSelectionPanel);

        new SiteTransactionConfigLoader(toolkitService).load(new SiteDisplayer());


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

        TestSelectionManager testSelectionManager = new TestSelectionManager(this);

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

    void buildExternalRegistryRepository() {

    }

    class SiteDisplayer implements CompletionHandler<TransactionOfferings> {

        @Override
        public void OnCompletion(TransactionOfferings transactionOfferings) {
            siteSelectionPanel.add(new HTML("<h2>Responding Gateway support actors</h2>" +
                    "The Responding Gateway (system under test) must be supported by a Document Repository and Document Registry " +
                    "that are publicly accessible so that test data can be loaded into them. Select the Repository and Registry " +
                    "supporting your Responding Gateway. If they are not on this selection list then update toolkit adding " +
                    "their configurations."
            ));



            siteSelectionPanel.add(new HTML("<h3>Repository Selection</h3>"));
            List<TransactionType> transactionTypes1 = new ArrayList<TransactionType>();
            transactionTypes1.add(TransactionType.PROVIDE_AND_REGISTER);
            siteSelectionPanel.add(new SiteSelectionWidget(transactionOfferings, transactionTypes1, couplings, getCurrentTestSession()).build(null, ""));


            siteSelectionPanel.add(new HTML("<h3>Registry Selection</h3>"));
            List<TransactionType> transactionTypes2 = new ArrayList<TransactionType>();
            transactionTypes2.add(TransactionType.REGISTER);
            siteSelectionPanel.add(new SiteSelectionWidget(transactionOfferings, transactionTypes2, couplings, getCurrentTestSession()).build(null, ""));
        }
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

            List<String> selectedSections = testSelectionManager.getSelectedSections();

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
            toolkitService.runMesaTest(getCurrentTestSession(), getSiteSelection(), new TestInstance(testToRun), selectedSections, parms, true, queryCallback);
        }

    }

    Button addTestEnvironmentInspectorButton(final String siteName) {
        Button button = new Button("Inspect Test Data - " + siteName);
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
