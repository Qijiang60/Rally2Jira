package com.ceb.rallytojira.rest.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
	private static Map<String, Boolean> userStatusMap;
	private static Map<String, Map<String, String>> elementMapping = new HashMap<String, Map<String, String>>();
	private static Map<String, String> workflowStatusMapping;
	private static Map<String, String> priorityMapping;
	private static Map<String, List<String>> elementMap = new HashMap<String, List<String>>();

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
		List<String> elements = elementMap.get(artifactType.getCode());
		if (elements == null) {
			elements = new ArrayList<String>();
			elementMap.put(artifactType.getCode(), elements);
			FileReader fr = getFileReader(project, artifactType.getCode());
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
		}
		return elements;

	}

	private static FileReader getFileReader(String project, String artifactType) throws FileNotFoundException {
		File f = new File("mappings/" + project + "/" + artifactType);
		if (!f.exists()) {
			f = new File("mappings/Common/" + artifactType);
		}
		return new FileReader(f);
	}

	public static Map<String, String> getElementMapping(RallyObject artifactType, String project) throws IOException {
		Map<String, String> artifactMap = elementMapping.get(artifactType.getCode());
		if (artifactMap == null) {
			artifactMap = new HashMap<String, String>();
			elementMapping.put(artifactType.getCode(), artifactMap);
			FileReader fr = getFileReader(project, artifactType.getCode());
			BufferedReader br = new BufferedReader(fr);
			String stringRead = br.readLine();
			int i = 0;
			while (stringRead != null) {
				if (i > 0 && Utils.isNotEmpty(stringRead)) {
					StringTokenizer st = new StringTokenizer(stringRead, ",");
					String jiraField = st.nextToken();
					if (!"NULL".equals(jiraField)) {
						String rallyField = st.nextToken();
						artifactMap.put(jiraField, rallyField);
					}
				}
				i++;
				stringRead = br.readLine();
			}
			br.close();
		}
		return artifactMap;
	}

	public static Map<String, String> getWorkflowStatusMapping(String project) throws IOException {
		if (workflowStatusMapping == null) {
			workflowStatusMapping = new HashMap<String, String>();
			FileReader fr = getFileReader(project, "workflow_status_mapping");
			BufferedReader br = new BufferedReader(fr);
			String stringRead = br.readLine();
			int i = 0;
			while (stringRead != null) {
				if (i > 0 && Utils.isNotEmpty(stringRead)) {
					StringTokenizer st = new StringTokenizer(stringRead, ",");
					String jiraField = st.nextToken();
					if (!"NULL".equals(jiraField)) {
						String rallyField = st.nextToken();
						workflowStatusMapping.put(jiraField, rallyField);
					}
				}
				i++;
				stringRead = br.readLine();
			}
			br.close();
		}
		return workflowStatusMapping;
	}

	public static Map<String, String> getPriorityMapping(String project) throws IOException {
		if (priorityMapping == null) {
			priorityMapping = new HashMap<String, String>();
			FileReader fr = getFileReader(project, "priorities_mapping");
			BufferedReader br = new BufferedReader(fr);
			String stringRead = br.readLine();
			int i = 0;
			while (stringRead != null) {
				if (i > 0 && Utils.isNotEmpty(stringRead)) {
					StringTokenizer st = new StringTokenizer(stringRead, ",");
					String jiraField = st.nextToken();
					if (!"NULL".equals(jiraField)) {
						String rallyField = st.nextToken();
						priorityMapping.put(jiraField, rallyField);
					}
				}
				i++;
				stringRead = br.readLine();
			}
			br.close();
		}
		return priorityMapping;
	}

	public static void printJson(Map<String, Object> data) {

		System.out.println(mapToJsonString(data));

	}

	public static String mapToJsonString(Map<String, Object> data) {
		Gson gson = new GsonBuilder().disableHtmlEscaping().create();
		String s = gson.toJson(data);
		s = removeStyleTags(s);
		Source htmlSource = new Source(s);
		Renderer r = htmlSource.getRenderer();
		r.setMaxLineLength(5000);
		s = r.toString();
		s = s.replace("\r\n", "\\n");
		s = s.replace("\t", "\\t");
		return s;
	}

	private static String removeStyleTags(String s) {
		return s.replaceAll("( style=\\\\\\\"[^\\\"]*\\\\\\\")", "");

	}

	public static JsonObject jerseyRepsonseToJsonObject(ClientResponse response) {
		if (response.getStatus() == 204) {
			return new JsonObject();
		}
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
	public static Map getJiraValue(String jiraKey, String rallyKey, JsonObject userStory, JsonObject project) throws IOException {
		List<String> values = getValueFromTree(rallyKey, userStory);
		if (jiraKey.equals("labels[]")) {
			List<String> labelsWithoutSpaces = new ArrayList<String>();
			for (String s : values) {
				labelsWithoutSpaces.add(s.replace(" ", ""));
			}

			values = labelsWithoutSpaces;
		}
		if (jiraKey.equals("reporter.name")) {
			List<String> jiraUser = new ArrayList<String>();
			for (String s : values) {
				jiraUser.add(lookupJiraUsername(getJsonObjectName(project), s));
			}
			values = jiraUser;
		}
		if (jiraKey.equals("priority.name")) {
			List<String> translatedPriority = new ArrayList<String>();
			for (String s : values) {
				String jiraPriority = getJiraPriority(getJsonObjectName(project), s);
				translatedPriority.add(jiraPriority.substring(0, jiraPriority.indexOf("~")));
			}
			values = translatedPriority;
		}
		if (jiraKey.equals("summary")) {
			List<String> truncatedSummary = new ArrayList<String>();
			for (String s : values) {
				if (s.length() > 255) {
					truncatedSummary.add(s.substring(0, 255));
				} else {
					truncatedSummary.add(s);
				}
			}
			values = truncatedSummary;
		}
		if (jiraKey.startsWith("timetracking")) {
			List<String> timeInHours = new ArrayList<String>();
			for (String s : values) {
				timeInHours.add(s + "h");
			}
			values = timeInHours;
		}
		if (values.size() > 0) {
			Map jiraMap = createJiraMap(jiraKey, values);
			return jiraMap;
		}
		return null;
	}

	private static String getJiraPriority(String project, String rallyPriority) throws IOException {
		Map<String, String> pMap = getPriorityMapping(project);
		for (String jiraPriority : pMap.keySet()) {
			if (pMap.get(jiraPriority).equalsIgnoreCase(rallyPriority)) {
				return jiraPriority;
			}
		}
		return null;
	}

	public static String lookupJiraUsername(String projectName, String rallyUserObjectID) throws IOException {
		if (jiraRallyUserMap == null) {
			createJiraRallyUserMap(projectName);
		}
		return jiraRallyUserMap.get(rallyUserObjectID);
	}

	public static Boolean getUserStatus(String projectName, String jiraUsername) throws IOException {
		if (userStatusMap == null) {
			createJiraRallyUserMap(projectName);
		}
		return userStatusMap.get(jiraUsername);
	}

	private static void createJiraRallyUserMap(String projectName) throws FileNotFoundException, IOException {
		jiraRallyUserMap = new TreeMap<String, String>();
		userStatusMap = new TreeMap<String, Boolean>();
		FileReader fr = new FileReader("mappings/jira_rally_user_mapping_" + projectName.replaceAll(" ", "_"));
		BufferedReader br = new BufferedReader(fr);
		String stringRead = br.readLine();
		int i = 0;
		while (stringRead != null) {
			if (i > 0 && Utils.isNotEmpty(stringRead)) {
				StringTokenizer st = new StringTokenizer(stringRead, "\t");
				String rallyObjectID = st.nextToken();
				String rallyDisplayName = st.nextToken();
				String jiraDisplayName = st.nextToken();
				String rallyUserName = st.nextToken();
				String jiraUserName = st.nextToken();
				String disabled = st.nextToken();
				String match = st.nextToken();
				jiraRallyUserMap.put(rallyObjectID, jiraUserName);
				userStatusMap.put(jiraUserName, Boolean.parseBoolean(disabled));

			}
			i++;
			stringRead = br.readLine();
		}
		br.close();
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
						String val = jel.getAsJsonObject().get(key).getAsString();
						if (Utils.isNotEmpty(val)) {
							lValues.add(val);
						}
					}
				} else {
					if (!je.getAsJsonObject().get(key).isJsonNull()) {
						String val = je.getAsJsonObject().get(key).getAsString();
						if (Utils.isNotEmpty(val)) {
							lValues.add(val);
						}
					}
				}
			}
		}
		return lValues;
	}

	public static String getJiraTransitionId(String project, String rallyStatus) throws IOException {
		Map<String, String> statusMap = getWorkflowStatusMapping(project);
		for (String jiraStatus : statusMap.keySet()) {
			String rallyStatusInMap = statusMap.get(jiraStatus);
			if (rallyStatusInMap.indexOf(";") > 0) {
				rallyStatusInMap = rallyStatusInMap.substring(0, rallyStatusInMap.indexOf(";"));
			}
			if (rallyStatus.equalsIgnoreCase(rallyStatusInMap)) {
				String jiraTransitionId = jiraStatus.substring(jiraStatus.indexOf(";") + 1);
				return jiraTransitionId;
			}
		}
		return null;
	}

	public static String setToString(Set<String> setOfStrings) {
		StringBuffer sb = new StringBuffer();
		for(String s : setOfStrings){
			sb.append(s +"\n");
		}
		return sb.toString();
	}
}
