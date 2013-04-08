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

	public JsonArray getReleasesForProject(JsonObject project)
			throws IOException {
		List<String> dataElements = Utils.elementsTobeFetched(Utils.getJsonObjectName(project), RallyObject.RELEASE);
		Map<String, String> filter = new LinkedHashMap<String, String>();
		filter.put("Project.ObjectID", project.get("ObjectID").getAsString());
		return client.searchObjects(RallyObject.RELEASE, filter, dataElements);

	}

	public JsonObject findRallyObjectByFormatteID(JsonObject project, String formattedID, RallyObject workProductType) throws IOException {
		List<String> dataElements = Utils.elementsTobeFetched(
				Utils.getJsonObjectName(project), workProductType);
		Map<String, String> filter = new LinkedHashMap<String, String>();
		filter.put("FormattedID", formattedID);
		return client.searchObjects(workProductType, filter, dataElements).get(0).getAsJsonObject();
	}

	public JsonArray getRallyObjectsForProject(JsonObject project, RallyObject objectType) throws IOException {
		List<String> dataElements = Utils.elementsTobeFetched(Utils.getJsonObjectName(project), objectType);
		Map<String, String> filter = new LinkedHashMap<String, String>();
		filter.put("Project.ObjectID", project.get("ObjectID").getAsString());
		return client.searchObjects(objectType, filter, dataElements);
	}

	public JsonObject findRallyObjectByObjectID(JsonObject project, RallyObject objectType, String objectID) throws IOException {
		List<String> dataElements = Utils.elementsTobeFetched(Utils.getJsonObjectName(project), objectType);
		Map<String, String> filter = new LinkedHashMap<String, String>();
		filter.put("ObjectID", objectID);
		JsonArray results = client.searchObjects(objectType, filter, dataElements);
		if (results.size() == 1) {
			return results.get(0).getAsJsonObject();
		}
		return null;
	}

}
