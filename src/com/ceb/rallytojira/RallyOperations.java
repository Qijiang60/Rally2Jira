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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.rallydev.rest.request.GetRequest;
import com.rallydev.rest.response.GetResponse;

public class RallyOperations {
	RallyJsonClient client;

	public RallyOperations() throws URISyntaxException {
		client = new RallyJsonClient();

	}

	public JsonObject findRallyObjectByFormatteID(JsonObject project, String formattedID, RallyObject workProductType) throws IOException {
		List<String> dataElements = Utils.elementsTobeFetched(
				Utils.getJsonObjectName(project), workProductType);
		Map<String, String> filter = new LinkedHashMap<String, String>();
		filter.put("FormattedID", formattedID);
		JsonArray arr = client.searchObjects(workProductType, filter, dataElements);
		if (arr.size() == 1) {
			return arr.get(0).getAsJsonObject();
		}

		return null;
	}

	// public JsonArray getRallyObjectsForProject(JsonObject project,
	// RallyObject objectType) throws IOException {
	// List<String> dataElements =
	// Utils.elementsTobeFetched(Utils.getJsonObjectName(project), objectType);
	// Map<String, String> filter = new LinkedHashMap<String, String>();
	// filter.put("Project.ObjectID", project.get("ObjectID").getAsString());
	// return client.searchObjects(objectType, filter, dataElements);
	// }
	//

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

	public JsonArray getAllWorkspaces() throws IOException {
		List<String> dataElements = new ArrayList<String>();
		dataElements.add("ObjectID");
		dataElements.add("Name");
		dataElements.add("Workspaces");
		JsonArray subscriptions = client.searchObjects(RallyObject.SUBSCRIPTION, null, dataElements);
		JsonArray workspacesWithProjects = new JsonArray();
		for (JsonElement subEle : subscriptions) {
			JsonArray workspaces = subEle.getAsJsonObject().get("Workspaces").getAsJsonArray();
			for (JsonElement workspaceEle : workspaces) {
				JsonObject response = getObjectFromRef(workspaceEle);
				workspacesWithProjects.add(response);
			}
		}
		return workspacesWithProjects;
	}

	public JsonObject getObjectFromRef(JsonElement objectElement) throws IOException {
		String url = objectElement.getAsJsonObject().get("_ref").getAsString();
		GetRequest request = new GetRequest(url);
		GetResponse response = client.getApi().get(request);
		return response.getObject();
	}


	public JsonArray getRallyObjectsForProjectAndWorkspace(JsonObject workspace, JsonObject project, RallyObject objectType) throws IOException {
		String workspaceRef = workspace.get("_ref").getAsString();
		String workspaceRefWithoutJs = workspaceRef.substring(0, workspaceRef.length() - 3);
		String url = "/" + objectType.getCode();
		int startIndex = 1;
		int maxPageSize = 200;
		int remaining = maxPageSize;
		int currentPage = 0;
		JsonArray allObjects = new JsonArray();
		while (remaining > 0) {
			startIndex = currentPage * maxPageSize + 1;
			GetRequest request = new GetRequest(url);
			request.addParam("workspace", workspaceRefWithoutJs);
			request.addParam("query", "(Project.ObjectID = " + project.get("ObjectID").getAsString() + ")");
			request.addParam("fetch", "false");
			request.addParam("pagesize", "200");
			request.addParam("start", "" + startIndex);
			GetResponse response = client.getApi().get(request);
			int totalResults = Integer.parseInt(response.getObject().get("TotalResultCount").getAsString());
			allObjects.addAll(response.getObject().get("Results").getAsJsonArray());
			currentPage++;
			remaining = totalResults - maxPageSize * currentPage;
		}
		return allObjects;
	}
}
