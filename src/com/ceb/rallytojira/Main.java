package com.ceb.rallytojira;

import java.util.TreeSet;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class Main {

	public static void main(String args[]) throws Exception {

		TreeSet<String> users = new TreeSet<String>();
		JiraRestOperations jira = new JiraRestOperations();
		int j = 0;
		for (int m = 0; m < 10; m++) {
			JsonElement xje = jira.callJira("https://agiletool.executiveboard.com/rest/api/latest/group?groupname=jira-users&expand=users[" + (m * 50 + 1) + ":" + (m * 50 + 50) + "]");
			JsonArray x = xje.getAsJsonObject().get("users").getAsJsonObject().get("items").getAsJsonArray();
			for (JsonElement xj : x) {
				users.add(getStringValue(xj.getAsJsonObject(), "displayName") + "|" + getStringValue(xj.getAsJsonObject(), "name") + "|" + getStringValue(xj.getAsJsonObject(), "emailAddress") + "|"
						+ getStringValue(xj.getAsJsonObject(), "username"));
			}
			xje = jira.callJira("https://agiletool.executiveboard.com/rest/api/latest/group?groupname=shl-users&expand=users[" + (m * 50 + 1) + ":" + (m * 50 + 50) + "]");
			x = xje.getAsJsonObject().get("users").getAsJsonObject().get("items").getAsJsonArray();
			for (JsonElement xj : x) {
				users.add(getStringValue(xj.getAsJsonObject(), "displayName") + "|" + getStringValue(xj.getAsJsonObject(), "name") + "|" + getStringValue(xj.getAsJsonObject(), "emailAddress") + "|"
						+ getStringValue(xj.getAsJsonObject(), "username"));
			}
			xje = jira.callJira("https://agiletool.executiveboard.com/rest/api/latest/group?groupname=cebvclc-users&expand=users[" + (m * 50 + 1) + ":" + (m * 50 + 50) + "]");
			x = xje.getAsJsonObject().get("users").getAsJsonObject().get("items").getAsJsonArray();
			for (JsonElement xj : x) {
				users.add(getStringValue(xj.getAsJsonObject(), "displayName") + "|" + getStringValue(xj.getAsJsonObject(), "name") + "|" + getStringValue(xj.getAsJsonObject(), "emailAddress") + "|"
						+ getStringValue(xj.getAsJsonObject(), "username"));
			}

		}
		int i = 0;
		System.out.println(users.size());
		for (String s : users) {
			System.out.println(++i + "|" + s);
		}
	}

	private static String getStringValue(JsonObject jo, String field) {

		JsonElement je = jo.get(field);
		if (je == null)
		{
			return "";
		}
		return je.getAsString();

	}
}
