package gov.nist.toolkit.errorrecording.client;


import java.util.ArrayList;

import org.apache.commons.lang.StringEscapeUtils;

public class ValidationReportItem {
	
	private String name;
	private String status;
	private String dts;
	private String found;
	private String expected;
	private ArrayList<String> rfc_name = new ArrayList<String>();
	private String color;
	private int type;
	private ArrayList<String> rfc_link = new ArrayList<String>();
	
	//public enum ReportingLevel { SECTIONHEADING, SUCCESS, ERROR, WARNING,  INFO, CONTENT};
	
	public ValidationReportItem(ValidatorErrorItem.ReportingLevel type, String name, String dts, String found, String expected, String rfc) {
		this.name = StringEscapeUtils.escapeHtml(name);
		switch(type) {
		
		case SECTIONHEADING:
			this.type = 0;
			this.status = "";
			this.color = "color: black;font-weight:bold;";
			break;
		
		case D_SUCCESS:
			this.type = 1;
			this.status = "Success";
			this.color = "color: green";
			break;

		case D_ERROR:
			this.type = 1;
			this.status = "Error";
			this.color = "color: red";
			break;
			
		case D_WARNING:
			this.type = 1;
			this.status = "Warning";
			this.color = "color: blue";
			break;
			
		case D_INFO:
			this.type = 1;
			this.status = "Info";
			this.color = "color: purple";
			break;
			
		case DETAIL:
			this.type = 2;
			this.status = "Content";
			this.color = "color:black";
			break;
			
		default:
			this.type = 1;
			this.status = "";
			this.color = "color: black;";
			break;

		}
		this.dts = StringEscapeUtils.escapeHtml(dts);
		//this.found = StringEscapeUtils.escapeHtml(found);
		this.found = found;
		this.expected = StringEscapeUtils.escapeHtml(expected);
		rfc = StringEscapeUtils.escapeHtml(rfc);
		if(rfc.contains(";")) {
			String[] rfcSplit = rfc.split(";");
			if(rfcSplit.length % 2 == 0) {
				for(int i=0;i<rfcSplit.length-1;i=i+2) {
					this.rfc_name.add(rfcSplit[i]);
					this.rfc_link.add(rfcSplit[i]);
				}
			}
		} else {
			this.rfc_name.add(rfc);
		}

	}
	
	public ValidationReportItem(String name, String content) {
		this.name = StringEscapeUtils.escapeHtml(name);
		//this.found = content;
		this.found = StringEscapeUtils.escapeHtml(content);
		//this.found = this.found.replace(" ", "&nbsp;");
		//this.found = this.found.replace("\n", "<br />\n");
		this.type = 2;
		
		if(this.found.contains("attachment=") && this.found.contains("filename=")) {
			this.found = this.found.replace("attachment=", "");
			this.found = this.found.replace("filename=", "");
			this.name = this.found.split(";")[0];
			this.found = this.found.split(";")[1];
		}
		this.status = "Content";
		this.color = "color:black";
		this.dts = "";
		this.expected = "";
		this.rfc_name.add("-");
	}
	
	public ValidationReportItem(String name) {
		//this.name = StringEscapeUtils.escapeHtml(name);
		this.name = name;
		this.name = this.name.replace(" ", "&nbsp;");
		this.name = this.name.replace("\n", "<br />\n");
		this.type = 3;
		this.status = "Detail";
		this.color = "color:black";
		this.dts = "";
		this.found = "";
		this.expected = "";
		this.rfc_name.add("-");
		
		if(this.name.contains(";anchor=")) {
			String anchor = "";
			anchor = this.name.split(";anchor=")[1];
			this.name = this.name.split(";anchor=")[0];
			this.name = "<span id=\"" + anchor + "\">" + this.name + "</div>";
		}
		
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getDts() {
		return dts;
	}

	public void setDts(String dts) {
		this.dts = dts;
	}

	public String getFound() {
		return found;
	}

	public void setFound(String found) {
		this.found = found;
	}

	public String getExpected() {
		return expected;
	}

	public void setExpected(String expected) {
		this.expected = expected;
	}

	public ArrayList<String> getRfc_name() {
		return rfc_name;
	}

	public void setRfc_name(ArrayList<String> rfc_name) {
		this.rfc_name = rfc_name;
	}

	public String getColor() {
		return color;
	}

	public void setColor(String color) {
		this.color = color;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public ArrayList<String> getRfc_link() {
		return rfc_link;
	}

	public void setRfc_link(ArrayList<String> rfc_link) {
		this.rfc_link = rfc_link;
	}

}
