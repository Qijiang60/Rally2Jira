package com.ceb.rallytojira;

import java.io.IOException;
import java.net.URISyntaxException;

import com.ceb.rallytojira.rest.client.Utils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class RallyToJira {

	RallyOperations rally;
	JiraOperations jira;

	public RallyToJira() throws URISyntaxException {
		rally = new RallyOperations();
		jira = new JiraOperations();
	}

	public static void main(String[] args) throws URISyntaxException, IOException {
		RallyToJira rallyToJira = new RallyToJira();
		rallyToJira.process();

	}

	private void process() throws IOException {

		JsonArray project = rally.getProjectByName("Discussions");
		JsonObject jProject = project.get(0).getAsJsonObject();
		JsonArray releases = rally.getReleasesForProject(jProject);
		for (JsonElement release : releases) {
			String versionId = jira.createVersion(jProject, release.getAsJsonObject());
			System.out.println(versionId);
		}
		int i = 0;
		for (JsonElement release : releases) {
			JsonObject jRelease = release.getAsJsonObject();
			String versionId = jira.findProjectVersionByName(jProject, Utils.getJsonObjectName(jRelease));
			JsonArray userStories = rally.getUserStoriesForProjectAndRelease(jProject, jRelease);

			// for (JsonElement userStory : userStories) {
			// JsonObject jUserStory = userStory.getAsJsonObject();
			// if (jUserStory.get("Parent").isJsonNull()) {
			// jira.createIssueFromUserStory(jProject, versionId, jUserStory);
			// i++;
			// } else {
			// i++;
			// JsonObject rallyParentUserStory =
			// userStory.getAsJsonObject().get("Parent").getAsJsonObject();
			// JsonObject jiraParentUserStory =
			// jira.createIssueFromUserStory(jProject, versionId,
			// rallyParentUserStory);
			// jira.createSubUserStory(jProject, versionId, jUserStory,
			// jiraParentUserStory);
			//
			// }
			// if (i == 1) {
			// break;
			// }
			JsonArray defects = rally.getDefectsForProjectAndRelease(jProject, jRelease);
				i=0;
			for (JsonElement defect : defects) {
				JsonObject jDefect = defect.getAsJsonObject();
				if (jDefect.get("Requirement") == null || jDefect.get("Requirement").isJsonNull()) {
					// jira.createIssueFromDefect(jProject, versionId, jDefect);
					// i++;
				} else {
					i++;
					JsonObject rallyDefectUserStory = jDefect.getAsJsonObject().get("Requirement").getAsJsonObject();
					rallyDefectUserStory = rally.findUserStoryByFormatteID(jProject,rallyDefectUserStory.getAsJsonObject().get("FormattedID").getAsString()).get(0).getAsJsonObject();
					jira.createIssueFromDefectWithUserStory(jProject, versionId, rallyDefectUserStory,jDefect);
					
//					JsonObject jiraParentUserStory = jira.createIssueFromUserStory(jProject, versionId,
//							rallyParentUserStory);
//					jira.createSubUserStory(jProject, versionId, jUserStory, jiraParentUserStory);

				}
				if (i > 0) {
					break;
				}

			}

		}
	}

	private void process1() throws IOException {
		JsonArray projects = rally.getAllProjects();
		for (JsonElement project : projects) {
			JsonObject jProject = project.getAsJsonObject();
			String projectName = jProject.get("Name").getAsString();
			if ("Discussions".equals(projectName)) {
				System.out.println(projectName);
				JsonArray releases = rally.getReleasesForProject(jProject);
				for (JsonElement release : releases) {
					String releaseName = release.getAsJsonObject().get("Name").getAsString();
					System.out.println(releaseName);
				}
				JsonArray iterations = rally.getIterationsForProject(jProject);
				for (JsonElement iteration : iterations) {
					String iterationName = iteration.getAsJsonObject().get("Name").getAsString();
					System.out.println(iterationName);
				}
			}

			System.out.println("------------------------------------------------------------");
		}
	}

}
