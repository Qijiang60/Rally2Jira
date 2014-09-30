package com.ceb.rallytojira.rest.client;

import java.net.URI;
import java.net.URISyntaxException;

import com.ceb.rallytojira.rest.api.JiraRestApi;
import com.ceb.rallytojira.rest.api.JiraSoapApi;

public class JiraJsonClient {

	public JiraRestApi getJiraRestApi() throws URISyntaxException {
		return new JiraRestApi(new URI("https://agiletool.executiveboard.com"),
				"hagarwal", "iamOkie#904");
	}

	public JiraSoapApi getJiraSoapApi() throws Exception {
		return new JiraSoapApi(new URI("https://agiletool.executiveboard.com"),
				"hagarwal", "iamOkie#904");
	}

	public JiraRestApi getInnotasRestApi() throws URISyntaxException {
		return new JiraRestApi(new URI("https://agiletool.executiveboard.com"),
				"innotas", "Password1");
	}
}
