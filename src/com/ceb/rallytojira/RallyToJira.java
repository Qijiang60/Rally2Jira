package com.ceb.rallytojira;

import java.io.IOException;
import java.net.URISyntaxException;

import com.ceb.rallytojira.rest.client.Utils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

public class RallyToJira {

	RallyOperations rally;
	JiraOperations jira;

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
		JsonArray project = rally.getProjectByName("Discussions");
		JsonObject jProject = project.get(0).getAsJsonObject();
		JsonArray releases = rally.getReleasesForProject(jProject);
		for (JsonElement release : releases) {
			String versionId = jira.createVersion(jProject,
					release.getAsJsonObject());
			System.out.println(versionId);
		}
		for (JsonElement release : releases) {
			JsonObject jRelease = release.getAsJsonObject();
			String versionId = jira.findProjectVersionByName(jProject,
					Utils.getJsonObjectName(jRelease));
			JsonArray userStories = rally.getUserStoriesForProjectAndRelease(jProject, jRelease);
			for(JsonElement userStory : userStories){
				if(userStory.getAsJsonObject().get("Parent").isJsonNull()){
					//jira.createIssue(userStory);
				}else{
					System.out.println(userStory.getAsJsonObject().get("Parent").getAsJsonObject().get("FormattedID"));
					//jira.createIssue(parentUserStory);
					//jira.createSubTask(userStory);
					
				}
				//jira.createIssueInVersionFromUserStory(jProject, versionId, userStory.getAsJsonObject());
			}
			System.out.println(topLevelUserStoryCount);

			System.out.println(versionId);
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
					String releaseName = release.getAsJsonObject().get("Name")
							.getAsString();
					System.out.println(releaseName);
				}
				JsonArray iterations = rally.getIterationsForProject(jProject);
				for (JsonElement iteration : iterations) {
					String iterationName = iteration.getAsJsonObject()
							.get("Name").getAsString();
					System.out.println(iterationName);
				}
				JsonArray userStories = rally
						.getUserStoriesForProject(jProject);
				for (JsonElement userStory : userStories) {
					String userStoryName = userStory.getAsJsonObject()
							.get("Name").getAsString();
					JsonElement userStoryIteration = userStory
							.getAsJsonObject().get("Iteration");
					String userStoryIterationName = "";
					if (userStoryIteration.isJsonObject()) {
						userStoryIterationName = userStoryIteration
								.getAsJsonObject().get("Name").getAsString();
					}
					System.out.println(userStoryName + " | "
							+ userStoryIterationName);
				}
				JsonArray defects = rally.getDefectsForProject(jProject);
				for (JsonElement defect : defects) {
					String defectName = defect.getAsJsonObject().get("Name")
							.getAsString();
					System.out.println(defectName);
				}
			}

			System.out
					.println("------------------------------------------------------------");
		}
	}

}
