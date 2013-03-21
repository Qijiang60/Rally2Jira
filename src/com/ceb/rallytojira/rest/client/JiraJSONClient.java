package com.ceb.rallytojira.rest.client;

import java.net.URI;
import java.net.URISyntaxException;

import com.ceb.rallytojira.rest.api.JiraRestApi;
import com.rallydev.rest.RallyRestApi;

public class JiraJSONClient {

	public JiraRestApi getJiraRestApi() throws URISyntaxException {
		return new JiraRestApi(new URI("http://scrumportal.executiveboard.com"),
				"hagarwal", "Welcome234!");
	}
}
