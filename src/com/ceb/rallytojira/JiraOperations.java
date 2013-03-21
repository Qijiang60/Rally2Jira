package com.ceb.rallytojira;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import com.ceb.rallytojira.rest.api.JiraRestApi;
import com.ceb.rallytojira.rest.client.JiraJSONClient;
import com.google.gson.Gson;
import com.sun.jersey.api.client.ClientResponse;

public class JiraOperations {
	JiraRestApi api;

	public JiraOperations() throws URISyntaxException {
		JiraJSONClient client = new JiraJSONClient();
		api = client.getJiraRestApi();
	}

	public ClientResponse getAllProjects() throws IOException {
		return api.doGet("/rest/api/latest/issue/createmeta");

	}

	public ClientResponse createIssue() throws IOException {
		Map issue = new LinkedHashMap();

		Map data = new LinkedHashMap();
		Map project = new LinkedHashMap();
		project.put("key", "DISC");
		data.put("project", project);
		data.put("summary", "test issue created by rest api");
		data.put("description", "test issue created by rest api");
		Map issuetype = new LinkedHashMap();
		issuetype.put("name", "Bug");
		data.put("issuetype", issuetype);
		issue.put("fields", data);
		Gson gson = new Gson();
		String s = gson.toJson(issue);
		System.out.println(s);
		//return null;
		return api.doPost("/rest/api/latest/issue", s);

	}
	
	public ClientResponse attachFile() throws IOException {
	
		return api.doMultipartPost("/rest/api/latest/issue/DISC-1/attachments", "abc");

	}

}
