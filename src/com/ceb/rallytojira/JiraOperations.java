package com.ceb.rallytojira;

import java.io.File;
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
			ClientResponse response = api.doPost("/rest/api/latest/version", Utils.mapToJsonString(data));
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

	public ClientResponse attachFile(String issueKey, File file) throws IOException {

		return api.doMultipartPost("/rest/api/latest/issue/" + issueKey + "/attachments", file);

	}

	public JsonObject createIssueInJira(JsonObject project, String jiraVersionId, JsonObject rallyWorkProduct, RallyObject workProductType, String jiraIssueType) throws Exception {
		Map<String, Object> postData = getIssueCreateMap(project, jiraVersionId, rallyWorkProduct, workProductType, jiraIssueType);
		ClientResponse response = api.doPost("/rest/api/latest/issue", Utils.mapToJsonString(postData));
		return processJiraResponse(response);
	}

	private JsonObject processJiraResponse(ClientResponse response) throws Exception {
		JsonObject jResponse = Utils.jerseyRepsonseToJsonObject(response);
		if (jResponse.get("errorMessages") != null) {
			System.out.println(jResponse);
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
				String topKey = (String) jiraMap.keySet().toArray()[0];
				if (issueData.containsKey(topKey)) {
					Object val = issueData.get(topKey);
					if (val instanceof Map) {
						((Map) val).putAll((Map) jiraMap.get(topKey));
					} else {
						Map m = new HashMap();
						m.putAll((Map) issueData.get(topKey));
						m.putAll((Map) jiraMap.get(topKey));
						issueData.put(topKey, m);
					}
				} else {
					issueData.putAll(jiraMap);
				}
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

	public void deleteAllIssues(JsonObject project) throws IOException {
		String url = "/rest/api/latest/search?jql=project=" + Utils.getJiraProjectNameForRallyProject(project) + "&maxResults=200";
		JsonArray issues = Utils.jerseyRepsonseToJsonObject(api.doGet(url)).get("issues").getAsJsonArray();
		for (JsonElement issue : issues) {
			String issueKey = issue.getAsJsonObject().get("key").getAsString();
			api.doDelete("/rest/api/2/issue/" + issueKey + "?deleteSubtasks=true");
		}

	}

	public JsonObject getRallyAttachment(String URL) throws Exception {
		ClientResponse response = api.doRallyGet(URL);
		return processJiraResponse(response);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public JsonObject addComment(String issueKey, String comment) throws Exception {
		Map commentMap = new HashMap();
		commentMap.put("body", comment);
		ClientResponse response = api.doPost("/rest/api/latest/issue/" + issueKey + "/comment", Utils.mapToJsonString(commentMap));
		return processJiraResponse(response);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public JsonObject logWork(String issueKey, String loggedHours) throws Exception {
		Map workLogMap = new HashMap();
		workLogMap.put("timeSpent", loggedHours + "h");
		// JsonObject jiraIssue = findIssueByIssueKey(issueKey);
		// JsonElement assignee =
		// jiraIssue.get("fields").getAsJsonObject().get("assignee");
		// JsonElement assigneeName = assignee.getAsJsonObject().get("name");
		// if (assigneeName != null && !assigneeName.isJsonNull()) {
		// Map author = new HashMap();
		// author.put("name", assigneeName.getAsString());
		// workLogMap.put("author", author);
		// Map updateAuthor = new HashMap();
		// updateAuthor.put("name", assigneeName.getAsString());
		// workLogMap.put("updateAuthor", updateAuthor);
		// }
		workLogMap.put("timeSpent", loggedHours + "h");
		Utils.printJson(workLogMap);
		ClientResponse response = api.doPost("/rest/api/latest/issue/" + issueKey + "/worklog", Utils.mapToJsonString(workLogMap));
		return processJiraResponse(response);
	}

	public JsonObject findIssueByIssueKey(String issueKey) throws Exception {
		ClientResponse response = api.doGet("/rest/api/2/issue/" + issueKey);
		return processJiraResponse(response);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public JsonObject updateWorkflowStatus(String project, String issueKey, String rallyStatus) throws Exception {
		Map transitionMap = new HashMap();
		String jiraTransitionId = Utils.getJiraTransitionId(project, rallyStatus);
		Map transitionIdMap = new HashMap();
		transitionIdMap.put("id", jiraTransitionId);
		transitionMap.put("transition", transitionIdMap);
		Utils.printJson(transitionMap);
		ClientResponse response = api.doPost("/rest/api/latest/issue/" + issueKey + "/transitions", Utils.mapToJsonString(transitionMap));
		return processJiraResponse(response);

	}

	public JsonObject updateIssueAssignee(String projectName, String issueKey, String rallyOwnerName) throws Exception {
		String jiraUsername = Utils.lookupJiraUsername(rallyOwnerName);

		Map<String, Object> m = new HashMap<String, Object>();
		m.put("name", jiraUsername);
		Utils.printJson(m);
		ClientResponse response = api.doPut("/rest/api/latest/issue/" + issueKey + "/assignee", Utils.mapToJsonString(m));
		return processJiraResponse(response);
	}

}
