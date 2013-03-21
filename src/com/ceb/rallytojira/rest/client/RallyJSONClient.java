package com.ceb.rallytojira.rest.client;

import java.net.URI;
import java.net.URISyntaxException;

import com.rallydev.rest.RallyRestApi;

public class RallyJSONClient {

	public RallyRestApi getRallyRestApi() throws URISyntaxException {
		return new RallyRestApi(new URI("https://rally1.rallydev.com"),
				"hagarwal@executiveboard.com", "harshag12");
	}

}
