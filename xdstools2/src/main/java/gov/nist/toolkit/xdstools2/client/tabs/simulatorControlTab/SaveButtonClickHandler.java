package gov.nist.toolkit.xdstools2.client.tabs.simulatorControlTab;

import gov.nist.toolkit.actorfactory.client.SimulatorConfig;
import gov.nist.toolkit.xdstools2.client.PopupMessage;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import gov.nist.toolkit.xdstools2.client.tabs.TestSessionState;

class SaveButtonClickHandler implements ClickHandler {
	SimulatorControlTab simulatorControlTab;
	SimulatorConfig config;
	TestSessionState testSessionState;
	
	SaveButtonClickHandler(SimulatorControlTab simulatorControlTab, SimulatorConfig config, TestSessionState testSessionState) {
		this.simulatorControlTab = simulatorControlTab;
		this.config = config;
		this.testSessionState = testSessionState;
	}

	public void onClick(ClickEvent event) {
		config.updateDocTypeSelection();
		simulatorControlTab.toolkitService.putSimConfig(config, new AsyncCallback<String>() {

			public void onFailure(Throwable caught) {
				new PopupMessage("saveSimConfig:" + caught.getMessage());
			}

			public void onSuccess(String result) {
				// reload simulators to get updates
				new LoadSimulatorsClickHandler(simulatorControlTab, testSessionState).onClick(null);
				
			}
			
		});
	}
	
}
