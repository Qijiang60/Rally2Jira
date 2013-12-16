package com.ceb.rallytojira;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

import com.ceb.rallytojira.domain.RallyObject;
import com.ceb.rallytojira.rest.client.Utils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class ExportTestCases {

	RallyOperations rally;
	JiraRestOperations jira;
	JiraSoapOperations jiraSoap;
	Map<String, String> releaseVersionMap = new HashMap<String, String>();
	int counter = 0;
	int limit = 30000000;
	int progress = 0;

	public ExportTestCases() throws Exception {
		rally = new RallyOperations();
		jira = new JiraRestOperations();
		jiraSoap = new JiraSoapOperations();
	}

	public static void main(String[] args) throws URISyntaxException,
			Exception {
		ExportTestCases rallyToJira = new ExportTestCases();
		rallyToJira.process();

	}

	private void process() throws Exception {

		Map<String, List<String>> projectMapping = Utils.getProjectMapping();
		JsonArray workspaces = rally.getAllWorkspaces();

		for (JsonElement workspaceEle : workspaces) {
			JsonObject workspace = workspaceEle.getAsJsonObject();
			JsonArray projects = workspace.get("Projects").getAsJsonArray();
			for (JsonElement projEle : projects) {
				JsonObject project = rally.getObjectFromRef(projEle);
				String key = Utils.getKeyForWorkspaceAndProject(workspace, project);
				// if (projectMapping.containsKey(key)) {
				System.out.println(key);
				rally.updateDefaultWorkspace(workspace, project);
				exportTestCases(workspace, project, key);
				// }
			}
		}
	}

	private void exportTestCases(JsonObject workspace, JsonObject project, String keyFile) throws Exception {
		keyFile = keyFile.replaceAll("\\\\", "_");
		keyFile = keyFile.replaceAll("/", "_");

		File f = new File("C:\\CLCTestCases\\" + keyFile + ".xls");
		if (f.exists()) {
			return;
		}
		List<String> elementsToBeFetched = Utils.elementsTobeFetched(RallyObject.TEST_CASE);
		JsonArray testCases = rally.getRallyObjectsForProject(project, RallyObject.TEST_CASE);
		int size = testCases.size();
		System.out.println(testCases.size());
		HSSFWorkbook workbook = new HSSFWorkbook();
		HSSFSheet sheet = workbook.createSheet("Test Cases");
		int rownum = 0;
		int cellnum = 0;
		Row row = sheet.createRow(rownum++);
		for (String key : elementsToBeFetched) {
			Cell cell = row.createCell(cellnum++);
			cell.setCellValue(key);
			if (key.equals("WorkProduct")) {
				cell = row.createCell(cellnum++);
				cell.setCellValue("User Story ID");

			}
		}
		for (JsonElement testCaseEle : testCases) {

			JsonObject testCase = rally.findRallyObjectByObjectID(RallyObject.TEST_CASE, testCaseEle.getAsJsonObject().get("ObjectID").getAsString());
			row = sheet.createRow(rownum++);
			cellnum = 0;
			System.out.println(rownum + "/" + size);
			for (String key : elementsToBeFetched) {
				JsonElement valueE = testCase.get(key);

				String value = "";
				if (valueE != null && !valueE.isJsonNull()) {
					if (valueE.isJsonObject()) {
						value = valueE.getAsJsonObject().get("Name").getAsString();
					} else {
						if (valueE.isJsonArray()) {
							for (JsonElement je : valueE.getAsJsonArray()) {
								value = value + ", " + je.getAsJsonObject().get("Name").getAsString();
							}
						} else {
							value = valueE.getAsString();
						}
					}
				}
				Cell cell = row.createCell(cellnum++);
				String s = Utils.clean(value);
				int index = s.length();
				if (index > 32766) {
					index = 32766;
				}
				cell.setCellValue(s.substring(0, index));
				if (key.equals("WorkProduct")) {
					cell = row.createCell(cellnum++);
					if (valueE != null && !valueE.isJsonNull()) {
						cell.setCellValue(Utils.clean(valueE.getAsJsonObject().get("FormattedID").getAsString()));
					}
				}

			}
		}
		try {
			FileOutputStream out = new FileOutputStream(f);
			workbook.write(out);
			out.close();
			System.out.println("Excel written successfully..");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
