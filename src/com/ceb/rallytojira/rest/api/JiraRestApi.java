package com.ceb.rallytojira.rest.api;

import java.io.File;
import java.net.URI;

import javax.ws.rs.core.MediaType;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.Base64;
import com.sun.jersey.multipart.FormDataMultiPart;
import com.sun.jersey.multipart.file.FileDataBodyPart;

public class JiraRestApi {

	protected URI server;
	protected Client client;
	protected String auth;

	public JiraRestApi(URI server, String username, String password) {
		this.server = server;

		auth = new String(Base64.encode(username + ":" + password));
		client = Client.create();
	}

	public ClientResponse doGet(String url) {
		System.out.println("doGet: " + server + url);
		WebResource webResource = client.resource(server + url);
		ClientResponse response = webResource
				.header("Authorization", "Basic " + auth)
				.type("application/json").accept("application/json")
				.get(ClientResponse.class);
		return response;

	}

	public ClientResponse doPost(String url, String data) {
		WebResource webResource = client.resource(server + url);
		ClientResponse response = webResource
				.header("Authorization", "Basic " + auth)
				.type("application/json").accept("application/json")
				.post(ClientResponse.class, data);

		return response;

	}

	public ClientResponse doMultipartPost(String url, String fileName) {
		WebResource webResource = client.resource(server + url);
		FormDataMultiPart form = new FormDataMultiPart();
		FileDataBodyPart fdp = new FileDataBodyPart("file", new File(
				"C:\\Users\\hagarwal\\Documents\\4797_001.pdf"),
				MediaType.APPLICATION_OCTET_STREAM_TYPE);
		form.bodyPart(fdp);
		ClientResponse response = webResource
				.header("X-Atlassian-Token", "nocheck")
				.header("Authorization", "Basic " + auth)
				.type(MediaType.MULTIPART_FORM_DATA)
				.post(ClientResponse.class, form);
		System.out.println(response.getStatus());

		return response;

	}

	public ClientResponse doDelete(String url) {
		System.out.println("doDelete: " + server + url);
		WebResource webResource = client.resource(server + url);
		ClientResponse response = webResource
				.header("Authorization", "Basic " + auth)
				.type("application/json").accept("application/json")
				.delete(ClientResponse.class);
		return response;

	}
}
