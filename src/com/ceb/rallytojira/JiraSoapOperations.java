package com.ceb.rallytojira;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.swift.common.soap.jira.JiraSoapService;
import org.swift.common.soap.jira.RemoteAuthenticationException;
import org.swift.common.soap.jira.RemoteException;
import org.swift.common.soap.jira.RemoteGroup;
import org.swift.common.soap.jira.RemoteIssue;
import org.swift.common.soap.jira.RemotePermissionException;
import org.swift.common.soap.jira.RemoteProject;
import org.swift.common.soap.jira.RemoteUser;
import org.swift.common.soap.jira.RemoteValidationException;

import com.ceb.rallytojira.rest.api.JiraSoapApi;
import com.ceb.rallytojira.rest.client.JiraJsonClient;
import com.ceb.rallytojira.rest.client.Utils;
import com.google.gson.JsonObject;

public class JiraSoapOperations {
	JiraSoapApi api;
	Map<String, RemoteGroup> groups = new HashMap<String, RemoteGroup>();

	public JiraSoapOperations() throws Exception {
		JiraJsonClient client = new JiraJsonClient();
		api = client.getJiraSoapApi();
	}

	public void addUserToGroup(String username, String groupName) throws Exception {
		if (Utils.isNotEmpty(username) && Utils.isNotEmpty(groupName)) {
			JiraSoapService soapy = api.getfJiraSoapService();
			String token = api.getfToken();
			RemoteUser user = soapy.getUser(token, username);
			if (Utils.isNotEmpty(user)) {
				RemoteGroup group = getGroup(groupName);
				soapy.addUserToGroup(token, group, user);
			}
		}
	}

	private RemoteGroup getGroup(String groupName) throws RemotePermissionException, RemoteValidationException, RemoteAuthenticationException, RemoteException, java.rmi.RemoteException {
		if (groups.containsKey(groupName)) {
			return groups.get(groupName);
		}
		JiraSoapService soapy = api.getfJiraSoapService();
		String token = api.getfToken();
		RemoteGroup group = soapy.getGroup(token, groupName);
		groups.put(groupName, group);
		return group;
	}

	public void removeUserFromGroup(String username, String groupName) throws Exception {
		if (Utils.isNotEmpty(username) && Utils.isNotEmpty(groupName)) {
			JiraSoapService soapy = api.getfJiraSoapService();
			String token = api.getfToken();
			RemoteUser user = soapy.getUser(token, username);
			if (Utils.isNotEmpty(user)) {
				RemoteGroup group = getGroup(groupName);
				soapy.removeUserFromGroup(token, group, user);
			}

		}

	}

	public void createProject(JsonObject workspace, JsonObject project) throws IOException {
		String jiraKey = Utils.getJiraProjectKeyForRallyProject(workspace, project);
		String jiraName = Utils.getJiraProjectNameForRallyProject(workspace, project);
		JiraSoapService soapy = api.getfJiraSoapService();
		String token = api.getfToken();
		try {
			RemoteProject jiraProject = soapy.getProjectByKey(token, jiraKey);
		} catch (Exception e) {
			soapy.createProject(token, jiraKey, jiraName, "", "", "rally_jira_migration", null, null, null);
		}

	}

	public JiraSoapApi getApi() {
		return api;
	}

	
}
