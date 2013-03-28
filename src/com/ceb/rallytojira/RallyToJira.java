package com.ceb.rallytojira;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.binary.Base64;

import com.ceb.rallytojira.domain.RallyObject;
import com.ceb.rallytojira.rest.client.Utils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class RallyToJira {

	RallyOperations rally;
	JiraOperations jira;
	Map<String, String> releaseVersionMap = new HashMap<String, String>();
	int counter = 0;
	int limit = 50000;

	public RallyToJira() throws URISyntaxException {
		rally = new RallyOperations();
		jira = new JiraOperations();
	}

	public static void main(String[] args) throws URISyntaxException,
			Exception {
		RallyToJira rallyToJira = new RallyToJira();
		rallyToJira.process();

	}

	private void deleteAllIssuesInJira(JsonObject project) throws IOException {
		jira.deleteAllIssues(project);

	}

	private void process() throws Exception {
		JsonObject project = rally.getProjectByName("Workspace").get(0).getAsJsonObject();
		// deleteAllIssuesInJira(project);
		createReleases(project);
	}

	private void createReleases(JsonObject project) throws Exception {
		JsonArray releases = rally.getReleasesForProject(project);

		for (JsonElement release : releases) {
			String jiraVersionId = jira.createVersion(project, release.getAsJsonObject());
			releaseVersionMap.put(release.getAsJsonObject().get("ObjectID").getAsString(), jiraVersionId);
		}

		createRallyJiraUserMap(project);
		// createTasks(project);
		// createDefects(project);
		// createUserStories(project);

	}

	private void createRallyJiraUserMap(JsonObject project) throws IOException {
		Set<JsonObject> allUsers = getAllUsers(project);
		System.out.println(allUsers.size());
		BufferedWriter bw = new BufferedWriter(new FileWriter("mappings/jira_rally_user_mapping_" + Utils.getJsonObjectName(project).replaceAll(" ", "_") + ".csv"));
		for (JsonObject rallyUser : allUsers) {
			System.out.println(rallyUser.get("UserName").getAsString());
			String rallyLastname = isNotJsonNull(rallyUser, "LastName")?rallyUser.get("LastName").getAsString():"";
			String rallyFirstname = isNotJsonNull(rallyUser, "FirstName")?rallyUser.get("FirstName").getAsString():"";
			String rallyDisplayName = rallyLastname + ", " + rallyFirstname;
			try {
				JsonObject jiraUser = jira.findJiraUser(rallyUser);
				String jiraDisplayName = isNotJsonNull(jiraUser, "displayName") ? jiraUser.get("displayName").getAsString() : "";
				String jiraUserName = jiraUser.get("name").getAsString();
				if (rallyDisplayName.equals(jiraDisplayName)) {
					bw.write("\n" + rallyDisplayName + "," + jiraDisplayName + "," + jiraUserName + "," + rallyUser.get("Disabled").getAsString() + ",Y");
				} else {
					bw.write("\n" + rallyDisplayName + "," + jiraDisplayName + "," + jiraUserName + "," + rallyUser.get("Disabled").getAsString() + ",N");
				}
			} catch (Exception ex) {
				bw.write("\n" + rallyDisplayName + "," + "" + "," + "" + "," + rallyUser.get("Disabled").getAsString() + ",Y");
			}
			bw.flush();
			if (doBreak()) {
				break;
			}
		}
		bw.close();
	}

	private Set<JsonObject> getAllUsers(JsonObject project) throws IOException {
		Set<JsonObject> allUsers = new HashSet<JsonObject>();
		JsonArray tasks = rally.getRallyObjectsForProject(project, RallyObject.TASK);
		for (JsonElement jeTask : tasks) {
			addOwnerToSet(allUsers, jeTask, project);
			if (doBreak()) {
				break;
			}
		}
		JsonArray defects = rally.getRallyObjectsForProject(project, RallyObject.DEFECT);
		for (JsonElement jeDefect : defects) {
			addOwnerToSet(allUsers, jeDefect, project);
			if (doBreak()) {
				break;
			}
		}
		JsonArray userStories = rally.getRallyObjectsForProject(project, RallyObject.USER_STORY);
		for (JsonElement jeUserStory : userStories) {
			addOwnerToSet(allUsers, jeUserStory, project);
			if (doBreak()) {
				break;
			}
		}
		return allUsers;
	}

	private void addOwnerToSet(Set<JsonObject> allUsers, JsonElement jeRallyWorkProduct, JsonObject project) throws IOException {
		JsonObject rallyWorkProduct = jeRallyWorkProduct.getAsJsonObject();
		if (isNotJsonNull(rallyWorkProduct, "Owner")) {
			JsonObject owner = rallyWorkProduct.get("Owner").getAsJsonObject();
			if (isNotJsonNull(owner, "ObjectID")) {
				allUsers.add(rally.findRallyObjectByObjectID(project, RallyObject.USER, owner.get("ObjectID").getAsString()));
			}
		}
	}

	private void createTasks(JsonObject project) throws Exception {
		JsonArray tasks = rally.getRallyObjectsForProject(project, RallyObject.TASK);
		for (JsonElement jeTask : tasks) {
			JsonObject task = jeTask.getAsJsonObject();
			findOrCreateIssueInJiraForTask(project, task);
			if (doBreak()) {
				break;
			}
		}
	}

	private boolean doBreak() {
		counter++;
		if (counter > limit) {
			System.out.println(counter);
			counter = 0;
			return true;
		}
		return false;
	}

	private void findOrCreateIssueInJiraForTask(JsonObject project, JsonObject task) throws Exception {
		String rallyFormattedId = task.get("FormattedID").getAsString();
		JsonObject jiraIssue = jira.findIssueByRallyFormattedID(rallyFormattedId);
		String jiraVersionId = getJiraVersionIdForRelease(task);
		if (Utils.isEmpty(jiraIssue)) {

			if (task.get("WorkProduct") == null || task.get("WorkProduct").isJsonNull()) {
				jiraIssue = createIssueInJiraAndProcessSpecialItems(project, jiraVersionId, task, RallyObject.TASK, "Task");
			} else {
				JsonObject rallyTaskWorkProduct = task.get("WorkProduct").getAsJsonObject();
				String workProductType = rallyTaskWorkProduct.get("_type").getAsString();
				String workProductFormattedID = rallyTaskWorkProduct.get("FormattedID").getAsString();
				if (workProductType.equalsIgnoreCase("hierarchicalrequirement")) {
					rallyTaskWorkProduct = rally.findRallyObjectByFormatteID(project, workProductFormattedID, RallyObject.USER_STORY);
					JsonObject jiraParentIssue = findOrCreateIssueInJiraForUserStory(project, rallyTaskWorkProduct);
					addParentFields(task, jiraParentIssue, rallyTaskWorkProduct);
					jiraIssue = createIssueInJiraAndProcessSpecialItems(project, jiraVersionId, task, RallyObject.TASK, "Sub-task");
				} else {
					if (workProductType.equalsIgnoreCase("defect")) {
						rallyTaskWorkProduct = rally.findRallyObjectByFormatteID(project, workProductFormattedID, RallyObject.DEFECT);
						JsonObject jiraParentIssue = findOrCreateIssueInJiraForDefect(project, rallyTaskWorkProduct);
						addParentFields(task, jiraParentIssue, rallyTaskWorkProduct);
						jiraIssue = createIssueInJiraAndProcessSpecialItems(project, jiraVersionId, task, RallyObject.TASK, "Sub-task");
					} else {
						jiraIssue = createIssueInJiraAndProcessSpecialItems(project, jiraVersionId, task, RallyObject.TASK, "Task");
					}

				}
			}

		}
	}

	private JsonElement getParentKey(JsonObject jiraParentIssue) {
		JsonElement parentsParent = jiraParentIssue.get("parent");
		if (parentsParent == null || parentsParent.isJsonNull() || parentsParent.getAsJsonObject().get("key") == null || parentsParent.getAsJsonObject().get("key").isJsonNull()) {
			return jiraParentIssue.get("key");
		}
		return parentsParent.getAsJsonObject().get("key");
	}

	private void createDefects(JsonObject project) throws Exception {
		JsonArray defects = rally.getRallyObjectsForProject(project, RallyObject.DEFECT);
		for (JsonElement jeDefect : defects) {
			JsonObject defect = jeDefect.getAsJsonObject();
			findOrCreateIssueInJiraForDefect(project, defect);
			if (doBreak()) {
				break;
			}
		}
	}

	private JsonObject findOrCreateIssueInJiraForDefect(JsonObject project, JsonObject defect) throws Exception {
		String rallyFormattedId = defect.get("FormattedID").getAsString();
		JsonObject jiraIssue = jira.findIssueByRallyFormattedID(rallyFormattedId);
		String jiraVersionId = getJiraVersionIdForRelease(defect);
		if (Utils.isEmpty(jiraIssue)) {
			if (defect.get("Requirement") == null || defect.get("Requirement").isJsonNull()) {
				jiraIssue = createIssueInJiraAndProcessSpecialItems(project, jiraVersionId, defect, RallyObject.DEFECT, "Bug");
			} else {
				JsonObject rallyDefectUserStory = rally.findRallyObjectByFormatteID(project, defect.get("Requirement").getAsJsonObject().get("FormattedID").getAsString(), RallyObject.USER_STORY);
				JsonObject jiraParentIssue = findOrCreateIssueInJiraForUserStory(project, rallyDefectUserStory);
				addParentFields(defect, jiraParentIssue, rallyDefectUserStory);
				jiraIssue = createIssueInJiraAndProcessSpecialItems(project, jiraVersionId, defect, RallyObject.DEFECT, "Defect");
			}
		}
		return jiraIssue;
	}

	private JsonObject createIssueInJiraAndProcessSpecialItems(JsonObject project, String jiraVersionId, JsonObject rallyWorkProduct, RallyObject workProductType, String jiraIssueType)
			throws Exception {
		JsonObject jiraIssue = jira.createIssueInJira(project, jiraVersionId, rallyWorkProduct, workProductType, jiraIssueType);
		processAttachments(project, rallyWorkProduct, jiraIssue);
		processNotes(project, rallyWorkProduct, jiraIssue);
		processWorkLog(project, rallyWorkProduct, jiraIssue);
		processStatus(project, rallyWorkProduct, jiraIssue);
		updateAssignee(project, rallyWorkProduct, jiraIssue);
		return jiraIssue;
	}

	private void updateAssignee(JsonObject project, JsonObject rallyWorkProduct, JsonObject jiraIssue) throws Exception {
		if (isNotJsonNull(rallyWorkProduct, "Owner") && isNotJsonNull(rallyWorkProduct.get("Owner").getAsJsonObject(), "_refObjectName")) {
			jira.updateIssueAssignee(Utils.getJsonObjectName(project), jiraIssue.get("key").getAsString(), rallyWorkProduct.get("Owner").getAsJsonObject().get("_refObjectName").getAsString());
		} else {
			if (isNotJsonNull(rallyWorkProduct, "rally-parent-owner")) {
				JsonElement jeRallyOwner = rallyWorkProduct.get("rally-parent-owner");
				if (jeRallyOwner != null && jeRallyOwner.isJsonObject()) {
					JsonObject rallyOwner = jeRallyOwner.getAsJsonObject();
					if (isNotJsonNull(rallyOwner.getAsJsonObject(), "_refObjectName")) {
						jira.updateIssueAssignee(Utils.getJsonObjectName(project), jiraIssue.get("key").getAsString(), rallyOwner.get("_refObjectName").getAsString());
					}
				}
			}
		}
	}

	private void processStatus(JsonObject project, JsonObject rallyWorkProduct, JsonObject jiraIssue) throws Exception {
		if (isNotJsonNull(rallyWorkProduct, "State")) {
			jira.updateWorkflowStatus(Utils.getJsonObjectName(project), jiraIssue.get("key").getAsString(), rallyWorkProduct.get("State").getAsString());
		}
		if (isNotJsonNull(rallyWorkProduct, "ScheduleState")) {
			jira.updateWorkflowStatus(Utils.getJsonObjectName(project), jiraIssue.get("key").getAsString(), rallyWorkProduct.get("ScheduleState").getAsString());
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

	private void processWorkLog(JsonObject project, JsonObject rallyWorkProduct, JsonObject jiraIssue) throws Exception {
		if (isNotJsonNull(rallyWorkProduct, "Actuals")) {
			String actualtime = rallyWorkProduct.get("Actuals").getAsString();
			try {
				int at = Integer.parseInt(actualtime);
				if (at > 0) {
					jira.logWork(jiraIssue.get("key").getAsString(), actualtime);
				}
			} catch (NumberFormatException ex) {
			}
		}

	}

	private void processNotes(JsonObject project, JsonObject rallyWorkProduct, JsonObject jiraIssue) throws Exception {
		if (isNotJsonNull(rallyWorkProduct, "Notes")) {
			jira.addComment(jiraIssue.get("key").getAsString(), rallyWorkProduct.get("Notes").getAsString());
		}

	}

	private void processAttachments(JsonObject project, JsonObject rallyWorkProduct, JsonObject jiraIssue) throws Exception {
		JsonElement jeAattachments = rallyWorkProduct.get("Attachments");
		if (!Utils.isEmpty(jeAattachments)) {
			JsonArray attachments = jeAattachments.getAsJsonArray();
			for (JsonElement attachment : attachments) {
				uploadAttachmentToJira(project, attachment.getAsJsonObject(), jiraIssue);
			}
		}

	}

	private void uploadAttachmentToJira(JsonObject project, JsonObject attachment, JsonObject jiraIssue) throws Exception {
		attachment = rally.findRallyObjectByObjectID(project, RallyObject.ATTACHMENT, attachment.get("ObjectID").getAsString());
		String fileName = attachment.get("Name").getAsString();
		String description = isNotJsonNull(attachment, "Description") ? attachment.get("Description").getAsString() : "";
		JsonObject attachmentContent = jira.getRallyAttachment(attachment.get("Content").getAsJsonObject().get("_ref").getAsString());
		String base64Content = attachmentContent.get("AttachmentContent").getAsJsonObject().get("Content").getAsString();
		byte[] decodedString = Base64.decodeBase64(base64Content);
		File f = new File("/RallyAttachments/" + fileName);
		FileOutputStream outFile = new FileOutputStream(f);
		outFile.write(decodedString);
		outFile.close();
		jira.attachFile(jiraIssue.get("key").getAsString(), f);
		f.delete();
		if (Utils.isNotEmpty(description)) {
			jira.addComment(jiraIssue.get("key").getAsString(), description);
		}

	}

	private void createUserStories(JsonObject project) throws Exception {
		JsonArray userStories = rally.getRallyObjectsForProject(project, RallyObject.USER_STORY);

		for (JsonElement jeUserStory : userStories) {
			JsonObject userStory = jeUserStory.getAsJsonObject();
			findOrCreateIssueInJiraForUserStory(project, userStory);
			if (doBreak()) {
				break;
			}
		}
	}

	private JsonObject findOrCreateIssueInJiraForUserStory(JsonObject project, JsonObject userStory) throws Exception {
		String rallyFormattedId = userStory.get("FormattedID").getAsString();
		JsonObject jiraIssue = jira.findIssueByRallyFormattedID(rallyFormattedId);
		String jiraVersionId = getJiraVersionIdForRelease(userStory);
		if (Utils.isEmpty(jiraIssue)) {
			if (userStory.get("Parent") == null || userStory.get("Parent").isJsonNull()) {
				jiraIssue = createIssueInJiraAndProcessSpecialItems(project, jiraVersionId, userStory, RallyObject.USER_STORY, "Story");
			} else {
				JsonObject rallyParentUserStory = rally.findRallyObjectByFormatteID(project, userStory.get("Parent").getAsJsonObject().get("FormattedID").getAsString(), RallyObject.USER_STORY);
				JsonObject jiraParentIssue = findOrCreateIssueInJiraForUserStory(project, rallyParentUserStory);
				addParentFields(userStory, jiraParentIssue, rallyParentUserStory);
				jiraIssue = createIssueInJiraAndProcessSpecialItems(project, jiraVersionId, userStory, RallyObject.USER_STORY, "Sub-story");
			}
		}
		return jiraIssue;
	}

	private void addParentFields(JsonObject rallyWorkProduct, JsonObject jiraParentIssue, JsonObject rallyParentWorkProduct) {
		rallyWorkProduct.add("jira-parent-key", getParentKey(jiraParentIssue));
		rallyWorkProduct.add("rally-parent-owner", rallyParentWorkProduct.get("Owner"));
	}

	private String getJiraVersionIdForRelease(JsonObject rallyObject) {
		if (rallyObject.get("Release") != null && !rallyObject.get("Release").isJsonNull()) {
			return releaseVersionMap.get(rallyObject.get("Release").getAsJsonObject().get("ObjectID").getAsString());
		}
		return null;
	}
}
