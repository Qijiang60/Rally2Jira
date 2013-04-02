package com.ceb.rallytojira.rest.client;

import java.net.URI;
import java.net.URISyntaxException;

import com.ceb.rallytojira.rest.api.JiraRestApi;

public class JiraJsonClient {

	public JiraRestApi getJiraRestApi() throws URISyntaxException {
		return new JiraRestApi(new URI("http://206.16.231.96"),
				"rally_jira_migration", "rally_jira_migration");
	}
}
