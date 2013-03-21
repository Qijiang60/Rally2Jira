package com.ceb.rallytojira;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.ceb.rallytojira.domain.RallyObject;
import com.ceb.rallytojira.rest.client.RallyJsonClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class RallyOperations {
	RallyJsonClient client;

	public RallyOperations() throws URISyntaxException {
		client = new RallyJsonClient();

	}

	public JsonArray getAllProjects() throws IOException {
		List<String> dataElements = new ArrayList<String>();
		dataElements.add("ObjectID");
		dataElements.add("Name");
		return client.searchObjects(RallyObject.PROJECT, null, dataElements);

	}

	public JsonArray getReleasesForProject(JsonObject project)
			throws IOException {
		List<String> dataElements = new ArrayList<String>();
		dataElements.add("FormattedID");
		dataElements.add("Name");
		return getArtifactsForProject(RallyObject.RELEASE, project,
				dataElements);

	}

	public JsonArray getIterationsForProject(JsonObject project)
			throws IOException {
		List<String> dataElements = new ArrayList<String>();
		dataElements.add("FormattedID");
		dataElements.add("Name");
		return getArtifactsForProject(RallyObject.ITERATION, project,
				dataElements);

	}

	public JsonArray getUserStoriesForProject(JsonObject project)
			throws IOException {
		List<String> dataElements = new ArrayList<String>();
		dataElements.add("FormattedID");
		dataElements.add("Name");
		return getArtifactsForProject(RallyObject.USER_STORY, project,
				dataElements);

	}

	public JsonArray getDefectsForProject(JsonObject project)
			throws IOException {
		List<String> dataElements = new ArrayList<String>();
		dataElements.add("FormattedID");
		dataElements.add("Name");
		return getArtifactsForProject(RallyObject.DEFECT, project, dataElements);

	}

	private JsonArray getArtifactsForProject(RallyObject artifactType,
			JsonObject project, List<String> dataElements) throws IOException {
		Map<String, String> filter = new LinkedHashMap<String, String>();
		filter.put("Project.ObjectID", project.get("ObjectID").getAsString());
		return client.searchObjects(artifactType, filter, dataElements);

	}
}
