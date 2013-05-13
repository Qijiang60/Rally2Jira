package com.ceb.rallytojira;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;

import com.ceb.rallytojira.domain.RallyObject;
import com.ceb.rallytojira.rest.client.Utils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class RallyToJiraSetup3 {

	RallyOperations rally;
	JiraRestOperations jira;
	JiraSoapOperations jiraSoap;
	Map<String, String> releaseVersionMap = new HashMap<String, String>();
	int counter = 0;
	int limit = 30000000;
	int progress = 0;

	public RallyToJiraSetup3() throws Exception {
		rally = new RallyOperations();
		jira = new JiraRestOperations();
		jiraSoap = new JiraSoapOperations();
	}

	public static void main(String[] args) throws URISyntaxException,
			Exception {
		RallyToJiraSetup3 rallyToJira = new RallyToJiraSetup3();
		rallyToJira.process();

	}

	private void process() throws Exception {

		Map<String, List<String>> projectMapping = Utils.getProjectMapping();
		JsonArray workspaces = rally.getAllWorkspaces();

		for (JsonElement workspaceEle : workspaces) {
			JsonObject workspace = workspaceEle.getAsJsonObject();
			JsonArray projects = workspace.get("Projects").getAsJsonArray();
			for (JsonElement projEle : projects) {
				JsonObject project = rally.getObjectFromRef(projEle);
				String key = Utils.getKeyForWorkspaceAndProject(workspace, project);
				if (projectMapping.containsKey(key)) {
					System.out.println(key);
					rally.updateDefaultWorkspace(workspace, project);
					createProject(workspace, project);
					// RallyToJiraSetup1.createRallyJiraUserMap(workspace,
					// project, rally, jira);
//					RallyToJiraSetup1.addUsersToProjectRole(Utils.getJiraProjectKeyForRallyProject(workspace,
//							project));
					createReleases(workspace, project);
					createTasks(workspace, project);
//					createDefects(workspace, project);
					createUserStories(workspace, project);
				}
			}
		}
	}

	private void createProject(JsonObject workspace, JsonObject project) throws IOException {
		jiraSoap.createProject(workspace, project);

	}

	private void deleteDuplicates(JsonObject workspace, JsonObject project) throws IOException {

		JsonArray userStories = rally.getRallyObjectsForProject(project, RallyObject.USER_STORY);
		progress = 0;
		int totalUserStories = userStories.size();
		for (JsonElement jeUserStory : userStories) {
			System.out.println("**USER STORY " + progress++ + " of " + totalUserStories + " *************************************");
			JsonObject userStory = jeUserStory.getAsJsonObject();
			deleteDuplicates(workspace, project, userStory.get("FormattedID").getAsString());
		}
		JsonArray defects = rally.getRallyObjectsForProject(project, RallyObject.DEFECT);
		progress = 0;
		int totalDefects = defects.size();
		for (JsonElement jeDefect : defects) {
			System.out.println("**DEFECT " + progress++ + " of " + totalDefects + " *************************************");
			JsonObject defect = jeDefect.getAsJsonObject();
			deleteDuplicates(workspace, project, defect.get("FormattedID").getAsString());
		}
		JsonArray tasks = rally.getRallyObjectsForProject(project, RallyObject.TASK);
		progress = 0;
		int totalTasks = tasks.size();
		for (JsonElement jeTask : tasks) {
			System.out.println("**TASK " + progress++ + " of " + totalTasks + " *************************************");
			JsonObject task = jeTask.getAsJsonObject();
			deleteDuplicates(workspace, project, task.get("FormattedID").getAsString());
		}
	}

	private void deleteDuplicates(JsonObject workspace, JsonObject project, String rallyFormattedId) throws IOException {
		jira.deleteDuplicateIssue(Utils.getJiraProjectKeyForRallyProject(workspace, project), rallyFormattedId);

	}

	private void createReleases(JsonObject workspace, JsonObject project) throws Exception {
		JsonArray releases = rally.getRallyObjectsForProject(project, RallyObject.RELEASE);
		for (JsonElement release : releases) {
			if (Utils.migrateRelease(workspace, project, release.getAsJsonObject())) {
				String jiraVersionId = jira.createVersion(workspace, project, release.getAsJsonObject());
				releaseVersionMap.put(release.getAsJsonObject().get("_ref").getAsString(), jiraVersionId);
			}
		}
	}

	private void createTasks(JsonObject workspace, JsonObject project) throws Exception {
		JsonArray tasks = rally.getRallyObjectsForProject(project, RallyObject.TASK);
		progress = 0;
		int totalTasks = tasks.size();
		System.out.println(totalTasks);
		for (JsonElement jeTask : tasks) {
			System.out.println("**TASK " + progress++ + " of " + totalTasks + " *************************************");
			JsonObject task = jeTask.getAsJsonObject();
			findOrCreateIssueInJiraForTask(workspace, project, task);
		}
	}

	private void createDefects(JsonObject workspace, JsonObject project) throws Exception {
		JsonArray defects = rally.getRallyObjectsForProject(project, RallyObject.DEFECT);
		progress = 0;
		int totalDefects = defects.size();
		for (JsonElement jeDefect : defects) {
			System.out.println("**DEFECT " + progress++ + " of " + totalDefects + " *************************************");

			JsonObject defect = jeDefect.getAsJsonObject();
			findOrCreateIssueInJiraForDefect(workspace, project, defect.get("FormattedID").getAsString());
		}
	}

	private void createUserStories(JsonObject workspace, JsonObject project) throws Exception {
		JsonArray userStories = rally.getRallyObjectsForProject(project, RallyObject.USER_STORY);
		progress = 0;
		int totalUserStories = userStories.size();
		for (JsonElement jeUserStory : userStories) {
			System.out.println("**USER STORY " + progress++ + " of " + totalUserStories + " *************************************");
			JsonObject userStory = jeUserStory.getAsJsonObject();
			findOrCreateIssueInJiraForUserStory(workspace, project, userStory.get("FormattedID").getAsString());
		}
	}

	private void findOrCreateIssueInJiraForTask(JsonObject workspace, JsonObject project, JsonObject task) throws Exception {
		String rallyFormattedId = task.get("FormattedID").getAsString();
		JsonObject jiraIssue = jira.findIssueByRallyFormattedID(Utils.getJiraProjectKeyForRallyProject(workspace, project), rallyFormattedId);
		if (Utils.isEmpty(jiraIssue)) {
			String jiraVersionId = getJiraVersionIdForRelease(task);
			if ("DO_NOT_MIGRATE".equals(jiraVersionId)) {
				return;
			}
			if (task.get("WorkProduct") == null || task.get("WorkProduct").isJsonNull()) {
				jiraIssue = createIssueInJiraAndProcessSpecialItems(workspace, project, jiraVersionId, task, RallyObject.TASK, "Task");
			} else {
				JsonObject rallyTaskWorkProduct = task.get("WorkProduct").getAsJsonObject();
				String workProductType = rallyTaskWorkProduct.get("_type").getAsString();
				String workProductFormattedID = rallyTaskWorkProduct.get("FormattedID").getAsString();
				if (workProductType.equalsIgnoreCase("hierarchicalrequirement")) {
					JsonObject jiraParentIssue = findOrCreateIssueInJiraForUserStory(workspace, project, workProductFormattedID);
					jiraParentIssue = jira.findIssueByIssueKey(jiraParentIssue.get("key").getAsString());
					addParentFields(task, jiraParentIssue);
					jiraIssue = createIssueInJiraAndProcessSpecialItems(workspace, project, jiraVersionId, task, RallyObject.TASK, "Sub-task");
				} else {
					if (workProductType.equalsIgnoreCase("defect")) {
						JsonObject jiraParentIssue = findOrCreateIssueInJiraForDefect(workspace, project, workProductFormattedID);
						jiraParentIssue = jira.findIssueByIssueKey(jiraParentIssue.get("key").getAsString());
						addParentFields(task, jiraParentIssue);
						jiraIssue = createIssueInJiraAndProcessSpecialItems(workspace, project, jiraVersionId, task, RallyObject.TASK, "Sub-task");
					} else {
						jiraIssue = createIssueInJiraAndProcessSpecialItems(workspace, project, jiraVersionId, task, RallyObject.TASK, "Task");
					}

				}
			}
		}
	}

	private JsonObject findOrCreateIssueInJiraForUserStory(JsonObject workspace, JsonObject project, String userStoryFormattedID) throws Exception {
		JsonObject jiraIssue = jira.findIssueByRallyFormattedID(Utils.getJiraProjectKeyForRallyProject(workspace, project), userStoryFormattedID);
		if (Utils.isEmpty(jiraIssue)) {
			JsonObject userStory = rally.findRallyObjectByFormatteID(project, userStoryFormattedID, RallyObject.USER_STORY);
			if (Utils.isNotEmpty(userStory)) {
				String jiraVersionId = getJiraVersionIdForRelease(userStory);
				if ("DO_NOT_MIGRATE".equals(jiraVersionId)) {
					return null;
				}
				if (isJsonNull(userStory, "Parent")) {
					jiraIssue = createIssueInJiraAndProcessSpecialItems(workspace, project, jiraVersionId, userStory, RallyObject.USER_STORY, "Story");
				} else {
					String parentUserStoryFormattedID = userStory.get("Parent").getAsJsonObject().get("FormattedID").getAsString();
					JsonObject jiraParentIssue = findOrCreateIssueInJiraForUserStory(workspace, project, parentUserStoryFormattedID);
					jiraParentIssue = jira.findIssueByIssueKey(jiraParentIssue.get("key").getAsString());
					addParentFields(userStory, jiraParentIssue);
					jiraIssue = createIssueInJiraAndProcessSpecialItems(workspace, project, jiraVersionId, userStory, RallyObject.USER_STORY, "Sub-story");
				}
			}
		}
		return jiraIssue;
	}

	private JsonObject findOrCreateIssueInJiraForDefect(JsonObject workspace, JsonObject project, String defectFormattedID) throws Exception {
		JsonObject jiraIssue = jira.findIssueByRallyFormattedID(Utils.getJiraProjectKeyForRallyProject(workspace, project), defectFormattedID);

		if (Utils.isEmpty(jiraIssue)) {
			JsonObject defect = rally.findRallyObjectByFormatteID(project, defectFormattedID, RallyObject.DEFECT);
			String jiraVersionId = getJiraVersionIdForRelease(defect);
			if ("DO_NOT_MIGRATE".equals(jiraVersionId)) {
				return null;
			}
			if (isJsonNull(defect, "Requirement")) {
				jiraIssue = createIssueInJiraAndProcessSpecialItems(workspace, project, jiraVersionId, defect, RallyObject.DEFECT, "Bug");
			} else {
				String parentDefectFormattedID = defect.get("Requirement").getAsJsonObject().get("FormattedID").getAsString();
				JsonObject jiraParentIssue = findOrCreateIssueInJiraForUserStory(workspace, project, parentDefectFormattedID);
				if (Utils.isNotEmpty(jiraParentIssue)) {
					jiraParentIssue = jira.findIssueByIssueKey(jiraParentIssue.get("key").getAsString());
					addParentFields(defect, jiraParentIssue);
					jiraIssue = createIssueInJiraAndProcessSpecialItems(workspace, project, jiraVersionId, defect, RallyObject.DEFECT, "Defect");
				} else {
					jiraIssue = createIssueInJiraAndProcessSpecialItems(workspace, project, jiraVersionId, defect, RallyObject.DEFECT, "Bug");
				}
			}
		}
		return jiraIssue;
	}

	private JsonObject createIssueInJiraAndProcessSpecialItems(JsonObject workspace, JsonObject project, String jiraVersionId, JsonObject rallyWorkProduct, RallyObject workProductType,
			String jiraIssueType)
			throws Exception {
		JsonObject jiraIssue = jira.createIssueInJira(workspace, project, jiraVersionId, rallyWorkProduct, workProductType, jiraIssueType);
		processAttachments(project, rallyWorkProduct, jiraIssue);
		processNotes(project, rallyWorkProduct, jiraIssue);
		processWorkLog(project, rallyWorkProduct, jiraIssue);
		boolean stateChanged = processStatus(project, rallyWorkProduct, jiraIssue);
		updateAssignee(workspace, project, rallyWorkProduct, jiraIssue);
		updateCreateAndUpdateDate(project, rallyWorkProduct, jiraIssue, stateChanged);
		return jiraIssue;
	}

	private void updateCreateAndUpdateDate(JsonObject project, JsonObject rallyWorkProduct, JsonObject jiraIssue, boolean stateChanged) throws Exception {
		jira.updateDatesInDatabase(Utils.getJsonObjectName(project), jiraIssue.get("id").getAsString(), rallyWorkProduct, stateChanged);
	}

	private void updateAssignee(JsonObject workspace, JsonObject project, JsonObject rallyWorkProduct, JsonObject jiraIssue) throws Exception {
		if (isNotJsonNull(rallyWorkProduct, "Owner") && isNotJsonNull(rallyWorkProduct.get("Owner").getAsJsonObject(), "ObjectID")) {
			jira.updateIssueAssignee(Utils.getJiraProjectKeyForRallyProject(workspace, project), jiraIssue.get("key").getAsString(), rallyWorkProduct.get("Owner").getAsJsonObject().get("ObjectID")
					.getAsString(), rallyWorkProduct.get("Owner").getAsJsonObject().get("_refObjectName").getAsString());
		} else {
			if (isNotJsonNull(rallyWorkProduct, "rally-parent-owner")) {
				JsonElement jeRallyOwner = rallyWorkProduct.get("rally-parent-owner");
				if (jeRallyOwner != null && jeRallyOwner.isJsonObject()) {
					JsonObject rallyOwner = jeRallyOwner.getAsJsonObject();
					if (isNotJsonNull(rallyOwner.getAsJsonObject(), "ObjectID")) {
						jira.updateIssueAssignee(Utils.getJsonObjectName(project), jiraIssue.get("key").getAsString(), rallyOwner.get("ObjectID").getAsString(), rallyOwner.get("_refObjectName")
								.getAsString());
					}
				}
			}
		}
	}

	private boolean processStatus(JsonObject project, JsonObject rallyWorkProduct, JsonObject jiraIssue) throws Exception {
		if (isNotJsonNull(rallyWorkProduct, "ScheduleState")) {
			String rallyStatus = rallyWorkProduct.get("ScheduleState").getAsString();
			System.out.println(rallyStatus);
			String jiraTransitionId = Utils.getJiraTransitionId(rallyStatus);
			if ("1".equals(jiraTransitionId)) {
				return false;
			}
			jira.updateWorkflowStatus(jiraIssue.get("key").getAsString(), jiraTransitionId, rallyStatus);
		}
		if (isNotJsonNull(rallyWorkProduct, "State")) {
			String rallyStatus = rallyWorkProduct.get("State").getAsString();
			System.out.println(rallyStatus);
			String jiraTransitionId = Utils.getJiraTransitionId(rallyStatus);
			if (Utils.isEmpty(jiraTransitionId) || jiraTransitionId.equals("1")) {
				return false;
			}
			jira.updateWorkflowStatus(jiraIssue.get("key").getAsString(), jiraTransitionId, rallyStatus);
		}
		return true;

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
			if (Utils.isNotEmpty(rallyWorkProduct.get("Notes").getAsString())) {
				jira.addComment(jiraIssue.get("key").getAsString(), rallyWorkProduct.get("Notes").getAsString());
			}
		}

	}

	private void processAttachments(JsonObject project, JsonObject rallyWorkProduct, JsonObject jiraIssue) throws Exception {
		JsonElement jeAattachments = rallyWorkProduct.get("Attachments");
		if (isNotJsonNull(rallyWorkProduct, "Attachments")) {
			JsonArray attachments = jeAattachments.getAsJsonArray();
			for (JsonElement attachment : attachments) {
				uploadAttachmentToJira(project, attachment.getAsJsonObject(), jiraIssue);
			}
		}

	}

	private void uploadAttachmentToJira(JsonObject project, JsonObject attachment, JsonObject jiraIssue) throws Exception {
		attachment = rally.findRallyObjectByObjectID(RallyObject.ATTACHMENT, attachment.get("ObjectID").getAsString());
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

	private JsonElement getParentKey(JsonObject jiraParentIssue) {
		JsonElement parentsParent = jiraParentIssue.get("fields").getAsJsonObject().get("parent");
		if (parentsParent == null || isJsonNull(parentsParent.getAsJsonObject(), "key")) {
			return jiraParentIssue.get("key");
		}
		return parentsParent.getAsJsonObject().get("key");
	}

	private void addParentFields(JsonObject rallyWorkProduct, JsonObject jiraParentIssue) {
		rallyWorkProduct.add("jira-parent-key", getParentKey(jiraParentIssue));
		rallyWorkProduct.add("rally-parent-owner", rallyWorkProduct.get("Owner"));
	}

	private String getJiraVersionIdForRelease(JsonObject rallyObject) {
		if (rallyObject.get("Release") != null && !rallyObject.get("Release").isJsonNull()) {
			String release = releaseVersionMap.get(rallyObject.get("Release").getAsJsonObject().get("_ref").getAsString());
			if (Utils.isEmpty(release)) {
				return "DO_NOT_MIGRATE";
			}
			return release;
		}
		return null;
	}

	private boolean deleteAllIssuesInJira(JsonObject workspace, JsonObject project) throws IOException {
		return jira.deleteAllIssues(workspace, project);
	}
}
