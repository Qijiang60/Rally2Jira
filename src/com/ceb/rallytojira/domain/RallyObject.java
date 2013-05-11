package com.ceb.rallytojira.domain;

public enum RallyObject {
	PROJECT("project"), RELEASE("release"), ITERATION("iteration"), USER_STORY(
			"hierarchicalrequirement"), DEFECT("defect"), TASK("task"), ATTACHMENT("attachment"), ATTACHMENT_CONTENT("AttachmentContent"), USER("User"), WORKSPACE("Workspace"), SUBSCRIPTION("Subscription"), TEST_CASE("TestCase");

	private String code;

	private RallyObject(String s) {
		code = s;
	}

	public String getCode() {
		return code;
	}

}
