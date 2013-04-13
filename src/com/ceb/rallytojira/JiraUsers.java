package com.ceb.rallytojira;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import org.swift.common.soap.jira.RemoteValidationException;

import com.ceb.rallytojira.rest.client.Utils;

public class JiraUsers {

	public static void main(String[] args) throws Exception {

		Set<String> activeUsers = new TreeSet<String>();
		Set<String> inActiveUsers = new TreeSet<String>();
		String ext = "jira_rally_user_mapping_";
		String FILE_DIR = "mappings";
		GenericExtFilter filter = new GenericExtFilter(ext);

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
