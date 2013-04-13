package com.ceb.rallytojira;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.ceb.rallytojira.domain.RallyObject;
import com.ceb.rallytojira.rest.client.Utils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class RallyToJiraSetup1 {

	RallyOperations rally;
	JiraRestOperations jira;
	Map<String, String> releaseVersionMap = new HashMap<String, String>();
	int counter = 0;
	int limit = 100000;
	int progress = 0;
	

	public RallyToJiraSetup1() throws URISyntaxException {
		rally = new RallyOperations();
		jira = new JiraRestOperations();
	}

	public static void main(String[] args) throws URISyntaxException,
			Exception {
		RallyToJiraSetup1 rallyToJira = new RallyToJiraSetup1();
		rallyToJira.process();

	}

	private void process() throws Exception {
		JsonObject project = rally.getProjectByName(RallyToJiraSetup3.PROJECT).get(0).getAsJsonObject();
		createRallyJiraUserMap(project);
	}

	private void createRallyJiraUserMap(JsonObject project) throws IOException {
		Set<String> allUsers = getAllUsers(project);
		System.out.println(allUsers.size());
		BufferedWriter bw = new BufferedWriter(new FileWriter("mappings/jira_rally_user_mapping_" + Utils.getJsonObjectName(project).replaceAll(" ", "_")));
		bw.write("\nRally ObjectID\tRally DisplayName\tJira DisplayName\tRally UserName\tJira UserName\tDisabled\tMatch");
		for (String rallyUserObjectID : allUsers) {
			JsonObject rallyUser = rally.findRallyObjectByObjectID(project, RallyObject.USER, rallyUserObjectID);
			if(Utils.isEmpty(rallyUser)){
				continue;
			}
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
			}
			bw.flush();

		}
		bw.close();
	}

	private Set<String> getAllUsers(JsonObject project) throws IOException {
		Set<String> allUsers = new HashSet<String>();
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

	private void addOwnerToSet(Set<String> allUsers, JsonElement jeRallyWorkProduct, JsonObject project) throws IOException {
		JsonObject rallyWorkProduct = jeRallyWorkProduct.getAsJsonObject();
		if (isNotJsonNull(rallyWorkProduct, "Owner")) {
			JsonObject owner = rallyWorkProduct.get("Owner").getAsJsonObject();
			if (isNotJsonNull(owner, "ObjectID")) {
				allUsers.add(owner.get("ObjectID").getAsString());
			}
		}
	}

	private boolean isNotJsonNull(JsonObject rallyWorkProduct, String field) {
		return !isJsonNull(rallyWorkProduct, field);
	}

	private boolean isJsonNull(JsonObject rallyWorkProduct, String field) {
		if (Utils.isEmpty(rallyWorkProduct) || Utils.isEmpty(rallyWorkProduct.get(field)) || rallyWorkProduct.get(field).isJsonNull()) {
			return true;
		}
		if (rallyWorkProduct.get(field).isJsonPrimitive()) {
			return Utils.isEmpty(rallyWorkProduct.get(field).getAsString());
		}
		return false;
	}

}
