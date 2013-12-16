package com.ceb.rallytojira;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class RallyToJiraSpecial1 {

	JiraRestOperations jira;
	Map<String, String> releaseVersionMap = new HashMap<String, String>();
	int counter = 0;
	int limit = 30000000;
	int progress = 0;

	public RallyToJiraSpecial1() throws Exception {
		jira = new JiraRestOperations();
	}

	public static void main(String[] args) throws URISyntaxException,
			Exception {
		RallyToJiraSpecial1 rallyToJira = new RallyToJiraSpecial1();
		rallyToJira.process();

	}

	private void process() throws Exception {
		Set<String> issueKeys = jira.searchIssues("project=SFDC AND issuetype=Test AND assignee in (tberg)");
		System.out.println(issueKeys.size());

		for (String key : issueKeys) {
			jira.addLink("SFDC-16110", key);
		}
	}
}
