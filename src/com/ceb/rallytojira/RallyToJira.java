package com.ceb.rallytojira;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import com.ceb.rallytojira.domain.RallyObject;
import com.ceb.rallytojira.rest.client.Utils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class RallyToJira {

	RallyOperations rally;
	JiraOperations jira;
	Map<String, String> releaseVersionMap = new HashMap<String, String>();

	public RallyToJira() throws URISyntaxException {
		rally = new RallyOperations();
		jira = new JiraOperations();
	}

	public static void main(String[] args) throws URISyntaxException,
			IOException {
		RallyToJira rallyToJira = new RallyToJira();
		rallyToJira.process();

	}

	private void process() throws IOException {
		JsonObject project = rally.getProjectByName("Discussions").get(0).getAsJsonObject();
		createReleases(project);
	}

	private void createReleases(JsonObject project) throws IOException {
		JsonArray releases = rally.getReleasesForProject(project);

		for (JsonElement release : releases) {
			String jiraVersionId = jira.createVersion(project, release.getAsJsonObject());
			releaseVersionMap.put(release.getAsJsonObject().get("ObjectID").getAsString(), jiraVersionId);
		}

		for (String releaseObjectID : releaseVersionMap.keySet()) {
			createTasks(project, releaseObjectID, releaseVersionMap.get(releaseObjectID));
			createDefects(project, releaseObjectID, releaseVersionMap.get(releaseObjectID));
			createUserStories(project, releaseObjectID, releaseVersionMap.get(releaseObjectID));

		}
	}

	private void createTasks(JsonObject project, String releaseObjectID, String jiraVersionId) throws IOException {
		JsonArray tasks = rally.getRallyObjectsForProjectAndRelease(project, releaseObjectID, RallyObject.TASK);
		for (JsonElement jeTask : tasks) {
			JsonObject task = jeTask.getAsJsonObject();
			findOrCreateIssueInJiraForTask(project, task);
		}
	}

	private void findOrCreateIssueInJiraForTask(JsonObject project, JsonObject task) throws IOException {
		String rallyFormattedId = task.get("FormattedID").getAsString();
		JsonObject jiraIssue = jira.findIssueByRallyFormattedID(rallyFormattedId);
		String jiraVersionId = releaseVersionMap.get(task.get("Release").getAsJsonObject().get("ObjectID").getAsString());
		if (Utils.isEmpty(jiraIssue)) {

			if (task.get("WorkProduct") == null || task.get("WorkProduct").isJsonNull()) {
				jiraIssue = jira.createIssueInJira(project, jiraVersionId, task, RallyObject.TASK, "Task");
			} else {
				JsonObject rallyTaskWorkProduct = task.get("WorkProduct").getAsJsonObject();
				String workProductType = rallyTaskWorkProduct.get("_type").getAsString();
				if (workProductType.equalsIgnoreCase("hierarchicalrequirement")) {
					JsonObject jiraParentIssue = findOrCreateIssueInJiraForUserStory(project, rallyTaskWorkProduct);
					task.add("jira-parent-key", jiraParentIssue.get("key"));
					jiraIssue = jira.createIssueInJira(project, jiraVersionId, task, RallyObject.TASK, "Sub-task");
				} else {
					if (workProductType.equalsIgnoreCase("defect")) {
						JsonObject jiraParentIssue = findOrCreateIssueInJiraForDefect(project, rallyTaskWorkProduct);
						task.add("jira-parent-key", jiraParentIssue.get("key"));
						jiraIssue = jira.createIssueInJira(project, jiraVersionId, task, RallyObject.TASK, "Sub-task");
					} else {
						jiraIssue = jira.createIssueInJira(project, jiraVersionId, task, RallyObject.TASK, "Task");
					}

				}
			}

		}
	}

	private void createDefects(JsonObject project, String releaseObjectID, String jiraVersionId) throws IOException {
		JsonArray defects = rally.getRallyObjectsForProjectAndRelease(project, releaseObjectID, RallyObject.DEFECT);
		for (JsonElement jeDefect : defects) {
			JsonObject defect = jeDefect.getAsJsonObject();
			findOrCreateIssueInJiraForDefect(project, defect);
		}
	}

	private JsonObject findOrCreateIssueInJiraForDefect(JsonObject project, JsonObject defect) throws IOException {
		String rallyFormattedId = defect.get("FormattedID").getAsString();
		JsonObject jiraIssue = jira.findIssueByRallyFormattedID(rallyFormattedId);
		String jiraVersionId = releaseVersionMap.get(defect.get("Release").getAsJsonObject().get("ObjectID").getAsString());
		if (Utils.isEmpty(jiraIssue)) {
			if (defect.get("Requirement") == null || defect.get("Requirement").isJsonNull()) {
				jiraIssue = jira.createIssueInJira(project, jiraVersionId, defect, RallyObject.DEFECT, "Bug");
			} else {
				JsonObject rallyDefectUserStory = rally.findRallyObjectByFormatteID(project, defect.get("Requirement").getAsJsonObject().get("FormattedID").getAsString(), RallyObject.USER_STORY);
				JsonObject jiraParentIssue = findOrCreateIssueInJiraForUserStory(project, rallyDefectUserStory);
				defect.add("jira-parent-key", jiraParentIssue.get("key"));
				jiraIssue = jira.createIssueInJira(project, jiraVersionId, defect, RallyObject.DEFECT, "Defect");
			}
		}
		return jiraIssue;
	}

	private void createUserStories(JsonObject project, String releaseObjectID, String jiraVersionId) throws IOException {
		JsonArray userStories = rally.getRallyObjectsForProjectAndRelease(project, releaseObjectID, RallyObject.USER_STORY);

		for (JsonElement jeUserStory : userStories) {
			JsonObject userStory = jeUserStory.getAsJsonObject();
			findOrCreateIssueInJiraForUserStory(project, userStory);
		}
	}

	private JsonObject findOrCreateIssueInJiraForUserStory(JsonObject project, JsonObject userStory) throws IOException {
		String rallyFormattedId = userStory.get("FormattedID").getAsString();
		JsonObject jiraIssue = jira.findIssueByRallyFormattedID(rallyFormattedId);
		String jiraVersionId = releaseVersionMap.get(userStory.get("Release").getAsJsonObject().get("ObjectID").getAsString());
		if (Utils.isEmpty(jiraIssue)) {
			if (userStory.get("Parent") == null || userStory.get("Parent").isJsonNull()) {
				jiraIssue = jira.createIssueInJira(project, jiraVersionId, userStory, RallyObject.USER_STORY, "Story");
			} else {
				JsonObject rallyParentUserStory = rally.findRallyObjectByFormatteID(project, userStory.get("Parent").getAsJsonObject().get("FormattedID").getAsString(), RallyObject.USER_STORY);
				JsonObject jiraParentIssue = jira.createIssueInJira(project, jiraVersionId, rallyParentUserStory, RallyObject.USER_STORY, "Story");
				userStory.add("jira-parent-key", jiraParentIssue.get("key"));
				jiraIssue = jira.createIssueInJira(project, jiraVersionId, userStory, RallyObject.USER_STORY, "Sub-story");
			}
		}
		return jiraIssue;
	}
}
