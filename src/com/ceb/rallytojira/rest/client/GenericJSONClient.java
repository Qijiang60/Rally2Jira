package com.ceb.rallytojira.rest.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.ceb.rallytojira.rest.api.StreamClient;
import com.ceb.rallytojira.rest.api.StreamClientException;

public class GenericJSONClient {
	private StreamClient client;

	private static Logger logger = Logger.getLogger(GenericJSONClient.class
			.getName());

	public GenericJSONClient(String actionLogPK) {
		logger = Logger.getLogger(actionLogPK);
	}

	private String getApiURL(String serverName) {
		return "https://rally1.rallydev.com/slm";
	}

	public JSONObject searchObject(String objectCode, String objectName,
			Map<String, Object> filter, List<String> dataElements)
			throws Exception {

		JSONObject object = null;
		if (Utils.isEmpty(dataElements)) {
			dataElements = new ArrayList<String>();
		}
		dataElements.add("ID");
		dataElements.add("name");

		if (Utils.isNotEmpty(objectName)) {
			if (Utils.isEmpty(filter)) {
				filter = new HashMap<String, Object>();
			}

			filter.put("name_Mod", "cieq");
			filter.put("name", objectName);
		}
		logger.debug("Search: [" + objectCode + "] [" + filter + "]");

		JSONArray results = client.search(objectCode, filter,
				dataElements.toArray(new String[dataElements.size()]));

		logger.debug(results.length() + " record(s)"
				+ " found. Printing upto 3 IDs...");
		for (int i = 0; i < results.length(); i++) {
			JSONObject temp = results.getJSONObject(i);
			logger.debug("[" + (i + 1) + "/" + results.length() + "]: ["
					+ temp.getString("ID") + "]");
			if (i == 2) {
				break;
			}
		}
		if (results.length() > 1) {
			throw new Exception("Found multiple objects. [" + objectCode
					+ "] [" + filter + "]");
		}
		if (results.length() == 1) {
			object = results.getJSONObject(0);
		}
		return object;
	}

	public void logout() throws StreamClientException {
		client.logout();
	}

	public JSONObject create(String objCode, Map<String, Object> map)
			throws StreamClientException {
		return client.post(objCode, map);
	}

	public boolean update(String objCode, String objectId,
			Map<String, Object> map, List<String> errors) {
		Map<String, Object> temp = new HashMap<String, Object>();
		boolean updateSuccessful = true;
		for (String key : map.keySet()) {
			Object value = map.get(key);
			temp.put(key, value);
			try {
				// logger.debug("Update: " + "[" + objCode + "] [" + key + "] ["
				// + value + "]");
				client.put(objCode, objectId, temp);
			} catch (Exception e) {
				updateSuccessful = false;
				Utils.addError(errors, e.getMessage(), logger);
			}
			temp.clear();
		}
		return updateSuccessful;
	}

	public JSONObject executeAction(String objCode, String objectId,
			String actionName, Map<String, Object> map)
			throws StreamClientException {
		return client.action(objCode, objectId, actionName, map);
	}

	public void useCachedSession(String server, String sessionId) {
		client = new StreamClient(getApiURL(server));
		client.setSessionId(sessionId);
	}

}