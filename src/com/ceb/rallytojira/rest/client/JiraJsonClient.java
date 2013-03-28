package com.ceb.rallytojira.rest.client;

import java.net.URI;
import java.net.URISyntaxException;

import com.ceb.rallytojira.rest.api.JiraRestApi;

public class JiraJsonClient {

	public JiraRestApi getJiraRestApi() throws URISyntaxException {
		return new JiraRestApi(new URI("http://172.22.26.20"),
				"hagarwal", "Welcome345!");
	}
}
