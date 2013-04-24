package com.ceb.rallytojira;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import com.ceb.rallytojira.domain.RallyObject;
import com.ceb.rallytojira.rest.client.Utils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class RallyToJiraSetup1 {

	public static boolean createRallyJiraUserMap(JsonObject workspace, JsonObject project, RallyOperations rally, JiraRestOperations jira) throws IOException {
		String key = Utils.getJiraProjectKeyForRallyProject(workspace, project);
		File f = new File("mappings/jira_rally_user_mapping_" + key);
		// if (f.exists()) {
		// return true;
		// }
		boolean success = true;
		Set<JsonObject> allUsers = getAllUsers(project, rally);
		System.out.println(allUsers.size());
		Map<String, List<String>> allUsersMap = JiraUsers.getAllUsersMap(true);

		BufferedWriter bw = new BufferedWriter(new FileWriter(f));
		bw.write("\nRally ObjectID\tRally DisplayName\tJira DisplayName\tRally UserName\tJira UserName\tDisabled\tMatch");
		for (JsonObject rallyUserObject : allUsers) {
			JsonObject rallyUser = rally.getObjectFromRef(rallyUserObject);
			if (Utils.isEmpty(rallyUser)) {
				continue;
			}
			String rallyUserObjectID = rallyUser.get("ObjectID").getAsString();
			if (allUsersMap.containsKey(rallyUserObjectID)) {
				String jiraSearch = allUsersMap.get(rallyUserObjectID).get(0);
				String jiraDisplayName = allUsersMap.get(rallyUserObjectID).get(1);
				String rallyUserName = allUsersMap.get(rallyUserObjectID).get(2);
				String jiraUserName = allUsersMap.get(rallyUserObjectID).get(3);
				String disabled = allUsersMap.get(rallyUserObjectID).get(4);
				String match = allUsersMap.get(rallyUserObjectID).get(5);
				bw.write("\n" + rallyUserObjectID + "\t" + jiraSearch + "\t" + jiraDisplayName + "\t" + rallyUserName + "\t" + jiraUserName + "\t" + disabled
						+ "\t" + match);
			} else {
				String rallyUserName = rallyUser.get("UserName").getAsString();
				String rallyLastname = isNotJsonNull(rallyUser, "LastName") ? rallyUser.get("LastName").getAsString() : "";
				String rallyFirstname = isNotJsonNull(rallyUser, "FirstName") ? rallyUser.get("FirstName").getAsString() : "";
				String jiraSearch = rallyLastname + ", " + rallyFirstname;
				try {
					JsonObject jiraUser = jira.findJiraUser(jiraSearch);
					String jiraDisplayName = isNotJsonNull(jiraUser, "displayName") ? jiraUser.get("displayName").getAsString() : "";
					String jiraUserName = jiraUser.get("name").getAsString();
					if (jiraSearch.equals(jiraDisplayName)) {
						bw.write("\n" + rallyUserObjectID + "\t" + jiraSearch + "\t" + jiraDisplayName + "\t" + rallyUserName + "\t" + jiraUserName + "\t" + rallyUser.get("Disabled").getAsString()
								+ "\tY");
					} else {
						bw.write("\n" + rallyUserObjectID + "\t" + jiraSearch + "\t" + jiraDisplayName + "\t" + rallyUserName + "\t" + jiraUserName + "\t" + rallyUser.get("Disabled").getAsString()
								+ "\tN");
					}
				} catch (Exception ex) {
					ex.printStackTrace();
					bw.write("\n" + rallyUserObjectID + "\t" + jiraSearch + "\t" + "<?jiraDisplayName?>" + "\t" + rallyUserName + "\t" + "<?jiraUserName?>" + "\t"
							+ rallyUser.get("Disabled").getAsString() + "\tN");
					success = false;
				}
				bw.flush();
			}

		}
		bw.close();
		return success;
	}

	private static Set<JsonObject> getAllUsers(JsonObject project, RallyOperations rally) throws IOException {
		JsonObject proj = rally.getObjectFromRef(project);
		JsonArray teamMembers = proj.get("TeamMembers").getAsJsonArray();
		Set<JsonObject> allUsers = new HashSet<JsonObject>();
		for (JsonElement teamMember : teamMembers) {
			allUsers.add(teamMember.getAsJsonObject());
		}
		JsonArray editors = proj.get("Editors").getAsJsonArray();
		for (JsonElement editor : editors) {
			allUsers.add(editor.getAsJsonObject());
		}
		allUsers.add(project.get("Owner").getAsJsonObject());
		JsonArray tasks = rally.getRallyObjectsForProject(project, RallyObject.TASK);
		for (JsonElement jeTask : tasks) {
			addOwnerToSet(allUsers, jeTask, project);

		}
		tasks = null;
		JsonArray defects = rally.getRallyObjectsForProject(project, RallyObject.DEFECT);
		for (JsonElement jeDefect : defects) {
			addOwnerToSet(allUsers, jeDefect, project);

		}
		defects = null;
		JsonArray userStories = rally.getRallyObjectsForProject(project, RallyObject.USER_STORY);
		for (JsonElement jeUserStory : userStories) {
			addOwnerToSet(allUsers, jeUserStory, project);

		}

		return allUsers;
	}

	private static void addOwnerToSet(Set<JsonObject> allUsers, JsonElement jeRallyWorkProduct, JsonObject project) throws IOException {
		JsonObject rallyWorkProduct = jeRallyWorkProduct.getAsJsonObject();
		if (isNotJsonNull(rallyWorkProduct, "Owner")) {
			JsonObject owner = rallyWorkProduct.get("Owner").getAsJsonObject();
			allUsers.add(owner);
		}
	}

	private static boolean isNotJsonNull(JsonObject rallyWorkProduct, String field) {
		return !isJsonNull(rallyWorkProduct, field);
	}

	private static boolean isJsonNull(JsonObject rallyWorkProduct, String field) {
		if (Utils.isEmpty(rallyWorkProduct) || Utils.isEmpty(rallyWorkProduct.get(field)) || rallyWorkProduct.get(field).isJsonNull()) {
			return true;
		}
		if (rallyWorkProduct.get(field).isJsonPrimitive()) {
			return Utils.isEmpty(rallyWorkProduct.get(field).getAsString());
		}
		return false;
	}

	public static void addUsersToProjectRole(String jiraProjectKey) throws Exception {
		JiraRestOperations jira = new JiraRestOperations();
		jira.addUserToProjectRoles(jiraProjectKey, "rally_jira_migration", new String[] { "Developers", "Users" });
		jira.addUserToProjectRoles(jiraProjectKey, "hagarwal", new String[] { "Developers", "Users" });
		jira.addUserToProjectRoles(jiraProjectKey, "tberg", new String[] { "Developers", "Users" });
		BufferedReader br = new BufferedReader(new FileReader("mappings/jira_rally_user_mapping_" + jiraProjectKey));
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
