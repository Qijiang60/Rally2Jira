package com.ceb.rallytojira;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
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

	@SuppressWarnings("unchecked")
	public String createVersion(JsonObject project, JsonObject release) throws IOException {
		String projectName = Utils.getJsonObjectName(project);
		String versionId = findProjectVersionByName(project, Utils.getJsonObjectName(release));
		if (Utils.isEmpty(versionId)) {
			Map<String, String> mapping = Utils.getElementMapping(RallyObject.RELEASE, projectName);
			Map<String, Object> data = new LinkedHashMap<String, Object>();
			data.put("project", Utils.getJiraProjectNameForRallyProject(project));

			for (String key : mapping.keySet()) {
				data.putAll(Utils.getJiraValue(key, mapping.get(key), release));
			}
			Utils.printJson(data);
			ClientResponse response = api.doPost("/rest/api/latest/version", Utils.listToJsonString(data));
			JsonObject jResponse = Utils.jerseyRepsonseToJsonObject(response);

			System.out.println(jResponse);
			versionId = jResponse.get("id").getAsString();
		}
		return versionId;

	}

	public String findProjectVersionByName(JsonObject project, String versionName) throws IOException {
		JsonArray projectVersions = findProjectVersions(project);
		for (JsonElement version : projectVersions) {
			if (version.getAsJsonObject().get("name").getAsString().equals(versionName)) {
				return version.getAsJsonObject().get("id").getAsString();
			}
		}
		return null;
	}

	public JsonArray findProjectVersions(JsonObject project) throws IOException {
		ClientResponse response = api.doGet("/rest/api/latest/project/"
				+ Utils.getJiraProjectNameForRallyProject(project) + "/versions");
		JsonArray versions = Utils.jerseyRepsonseToJsonArray(response);
		return versions;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
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

		return api.doMultipartPost("/rest/api/latest/issue/DISC-1/attachments", "abc");

	}

	public JsonObject findOrCreateIssue(JsonObject project, String versionId, JsonObject jObj, String issueType)
			throws IOException {
		String rallyFormattedId = jObj.get("FormattedID").getAsString();
		JsonObject issue = findIssueByRallyFormattedID(rallyFormattedId);
		if (Utils.isEmpty(issue)) {
			Map<String, Object> postData = getIssueCreateMap(project, versionId, jObj, issueType);
			ClientResponse response = api.doPost("/rest/api/latest/issue", Utils.listToJsonString(postData));
			JsonObject jResponse = Utils.jerseyRepsonseToJsonObject(response);
			System.out.println(jResponse);
			issue = jResponse;
		}
		return issue;
	}

	public JsonObject createIssueFromUserStory(JsonObject project, String versionId, JsonObject userStory)
			throws IOException {

		return findOrCreateIssue(project, versionId, userStory, "Story");
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map<String, Object> getIssueCreateMap(JsonObject project, String versionId, JsonObject rallyIssue,
			String issueType) throws IOException {
		String projectName = Utils.getJsonObjectName(project);
		Map<String, String> mapping = Utils.getElementMapping(RallyObject.USER_STORY, projectName);
		Map<String, Object> postData = new LinkedHashMap<String, Object>();

		Map<String, Object> issueData = new LinkedHashMap<String, Object>();

		Map projectKey = new HashMap();
		projectKey.put("key", Utils.getJiraProjectNameForRallyProject(project));
		issueData.put("project", projectKey);

		Map issuetype = new HashMap();
		issuetype.put("name", issueType);
		issueData.put("issuetype", issuetype);

		Map version = new HashMap();
		version.put("id", versionId);
		List versions = new ArrayList();
		versions.add(version);
		issueData.put("fixVersions", versions);

		for (String key : mapping.keySet()) {
			Map jiraMap = Utils.getJiraValue(key, mapping.get(key), rallyIssue);
			issueData.putAll(jiraMap);

		}
		if (rallyIssue.get("jira-parent-key") != null && !rallyIssue.get("jira-parent-key").isJsonNull()) {
			Map parentStoryKey = new HashMap();
			parentStoryKey.put("key", rallyIssue.get("jira-parent-key").getAsString());
			Map parentStory = new HashMap();
			parentStory.put("parent", parentStoryKey);
			issueData.putAll(parentStory);
		}
		postData.put("fields", issueData);
		Utils.printJson(postData);
		return postData;
	}

	private JsonObject findIssueByRallyFormattedID(String rallyFormattedId) {
		String url = "/rest/api/latest/search?jql=Rally_FormattedID~" + rallyFormattedId;
		JsonArray issues = Utils.jerseyRepsonseToJsonObject(api.doGet(url)).get("issues").getAsJsonArray();
		if (issues.size() > 0) {
			return issues.get(0).getAsJsonObject();
		}
		return null;
	}

	public JsonObject createSubUserStory(JsonObject project, String versionId, JsonObject userStory,
			JsonObject jiraParentIssue) throws IOException {
		userStory.add("jira-parent-key", jiraParentIssue.get("key"));

		return findOrCreateIssue(project, versionId, userStory, "Sub-story");

	}

	public JsonObject createIssueFromDefect(JsonObject project, String versionId, JsonObject defect) throws IOException {
		return findOrCreateIssue(project, versionId, defect, "Bug");

	}

	public JsonObject createIssueFromDefectWithUserStory(JsonObject project, String versionId,
			JsonObject rallyDefectUserStory, JsonObject defect) throws IOException {
		JsonObject jiraIssue = findOrCreateIssue(project, versionId, rallyDefectUserStory, "Story");
		defect.add("jira-parent-key", jiraIssue.get("key"));
		return findOrCreateIssue(project, versionId, defect, "Defect");

	}

}
