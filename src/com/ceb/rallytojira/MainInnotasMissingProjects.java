package com.ceb.rallytojira;

import java.util.Set;
import java.util.TreeSet;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

public class MainInnotasMissingProjects {

	public static void main(String args[]) throws Exception {

		Set<String> allProjects = new TreeSet<String>();
		Set<String> innotasProjects = new TreeSet<String>();
		JiraRestOperations jira = new JiraRestOperations();
		JsonElement xje = jira.callJira("https://agiletool.executiveboard.com/rest/api/latest/project");
		JsonArray a = xje.getAsJsonArray();
		for (JsonElement j : a) {
			allProjects.add(j.getAsJsonObject().get("key").getAsString());
		}
		System.out.println(allProjects.size());
		System.out.println("*******************************************************************************************************************");
		JiraRestOperations jiraInnotas = new JiraRestOperations(true);
		JsonElement xije = jiraInnotas.callJira("https://agiletool.executiveboard.com/rest/api/latest/project");
		a = xije.getAsJsonArray();
		for (JsonElement j : a) {
			innotasProjects.add(j.getAsJsonObject().get("key").getAsString());
		}
		System.out.println(innotasProjects.size());
		System.out.println("*******************************************************************************************************************");
		for(String s : allProjects){
			if(!innotasProjects.contains(s)){
				System.out.println(s);
			}
		}
	}

}
