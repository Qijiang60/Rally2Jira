package com.ceb.rallytojira;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.Map;

import com.ceb.rallytojira.domain.RallyObject;
import com.ceb.rallytojira.rest.api.JiraRestApi;
import com.ceb.rallytojira.rest.client.JiraJsonClient;
import com.ceb.rallytojira.rest.client.Utils;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sun.jersey.api.client.ClientResponse;

public class JiraOperations {
	JiraRestApi api;

	public JiraOperations() throws URISyntaxException {
		JiraJsonClient client = new JiraJsonClient();
		api = client.getJiraRestApi();
	}

	public ClientResponse getAllProjects() throws IOException {
		return api.doGet("/rest/api/latest/issue/createmeta");

	}

	public String createVersion(JsonObject project, JsonObject release)
			throws IOException {
		String projectName = Utils.getJsonObjectName(project);
		String versionId = findProjectVersionByName(project,
				Utils.getJsonObjectName(release));
		if (Utils.isEmpty(versionId)) {
			Map<String, String> mapping = Utils.getElementMapping(
					RallyObject.RELEASE, projectName);
			Map<String, String> data = new LinkedHashMap<>();
			data.put("project",
					Utils.getJiraProjectNameForRallyProject(project));

			for (String key : mapping.keySet()) {
				data.put(key, release.get(mapping.get(key)).getAsString());
			}
			Utils.printJson(data);
			ClientResponse response = api.doPost("/rest/api/latest/version",
					Utils.listToJsonString(data));
			JsonObject jResponse = Utils.jerseyRepsonseToJsonObject(response);

			System.out.println(jResponse);
			versionId = jResponse.get("id").getAsString();
		}
		return versionId;

	}

	public String findProjectVersionByName(JsonObject project,
			String versionName) throws IOException {
		JsonArray projectVersions = findProjectVersions(project);
		for (JsonElement version : projectVersions) {
			if (version.getAsJsonObject().get("name").getAsString()
					.equals(versionName)) {
				return version.getAsJsonObject().get("id").getAsString();
			}
		}
		return null;
	}

	public JsonArray findProjectVersions(JsonObject project) throws IOException {
		ClientResponse response = api.doGet("/rest/api/latest/project/"
				+ Utils.getJiraProjectNameForRallyProject(project)
				+ "/versions");
		JsonArray versions = Utils.jerseyRepsonseToJsonArray(response);
		return versions;
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
		// return null;
		return api.doPost("/rest/api/latest/issue", s);

	}

	public ClientResponse attachFile() throws IOException {

		return api.doMultipartPost("/rest/api/latest/issue/DISC-1/attachments",
				"abc");

	}

//	public void createIssueInVersionFromUserStory(JsonObject project, String versionId, JsonObject userTory) {
//		String projectName = Utils.getJsonObjectName(project);
//		String versionId = findProjectVersionByName(project,
//				Utils.getJsonObjectName(release));
//		if (Utils.isEmpty(versionId)) {
//			Map<String, String> mapping = Utils.getElementMapping(
//					RallyObject.RELEASE, projectName);
//			Map<String, String> data = new LinkedHashMap<>();
//			data.put("project",
//					Utils.getJiraProjectNameForRallyProject(project));
//
//			for (String key : mapping.keySet()) {
//				data.put(key, release.get(mapping.get(key)).getAsString());
//			}
//			Utils.printJson(data);
//			ClientResponse response = api.doPost("/rest/api/latest/version",
//					Utils.listToJsonString(data));
//			JsonObject jResponse = Utils.jerseyRepsonseToJsonObject(response);
//
//			System.out.println(jResponse);
//			versionId = jResponse.get("id").getAsString();
//		}
//		return versionId;
//
//	}

}
