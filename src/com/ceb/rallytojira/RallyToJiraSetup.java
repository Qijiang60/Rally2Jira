package com.ceb.rallytojira;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import com.ceb.rallytojira.domain.RallyObject;
import com.ceb.rallytojira.rest.client.Utils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class RallyToJiraSetup {

	RallyOperations rally;
	JiraOperations jira;
	Map<String, String> releaseVersionMap = new HashMap<String, String>();
	int counter = 0;
	int limit = 100000;
	int progress = 0;

	public RallyToJiraSetup() throws URISyntaxException {
		rally = new RallyOperations();
		jira = new JiraOperations();
	}

	public static void main(String[] args) throws URISyntaxException,
			Exception {
		RallyToJiraSetup rallyToJira = new RallyToJiraSetup();
		rallyToJira.process();

	}

	private void process() throws Exception {
		JsonObject project = rally.getProjectByName(RallyToJira.PROJECT).get(0).getAsJsonObject();
		addUsersToProjectRole(project);
	}

	private void addUsersToProjectRole(JsonObject project) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader("mappings/jira_rally_user_mapping_" + RallyToJira.PROJECT.replaceAll(" ", "_")));
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
				jira.addUserToProjectRoles(project, jiraUserName, new String[]{"Developers","Users"});
				
			}
			line = br.readLine();
		}
		br.close();
	}


}
