package com.ceb.rallytojira.rest.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;

import net.htmlparser.jericho.Renderer;
import net.htmlparser.jericho.Source;

import org.apache.log4j.Logger;

import com.ceb.rallytojira.domain.RallyObject;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.jersey.api.client.ClientResponse;

public class Utils {
	private static Map<String, String> jiraRallyUserMap;

	public static boolean isEmpty(String s) {
		return s == null || s.length() == 0 || s.equalsIgnoreCase("null") || s.replace(" ", "").length() == 0;
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

	public static void addError(List<String> errors, String errorMessage, Logger logger) {
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
			File newFile = new File(fileName + "_" + Utils.getDate(date, "MMddyyyy_HHmmss"));
			oldFile.renameTo(newFile);
		}
	}

	public static List<String> elementsTobeFetched(String project, RallyObject artifactType) throws IOException {
		List<String> elements = new ArrayList<String>();
		FileReader fr = new FileReader("mappings/" + project + "/" + artifactType.getCode());
		BufferedReader br = new BufferedReader(fr);
		String stringRead = br.readLine();

		while (stringRead != null) {
			if (Utils.isNotEmpty(stringRead)) {
				StringTokenizer st = new StringTokenizer(stringRead, ",");
				String jiraField = st.nextToken();
				String rallyField = st.nextToken();
				if (rallyField.indexOf(".") > 1) {
					rallyField = rallyField.substring(0, rallyField.indexOf("."));
				}
				elements.add(rallyField);
			}
			stringRead = br.readLine();
		}
		br.close();
		return elements;

	}

	public static Map<String, String> getElementMapping(RallyObject artifactType, String project) throws IOException {
		Map<String, String> elementMapping = new LinkedHashMap<String, String>();
		FileReader fr = new FileReader("mappings/" + project + "/" + artifactType.getCode());
		BufferedReader br = new BufferedReader(fr);
		String stringRead = br.readLine();
		int i = 0;
		while (stringRead != null) {
			if (i > 0 && Utils.isNotEmpty(stringRead)) {
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

	public static void printJson(Map<String, Object> data) {

		System.out.println(listToJsonString(data));

	}

	public static String listToJsonString(Map<String, Object> data) {
		Gson gson = new GsonBuilder().disableHtmlEscaping().create();
		String s = gson.toJson(data);
		Source htmlSource = new Source(s);
		Renderer r = htmlSource.getRenderer();
		r.setMaxLineLength(5000);
		s = r.toString();
		s = s.replace("\r\n", "\\n");
		s = s.replace("\t", "\\t");
		return s;
	}

	public static JsonObject jerseyRepsonseToJsonObject(ClientResponse response) {
		return (JsonObject) (new JsonParser()).parse(response.getEntity(String.class));

	}

	public static JsonArray jerseyRepsonseToJsonArray(ClientResponse response) {
		return (JsonArray) (new JsonParser()).parse(response.getEntity(String.class));

	}

	public static String getJsonObjectName(JsonObject jo) {
		return jo.get("Name").getAsString();
	}

	public static String getJiraProjectNameForRallyProject(JsonObject project) throws IOException {
		return getElementMapping(RallyObject.PROJECT, getJsonObjectName(project)).get(getJsonObjectName(project));
	}

	@SuppressWarnings({ "rawtypes" })
	public static Map getJiraValue(String jiraKey, String rallyKey, JsonObject userStory) throws IOException {
		List<String> values = getValueFromTree(rallyKey, userStory);
		if (jiraKey.equals("assignee.name")) {
			if (values.size() > 0) {
				String jiraUsername = lookupJiraUsername(values.get(0));
				values.set(0, jiraUsername);
			}
		}
		if (jiraKey.equals("labels[]")) {
			List<String> labelsWithoutSpaces = new ArrayList<String>();
			for (String s : values) {
				labelsWithoutSpaces.add(s.replace(" ", ""));
			}
			values = labelsWithoutSpaces;
		}
		Map jiraMap = createJiraMap(jiraKey, values);
		return jiraMap;
	}

	private static String lookupJiraUsername(String rallyUsername) throws IOException {
		if (jiraRallyUserMap == null) {
			jiraRallyUserMap = new TreeMap<String, String>();
			FileReader fr = new FileReader("mappings/usernames");
			BufferedReader br = new BufferedReader(fr);
			String stringRead = br.readLine();
			int i = 0;
			while (stringRead != null) {
				if (i > 0 && Utils.isNotEmpty(stringRead)) {
					StringTokenizer st = new StringTokenizer(stringRead, ",");
					String rallyname = st.nextToken();
					String jiraname = st.nextToken();
					jiraRallyUserMap.put(rallyname, jiraname);

				}
				i++;
				stringRead = br.readLine();
			}
			br.close();
		}
		if (jiraRallyUserMap.containsKey(rallyUsername)) {
			return jiraRallyUserMap.get(rallyUsername);
		}
		String constructUsername = rallyUsername;
		if (rallyUsername.contains(" ")) {
			constructUsername = rallyUsername.substring(0, 1) + rallyUsername.substring(rallyUsername.indexOf(" ") + 1);
		}

		return constructUsername;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static Map createJiraMap(String key, Object value) {
		if (key.indexOf(".") > 0) {
			String parentKey = key.substring(0, key.lastIndexOf("."));
			String childKey = key.substring(key.lastIndexOf(".") + 1);
			Map m = createJiraMap(childKey, value);
			if (m == null) {
				return null;
			}
			return createJiraMap(parentKey, m);
		}
		Map m = new HashMap();
		boolean isArray = false;
		if (key.endsWith("[]")) {
			key = key.substring(0, key.indexOf("[]"));
			isArray = true;
		}
		if (value instanceof List) {
			List l = (List<String>) value;
			if (isArray) {
				m.put(key, value);
			} else {
				if (l.size() > 0) {
					m.put(key, l.get(0));
				} else {
					m = null;
				}
			}
		} else {
			m.put(key, value);
		}
		return m;
	}

	private static List<String> getValueFromTree(String key, JsonElement je) {
		List<String> lValues = new ArrayList<String>();
		if (key.indexOf(".") > 0) {
			String parentKey = key.substring(0, key.indexOf("."));
			String childkey = key.substring(key.indexOf(".") + 1);
			return getValueFromTree(childkey, je.getAsJsonObject().get(parentKey));

		} else {
			if (!je.isJsonNull()) {
				if (je.isJsonArray()) {
					JsonArray jArr = je.getAsJsonArray();
					for (JsonElement jel : jArr) {
						lValues.add(jel.getAsJsonObject().get(key).getAsString());
					}
				} else {
					lValues.add(je.getAsJsonObject().get(key).getAsString());
				}
			}
		}
		return lValues;
	}
}
