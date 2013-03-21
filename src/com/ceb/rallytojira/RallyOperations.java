package com.ceb.rallytojira;

import java.io.IOException;
import java.net.URISyntaxException;

import com.ceb.rallytojira.rest.client.RallyJSONClient;
import com.rallydev.rest.RallyRestApi;
import com.rallydev.rest.request.QueryRequest;
import com.rallydev.rest.response.QueryResponse;
import com.rallydev.rest.util.Fetch;

public class RallyOperations {
	RallyRestApi api;

	public RallyOperations() throws URISyntaxException {
		RallyJSONClient client = new RallyJSONClient();
		api = client.getRallyRestApi();
	}

	public String getWsapiVersion() {

		return api.getWsapiVersion();

	}

	public QueryResponse getAllProjects() throws IOException {
		QueryRequest request = new QueryRequest("attachment");
		request.setFetch(new Fetch("FormattedID", "Name"));
		QueryResponse queryResponse = api.query(request);
		return queryResponse;

	}

}
