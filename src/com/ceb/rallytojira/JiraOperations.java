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
	public String createVersion(JsonObject project, JsonObject release)
			throws IOException {
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

	public ClientResponse attachFile() throws IOException {

		return api.doMultipartPost("/rest/api/latest/issue/DISC-1/attachments", "abc");

	}

	public JsonObject createIssueInJira(JsonObject project, String jiraVersionId, JsonObject rallyWorkProduct, RallyObject workProductType, String jiraIssueType) throws Exception {
		Map<String, Object> postData = getIssueCreateMap(project, jiraVersionId, rallyWorkProduct, workProductType, jiraIssueType);
		ClientResponse response = api.doPost("/rest/api/latest/issue", Utils.listToJsonString(postData));
		JsonObject jResponse = Utils.jerseyRepsonseToJsonObject(response);
		System.out.println(jResponse);
		if (jResponse.get("errorMessages") != null) {
			throw new Exception("error");
		}
		return jResponse;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map<String, Object> getIssueCreateMap(JsonObject project, String versionId, JsonObject rallyIssue, RallyObject workProductType, String jiraIssueType) throws IOException {
		String projectName = Utils.getJsonObjectName(project);
		Map<String, String> mapping = Utils.getElementMapping(workProductType, projectName);
		Map<String, Object> postData = new LinkedHashMap<String, Object>();
		Map<String, Object> issueData = new LinkedHashMap<String, Object>();

		Map projectKey = new HashMap();
		projectKey.put("key", Utils.getJiraProjectNameForRallyProject(project));
		issueData.put("project", projectKey);

		Map issuetype = new HashMap();
		issuetype.put("name", jiraIssueType);
		issueData.put("issuetype", issuetype);

		if (!Utils.isEmpty(versionId)) {
			Map version = new HashMap();
			version.put("id", versionId);
			List versions = new ArrayList();
			versions.add(version);
			issueData.put("fixVersions", versions);
		}
		for (String key : mapping.keySet()) {
			Map jiraMap = Utils.getJiraValue(key, mapping.get(key), rallyIssue);
			if (jiraMap != null) {
				issueData.putAll(jiraMap);
			}

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

	JsonObject findIssueByRallyFormattedID(String rallyFormattedId) {
		String url = "/rest/api/latest/search?jql=Rally_FormattedID~" + rallyFormattedId;
		JsonArray issues = Utils.jerseyRepsonseToJsonObject(api.doGet(url)).get("issues").getAsJsonArray();
		if (issues.size() > 0) {
			return issues.get(0).getAsJsonObject();
		}
		return null;
	}

	public JsonObject createSubUserStory(JsonObject project, String versionId, JsonObject userStory, JsonObject jiraParentIssue) throws Exception {
		userStory.add("jira-parent-key", jiraParentIssue.get("key"));
		return createIssueInJira(project, versionId, userStory, RallyObject.USER_STORY, "Sub-story");

	}

	public void deleteAllIssues(JsonObject project) throws IOException {
		String url = "/rest/api/latest/search?jql=project=" + Utils.getJiraProjectNameForRallyProject(project) + "&maxResults=100";
		JsonArray issues = Utils.jerseyRepsonseToJsonObject(api.doGet(url)).get("issues").getAsJsonArray();
		for (JsonElement issue : issues) {
			String issueKey = issue.getAsJsonObject().get("key").getAsString();
			api.doDelete("/rest/api/2/issue/" + issueKey + "?deleteSubtasks=true");
		}

	}

}
