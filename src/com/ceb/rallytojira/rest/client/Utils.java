package com.ceb.rallytojira.rest.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

import com.ceb.rallytojira.domain.RallyObject;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.jersey.api.client.ClientResponse;

public class Utils {

	public static boolean isEmpty(String s) {
		return s == null || s.length() == 0 || s.equalsIgnoreCase("null")
				|| s.replace(" ", "").length() == 0;
	}

	public static boolean isNotEmpty(String s) {
		return !isEmpty(s);
	}

	public static boolean isEmpty(Object o) {
		return o == null;
	}

	public static boolean isNotEmpty(Object o) {
		return !isEmpty(o);
	}

	public static String listToString(List<String> list) {
		StringBuilder sb = new StringBuilder();
		for (String s : list) {
			sb.append(s + " | ");
		}
		return sb.toString();
	}

	public static void addError(List<String> errors, String errorMessage,
			Logger logger) {
		logger.debug("Error: " + errorMessage);
		errors.add((errors.size() + 1) + ". [" + errorMessage + "]");
	}

	public static String getDate(Date date, String format) {
		if (isEmpty(date)) {
			date = new Date();
		}
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		return sdf.format(date);
	}

	public static void rollOverFile(Date date, String fileName) {
		File oldFile = new File(fileName);
		if (oldFile.exists()) {
			File newFile = new File(fileName + "_"
					+ Utils.getDate(date, "MMddyyyy_HHmmss"));
			oldFile.renameTo(newFile);
		}
	}

	public static List<String> elementsTobeFetched(String project,
			RallyObject artifactType) throws IOException {
		List<String> elements = new ArrayList<String>();
		FileReader fr = new FileReader("mappings/" + project + "/"
				+ artifactType.getCode());
		BufferedReader br = new BufferedReader(fr);
		String stringRead = br.readLine();

		while (stringRead != null) {
			if (Utils.isNotEmpty(stringRead)) {
				StringTokenizer st = new StringTokenizer(stringRead, ",");
				String jiraField = st.nextToken();
				System.out.println(jiraField);
				String rallyField = st.nextToken();
				elements.add(rallyField);
			}
			stringRead = br.readLine();
		}
		br.close();
		return elements;

	}

	public static Map<String, String> getElementMapping(
			RallyObject artifactType, String project) throws IOException {
		Map<String, String> elementMapping = new LinkedHashMap<String, String>();
		FileReader fr = new FileReader("mappings/" + project + "/"
				+ artifactType.getCode());
		BufferedReader br = new BufferedReader(fr);
		String stringRead = br.readLine();
		int i = 0;
		while (stringRead != null) {
			if (i > 0) {
				StringTokenizer st = new StringTokenizer(stringRead, ",");
				String jiraField = st.nextToken();
				if (!"NULL".equals(jiraField)) {
					String rallyField = st.nextToken();
					elementMapping.put(jiraField, rallyField);
				}
			}
			i++;
			stringRead = br.readLine();
		}
		br.close();
		return elementMapping;
	}

	public static void printJson(Map<String, String> data) {

		System.out.println(listToJsonString(data));

	}

	public static String listToJsonString(Map<String, String> data) {
		Gson gson = new Gson();
		String s = gson.toJson(data);
		return s;
	}

	public static JsonObject jerseyRepsonseToJsonObject(ClientResponse response) {
		return (JsonObject) (new JsonParser()).parse(response
				.getEntity(String.class));

	}

	public static JsonArray jerseyRepsonseToJsonArray(ClientResponse response) {
		return (JsonArray) (new JsonParser()).parse(response
				.getEntity(String.class));

	}

	public static String getJsonObjectName(JsonObject jo) {
		return jo.get("Name").getAsString();
	}

	public static String getJiraProjectNameForRallyProject(JsonObject project)
			throws IOException {
		return getElementMapping(RallyObject.PROJECT,
				getJsonObjectName(project)).get(getJsonObjectName(project));
	}
}
