package com.ceb.rallytojira;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.ceb.rallytojira.domain.RallyObject;
import com.ceb.rallytojira.rest.client.RallyJsonClient;
import com.ceb.rallytojira.rest.client.Utils;
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

	public JsonArray getProjectByName(String name) throws IOException {
		List<String> dataElements = new ArrayList<String>();
		dataElements.add("ObjectID");
		dataElements.add("Name");
		Map<String, String> filter = new LinkedHashMap<String, String>();
		filter.put("Name", name);
		return client.searchObjects(RallyObject.PROJECT, filter, dataElements);
	}

	public JsonArray getReleasesForProject(JsonObject project) throws IOException {
		List<String> dataElements = Utils.elementsTobeFetched(Utils.getJsonObjectName(project), RallyObject.RELEASE);
		return getArtifactsForProject(RallyObject.RELEASE, project, dataElements);

	}

	public JsonArray getIterationsForProject(JsonObject project) throws IOException {
		List<String> dataElements = new ArrayList<String>();
		dataElements.add("FormattedID");
		dataElements.add("Name");
		return getArtifactsForProject(RallyObject.ITERATION, project, dataElements);

	}

	private JsonArray getArtifactsForProject(RallyObject artifactType, JsonObject project, List<String> dataElements)
			throws IOException {
		Map<String, String> filter = new LinkedHashMap<String, String>();
		filter.put("Project.ObjectID", project.get("ObjectID").getAsString());
		return client.searchObjects(artifactType, filter, dataElements);

	}

	public JsonArray getUserStoriesForProjectAndRelease(JsonObject project, JsonObject release) throws IOException {
		List<String> dataElements = Utils.elementsTobeFetched(Utils.getJsonObjectName(project), RallyObject.USER_STORY);
		Map<String, String> filter = new LinkedHashMap<String, String>();
		filter.put("Project.ObjectID", project.get("ObjectID").getAsString());
		filter.put("Release.ObjectID", release.get("ObjectID").getAsString());
		return client.searchObjects(RallyObject.USER_STORY, filter, dataElements);

	}

	public JsonArray getDefectsForProjectAndRelease(JsonObject project, JsonObject release) throws IOException {
		List<String> dataElements = Utils.elementsTobeFetched(Utils.getJsonObjectName(project), RallyObject.DEFECT);
		Map<String, String> filter = new LinkedHashMap<String, String>();
		filter.put("Project.ObjectID", project.get("ObjectID").getAsString());
		filter.put("Release.ObjectID", release.get("ObjectID").getAsString());
		return client.searchObjects(RallyObject.DEFECT, filter, dataElements);
	}

	public JsonArray findUserStoryByFormatteID(JsonObject project, String formattedID) throws IOException {
		List<String> dataElements = Utils.elementsTobeFetched(Utils.getJsonObjectName(project), RallyObject.USER_STORY);
		Map<String, String> filter = new LinkedHashMap<String, String>();
		filter.put("FormattedID", formattedID);
		return client.searchObjects(RallyObject.USER_STORY, filter, dataElements);
	}
}
