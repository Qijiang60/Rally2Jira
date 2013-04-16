package com.ceb.rallytojira;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
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
import com.google.gson.JsonParser;
import com.sun.jersey.api.client.ClientResponse;

public class JiraRestOperations {
	JiraRestApi api;

	public JiraRestOperations() throws URISyntaxException {
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
				Map m = Utils.getJiraValue(key, mapping.get(key), release, project);
				if (Utils.isNotEmpty(m)) {
					data.putAll(m);
				}
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
					.equals(versionName.replaceAll("  ", " "))) {
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
		Utils.printJson(postData);
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
			Map jiraMap = Utils.getJiraValue(key, mapping.get(key), rallyIssue, project);
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
		// Utils.printJson(postData);
		return postData;
	}

	JsonObject findIssueByRallyFormattedID(String projectKey, String rallyFormattedId) {
		String url = "/rest/api/latest/search?jql=Rally_FormattedID~" + rallyFormattedId;
		JsonArray issues = Utils.jerseyRepsonseToJsonObject(api.doGet(url)).get("issues").getAsJsonArray();

		if (issues.size() > 0) {
			for (JsonElement je : issues) {
				if (je.getAsJsonObject().get("key").getAsString().startsWith(projectKey + "-")) {
					return je.getAsJsonObject();
				}
			}
		}

		return null;
	}

	public boolean deleteAllIssues(JsonObject project) throws IOException {
		String url = "/rest/api/latest/search?jql=project=" + Utils.getJiraProjectNameForRallyProject(project) + "&maxResults=200";
		JsonArray issues = Utils.jerseyRepsonseToJsonObject(api.doGet(url)).get("issues").getAsJsonArray();
		for (JsonElement issue : issues) {
			String issueKey = issue.getAsJsonObject().get("key").getAsString();
			api.doDelete("/rest/api/2/issue/" + issueKey + "?deleteSubtasks=true");
		}
		if (issues.size() == 200) {
			return true;
		}
		return false;

	}

	public JsonObject getRallyAttachment(String URL) throws Exception {
		ClientResponse response = api.doRallyGet(URL);
		return processJiraResponse(response);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public JsonObject addComment(String issueKey, String comment) throws Exception {
		Map commentMap = new HashMap();
		commentMap.put("body", comment);
		String s = Utils.mapToJsonString(commentMap);
		if (Utils.isNotEmpty(comment) && !"{}".equalsIgnoreCase(s)) {
			ClientResponse response = api.doPost("/rest/api/latest/issue/" + issueKey + "/comment", Utils.mapToJsonString(commentMap));
			// return processJiraResponse(response);
		}
		return null;
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
	public void updateWorkflowStatus(String issueKey, String jiraTransitionId, String rallyStatus) throws Exception {
		Map transitionMap = new HashMap();
		Map transitionIdMap = new HashMap();
		transitionIdMap.put("id", jiraTransitionId);
		transitionMap.put("transition", transitionIdMap);
		// Utils.printJson(transitionMap);
		ClientResponse response = api.doPost("/rest/api/latest/issue/" + issueKey + "/transitions", Utils.mapToJsonString(transitionMap));
		try {
			processJiraResponse(response);
		} catch (Exception e) {
			System.out.println("Rally status: " + rallyStatus);
			throw e;
		}

	}

	public void updateIssueAssignee(String projectName, String issueKey, String rallyOwnerObjectID) throws Exception {
		String jiraUsername = Utils.lookupJiraUsername(projectName, rallyOwnerObjectID);

		if (Utils.isEmpty(jiraUsername)) {
			System.err.println(" Rally user: " +
					rallyOwnerObjectID + ", Jira User " + jiraUsername);
		} else {
			Map<String, Object> m = new HashMap<String, Object>();
			m.put("name", jiraUsername);
			ClientResponse response = api.doPut("/rest/api/latest/issue/" + issueKey + "/assignee", Utils.mapToJsonString(m));
			processJiraResponse(response);
		}
	}

	public JsonObject findJiraUser(String searchString) throws Exception {
		ClientResponse response = api.doGet("/rest/api/latest/user/search?username=" + URLEncoder.encode(searchString, "UTF-8"));
		JsonElement jElement = (new JsonParser()).parse(response.getEntity(String.class));
		if (jElement.isJsonObject()) {
			return jElement.getAsJsonObject();
		} else {
			if (jElement.isJsonArray()) {
				JsonArray ja = jElement.getAsJsonArray();
				if (ja.size() == 1) {
					return ja.get(0).getAsJsonObject();
				} else {
					throw new Exception(ja.toString());
				}
			}
		}
		return null;
	}

	public void updateDatesInDatabase(String projectName, String databaseId, JsonObject rallyWorkProduct, boolean stateChanged) throws Exception {
		String creationDate = rallyWorkProduct.get("CreationDate").getAsString();
		String lastUpdateDate = rallyWorkProduct.get("LastUpdateDate").getAsString();
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("issueDbId", databaseId);
		data.put("creationDate", creationDate);
		data.put("lastUpdateDate", lastUpdateDate);
		data.put("stateChanged", stateChanged);
		ClientResponse response = api.doPost("/rest/db/latest/update/issue/createAndUpdateDate", Utils.mapToJsonString(data));
		JsonObject jObj = processJiraResponse(response);
		if (jObj.get("errors").getAsJsonArray().size() > 0) {
			throw new Exception(jObj.toString());
		}
	}

	public void updateIssueAssigneeAsRallyJiraMigrationUser(String issueKey) throws Exception {

		Map<String, Object> m = new HashMap<String, Object>();
		m.put("name", "rally_jira_migration");
		ClientResponse response = api.doPut("/rest/api/latest/issue/" + issueKey + "/assignee", Utils.mapToJsonString(m));
		processJiraResponse(response);

	}

	public void addUserToProjectRoles(String jiraProjectKey, String jiraUserName, String[] roles) throws Exception {
		ClientResponse response = api.doGet("/rest/api/latest/project/" + jiraProjectKey + "/role");
		jiraUserName = jiraUserName.trim();
		int index = 19;
		JsonObject projectRoles = Utils.jerseyRepsonseToJsonObject(response);
		ClientResponse userRoleResp = api.doGet(projectRoles.get("Users").getAsString().substring(index));
		JsonObject oUsersRole = Utils.jerseyRepsonseToJsonObject(userRoleResp);
		ClientResponse developersRoleResp = api.doGet(projectRoles.get("Developers").getAsString().substring(index));
		JsonObject oDevelopersRole = Utils.jerseyRepsonseToJsonObject(developersRoleResp);
		System.out.println(jiraUserName);

		Map m = new HashMap();
		List u = new ArrayList();
		u.add(jiraUserName);
		m.put("user", u);
		String s = Utils.mapToJsonString(m);
		System.out.println(s);
		ClientResponse resp = api.doPost("/rest/api/latest/project/" + jiraProjectKey + "/role" + "/" + oUsersRole.get("id").getAsString(), s);
		try {
			processJiraResponse(resp);
			System.out.println("Added " + jiraUserName + " to Users");
		} catch (Exception e) {
			e.printStackTrace();
		}

		Map m1 = new HashMap();
		List u1 = new ArrayList();
		u1.add(jiraUserName);
		m1.put("user", u);
		String s1 = Utils.mapToJsonString(m1);
		System.out.println(s1);
		resp = api.doPost("/rest/api/latest/project/" + jiraProjectKey + "/role" + "/" + oDevelopersRole.get("id").getAsString(), s1);
		try {
			processJiraResponse(resp);
			System.out.println("Added " + jiraUserName + " to Users");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void deleteDuplicateIssue(String projectKey, String rallyFormattedId) {

		String url = "/rest/api/latest/search?jql=Rally_FormattedID~" + rallyFormattedId + "&maxResults=200";
		boolean duplicate = false;
		boolean loop = true;
		String issueKeyToSave = "";
		while (loop) {
			JsonArray issues = Utils.jerseyRepsonseToJsonObject(api.doGet(url)).get("issues").getAsJsonArray();

			if (issues.size() > 1) {
				for (JsonElement je : issues) {
					String issueKey = je.getAsJsonObject().get("key").getAsString();
					if (issueKey.startsWith(projectKey + "-")) {
						if (duplicate && !issueKey.equals(issueKeyToSave)) {
							api.doDelete("/rest/api/2/issue/" + issueKey + "?deleteSubtasks=true");
						}else{
							issueKeyToSave = issueKey;
						}
						duplicate = true;
					}
				}
			}
			if (issues.size() < 200) {
				loop = false;
			}
		}

	}

}
