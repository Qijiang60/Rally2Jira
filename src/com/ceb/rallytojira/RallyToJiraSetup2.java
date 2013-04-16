package com.ceb.rallytojira;

import java.io.BufferedReader;
import java.io.FileReader;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import com.ceb.rallytojira.rest.client.Utils;
import com.google.gson.JsonObject;

public class RallyToJiraSetup2 {

	RallyOperations rally;
	JiraRestOperations jira;
	Map<String, String> releaseVersionMap = new HashMap<String, String>();
	int counter = 0;
	int limit = 100000;
	int progress = 0;
	String projectName = "Web Hierarchy Tool";

	public RallyToJiraSetup2() throws URISyntaxException {
		rally = new RallyOperations();
		jira = new JiraRestOperations();
	}

	public static void main(String[] args) throws URISyntaxException,
			Exception {
		RallyToJiraSetup2 rallyToJira = new RallyToJiraSetup2();
		rallyToJira.process();

	}

	private void process() throws Exception {
		JsonObject project = rally.getProjectByName("Support/Development").get(0).getAsJsonObject();
		String jiraProjectKey = "WHT";
		addUsersToProjectRole(jiraProjectKey);
	}

	private void addUsersToProjectRole(String jiraProjectKey) throws Exception {
		jira.addUserToProjectRoles(jiraProjectKey, "rally_jira_migration", new String[] { "Developers", "Users" });
		jira.addUserToProjectRoles(jiraProjectKey, "hagarwal", new String[] { "Developers", "Users" });
		jira.addUserToProjectRoles(jiraProjectKey, "tberg", new String[] { "Developers", "Users" });
		BufferedReader br = new BufferedReader(new FileReader("mappings/jira_rally_user_mapping_" + projectName.replaceAll(" ", "_")));
		String line = br.readLine();
		while (line != null) {
			if (Utils.isNotEmpty(line)) {
				line = line.replaceAll("\\t", " | ");
				StringTokenizer st = new StringTokenizer(line, "|");
				String rallyObjectId = st.nextToken();
				String rallyDisplayName = st.nextToken();
				String jiraDisplayName = st.nextToken();
				String rallyUserName = st.nextToken();
				String jiraUserName = st.nextToken();
				String disable = st.nextToken();
				String match = st.nextToken();
				if (Utils.isNotEmpty(jiraUserName)) {
					jira.addUserToProjectRoles(jiraProjectKey, jiraUserName, new String[] { "Developers", "Users" });
				}

			}
			line = br.readLine();
		}
		br.close();
	}

}
