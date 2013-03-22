package com.ceb.rallytojira;

import java.io.IOException;
import java.net.URISyntaxException;

import com.sun.jersey.api.client.ClientResponse;

public class CopyOfRallyToJira {

	RallyOperations rally;
	JiraOperations jira;

	public CopyOfRallyToJira() throws URISyntaxException {
		rally = new RallyOperations();
		jira = new JiraOperations();
	}

	public static void main(String[] args) throws URISyntaxException,
			IOException {
		CopyOfRallyToJira rallyToJira = new CopyOfRallyToJira();
		rallyToJira.process();

	}

	private void process() throws IOException {
		// System.out.println(rally.getWsapiVersion());
		// QueryResponse projects = rally.getAllProjects();
		// JsonArray l = projects.getResults();
		// Iterator<JsonElement> pi = l.iterator();
		// while (pi.hasNext()) {
		// JsonObject jo = pi.next().getAsJsonObject();
		// System.out.println(jo.get("Name"));
		// }

//		ClientResponse response = jira.getAllProjects();
//		String strResp = response.getEntity(String.class);
//		JsonElement je = (new JsonParser()).parse(strResp);
//		if (je.isJsonObject()) {
//			JsonObject jo = je.getAsJsonObject();
//			System.out.println(jo);
//		} else {
//			if (je.isJsonArray()) {
//				JsonArray ja = je.getAsJsonArray();
//				Iterator<JsonElement> pi = ja.iterator();
//				while (pi.hasNext()) {
//					JsonObject jo = pi.next().getAsJsonObject();
//					System.out.println(jo);
//				}
//			}
//		}

//		ClientResponse response = jira.createIssue();
//		String strResp = response.getEntity(String.class);
//		System.out.println(strResp);
		ClientResponse response = jira.attachFile();
		String strResp = response.getEntity(String.class);
		System.out.println(strResp);
	
		}

}
