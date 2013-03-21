package com.ceb.rallytojira;

import javax.naming.AuthenticationException;

import org.json.JSONException;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.Base64;

public class JerseyTest {
	public static void main(String[] args) throws AuthenticationException, JSONException {
		JerseyTest.getAllIssues("hagarwal", "Welcome234!",
				"http://scrumportal.executiveboard.com");
	}

	private static void getAllIssues(String username, String password,
			String url) throws AuthenticationException, JSONException {
		
//		final int statusCode = response.getStatus();
//		if (statusCode == 401) {
//			throw new AuthenticationException("Invalid Username or Password");
//		}
//		final String stringResponse = response.getEntity(String.class);
//		System.out.println("sr: " + stringResponse);
		
	}

}
