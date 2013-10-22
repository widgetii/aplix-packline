package ru.aplix.packline;

import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.JAXBException;

import junit.framework.TestCase;
import ru.aplix.packline.conf.Configuration;
import ru.aplix.packline.conf.PostService;

public class PostJDBCDriverTestManual extends TestCase {

	private static final String[] FIELDS = new String[] { "ADRES", "CITY", "AREA", "REGION" };
	private static final String CONTAINER_ID = "123456789";

	public void testDriver() throws FileNotFoundException, MalformedURLException, JAXBException, ClassNotFoundException, SQLException {
		String fieldsAsString = Arrays.toString(FIELDS).replaceAll("[\\[\\]]", "");
		String query = String.format("SELECT %s FROM POST WHERE CONTAINER_ID='%s';", fieldsAsString, CONTAINER_ID);

		PostService psConf = Configuration.getInstance().getPostService();

		final Map<String, String> fields = new Hashtable<String, String>();
		openResultSet(psConf, query, new RecordListener() {
			@Override
			public void OnRecordFetched(String columnName, String columnValue) {
				fields.put(columnName, columnValue);
			}
		});

		for (String s : FIELDS) {
			assertTrue(fields.containsKey(s));
		}
	}

	private void openResultSet(PostService psConf, String query, RecordListener listener) throws ClassNotFoundException, java.sql.SQLException {
		// Register database JDBC driver
		Class.forName(ru.aplix.packline.jdbc.PostDriver.class.getName());

		java.sql.Connection con = null;
		java.sql.Statement stmt = null;
		java.sql.ResultSet rs = null;
		try {
			// Connect to the database
			con = java.sql.DriverManager.getConnection(psConf.getServiceAddress(), psConf.getUserName(), psConf.getPassword());
			con.setTransactionIsolation(java.sql.Connection.TRANSACTION_READ_COMMITTED);

			// Execute main query
			stmt = con.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY, java.sql.ResultSet.CONCUR_READ_ONLY);
			rs = stmt.executeQuery(query);

			// Gather columns info
			Map<String, Integer> columnsInfo = new HashMap<String, Integer>();
			java.sql.ResultSetMetaData rsMetaData = rs.getMetaData();
			for (int i = 1; i <= rsMetaData.getColumnCount(); i++) {
				String columnName = rsMetaData.getColumnLabel(i);
				Integer columnType = rsMetaData.getColumnType(i);
				columnsInfo.put(columnName, columnType);
			}

			// Enumerate records in result set
			while (rs.next()) {
				for (Entry<String, Integer> entry : columnsInfo.entrySet()) {
					String columnName = entry.getKey();
					listener.OnRecordFetched(columnName, rs.getString(columnName));
				}
			}
		} finally {
			// Close all resources
			if (rs != null) {
				rs.close();
			}

			if (stmt != null) {
				stmt.close();
			}

			if (con != null) {
				con.close();
			}
		}
	}

	/**
	 *
	 */
	public interface RecordListener {
		void OnRecordFetched(String columnName, String columnValue);
	}
}
