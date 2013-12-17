package com.ceb.rallytojira.rest.client;

import java.net.URI;
import java.net.URISyntaxException;

import com.ceb.rallytojira.rest.api.JiraRestApi;
import com.ceb.rallytojira.rest.api.JiraSoapApi;

public class JiraJsonClient {

	public JiraRestApi getJiraRestApi() throws URISyntaxException {
		return new JiraRestApi(new URI("https://agiletool.executiveboard.com"),
				"hagarwal", "iamOkie#234");
	}

	public JiraSoapApi getJiraSoapApi() throws Exception {
		return new JiraSoapApi(new URI("https://agiletool.executiveboard.com"),
				"rally_jira_migration", "rally_jira_migration");
	}
}
