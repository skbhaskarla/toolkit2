package gov.nist.toolkit.xdstools2.client.tabs;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import gov.nist.toolkit.results.client.Result;
import gov.nist.toolkit.results.client.TestInstance;
import gov.nist.toolkit.xdstools2.client.PopupMessage;
import gov.nist.toolkit.xdstools2.client.TabContainer;
import gov.nist.toolkit.xdstools2.client.ToolkitService;
import gov.nist.toolkit.xdstools2.client.ToolkitServiceAsync;
import gov.nist.toolkit.xdstools2.client.inspector.MetadataInspectorTab;
import gov.nist.toolkit.xdstools2.client.siteActorManagers.GetDocumentsSiteActorManager;
import gov.nist.toolkit.xdstools2.client.tabs.genericQueryTab.GenericQueryTab;

import java.util.List;

public class TestLogListingTab extends GenericQueryTab {
	final protected ToolkitServiceAsync toolkitService = GWT
	.create(ToolkitService.class);

	FlexTable grid = new FlexTable();
	HorizontalPanel testSessionPanel = new HorizontalPanel();
	ListBox testSessionList = new ListBox();
	String testSession = null;
	boolean isPrivateTesting = false;


	public TestLogListingTab() {
		super(new GetDocumentsSiteActorManager());
	}

	public void onTabLoad(TabContainer container, boolean select, String eventName) {
		myContainer = container;
		topPanel = new VerticalPanel();


		container.addTab(topPanel, "TestLog Listing", select);
		addCloseButton(container,topPanel, null);

		HTML title = new HTML();
		title.setHTML("<h2>TestLog Listing</h2>");
		topPanel.add(title);
		
		// test session
		testSessionPanel.setVisible(false);
		topPanel.add(testSessionPanel);
		
		HTML testSessionLabel = new HTML();
		testSessionLabel.setText("Test Session: ");
		testSessionPanel.add(testSessionLabel);
		
		testSessionPanel.add(testSessionList);
		testSessionList.addChangeHandler(new TestSessionChangeHandler());
		loadTestSessionEnabled();
		
		
		topPanel.add(grid);
		
	}
	
	void loadTestSessionEnabled() {
		toolkitService.isPrivateMesaTesting(new AsyncCallback<Boolean>() {

			public void onFailure(Throwable caught) {
				new PopupMessage("isPrivateMesaTesting: " + caught.getMessage());
			}

			public void onSuccess(Boolean result) {
				isPrivateTesting = result;
				if (!result)
					return;
				loadTestSessionNames();
			}
			
		});
	}
	
	void loadTestSessionNames() {
		toolkitService.getMesaTestSessionNames(new AsyncCallback<List<String>>() {

			public void onFailure(Throwable caught) {
				new PopupMessage("getMesaTestSessionNames: " + caught.getMessage());
			}

			public void onSuccess(List<String> result) {
				testSessionList.clear();
				testSessionList.addItem("-- Choose --", "");
				for (String val : result)
					testSessionList.addItem(val);
				
				testSessionPanel.setVisible(true);
			}
			
		});
	}
	
	
	void loadTestNumbers() {
        grid.clear();
		toolkitService.getTestlogListing(testSession, new AsyncCallback<List<TestInstance>>() {

			public void onFailure(Throwable caught) {
				new PopupMessage("getTestlogListing: " + caught.getMessage());			
			}

			public void onSuccess(List<TestInstance> result) {
				int row=0;
				
				for (TestInstance testInstance : result) {
					HTML h = new HTML();
					h.setText(testInstance.getId());
					grid.setWidget(row, 0, h);
					Button inspect = new Button("Inspect");
					inspect.addClickHandler(new InspectButtonClickHandler(testInstance));
					grid.setWidget(row, 1, inspect);
					
					row++;
				}
			}
			
		});
	}
	
	class InspectButtonClickHandler implements ClickHandler {
		TestInstance testInstance;
		
		InspectButtonClickHandler(TestInstance testInstance) {
			this.testInstance = testInstance;
		}

		public void onClick(ClickEvent event) {
			toolkitService.getLogContent(testSession, testInstance, new AsyncCallback<List<Result>>() {

				public void onFailure(Throwable caught) {
					new PopupMessage("getLogsForTest: " + caught.getMessage());			
				}

				public void onSuccess(List<Result> results) {
					MetadataInspectorTab itab = new MetadataInspectorTab();
					itab.setToolkitService(toolkitService);
					itab.setResults(results);
					itab.setSiteSpec(null);
					itab.onTabLoad(myContainer, true, null);
				}
				
			});
		}
	}

	class TestSessionChangeHandler implements ChangeHandler {

		public void onChange(ChangeEvent event) {
			int selectionI = testSessionList.getSelectedIndex();
			testSession = testSessionList.getItemText(selectionI);
			if ("".equals(testSession))
				testSession = null;

			loadTestNumbers();

		}
		
	}
	

	public String getWindowShortName() {
		return "testloglisting";
	}

}
