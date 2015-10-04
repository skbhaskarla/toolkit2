package gov.nist.toolkit.xdstools2.client.tabs;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import gov.nist.toolkit.actortransaction.client.ActorType;
import gov.nist.toolkit.actortransaction.client.TransactionType;
import gov.nist.toolkit.results.client.TestId;
import gov.nist.toolkit.xdstools2.client.*;
import gov.nist.toolkit.xdstools2.client.siteActorManagers.GetDocumentsSiteActorManager;
import gov.nist.toolkit.xdstools2.client.tabs.genericQueryTab.GenericQueryTab;

import java.util.*;

public class MesaTestTab extends GenericQueryTab {
	final protected ToolkitServiceAsync toolkitService = GWT
	.create(ToolkitService.class);

	final String allSelection = "-- All --";
	final String chooseSelection = "-- Choose --";

	ListBox selectActorList = new ListBox();
	ListBox selectTestList = new ListBox();
	ListBox selectSectionList = new ListBox();
	Button selectSectionViewButton = new Button("View this section's testplan");
	ScrollPanel readmeBox = new ScrollPanel();
	TextBox patientIdBox = new TextBox();
	TextBox altPatientIdBox = new TextBox();
	Map<String, String> actorCollectionMap;  // name => description
	String selectedActor;
	String selectedTest;
	String selectedSection = allSelection;
	Map<String, String> testCollectionMap;  // name => description for selected actor
	List<String> sections = new ArrayList<String>();
	int row = 0;
	HorizontalPanel selectSectionPanel = new HorizontalPanel();
//	TestSessionSelector testSessionSelector;


	public MesaTestTab() {
		super(new GetDocumentsSiteActorManager());
	}
	
	public void onTabLoad(TabContainer container, boolean select) {
	}

	public void onTabLoad(TabContainer container, boolean select, String eventName) {
		myContainer = container;
		topPanel = new VerticalPanel();


		container.addTab(topPanel, eventName, select);
		addCloseButton(container,topPanel, null);
//		testSessionSelector = TestSessionSelector.getInstance(toolkitService, new Panel(menuPanel));

		HTML title = new HTML();
		title.setHTML("<h2>" + eventName + "</h2>");
		topPanel.add(title);
		
//		// test session
//		testSessionSelector = new TestSessionSelector(toolkitService, new Panel(topPanel));
		
		// Actor Selection
		HorizontalPanel selectActorPanel = new HorizontalPanel();
		topPanel.add(selectActorPanel);
		
		HTML selectTestCollectionLabel = new HTML();
		selectTestCollectionLabel.setText("Select Actor Name: ");
		selectActorPanel.add(selectTestCollectionLabel);
		
		selectActorPanel.add(selectActorList);
		loadActorNames();
		selectActorList.addChangeHandler(new ActorSelectionChangeHandler());
		
		// test selection
		HorizontalPanel selectTestPanel = new HorizontalPanel();
		topPanel.add(selectTestPanel);
		
		HTML selectTestLabel = new HTML();
		selectTestLabel.setText("Select Test: ");
		selectTestPanel.add(selectTestLabel);
		
		selectTestPanel.add(selectTestList);
		selectTestList.addChangeHandler(new TestSelectionChangeHandler());
		
		addReadme();

		// section selection
		topPanel.add(selectSectionPanel);
		
		HTML selectSectionLabel = new HTML();
		selectSectionLabel.setText("Select Section: ");
		selectSectionPanel.add(selectSectionLabel);
		
		selectSectionPanel.add(selectSectionList);
		
		selectSectionPanel.add(selectSectionViewButton);
		selectSectionViewButton.addClickHandler(new SelectSectionViewButtonClickHandler());

		
		// Patient ID
		HorizontalPanel patientIdPanel = new HorizontalPanel();
		topPanel.add(patientIdPanel);
		
//		HTML patientIdLabel = new HTML();
//		patientIdLabel.setText("Patient ID");
//		patientIdPanel.add(patientIdLabel);
//
//		patientIdBox.setWidth("400px");
//		patientIdPanel.add(patientIdBox);
	
		// Alt Patient ID
		HorizontalPanel altPatientIdPanel = new HorizontalPanel();
//		topPanel.add(altPatientIdPanel);
		
		HTML altPatientIdLabel = new HTML();
		altPatientIdLabel.setText("Alternate Patient ID");
		altPatientIdPanel.add(altPatientIdLabel);

		altPatientIdBox.setWidth("400px");
		altPatientIdPanel.add(altPatientIdBox);
	
		mainGrid = new FlexTable();
		
		topPanel.add(mainGrid);


	}

	class Runner implements ClickHandler {

		public void onClick(ClickEvent event) {
			resultPanel.clear();

//			if (!getCurrentTestSession().isEmpty()) {
//				new PopupMessage("Test Session must be selected");
//				return;
//			}

			if (!verifySiteProvided()) return;
			if (!verifyPidProvided()) return;

//			SiteSpec siteSpec = queryBoilerplate.getSiteSelection();
//			if (siteSpec == null) {
//				new PopupMessage("Site must be selected");
//				return;
//			}

			if (selectedTest == null) {
				new PopupMessage("Test must be selected");
				return;
			}

//			addStatusBox();
//			getGoButton().setEnabled(false);
//			getInspectButton().setEnabled(false);

			List<String> selectedSections = new ArrayList<String>();
			if (selectedSection.equals(allSelection)) {
				selectedSections.addAll(sections);
			} else
				selectedSections.add(selectedSection);

			Map<String, String> parms = new HashMap<>();
			parms.put("$patientid$", pidTextBox.getValue().trim());

//			String pid = patientIdBox.getText();
//			if (pid != null && !pid.equals("")) {
//				pid = pid.trim();
//				parms.put("$patientid$", pid);
//			}

			String altPid = altPatientIdBox.getText();
			if (altPid != null && !altPid.equals("")) {
				altPid = altPid.trim();
				parms.put("$altpatientid$", altPid);
			}

			rigForRunning();
			toolkitService.runMesaTest(getCurrentTestSession(), getSiteSelection(), new TestId(selectedTest), selectedSections, parms, true, queryCallback);

		}

	}



	void addReadme() {
		HTML readmeBefore = new HTML();
		readmeBefore.setHTML("<hr />");
		topPanel.add(readmeBefore);
		
		// readme box
		
		readmeBox.setSize("600px", "200px");
		topPanel.add(readmeBox);
		
		HTML readmeAfter = new HTML();
		readmeAfter.setHTML("<hr />");
		topPanel.add(readmeAfter);
	}
	
	class SelectSectionViewButtonClickHandler implements ClickHandler {

		public void onClick(ClickEvent event) {
			toolkitService.getTestplanAsText(new TestId(selectedTest), selectedSection, new AsyncCallback<String>() {

				public void onFailure(Throwable caught) {
					new PopupMessage("getTestplanAsText: " + caught.getMessage());
				}

				public void onSuccess(String result) {
					new TextViewerTab().onTabLoad(myContainer, true, result, selectedTest + "#" + selectedSection);
				}
				
			});
		}
		
	}
	
	void loadSectionNames() {
		toolkitService.getTestIndex(selectedTest, new AsyncCallback<List<String>>() {

			public void onFailure(Throwable caught) {
				new PopupMessage("getTestIndex: " + caught.getMessage());
			}

			public void onSuccess(List<String> result) {
				sections.clear();
				if (result == null) {
					// no index.idx - so sections
					selectSectionPanel.setVisible(false);
				} else {
					sections.addAll(result);
					selectSectionList.clear();
					selectSectionList.addItem(allSelection, allSelection);
					for (String section : result) {
						selectSectionList.addItem(section, section);
					}
					selectSectionList.addChangeHandler(new SectionSelectionChangeHandler());
					selectSectionPanel.setVisible(true);
				}
			}

		});
	}
	
	class SectionSelectionChangeHandler implements ChangeHandler {

		public void onChange(ChangeEvent event) {
			int i = selectSectionList.getSelectedIndex();
			selectedSection  = selectSectionList.getValue(i);
			if ("".equals(selectedSection))
				return;
		}
		
	}
	
	class TestSelectionChangeHandler implements ChangeHandler {

		public void onChange(ChangeEvent event) {
			int selectedI = selectTestList.getSelectedIndex();
			selectedTest = selectTestList.getValue(selectedI);
			if ("".equals(selectedTest))
				return;
			loadTestReadme();
			loadSectionNames();
			selectedSection = allSelection;		
		}
		
	}
	
	void loadTestReadme() {
		toolkitService.getTestReadme(selectedTest, new AsyncCallback<String>() {

			public void onFailure(Throwable caught) {
				new PopupMessage("getTestReadme: " + caught.getMessage());
			}

			public void onSuccess(String result) {
				Widget w = readmeBox.getWidget();
				if (w != null)
					readmeBox.remove(w);
				readmeBox.add(htmlize("README", result));
			}
			
		});
	}
	
	boolean isFilled(String x) {
		return x != null && !x.equals("");
	}
	
	boolean isRunable() {
		return isFilled(selectedActor) &&
				isFilled(selectedTest) &&
				isFilled(patientIdBox.getText()); 
				
	}
	
	class ActorSelectionChangeHandler implements ChangeHandler {

		public void onChange(ChangeEvent event) {
			int selectedI = selectActorList.getSelectedIndex();
			selectedActor = selectActorList.getValue(selectedI);
			if (selectedActor == null)
				return;
			if ("".equals(selectedActor))
				return;
			String sel = selectedActor;
			loadTestsForActor();
			
			readmeBox.clear();
			
			// these names are found in war/toolkit/testkit/actorcollections/xxxx.tc
			
			// list all sites

			ActorType act = ActorType.findActor(sel);
			if (act == null)
				return;
			
			List<TransactionType> tt = act.getTransactions();
			
			
			queryBoilerplate = addQueryBoilerplate( 
					new Runner(), 
					tt,
					new CoupledTransactions(),
					true);

		}

		
	}
	
	void loadTestsForActor() {
		toolkitService.getCollection("actorcollections", selectedActor, new AsyncCallback<Map<String, String>>() {

			public void onFailure(Throwable caught) {
				new PopupMessage("getCollection(actorcollections): " + selectedActor + " -----  " + caught.getMessage());
			}

			public void onSuccess(Map<String, String> result) {
				testCollectionMap = result;
				selectTestList.clear();
				selectTestList.addItem(chooseSelection, "");

				Set<String> testNumsSet = testCollectionMap.keySet();
				List<String> testNums = new ArrayList<String>();
				testNums.addAll(testNumsSet);
				testNums = new StringSort().sort(testNums);
				
				for (String name : testNums) {
					String description = testCollectionMap.get(name);
					selectTestList.addItem(name + " - " + description, name);
				}
}
		});
	}
	

	void loadActorNames() {
		toolkitService.getCollectionNames("actorcollections", new AsyncCallback<Map<String, String>>() {

			public void onFailure(Throwable caught) {
				new PopupMessage("getCollectionNames: " + caught.getMessage());
			}

			public void onSuccess(Map<String, String> result) {
				actorCollectionMap = result;
				selectActorList.clear();
				selectActorList.addItem(chooseSelection, "");
				
				for (String name : actorCollectionMap.keySet()) {
					String description = actorCollectionMap.get(name);
					selectActorList.addItem(description, name);
				}
				
				
			}
		});
	}
	

	HTML htmlize(String header, String in) {
		HTML h = new HTML(
				"<br />" +
				"<b>" + header + "</b><br /><br />" +

				in.replaceAll("<", "&lt;")
				.replaceAll("\t", "&nbsp;&nbsp;&nbsp;")
				.replaceAll(" ", "&nbsp;")
				.replaceAll("\n", "<br />")
		);
		return h;
	}


	public String getWindowShortName() {
		return "mesatest";
	}

}
