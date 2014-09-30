package com.ceb.rallytojira;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

import org.swift.common.soap.jira.RemoteUser;

public class JiraActiveUsers {

	public static void main(String[] args) throws Exception {

		Set<RemoteUser> activeUsers = new TreeSet<RemoteUser>(new RemoteUserComparator());

		JiraSoapOperations jira = new JiraSoapOperations();
		activeUsers.addAll(Arrays.asList(jira.getGroup("jira-users").getUsers()));
		activeUsers.addAll(Arrays.asList(jira.getGroup("shl-users").getUsers()));
		activeUsers.addAll(Arrays.asList(jira.getGroup("cebvclc-users").getUsers()));
		for (RemoteUser ru : activeUsers) {
			System.out.println(toString(ru));
		}
	}

	private static String toString(RemoteUser ru) {
		return ru.getEmail() + ": " + ru.getFullname();
	}

}

class RemoteUserComparator implements Comparator<RemoteUser> {

	@Override
	public int compare(RemoteUser o1, RemoteUser o2) {
		return o1.getEmail().compareTo(o2.getEmail());
	}

}