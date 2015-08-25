package gov.nist.toolkit.xdstools2.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import gov.nist.toolkit.results.client.AssertionResult;
import gov.nist.toolkit.results.client.CodesConfiguration;
import gov.nist.toolkit.results.client.CodesResult;
import gov.nist.toolkit.xdstools2.client.tabs.genericQueryTab.CodeEditButtonSelector;
import gov.nist.toolkit.xdstools2.client.tabs.genericQueryTab.GenericQueryTab;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bill on 8/25/15.
 */
public class CodeFilterBank {
    List<CodeFilter> codeFilters = new ArrayList<>();
    public CodesConfiguration codesConfiguration = null;
    public int codeBoxSize = 2;

    ToolkitServiceAsync toolkitService;
    GenericQueryTab genericQueryTab;

    public CodeFilterBank(ToolkitServiceAsync toolkitService, GenericQueryTab genericQueryTab) {
        this.toolkitService = toolkitService;
        this.genericQueryTab = genericQueryTab;
        toolkitService.getCodesConfiguration(loadCodeConfigCallback);
    }

    public void addCodeFilter(CodeFilter codeFilter) {
        codeFilters.add(codeFilter);
    }

    public CodeFilter getCodeFilter(String codeConfigurationName) {
        for (CodeFilter codeFilter : codeFilters) {
            if (codeFilter.codeName.equals(codeConfigurationName)) {
                return codeFilter;
            }
        }
        return null;
    }

    protected AsyncCallback<CodesResult> loadCodeConfigCallback = new AsyncCallback<CodesResult>() {

        public void onFailure(Throwable caught) {
            genericQueryTab.resultPanel.clear();
            genericQueryTab.resultPanel.add(GenericQueryTab.addHTML("<font color=\"#FF0000\">" + "Error running validation: " + caught.getMessage() + "</font>"));
        }

        public void onSuccess(CodesResult result) {
            for (AssertionResult a : result.result.assertions.assertions) {
                if (!a.status) {
                    genericQueryTab.resultPanel.add(GenericQueryTab.addHTML("<font color=\"#FF0000\">" + a.assertion + "</font>"));
                }
            }
            codesConfiguration = result.codesConfiguration;
            for (CodeFilter codeFilter : codeFilters) {
                codeFilter.editButton.addClickHandler(
                        new CodeEditButtonSelector(
                                genericQueryTab,
                                codesConfiguration.getCodeConfiguration(codeFilter.codeName),
                                codeFilter.selectedCodes
                        )

                );
            }
            enableCodeEditButtons();
        }

    };

    void enableCodeEditButtons() {
        for (CodeFilter codeFilter : codeFilters) {
            codeFilter.editButton.setEnabled(true);
        }
    }

}
