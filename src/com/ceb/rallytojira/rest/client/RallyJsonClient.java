package com.ceb.rallytojira.rest.client;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.ceb.rallytojira.domain.RallyObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.rallydev.rest.RallyRestApi;
import com.rallydev.rest.request.QueryRequest;
import com.rallydev.rest.response.QueryResponse;
import com.rallydev.rest.util.Fetch;
import com.rallydev.rest.util.QueryFilter;

public class RallyJsonClient {
	RallyRestApi api;
	JsonObject loggedInUserProfile;
	//String username = "hgarwal@executiveboard.com";

	String username = "hagarwal@executiveboard.com";

	public RallyJsonClient() throws URISyntaxException, IOException {
		login();

	}

	public void login() throws URISyntaxException, IOException {
		try {
			api.close();
		} catch (Exception e) {

		}
		api = new RallyRestApi(new URI("https://rally1.rallydev.com"),
				username, "harshag12");
		api.setWsapiVersion("1.43");

		List<String> dataElements = new ArrayList<String>();
		dataElements.add("UserProfile");
		Map<String, String> filter = new LinkedHashMap<String, String>();
		filter.put("UserName", username);
		JsonArray results = searchObjects(RallyObject.USER, filter, dataElements);
		JsonObject loggedInUser = results.get(0).getAsJsonObject();
		loggedInUserProfile = loggedInUser.get("UserProfile").getAsJsonObject();
	}

	public JsonArray searchObjects(RallyObject obj, Map<String, String> filter,
			List<String> dataElements) throws IOException {

		QueryRequest request = new QueryRequest(obj.getCode());
		request.setFetch(new Fetch(StringUtils.join(dataElements, ",")));
		if (filter != null) {
			QueryFilter queryFilter = null;
			for (String filterName : filter.keySet()) {
				QueryFilter tempFilter = new QueryFilter(filterName, "=",
						filter.get(filterName));
				if (queryFilter == null) {
					queryFilter = tempFilter;
				} else {
					queryFilter = queryFilter.and(tempFilter);
				}
			}
			request.setQueryFilter(queryFilter);
		}
		request.setLimit(100000);
		QueryResponse queryResponse = api.query(request);
		return queryResponse.getResults();

	}

	public RallyRestApi getApi() {
		return api;
	}

	public JsonObject getLoggedInUserProfile() {
		return loggedInUserProfile;
	}



}
