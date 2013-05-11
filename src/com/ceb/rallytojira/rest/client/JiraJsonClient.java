package com.ceb.rallytojira.rest.client;

import java.net.URI;
import java.net.URISyntaxException;

import com.ceb.rallytojira.rest.api.JiraRestApi;
import com.ceb.rallytojira.rest.api.JiraSoapApi;

public class JiraJsonClient {

	public JiraRestApi getJiraRestApi() throws URISyntaxException {
		return new JiraRestApi(new URI("https://206.16.231.96"),
				"rally_jira_migration", "rally_jira_migration");
	}

	public JiraSoapApi getJiraSoapApi() throws Exception {
		return new JiraSoapApi(new URI("https://206.16.231.96"),
				"rally_jira_migration", "rally_jira_migration");
	}
}
