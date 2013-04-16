package com.ceb.rallytojira;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
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
	Map<String, String> releaseVersionMap = new HashMap<String, String>();
	int counter = 0;
	int limit = 30000000;
	int progress = 0;
	public static String PROJECT = "Next Generation Web";

	public RallyToJiraSetup3() throws URISyntaxException {
		rally = new RallyOperations();
		jira = new JiraRestOperations();
	}

	public static void main(String[] args) throws URISyntaxException,
			Exception {
		RallyToJiraSetup3 rallyToJira = new RallyToJiraSetup3();
		rallyToJira.process();

	}

	private void process() throws Exception {
		// JsonObject project =
		// rally.getProjectByName(PROJECT).get(0).getAsJsonObject();
		// deleteDuplicates(project);
		// createReleases(project);
		//
		// String[] projectNames = new String[] { "EXBD",
		// "LE Move to CWS Integrated Backend", "Search Services",
		// "CMS Project", "Discussions",
		// "Iconoculture", "iMaps", "Infrastructure", "Middle Market - RTE",
		// "NGW Registration and Peer Networking", "SFDC", "Site Catalyst",
		// "Test Automation", "Test Automation - Web V2",
		// "Test Automation - Workspace", "Web Expansion", "Workspace",
		// "Next Generation Web" };
		// String[] projectNames = new String[] { "Site Catalyst",
		// "Test Automation", "Test Automation - Web V2",
		// "Test Automation - Workspace", "Web Expansion", "Workspace",
		// "Next Generation Web" };

		String[] projectNames = new String[] { "Support/Development" };
		for (String projectName : projectNames) {
			JsonObject project = rally.getProjectByName(projectName).get(0).getAsJsonObject();
			Utils.reinitialize();
			System.out.println(projectName + ", " + Utils.getJsonObjectName(project) + ", " + Utils.getJiraProjectNameForRallyProject(project));
			// try {
			// while (deleteAllIssuesInJira(project)) {
			// System.out.println("deleting ...");
			// }
			// } catch (Exception e) {
			//
			// }
			//deleteDuplicates(project);
			createReleases(project);

		}

	}

	private void deleteDuplicates(JsonObject project) throws IOException {

		JsonArray userStories = rally.getRallyObjectsForProject(project, RallyObject.USER_STORY);
		progress = 0;
		int totalUserStories = userStories.size();
		for (JsonElement jeUserStory : userStories) {
			System.out.println("**USER STORY " + progress++ + " of " + totalUserStories + " *************************************");
			JsonObject userStory = jeUserStory.getAsJsonObject();
			deleteDuplicates(project, userStory.get("FormattedID").getAsString());
		}
		JsonArray defects = rally.getRallyObjectsForProject(project, RallyObject.DEFECT);
		progress = 0;
		int totalDefects = defects.size();
		for (JsonElement jeDefect : defects) {
			System.out.println("**DEFECT " + progress++ + " of " + totalDefects + " *************************************");
			JsonObject defect = jeDefect.getAsJsonObject();
			deleteDuplicates(project, defect.get("FormattedID").getAsString());
		}
		JsonArray tasks = rally.getRallyObjectsForProject(project, RallyObject.TASK);
		progress = 0;
		int totalTasks = tasks.size();
		for (JsonElement jeTask : tasks) {
			System.out.println("**TASK " + progress++ + " of " + totalTasks + " *************************************");
			JsonObject task = jeTask.getAsJsonObject();
			deleteDuplicates(project, task.get("FormattedID").getAsString());
		}
	}

	private void deleteDuplicates(JsonObject project, String rallyFormattedId) throws IOException {
		jira.deleteDuplicateIssue(Utils.getJiraProjectNameForRallyProject(project), rallyFormattedId);

	}

	private void createReleases(JsonObject project) throws Exception {
		JsonArray releases = rally.getReleasesForProject(project);

		for (JsonElement release : releases) {
			String jiraVersionId = jira.createVersion(project, release.getAsJsonObject());
			releaseVersionMap.put(release.getAsJsonObject().get("ObjectID").getAsString(), jiraVersionId);
		}

		createTasks(project);
		createDefects(project);
		createUserStories(project);

	}

	private void createTasks(JsonObject project) throws Exception {
		JsonArray tasks = rally.getRallyObjectsForProject(project, RallyObject.TASK);
		progress = 0;
		int totalTasks = tasks.size();
		for (JsonElement jeTask : tasks) {
			System.out.println("**TASK " + progress++ + " of " + totalTasks + " *************************************");
			JsonObject task = jeTask.getAsJsonObject();
			findOrCreateIssueInJiraForTask(project, task);
			// if (doBreak()) {
			// break;
			// }
		}
	}

	private void createDefects(JsonObject project) throws Exception {
		JsonArray defects = rally.getRallyObjectsForProject(project, RallyObject.DEFECT);
		progress = 0;
		int totalDefects = defects.size();
		for (JsonElement jeDefect : defects) {
			System.out.println("**DEFECT " + progress++ + " of " + totalDefects + " *************************************");

			JsonObject defect = jeDefect.getAsJsonObject();
			findOrCreateIssueInJiraForDefect(project, defect.get("FormattedID").getAsString());
			// if (doBreak()) {
			// break;
			// }
		}
	}

	private void createUserStories(JsonObject project) throws Exception {
		JsonArray userStories = rally.getRallyObjectsForProject(project, RallyObject.USER_STORY);
		progress = 0;
		int totalUserStories = userStories.size();
		for (JsonElement jeUserStory : userStories) {
			System.out.println("**USER STORY " + progress++ + " of " + totalUserStories + " *************************************");
			JsonObject userStory = jeUserStory.getAsJsonObject();
			findOrCreateIssueInJiraForUserStory(project, userStory.get("FormattedID").getAsString());
			// if (doBreak()) {
			// break;
			// }
		}
	}

	private void findOrCreateIssueInJiraForTask(JsonObject project, JsonObject task) throws Exception {
		String rallyFormattedId = task.get("FormattedID").getAsString();
		JsonObject jiraIssue = jira.findIssueByRallyFormattedID(Utils.getJiraProjectNameForRallyProject(project), rallyFormattedId);
		if (Utils.isEmpty(jiraIssue)) {
			String jiraVersionId = getJiraVersionIdForRelease(task);
			if (task.get("WorkProduct") == null || task.get("WorkProduct").isJsonNull()) {
				jiraIssue = createIssueInJiraAndProcessSpecialItems(project, jiraVersionId, task, RallyObject.TASK, "Task");
			} else {
				JsonObject rallyTaskWorkProduct = task.get("WorkProduct").getAsJsonObject();
				String workProductType = rallyTaskWorkProduct.get("_type").getAsString();
				String workProductFormattedID = rallyTaskWorkProduct.get("FormattedID").getAsString();
				if (workProductType.equalsIgnoreCase("hierarchicalrequirement")) {
					JsonObject jiraParentIssue = findOrCreateIssueInJiraForUserStory(project, workProductFormattedID);
					addParentFields(task, jiraParentIssue);
					jiraIssue = createIssueInJiraAndProcessSpecialItems(project, jiraVersionId, task, RallyObject.TASK, "Sub-task");
				} else {
					if (workProductType.equalsIgnoreCase("defect")) {
						JsonObject jiraParentIssue = findOrCreateIssueInJiraForDefect(project, workProductFormattedID);
						addParentFields(task, jiraParentIssue);
						jiraIssue = createIssueInJiraAndProcessSpecialItems(project, jiraVersionId, task, RallyObject.TASK, "Sub-task");
					} else {
						jiraIssue = createIssueInJiraAndProcessSpecialItems(project, jiraVersionId, task, RallyObject.TASK, "Task");
					}

				}
			}
		}
	}

	private JsonObject findOrCreateIssueInJiraForUserStory(JsonObject project, String userStoryFormattedID) throws Exception {
		JsonObject jiraIssue = jira.findIssueByRallyFormattedID(Utils.getJiraProjectNameForRallyProject(project), userStoryFormattedID);
		if (Utils.isEmpty(jiraIssue)) {
			JsonObject userStory = rally.findRallyObjectByFormatteID(project, userStoryFormattedID, RallyObject.USER_STORY);
			if (Utils.isNotEmpty(userStory)) {
				String jiraVersionId = getJiraVersionIdForRelease(userStory);
				if (isJsonNull(userStory, "Parent")) {
					jiraIssue = createIssueInJiraAndProcessSpecialItems(project, jiraVersionId, userStory, RallyObject.USER_STORY, "Story");
				} else {
					String parentUserStoryFormattedID = userStory.get("Parent").getAsJsonObject().get("FormattedID").getAsString();
					JsonObject jiraParentIssue = findOrCreateIssueInJiraForUserStory(project, parentUserStoryFormattedID);
					addParentFields(userStory, jiraParentIssue);
					jiraIssue = createIssueInJiraAndProcessSpecialItems(project, jiraVersionId, userStory, RallyObject.USER_STORY, "Sub-story");
				}
			}
		}
		return jiraIssue;
	}

	private JsonObject findOrCreateIssueInJiraForDefect(JsonObject project, String defectFormattedID) throws Exception {
		JsonObject jiraIssue = jira.findIssueByRallyFormattedID(Utils.getJiraProjectNameForRallyProject(project), defectFormattedID);

		if (Utils.isEmpty(jiraIssue)) {
			JsonObject defect = rally.findRallyObjectByFormatteID(project, defectFormattedID, RallyObject.DEFECT);
			String jiraVersionId = getJiraVersionIdForRelease(defect);
			if (isJsonNull(defect, "Requirement")) {
				jiraIssue = createIssueInJiraAndProcessSpecialItems(project, jiraVersionId, defect, RallyObject.DEFECT, "Bug");
			} else {
				String parentDefectFormattedID = defect.get("Requirement").getAsJsonObject().get("FormattedID").getAsString();
				JsonObject jiraParentIssue = findOrCreateIssueInJiraForUserStory(project, parentDefectFormattedID);
				if (Utils.isNotEmpty(jiraParentIssue)) {
					addParentFields(defect, jiraParentIssue);
					jiraIssue = createIssueInJiraAndProcessSpecialItems(project, jiraVersionId, defect, RallyObject.DEFECT, "Defect");
				} else {
					jiraIssue = createIssueInJiraAndProcessSpecialItems(project, jiraVersionId, defect, RallyObject.DEFECT, "Bug");
				}
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
		boolean stateChanged = processStatus(project, rallyWorkProduct, jiraIssue);
		updateAssignee(project, rallyWorkProduct, jiraIssue);
		updateCreateAndUpdateDate(project, rallyWorkProduct, jiraIssue, stateChanged);
		return jiraIssue;
	}

	private void updateCreateAndUpdateDate(JsonObject project, JsonObject rallyWorkProduct, JsonObject jiraIssue, boolean stateChanged) throws Exception {
		jira.updateDatesInDatabase(Utils.getJsonObjectName(project), jiraIssue.get("id").getAsString(), rallyWorkProduct, stateChanged);
	}

	private void updateAssignee(JsonObject project, JsonObject rallyWorkProduct, JsonObject jiraIssue) throws Exception {
		if (isNotJsonNull(rallyWorkProduct, "Owner") && isNotJsonNull(rallyWorkProduct.get("Owner").getAsJsonObject(), "ObjectID")) {
			jira.updateIssueAssignee(Utils.getJsonObjectName(project), jiraIssue.get("key").getAsString(), rallyWorkProduct.get("Owner").getAsJsonObject().get("ObjectID").getAsString());
		} else {
			if (isNotJsonNull(rallyWorkProduct, "rally-parent-owner")) {
				JsonElement jeRallyOwner = rallyWorkProduct.get("rally-parent-owner");
				if (jeRallyOwner != null && jeRallyOwner.isJsonObject()) {
					JsonObject rallyOwner = jeRallyOwner.getAsJsonObject();
					if (isNotJsonNull(rallyOwner.getAsJsonObject(), "ObjectID")) {
						jira.updateIssueAssignee(Utils.getJsonObjectName(project), jiraIssue.get("key").getAsString(), rallyOwner.get("ObjectID").getAsString());
					}
				}
			}
		}
	}

	private boolean processStatus(JsonObject project, JsonObject rallyWorkProduct, JsonObject jiraIssue) throws Exception {
		if (isNotJsonNull(rallyWorkProduct, "ScheduleState")) {
			String rallyStatus = rallyWorkProduct.get("ScheduleState").getAsString();
			String jiraTransitionId = Utils.getJiraTransitionId(Utils.getJsonObjectName(project), rallyStatus);
			if ("1".equals(jiraTransitionId)) {
				return false;
			}
			jira.updateWorkflowStatus(jiraIssue.get("key").getAsString(), jiraTransitionId, rallyStatus);
		}
		if (isNotJsonNull(rallyWorkProduct, "State")) {
			String rallyStatus = rallyWorkProduct.get("State").getAsString();
			String jiraTransitionId = Utils.getJiraTransitionId(Utils.getJsonObjectName(project), rallyStatus);
			if (jiraTransitionId.equals("1")) {
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

	private JsonElement getParentKey(JsonObject jiraParentIssue) {
		JsonElement parentsParent = jiraParentIssue.get("parent");
		if (parentsParent == null || isJsonNull(parentsParent.getAsJsonObject(), "key")) {
			return jiraParentIssue.get("key");
		}
		return parentsParent.getAsJsonObject().get("key");
	}

	private void addParentFields(JsonObject rallyWorkProduct, JsonObject jiraParentIssue) {
		rallyWorkProduct.add("jira-parent-key", getParentKey(jiraParentIssue));
		rallyWorkProduct.add("rally-parent-owner", jiraParentIssue.get("Owner"));
	}

	private String getJiraVersionIdForRelease(JsonObject rallyObject) {
		if (rallyObject.get("Release") != null && !rallyObject.get("Release").isJsonNull()) {
			return releaseVersionMap.get(rallyObject.get("Release").getAsJsonObject().get("ObjectID").getAsString());
		}
		return null;
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

	private boolean deleteAllIssuesInJira(JsonObject project) throws IOException {
		return jira.deleteAllIssues(project);
	}
}
