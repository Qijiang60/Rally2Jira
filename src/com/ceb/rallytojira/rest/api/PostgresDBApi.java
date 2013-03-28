package com.ceb.rallytojira.rest.api;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class PostgresDBApi {
	static Connection con = null;
	static Statement st = null;
	static ResultSet rs = null;
	static SimpleDateFormat df = new SimpleDateFormat();;
	static PreparedStatement updateIssue = null;

	public static void updateIsueDates(String databaseId, String creationDate, String lastUpdateDate) throws ParseException, SQLException {

		String url = "jdbc:postgresql://172.22.26.20/jiradb";
		String user = "jiradbuser";
		String password = "j1r$d8us3x!";
		String updateString =
				"update JIRAISSUE " +
						"set CREATED = ? , UPDATED = ? where ID = ?";

		try {
			if (con == null) {
				con = DriverManager.getConnection(url, user, password);
				updateIssue = con.prepareStatement(updateString);
			}
			con.setAutoCommit(false);

			updateIssue.setDate(1, new java.sql.Date(df.parse(creationDate).getTime()));
			updateIssue.setDate(2, new java.sql.Date(df.parse(lastUpdateDate).getTime()));
			updateIssue.setInt(2, Integer.parseInt(databaseId));
			updateIssue.executeUpdate();
			con.commit();

		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			if (updateIssue != null) {
				updateIssue.close();
			}
			con.setAutoCommit(true);
		}

	}
}
