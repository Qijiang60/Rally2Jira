package com.ceb.rallytojira.rest.client;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.ceb.rallytojira.domain.RallyObject;
import com.google.gson.JsonArray;
import com.rallydev.rest.RallyRestApi;
import com.rallydev.rest.request.QueryRequest;
import com.rallydev.rest.response.QueryResponse;
import com.rallydev.rest.util.Fetch;
import com.rallydev.rest.util.QueryFilter;

public class RallyJsonClient {
	RallyRestApi api;

	public RallyJsonClient() throws URISyntaxException {
		api = new RallyRestApi(new URI("https://rally1.rallydev.com"),
				"hagarwal@executiveboard.com", "harshag12");
		api.setWsapiVersion("1.41");
		
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
}
