package gov.nist.toolkit.xdstools2.client.tabs.GatewayTestsTabs;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import gov.nist.toolkit.actorfactory.SimulatorProperties;
import gov.nist.toolkit.results.client.SiteSpec;
import gov.nist.toolkit.services.client.RawResponse;
import gov.nist.toolkit.services.client.RgOrchestrationRequest;
import gov.nist.toolkit.services.client.RgOrchestrationResponse;
import gov.nist.toolkit.xdstools2.client.PopupMessage;
import gov.nist.toolkit.xdstools2.client.tabs.genericQueryTab.GenericQueryTab;
import gov.nist.toolkit.xdstools2.client.widgets.buttons.ReportableButton;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
class BuildRGTestOrchestrationButton extends ReportableButton {
    private RGTestTab testTab;
    SiteSpec siteUnderTest;
    boolean useExposedRR;
    boolean useSimAsSUT;
    List<ReportableButton> linkedOrchestrationButtons = new ArrayList<>();

    BuildRGTestOrchestrationButton(RGTestTab testTab, Panel topPanel, String label, boolean useSimAsSUT) {
        super(topPanel, label);
        this.testTab = testTab;
        this.useSimAsSUT = useSimAsSUT;
    }

    public void addLinkedOrchestrationButton(ReportableButton orchestrationButton) {
        linkedOrchestrationButtons.add(orchestrationButton);
    }

    public void handleClick(ClickEvent event) {
        if (GenericQueryTab.empty(testTab.getCurrentTestSession())) {
            new PopupMessage("Must select test session first");
            return;
        }

        if (!useSimAsSUT && !testTab.isExposed() && !testTab.isExternal()) {
            new PopupMessage("Must select exposed or external Registry/Repositor");
            return;
        }

        useExposedRR = testTab.usingExposedRR();
        siteUnderTest = testTab.getSiteSelection();

        if (!useSimAsSUT && siteUnderTest == null) {
            new PopupMessage("Select a Responding Gateway to test");
            return;
        }

        // get rid of past reports
        for (ReportableButton b : linkedOrchestrationButtons) {
            b.clean();
        }

        RgOrchestrationRequest request = new RgOrchestrationRequest();
        request.setUserName(testTab.getCurrentTestSession());
//        request.setEnvironmentName(??????);
        request.setSiteUnderTest(siteUnderTest);
        request.setUseExposedRR(useExposedRR);
        request.setUseSimAsSUT(useSimAsSUT);

        testTab.toolkitService.buildRgTestOrchestration(request, new AsyncCallback<RawResponse>() {
            @Override
            public void onFailure(Throwable throwable) {
                handleError(throwable);
            }

            @Override
            public void onSuccess(RawResponse rawResponse) {
                if (handleError(rawResponse, RgOrchestrationResponse.class)) return;
                RgOrchestrationResponse orchResponse = (RgOrchestrationResponse) rawResponse;
                testTab.orch = orchResponse;
                panel().add(new HTML("<h2>Generated Environment</h2>"));
                FlexTable table = new FlexTable();
                panel().add(table);
                if (useSimAsSUT) {
                    simAsSUTReport(table, orchResponse);
                } else {
                    if (useExposedRR) {
                        exposedRRReport(table, orchResponse);
                    } else {
                        HTML instructions = externalRRReport(table, orchResponse);
                        panel().add(instructions);
                    }
                }
            }
        });
    }

    int displayPIDs(FlexTable table, RgOrchestrationResponse response, int row) {
        table.setHTML(row++, 0, "<h3>Patient IDs</h3>");
        table.setText(row, 0, "Single document Patient ID");
        table.setText(row++, 1, response.getOneDocPid().asString());
        table.setText(row, 0, "Two document Patient ID");
        table.setText(row++, 1, response.getTwoDocPid().asString());

        return row;
    }

    void simAsSUTReport(FlexTable table, RgOrchestrationResponse response) {
        int row = 0;

        row = displayPIDs(table, response, row);

        table.setHTML(row++, 0, "<h3>Simulators</h3>");

        table.setWidget(row, 0, new HTML("<h3>System under test</h3>"));
        table.setText(row++, 1, response.getSiteUnderTest().getName());

    }

    void exposedRRReport(FlexTable table, RgOrchestrationResponse response) {
        int row = 0;

        row = displayPIDs(table, response, row);

        table.setHTML(row++, 0, "<h3>Simulators</h3>");

        table.setWidget(row, 0, new HTML("<h3>System under test</h3>"));
        table.setText(row++, 1, response.getSiteUnderTest().getName());

    }

    HTML externalRRReport(FlexTable table, RgOrchestrationResponse response) {
        int row = 0;

        row = displayPIDs(table, response, row);

        table.setHTML(row++, 0, "<h3>Simulators</h3>");

        table.setWidget(row, 0, new HTML("<h3>System under test</h3>"));
        table.setText(row++, 1, response.getSiteUnderTest().getName());

        table.setHTML(row++, 0, "<h3>Supporting Registry/Repository");
        table.setText(row, 0, "Query");
        table.setText(row++, 1, response.getRegrepConfig().getConfigEle(SimulatorProperties.storedQueryEndpoint).asString());
        table.setText(row, 0, "Retrieve");
        table.setText(row++, 1, response.getRegrepConfig().getConfigEle(SimulatorProperties.retrieveEndpoint).asString());

        return new HTML(
                "Configure your Responding Gateway to use the above Registry and Repository."
        );
    }

}
