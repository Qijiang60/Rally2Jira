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

public class RallyToJira {

	RallyOperations rally;
	JiraOperations jira;
	Map<String, String> releaseVersionMap = new HashMap<String, String>();
	int counter = 0;
	int limit = 4;

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
		JsonObject project = rally.getProjectByName("Discussions").get(0).getAsJsonObject();
		deleteAllIssuesInJira(project);
		createReleases(project);
	}

	private void createReleases(JsonObject project) throws Exception {
		JsonArray releases = rally.getReleasesForProject(project);

		for (JsonElement release : releases) {
			String jiraVersionId = jira.createVersion(project, release.getAsJsonObject());
			releaseVersionMap.put(release.getAsJsonObject().get("ObjectID").getAsString(), jiraVersionId);
		}

		// createTasks(project);
		createDefects(project);
		// createUserStories(project);

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
				jiraIssue = createIssueInJiraAndProcessAttachments(project, jiraVersionId, task, RallyObject.TASK, "Task");
			} else {
				JsonObject rallyTaskWorkProduct = task.get("WorkProduct").getAsJsonObject();
				String workProductType = rallyTaskWorkProduct.get("_type").getAsString();
				if (workProductType.equalsIgnoreCase("hierarchicalrequirement")) {
					JsonObject jiraParentIssue = findOrCreateIssueInJiraForUserStory(project, rallyTaskWorkProduct);
					task.add("jira-parent-key", getParentKey(jiraParentIssue));
					jiraIssue = createIssueInJiraAndProcessAttachments(project, jiraVersionId, task, RallyObject.TASK, "Sub-task");
				} else {
					if (workProductType.equalsIgnoreCase("defect")) {
						JsonObject jiraParentIssue = findOrCreateIssueInJiraForDefect(project, rallyTaskWorkProduct);
						task.add("jira-parent-key", getParentKey(jiraParentIssue));
						jiraIssue = createIssueInJiraAndProcessAttachments(project, jiraVersionId, task, RallyObject.TASK, "Sub-task");
					} else {
						jiraIssue = createIssueInJiraAndProcessAttachments(project, jiraVersionId, task, RallyObject.TASK, "Task");
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
		if (!rallyFormattedId.equals("DE4103"))
			return null;

		JsonObject jiraIssue = jira.findIssueByRallyFormattedID(rallyFormattedId);
		String jiraVersionId = getJiraVersionIdForRelease(defect);
		if (Utils.isEmpty(jiraIssue)) {
			if (defect.get("Requirement") == null || defect.get("Requirement").isJsonNull()) {
				jiraIssue = createIssueInJiraAndProcessAttachments(project, jiraVersionId, defect, RallyObject.DEFECT, "Bug");
			} else {
				JsonObject rallyDefectUserStory = rally.findRallyObjectByFormatteID(project, defect.get("Requirement").getAsJsonObject().get("FormattedID").getAsString(), RallyObject.USER_STORY);
				JsonObject jiraParentIssue = findOrCreateIssueInJiraForUserStory(project, rallyDefectUserStory);
				defect.add("jira-parent-key", getParentKey(jiraParentIssue));
				jiraIssue = createIssueInJiraAndProcessAttachments(project, jiraVersionId, defect, RallyObject.DEFECT, "Defect");
			}
		}
		return jiraIssue;
	}

	private JsonObject createIssueInJiraAndProcessAttachments(JsonObject project, String jiraVersionId, JsonObject rallyWorkProduct, RallyObject workProductType, String jiraIssueType)
			throws Exception {
		JsonObject jiraIssue = jira.createIssueInJira(project, jiraVersionId, rallyWorkProduct, workProductType, jiraIssueType);
		processAttachments(project, rallyWorkProduct, jiraIssue);
		return jiraIssue;
	}

	private boolean processAttachments(JsonObject project, JsonObject rallyWorkProduct, JsonObject jiraIssue) throws IOException {
		JsonElement jeAattachments = rallyWorkProduct.get("Attachments");
		if (!Utils.isEmpty(jeAattachments)) {
			JsonArray attachments = jeAattachments.getAsJsonArray();
			for (JsonElement attachment : attachments) {
				uploadAttachmentToJira(project, attachment.getAsJsonObject(), jiraIssue);
			}
		}

		return false;
	}

	private void uploadAttachmentToJira(JsonObject project, JsonObject attachment, JsonObject jiraIssue) throws IOException {
		attachment = rally.findRallyObjectByObjectID(project, RallyObject.ATTACHMENT, attachment.get("ObjectID").getAsString());
		String fileName = attachment.get("Name").getAsString();
		// String description = attachment.get("Description").getAsString();
		JsonObject attachmentContent = jira.getRallyAttachment(attachment.get("Content").getAsJsonObject().get("_ref").getAsString());
		String base64Content = attachmentContent.get("AttachmentContent").getAsJsonObject().get("Content").getAsString();
		byte[] decodedString = Base64.decodeBase64(base64Content);
		File f = new File("/RallyAttachments/" + fileName);
		FileOutputStream outFile = new FileOutputStream(f);
		outFile.write(decodedString);
		outFile.close();
		jira.attachFile(jiraIssue.get("key").getAsString(), f);
		f.delete();
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
				jiraIssue = createIssueInJiraAndProcessAttachments(project, jiraVersionId, userStory, RallyObject.USER_STORY, "Story");
			} else {
				JsonObject rallyParentUserStory = rally.findRallyObjectByFormatteID(project, userStory.get("Parent").getAsJsonObject().get("FormattedID").getAsString(), RallyObject.USER_STORY);
				JsonObject jiraParentIssue = createIssueInJiraAndProcessAttachments(project, jiraVersionId, rallyParentUserStory, RallyObject.USER_STORY, "Story");
				userStory.add("jira-parent-key", getParentKey(jiraParentIssue));
				jiraIssue = createIssueInJiraAndProcessAttachments(project, jiraVersionId, userStory, RallyObject.USER_STORY, "Sub-story");
			}
		}
		return jiraIssue;
	}

	private String getJiraVersionIdForRelease(JsonObject rallyObject) {
		if (rallyObject.get("Release") != null && !rallyObject.get("Release").isJsonNull()) {
			return releaseVersionMap.get(rallyObject.get("Release").getAsJsonObject().get("ObjectID").getAsString());
		}
		return null;
	}
}
