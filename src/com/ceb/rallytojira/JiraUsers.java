package com.ceb.rallytojira;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

public class JiraUsers {

	public static void main(String[] args) throws IOException {

		String[] projectNames = new String[] { "CMS Project", "Discussions", "iMaps", "Infrastructure", "Site Catalyst", "Test Automation", "Test Automation - Web V2", "Test Automation - Workspace",
				"Workspace" };
		Set<String> activeUsers = new TreeSet<String>();
		Set<String> inActiveUsers = new TreeSet<String>();

		for (String project : projectNames) {

			BufferedReader br = new BufferedReader(new FileReader("mappings/jira_rally_user_mapping_" + project.replaceAll(" ", "_")));
			String line = br.readLine();
			while (line != null) {
				StringTokenizer st = new StringTokenizer(line, "\t");
				String rallyObjectId = st.nextToken();
				String rallyDisplayName = st.nextToken();
				String jiraDisplayName = st.nextToken();
				String rallyUserName = st.nextToken();
				String jiraUserName = st.nextToken();
				String disable = st.nextToken();
				String match = st.nextToken();
				line = br.readLine();
				if ("true".equalsIgnoreCase(disable)) {
					inActiveUsers.add(rallyUserName);
				} else {
					activeUsers.add(rallyUserName);
				}
			}

		}

		System.out.println(activeUsers);
		System.out.println("**************************************************************************************************");
		System.out.println(inActiveUsers);
	}

}
