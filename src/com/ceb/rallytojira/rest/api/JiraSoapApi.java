package com.ceb.rallytojira.rest.api;

import java.net.URI;

import org.swift.common.soap.jira.JiraSoapService;
import org.swift.common.soap.jira.JiraSoapServiceServiceLocator;

public class JiraSoapApi {

	private JiraSoapServiceServiceLocator fJiraSoapServiceGetter = new JiraSoapServiceServiceLocator();
	private JiraSoapService fJiraSoapService = null;
	private String fToken = null;

	public JiraSoapApi(URI server, String username, String password) throws Exception {
		try {
			String endPoint = "/rpc/soap/jirasoapservice-v2";
			fJiraSoapServiceGetter.setJirasoapserviceV2EndpointAddress(server + endPoint);
			fJiraSoapServiceGetter.setMaintainSession(true);
			fJiraSoapService = fJiraSoapServiceGetter.getJirasoapserviceV2();
			fToken = fJiraSoapService.login(username, password);
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

	public JiraSoapService getfJiraSoapService() {
		return fJiraSoapService;
	}

	public String getfToken() {
		return fToken;
	}
	
	

}
