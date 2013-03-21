package com.ceb.rallytojira;

import java.io.IOException;
import java.net.URISyntaxException;

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

	public static void main(String[] args) throws URISyntaxException,
			IOException {
		RallyToJira rallyToJira = new RallyToJira();
		rallyToJira.process();

	}

	private void process() throws IOException {
		JsonArray projects = rally.getAllProjects();
		for (JsonElement project : projects) {
			JsonObject jProject = project.getAsJsonObject();
			String projectName = jProject.get("Name")
					.getAsString();
			if ("Discussions".equals(projectName)) {
				System.out.println(projectName);
				JsonArray releases = rally.getReleasesForProject(jProject);
				for (JsonElement release : releases) {
					String releaseName = release.getAsJsonObject().get("Name")
							.getAsString();
					System.out.println(releaseName);
				}
				JsonArray iterations = rally
						.getIterationsForProject(jProject);
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
					System.out.println(userStoryName);
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
