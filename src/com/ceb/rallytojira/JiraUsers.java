package com.ceb.rallytojira;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import org.swift.common.soap.jira.RemoteValidationException;

import com.ceb.rallytojira.rest.client.Utils;

public class JiraUsers {
	static String ext = "jira_rally_user_mapping_";
	static String FILE_DIR = "mappings";
	static GenericExtFilter filter = new GenericExtFilter(ext);
	static Map<String, List<String>> allUserMap;

	public static void main(String[] args) throws Exception {

		Set<String> activeUsers = new TreeSet<String>();
		Set<String> inActiveUsers = new TreeSet<String>();

		File dir = new File(FILE_DIR);

		if (dir.isDirectory() == false) {
			System.out.println("Directory does not exists : " + FILE_DIR);
			return;
		}

		File[] list = dir.listFiles(filter);

		if (list.length == 0) {
			System.out.println("no files start with : " + ext);
			return;
		}
		//
		for (File file : list) {
			BufferedReader br = new BufferedReader(new FileReader(file));
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
						inActiveUsers.add(jiraUserName.trim().toLowerCase());
					} else {
						activeUsers.add(jiraUserName.trim().toLowerCase());
					}
				}
				line = br.readLine();
			}
			br.close();

		}
		inActiveUsers.remove("rally_jira_migration");
		inActiveUsers.remove("hagarwal");
		inActiveUsers.remove("tberg");
		activeUsers.remove("rally_jira_migration");
		activeUsers.remove("hagarwal");
		activeUsers.remove("tberg");

		System.out.println(Utils.setToString(activeUsers));
		System.out.println("**************************************************************************************************");
		System.out.println(Utils.setToString(inActiveUsers));

		for (String inactiveUser : inActiveUsers) {
			removeUserFromGroup(inactiveUser, "jira-users");
			removeUserFromGroup(inactiveUser, "jira-developers");
		}
		for (String activeUser : activeUsers) {
			addUserToGroup(activeUser, "jira-users");
			addUserToGroup(activeUser, "jira-developers");
		}

	}

	public static Map<String, List<String>> getAllUsersMap() throws IOException {
		if (allUserMap == null) {
			allUserMap = new HashMap<String, List<String>>();
			File dir = new File(FILE_DIR);
			File[] list = dir.listFiles(filter);
			for (File file : list) {
				BufferedReader br = new BufferedReader(new FileReader(file));
				String line = br.readLine();
				while (line != null) {
					if (Utils.isNotEmpty(line)) {
						line = line.replaceAll("\\t", " | ");
						StringTokenizer st = new StringTokenizer(line, "|");
						String rallyObjectId = st.nextToken().trim();
						String rallyDisplayName = st.nextToken().trim();
						String jiraDisplayName = st.nextToken().trim();
						String rallyUserName = st.nextToken().trim();
						String jiraUserName = st.nextToken().trim();
						String disable = st.nextToken().trim();
						String match = st.nextToken().trim();
						if (!allUserMap.containsKey(rallyObjectId)) {
							List<String> temp = new ArrayList<String>();
							temp.add(rallyDisplayName);
							temp.add(jiraDisplayName);
							temp.add(rallyUserName);
							temp.add(jiraUserName);
							temp.add(disable);
							temp.add(match);
							allUserMap.put(rallyObjectId, temp);
						}
					}
					line = br.readLine();
				}
				br.close();
			}
		}
		return allUserMap;

	}

	public static void addUserToGroup(String username, String group) throws Exception {
		System.out.println("addUserToGroup: " + username + ", " + group);
		JiraSoapOperations jira = new JiraSoapOperations();
		try {
			jira.addUserToGroup(username, group);
		} catch (Exception ex) {
			try {
				System.err.println(((RemoteValidationException) ex).getFaultString());
			} catch (Exception ex1) {
				ex.printStackTrace();
			}
		}

	}

	public static void removeUserFromGroup(String username, String group) throws Exception {
		System.out.println("removeUserFromGroup: " + username + ", " + group);

		JiraSoapOperations jira = new JiraSoapOperations();
		try {
			jira.removeUserFromGroup(username, group);
		} catch (Exception ex) {
			try {
				System.err.println(((RemoteValidationException) ex).getFaultString());
			} catch (Exception ex1) {
				ex.printStackTrace();
			}

		}
	}

}

class GenericExtFilter implements FilenameFilter {

	private String ext;

	public GenericExtFilter(String ext) {
		this.ext = ext;
	}

	public boolean accept(File dir, String name) {
		return (name.startsWith(ext));
	}
}
