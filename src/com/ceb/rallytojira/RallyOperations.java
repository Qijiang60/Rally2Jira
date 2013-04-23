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
import com.rallydev.rest.request.UpdateRequest;
import com.rallydev.rest.response.GetResponse;
import com.rallydev.rest.response.UpdateResponse;

public class RallyOperations {
	RallyJsonClient client;

	public RallyOperations() throws URISyntaxException, IOException {
		client = new RallyJsonClient();

	}

	public JsonObject findRallyObjectByFormatteID(JsonObject project, String formattedID, RallyObject workProductType) throws IOException {
		List<String> dataElements = Utils.elementsTobeFetched(workProductType);
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

	public JsonObject findRallyObjectByObjectID( RallyObject objectType, String objectID) throws IOException {
		List<String> dataElements = Utils.elementsTobeFetched(objectType);
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
		request.addParam("fetch", "true");
		GetResponse response = client.getApi().get(request);
		return response.getObject();
	}

	public JsonArray getRallyObjectsForProject(JsonObject project, RallyObject objectType) throws IOException {

		List<String> dataElements = Utils.elementsTobeFetched(objectType);
		Map<String, String> filter = new LinkedHashMap<String, String>();
		filter.put("Project.ObjectID", project.get("ObjectID").getAsString());
		return client.searchObjects(objectType, filter, dataElements);
	}

	public void updateDefaultWorkspace(JsonObject workspace, JsonObject project) throws IOException, URISyntaxException {
		JsonObject userProfile = client.getLoggedInUserProfile();
		String userProfileRef = client.getLoggedInUserProfile().get("_ref").getAsString();
		userProfile.add("DefaultWorkspace", workspace);
		userProfile.add("DefaultProject", project);
		UpdateRequest updateRequest = new UpdateRequest(userProfileRef, userProfile);
		UpdateResponse response = client.getApi().update(updateRequest);
		client.login();
		System.out.println(response.getObject());

	}
}
