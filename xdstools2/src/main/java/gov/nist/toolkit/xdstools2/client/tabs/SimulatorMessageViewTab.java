package gov.nist.toolkit.xdstools2.client.tabs;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.http.client.*;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import gov.nist.toolkit.actorfactory.client.SimId;
import gov.nist.toolkit.actortransaction.client.TransactionInstance;
import gov.nist.toolkit.http.client.HtmlMarkup;
import gov.nist.toolkit.results.client.Result;
import gov.nist.toolkit.results.client.SiteSpec;
import gov.nist.toolkit.xdstools2.client.Panel;
import gov.nist.toolkit.xdstools2.client.*;
import gov.nist.toolkit.xdstools2.client.inspector.MetadataInspectorTab;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimulatorMessageViewTab extends TabbedWindow {
	protected TabContainer myContainer;

	final protected ToolkitServiceAsync toolkitService = GWT
	.create(ToolkitService.class);

	HorizontalPanel simDisplayPanel = new HorizontalPanel();
	VerticalPanel simControlPanel = new VerticalPanel();
	VerticalPanel detailPanel = new VerticalPanel();
	HorizontalPanel inOutPanel = new HorizontalPanel();
	VerticalPanel transInPanel = new VerticalPanel();
	VerticalPanel transOutPanel = new VerticalPanel();
	VerticalPanel logPanel = new VerticalPanel();
	ScrollPanel scrollInPanel = new ScrollPanel();
	ScrollPanel scrollOutPanel = new ScrollPanel();
	ScrollPanel scrollLogPanel = new ScrollPanel();

	SimId simid = null;//new SimId("");
	String currentActor;
	String currentTransaction;
	String currentEvent;

	List<TransactionInstance> transactionInstances = null;
	TransactionInstance currentTransactionInstance = null;

	//	ActorNamesRadioButtonGroup simRadButtons;
	TransactionNamesRadioButtonGroup transactionRadButtons;

	//	HorizontalPanel actorNamesPanel = new HorizontalPanel();
	VerticalPanel transactionDisplayPanel = new VerticalPanel();
	VerticalPanel transactionNamesPanel = new VerticalPanel();
	ListBox transInstanceListBox = new ListBox();
	//	HTML ipAddressHTML = new HTML();
	ListBox simulatorNamesListBox = new ListBox();

	Button refreshButton = new Button("Refresh");
	Button inspectRequestButton = new Button("Inspect Request");
	Button inspectResponseButton = new Button("Inspect Response");
	Button deleteButton = new Button("Delete");
	
	HTML download = new HTML();
	
	public SimId getSimid() { return simid; }

	// If eventName is null then display list of simulators.  If non-null then it is
	// the simulator id. In this case do not allow simulator selection.
	public void onTabLoad(TabContainer container, boolean select, String simIdString) {
		myContainer = container;
		topPanel = new VerticalPanel();

		if (simIdString != null) {
            try {
                simid = new SimId(simIdString);
            } catch (Exception e) {
                new PopupMessage(e.getMessage());
                return;
            }
        }

		container.addTab(topPanel, simIdString + " Logs", select);
		addCloseButton(container, topPanel, null);

		topPanel.add(simDisplayPanel);
		simDisplayPanel.add(simControlPanel);
		simDisplayPanel.add(detailPanel);

		detailPanel.add(inOutPanel);
		detailPanel.add(logPanel);

		scrollInPanel.setWidth("500px");
		scrollInPanel.setHeight("300px");
		transInPanel.setBorderWidth(1);
		transInPanel.add(scrollInPanel);

		scrollOutPanel.setWidth("500px");
		scrollOutPanel.setHeight("300px");
		transOutPanel.setBorderWidth(1);
		transOutPanel.add(scrollOutPanel);

		scrollLogPanel.setWidth("1000px");
		scrollLogPanel.setHeight("300px");
		logPanel.setBorderWidth(1);
		logPanel.add(scrollLogPanel);

		inOutPanel.add(transInPanel);
		inOutPanel.add(transOutPanel);

		simControlPanel.add(HtmlMarkup.html(HtmlMarkup.h2("Transaction Log")));
		simControlPanel.add(HtmlMarkup.html(HtmlMarkup.h2("Simulator:")));

		if (simid == null) {
			simControlPanel.add(simulatorNamesListBox);
		} else {
			simControlPanel.add(new HTML("<h3>" + simid.toString() + "</h3>"));
			loadTransactionNames(simid);
		}
		loadSimulatorNamesListBox();
		simulatorNamesListBox.addChangeHandler(new SimulatorNameChangeHandler());

		//		simControlPanel.add(actorNamesPanel);
		//		loadActorNames();

		simControlPanel.add(transactionDisplayPanel);

		transactionDisplayPanel.add(transactionNamesPanel);

		transactionDisplayPanel.add(HtmlMarkup.html(HtmlMarkup.bold("Messages")));
		transInstanceListBox.setVisibleItemCount(20);
		transactionDisplayPanel.add(transInstanceListBox);

		transInstanceListBox.addChangeHandler(transactionInstanceChoiceChanged);

		refreshButton.addClickHandler(refreshClickHandler);
		transactionDisplayPanel.add(refreshButton);

		inspectRequestButton.addClickHandler(inspectRequestClickHandler);
		transactionDisplayPanel.add(inspectRequestButton);

		inspectResponseButton.addClickHandler(inspectResponseClickHandler);
		transactionDisplayPanel.add(inspectResponseButton);

//		deleteButton.addClickHandler(deleteClickHandler);
//		transactionDisplayPanel.add(deleteButton);
		
		
		transactionDisplayPanel.add(download);
		
		
	}

	class SimulatorNameChangeHandler implements ChangeHandler {

		public void onChange(ChangeEvent event) {
			int selectedI = simulatorNamesListBox.getSelectedIndex();
			String simName = simulatorNamesListBox.getItemText(selectedI);
			if (simName.equals(""))
				return;
			
			String[] parts = simName.split("\\.");
			if (parts.length == 2)
				currentActor = parts[1];
			
			simid = simNameToIdMap.get(simName);
			loadTransactionNames(simid);
		}

	}
	
	Map<String, SimId> simNameToIdMap;

	void loadSimulatorNamesListBox() {
		simulatorNamesListBox.clear();
		toolkitService.getActorSimulatorNameMap(new AsyncCallback<Map<String, SimId>>() {

			public void onFailure(Throwable caught) {
				new PopupMessage("getActorSimulatorNameMap: " + caught.getMessage());
			}

			public void onSuccess(Map<String, SimId> map) {
				simulatorNamesListBox.addItem("");
				simNameToIdMap = map;
				for (String name : map.keySet()) {
					simulatorNamesListBox.addItem(name);
				}
			}

		});
	}

	Map<String, String> fullNameMap= new HashMap<>();

	void loadTransactionNames(SimId simid) {
		simidFinal = simid;
		transInstanceListBox.clear();
		//getSimulatorTransactionNames
		toolkitService.getTransactionsForSimulator(simid, new AsyncCallback<List<String>>() {

			public void onFailure(Throwable caught) {
				new PopupMessage("getSimulatorTransactionNames: " + caught.getMessage());
			}

			public void onSuccess(List<String> transNames) {
				transactionNamesPanel.clear();
//				transactionNamesPanel.add(HtmlMarkup.html(HtmlMarkup.bold("Transaction: ")));
//
//
//				transactionRadButtons = new TransactionNamesRadioButtonGroup(new Panel(transactionNamesPanel), simidFinal);
//				transactionRadButtons.addButton("All");
//				transactionRadButtons.buttons.getRetrievedDocumentsModel(0).setValue(true);
//
//				// translate transNames into full descriptive names
//				List<String> fullNames = new ArrayList<>();
//				for (String name : transNames) {
//					TransactionType transType = TransactionType.find(name);
//					if (transType == null) continue;
//					if (transType.getId().equals("PIF")) continue;
//					fullNames.add(transType.getName());
//					fullNameMap.put(name, transType.getName());
//
//				}
//				transactionRadButtons.addButtons(fullNames);

				transactionChosen(simidFinal, "all");
			}
			
		});

//		toolkitService.getTransactionsForActor(actorName, new AsyncCallback<List<String>>() {
//
//			public void onFailure(Throwable caught) {
//				if (caught.getMessage() != null)
//					new PopupMessage("Error: " + caught.getMessage());			
//			}
//
//			public void onSuccess(List<String> result) {
//				transactionNamesPanel.clear();
//				transactionNamesPanel.add(html(bold("Transaction: ")));
//
//
//				transactionRadButtons = new TransactionNamesRadioButtonGroup(new Panel(transactionNamesPanel), actorNameFinal);
//				transactionRadButtons.addButtons(result);
//
//			}
//
//		});
	}


//	void actorChosen(String name) {
//		currentActor = name;
//		transactionNamesPanel.clear();
//		transInstanceListBox.clear();
//		clear();
//		loadTransactionNames(name);
//		currentEvent = null;
//	}


	TransactionInstance findTransactionInstance(String label) {
		if (label == null) return null;
		for (TransactionInstance ti : transactionInstances) {
			if (label.equals(ti.label)) return ti;
			if (label.equals(ti.labelInterpretedAsDate)) return ti;
		}
		return null;
	}

	void transactionChosen(SimId simid, String transName) {
		currentTransaction = transName;
		clear();
		transInstanceListBox.clear();
		currentEvent = null;
		
		if ("all".equalsIgnoreCase(transName))
			transName = null;

		toolkitService.getTransInstances(simid, "", transName, new AsyncCallback<List<TransactionInstance>>() {

			public void onFailure(Throwable caught) {
				if (caught.getMessage() != null)
					new PopupMessage("Error: " + caught.getMessage());			
			}

			public void onSuccess(List<TransactionInstance> result) {
				transactionInstances = result;
				transInstanceListBox.clear();
				
//				for (int i=result.size()-1; i >= 0; i--)
//					transInstanceListBox.addItem(result.getRetrievedDocumentsModel(i));
				for (TransactionInstance x : result) {
					transInstanceListBox.addItem(x.labelInterpretedAsDate + " " + x.nameInterpretedAsTransactionType, x.label);
				}

			}

		});

	}

	ChangeHandler transactionInstanceChoiceChanged = new ChangeHandler() {

		public void onChange(ChangeEvent event) {
			int selectedItem = transInstanceListBox.getSelectedIndex();
			String value = transInstanceListBox.getValue(selectedItem);
			TransactionInstance ti = findTransactionInstance(value);
			if (ti == null) return;
			currentTransactionInstance = ti;
			loadTransactionInstanceDetails(ti);
			
			String messageId = getMessageIdFromLabel(value);
			currentTransaction = getTransactionFromLabel(value);

			String u = "<a href=\"" +
			 "message/" + simid + "/" + currentActor + "/" + currentTransaction + "/" + messageId + "\"" +
//			" target=\"_blank\"" +
			">Download Message</a>";
			download.setHTML(u);


		}
	};

	HTML htmlize(String header, String in) {
		HTML h = new HTML(
				"<b>" + header + "</b><br /><br />" +

				in.replaceAll("<", "&lt;")
				.replaceAll("\n\n", "\n")
				.replaceAll("\t", "&nbsp;&nbsp;&nbsp;")
				.replaceAll(" ", "&nbsp;")
				.replaceAll("\n", "<br />")
		);
		return h;
	}
	
	String getMessageIdFromLabel(String label) {
		String[] parts = label.split(" ");
		if (parts.length == 2)
			return parts[0];
		return label;
	}
	
	String getTransactionFromLabel(String label) {
		String[] parts = label.split(" ");
		if (parts.length == 2)
			return parts[1];
		return label;
	}

	void loadTransactionInstanceDetails(TransactionInstance ti) {
		SimId simid = this.simidFinal;
		if (ti.actorType == null) return;
		String actor = ti.actorType.getShortName();
		String trans = ti.name;
		String messageId = ti.label;

		scrollInPanel.clear();
		scrollOutPanel.clear();
		scrollLogPanel.clear();

		toolkitService.getTransactionRequest(simid, actor, trans, messageId, new AsyncCallback<String>() {

			public void onFailure(Throwable caught) {
				if (caught.getMessage() != null)
					new PopupMessage("Error: " + caught.getMessage());			
			}

			public void onSuccess(String result) {
				scrollInPanel.add(htmlize("Request Message", result));
			}

		});

		toolkitService.getTransactionResponse(simid, actor, trans, messageId, new AsyncCallback<String>() {

			public void onFailure(Throwable caught) {
				if (caught.getMessage() != null)
					new PopupMessage("Error: " + caught.getMessage());			
			}

			public void onSuccess(String result) {
				scrollOutPanel.add(htmlize("Response Message", result));
			}

		});

		toolkitService.getTransactionLog(simid, actor, trans, messageId, new AsyncCallback<String>() {

			public void onFailure(Throwable caught) {
				if (caught.getMessage() != null)
					new PopupMessage("Error: " + caught.getMessage());			
			}

			public void onSuccess(String result) {
				scrollLogPanel.add(htmlize("Log", result));
			}

		});

	}

	ClickHandler refreshClickHandler = new ClickHandler() {

		public void onClick(ClickEvent event) {
			loadTransactionNames(simidFinal);
			transactionChosen(simidFinal, transName);

			clear();
		}

	};

	ClickHandler inspectRequestClickHandler = new ClickHandler() {
		@Override
		public void onClick(ClickEvent clickEvent) {
			try {
				toolkitService.getSimulatorEventRequest(currentTransactionInstance, new AsyncCallback<Result>() {
                    @Override
                    public void onFailure(Throwable throwable) {
                        new PopupMessage(throwable.getMessage());
                    }

                    @Override
                    public void onSuccess(Result result) {
    					List<Result> results = new ArrayList<Result>();
						results.add(result);
						MetadataInspectorTab tab = new MetadataInspectorTab();
						tab.setResults(results);
						Xdstools2.DEBUG("simdb is " + getSimid().toString());
						SiteSpec siteSpec = new SiteSpec(getSimid().toString(), currentTransactionInstance.actorType, null);
						tab.setSiteSpec(siteSpec);
						tab.setToolkitService(toolkitService);
						tab.onTabLoad(myContainer, true, null);
                    }
                });
			} catch (Exception e) {
				new PopupMessage(e.getMessage());
			}
		}
	};

	ClickHandler inspectResponseClickHandler = new ClickHandler() {
		@Override
		public void onClick(ClickEvent clickEvent) {
			try {
				toolkitService.getSimulatorEventResponse(currentTransactionInstance, new AsyncCallback<Result>() {
					@Override
					public void onFailure(Throwable throwable) {
						new PopupMessage(throwable.getMessage());
					}

					@Override
					public void onSuccess(Result result) {
						List<Result> results = new ArrayList<Result>();
						results.add(result);
						MetadataInspectorTab tab = new MetadataInspectorTab();
						tab.setResults(results);
						SiteSpec siteSpec = new SiteSpec(getSimid().toString(), currentTransactionInstance.actorType, null);
						tab.setSiteSpec(siteSpec);
						tab.setToolkitService(toolkitService);
						tab.onTabLoad(myContainer, true, null);
					}
				});
			} catch (Exception e) {
				new PopupMessage(e.getMessage());
			}
		}
	};

//	ClickHandler deleteClickHandler = new ClickHandler() {
//
//		public void onClick(ClickEvent event) {
//			String url = GWT.getModuleBaseURL() + "simulator/del"
//			+ "/" + simid
//			+ "/" + currentActor
//			+ "/" + getTransactionFromLabel(event.toString())
//			+ "/" + getMessageIdFromLabel(event.toString());
//
//			doDelete(url);
//		}
//
//	};

	void clear() {
		scrollInPanel.clear();
		scrollOutPanel.clear();
		scrollLogPanel.clear();
	}


	// several background operations need to make sure the 
	// SimServlet has initialized since that is where the servlet initialization
	// parameters are read.  Issue a HTTP Get for something that is known to fail
	// just to getRetrievedDocumentsModel the servlet to initialize
	public void initSimServlet() {

		String url = GWT.getModuleBaseURL() + "simulator/del"; 

		RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, url);

		try {
			@SuppressWarnings("unused")
			Request response = builder.sendRequest(null, new RequestCallback() {
				public void onError(Request request, Throwable exception) {
				}

				public void onResponseReceived(Request request, Response response) {
				}
			});
		} catch (RequestException e) {
			// Code omitted for clarity
		}

	}

//	public void doDelete(String url) {
//		RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, url);
//
//		try {
//			@SuppressWarnings("unused")
//			Request response = builder.sendRequest(null, new RequestCallback() {
//				public void onError(Request request, Throwable exception) {
//					if (exception.getMessage() != null)
//						new PopupMessage("Error: " + exception);
//				}
//
//				public void onResponseReceived(Request request, Response response) {
//					int status = response.getStatusCode();
//					if (status == 200) {
//						transactionChosen(currentActor, currentTransaction);					}
//					else
//						new PopupMessage("Failure");
//				}
//			});
//		} catch (RequestException e) {
//			// Code omitted for clarity
//		}
//	}


	String transName = "";
	SimId simidFinal = new SimId("");

	class TransactionNamesRadioButtonGroup extends RadioButtonGroup {
		SimId simid;

		TransactionNamesRadioButtonGroup(Panel p, SimId simid) {
			super("TransactionNamesGroup", p);
			this.simid = simid;
			simidFinal = simid;

			choiceChangedHandler  = new ValueChangeHandler<Boolean>() {

				public void onValueChange(ValueChangeEvent<Boolean> ignored) {

					RadioButton rb = null;
					for (RadioButton r : buttons) {
						if (r.getValue()) {
							rb = r;
							break;
						}
					}

					if (rb == null)
						return;

					transName = getNameForRadioButton(rb);
					if (transName == null) 
						return;
					
					if ("All".equals(transName))
						transName = null;

					transactionChosen(simidFinal, transName);

				}
			};
		}

	}

	public String getWindowShortName() {
		return "simmsgview";
	}



}
