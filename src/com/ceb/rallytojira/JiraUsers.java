package com.ceb.rallytojira;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import com.ceb.rallytojira.rest.client.Utils;

public class JiraUsers {

	public static void main(String[] args) throws IOException {

		// String[] projectNames = new String[] { "Test Automation" };
		String[] projectNames = new String[] { "CMS Project", "Discussions",
				"iMaps", "Infrastructure", "Site Catalyst", "Test Automation",
				"Test Automation - Web V2", "Test Automation - Workspace",
				"Workspace", "Iconoculture" };
		Set<String> activeUsers = new TreeSet<String>();
		Set<String> inActiveUsers = new TreeSet<String>();

		for (String project : projectNames) {
			BufferedReader br = new BufferedReader(new FileReader("mappings/jira_rally_user_mapping_" + project.replaceAll(" ", "_")));
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

					if ("true".equalsIgnoreCase(disable.trim())) {
						inActiveUsers.add(jiraUserName.trim());
					} else {
						activeUsers.add(jiraUserName.trim());
					}
				}
				line = br.readLine();
			}
			br.close();

		}

		System.out.println(Utils.setToString(activeUsers));
		System.out.println("**************************************************************************************************");
		System.out.println(Utils.setToString(inActiveUsers));
	}

}
