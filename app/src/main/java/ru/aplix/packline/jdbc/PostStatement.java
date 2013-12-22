package ru.aplix.packline.jdbc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import ru.aplix.packline.post.FieldList;
import ru.aplix.packline.post.PackingLinePortType;
import ru.aplix.packline.post.StringList;
import ru.aplix.packline.post.TagList;

public class PostStatement implements Statement {

	private static final int ERROR_CODE_INVALID_SQL_STATEMENT = 0x9876;

	private static final int MAX_QUERIES = 10;

	private Connection connection;
	private PackingLinePortType postServicePort;
	private Map<String, ResultSet> lastQueries;
	private ResultSet lastResultSet;
	private Pattern queryPostPattern;
	private Pattern queryMarkerCustPattern;
	private Pattern queryMarkerContPattern;

	@SuppressWarnings("serial")
	public PostStatement(Connection connection, PackingLinePortType postServicePort) {
		this.connection = connection;
		this.postServicePort = postServicePort;

		lastQueries = new LinkedHashMap<String, ResultSet>() {
			@Override
			protected boolean removeEldestEntry(java.util.Map.Entry<String, ResultSet> eldest) {
				return size() > MAX_QUERIES;
			}
		};

		queryPostPattern = Pattern.compile("\\s*SELECT\\s+([\\w\\s,]+)\\s+FROM\\s+POST\\s+WHERE\\s+CONTAINER_ID\\s*=\\s*'([\\w]+)'\\s+AND\\s+QUERY_ID\\s*=\\s*'([\\w]+)'\\s*;\\s*");
		queryMarkerCustPattern = Pattern
				.compile("\\s*SELECT\\s+[\\w\\s,]+\\s+FROM\\s+MARKERS\\s+WHERE\\s+CUSTOMER_CODE\\s*=\\s*'([\\w]+)'\\s+AND\\s+QUERY_ID\\s*=\\s*'([\\w]+)'\\s+LIMIT\\s+([\\d]+)\\s*;\\s*");
		queryMarkerContPattern = Pattern.compile("\\s*SELECT\\s+[\\w\\s,]+\\s+FROM\\s+MARKERS\\s+WHERE\\s+QUERY_ID\\s*=\\s*'([\\w]+)'\\s+LIMIT\\s+([\\d]+)\\s*;\\s*");
	}

	@Override
	public ResultSet executeQuery(String sql) throws SQLException {
		if (sql != null && lastQueries.containsKey(sql)) {
			lastResultSet = lastQueries.get(sql);
			if (lastResultSet != null) {
				lastResultSet.beforeFirst();
			}
			return lastResultSet;
		}

		int i = 0;
		ResultSet resultSet = null;
		boolean cachable = false;
		while (resultSet == null) {
			i++;
			try {
				switch (i) {
				case 1:
					resultSet = executePostQuery(sql);
					cachable = true;
					break;
				case 2:
					resultSet = executeMarkerCustQuery(sql);
					cachable = true;
					break;
				case 3:
					resultSet = executeMarkerContQuery(sql);
					cachable = true;
					break;
				default:
					throw new SQLException("Invalid SQL statement");
				}
			} catch (SQLException sqle) {
				if (sqle.getErrorCode() != ERROR_CODE_INVALID_SQL_STATEMENT) {
					throw sqle;
				}
			}
		}

		// Return result set with data
		lastResultSet = resultSet;
		if (cachable) {
			lastQueries.put(sql, lastResultSet);
		}
		return lastResultSet;
	}

	private ResultSet executePostQuery(String sql) throws SQLException {
		// Validate query
		Matcher queryMatcher = queryPostPattern.matcher(sql);
		if (!queryMatcher.matches() || queryMatcher.groupCount() != 3) {
			throw new SQLException("Invalid SQL statement", null, ERROR_CODE_INVALID_SQL_STATEMENT);
		}

		// Parse query parameters
		String containerId = queryMatcher.group(2);
		String allColumns = queryMatcher.group(1).replace(",", " ");

		StringList parameters = new StringList();
		String[] sArr = StringUtils.split(allColumns);
		for (String s : sArr) {
			parameters.getItems().add(s);
		}

		// Call remote post service
		FieldList fieldList = postServicePort.gatherInfo(containerId, parameters);
		if (fieldList == null) {
			throw new SQLException("Invalid response from post service");
		}

		// Return result set with data
		return new PostResultSet(this, fieldList.getItems());
	}

	private ResultSet executeMarkerCustQuery(String sql) throws SQLException {
		// Validate query
		Matcher queryMatcher = queryMarkerCustPattern.matcher(sql);
		if (!queryMatcher.matches() || queryMatcher.groupCount() != 3) {
			throw new SQLException("Invalid SQL statement", null, ERROR_CODE_INVALID_SQL_STATEMENT);
		}

		// Parse query parameters
		String customerCode = queryMatcher.group(1);
		String count = queryMatcher.group(3);

		// Call remote post service
		TagList tagList = postServicePort.generateTagsForIncomings(customerCode, Integer.parseInt(count));
		if (tagList == null) {
			throw new SQLException("Invalid response from post service");
		}

		// Return result set with data
		return new MarkerResultSet(this, tagList.getItems());
	}

	private ResultSet executeMarkerContQuery(String sql) throws SQLException {
		// Validate query
		Matcher queryMatcher = queryMarkerContPattern.matcher(sql);
		if (!queryMatcher.matches() || queryMatcher.groupCount() != 2) {
			throw new SQLException("Invalid SQL statement", null, ERROR_CODE_INVALID_SQL_STATEMENT);
		}

		// Parse query parameters
		String count = queryMatcher.group(2);

		// Call remote post service
		TagList tagList = postServicePort.generateTagsForContainers(Integer.parseInt(count));
		if (tagList == null) {
			throw new SQLException("Invalid response from post service");
		}

		// Return result set with data
		return new MarkerResultSet(this, tagList.getItems());
	}

	@Override
	public boolean execute(String sql) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public boolean execute(String sql, int[] columnIndexes) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public boolean execute(String sql, String[] columnNames) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public int[] executeBatch() throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public int executeUpdate(String sql) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public int executeUpdate(String sql, String[] columnNames) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void close() throws SQLException {
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public int getMaxFieldSize() throws SQLException {
		return 0;
	}

	@Override
	public void setMaxFieldSize(int max) throws SQLException {
	}

	@Override
	public int getMaxRows() throws SQLException {
		return 0;
	}

	@Override
	public void setMaxRows(int max) throws SQLException {
	}

	@Override
	public void setEscapeProcessing(boolean enable) throws SQLException {
	}

	@Override
	public int getQueryTimeout() throws SQLException {
		return 0;
	}

	@Override
	public void setQueryTimeout(int seconds) throws SQLException {
	}

	@Override
	public void cancel() throws SQLException {
	}

	@Override
	public SQLWarning getWarnings() throws SQLException {
		return null;
	}

	@Override
	public void clearWarnings() throws SQLException {
	}

	@Override
	public void setCursorName(String name) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public ResultSet getResultSet() throws SQLException {
		return lastResultSet;
	}

	@Override
	public int getUpdateCount() throws SQLException {
		return -1;
	}

	@Override
	public boolean getMoreResults() throws SQLException {
		return false;
	}

	@Override
	public void setFetchDirection(int direction) throws SQLException {
	}

	@Override
	public int getFetchDirection() throws SQLException {
		return ResultSet.FETCH_UNKNOWN;
	}

	@Override
	public void setFetchSize(int rows) throws SQLException {
	}

	@Override
	public int getFetchSize() throws SQLException {
		if (lastResultSet != null) {
			return lastResultSet.getFetchSize();
		} else {
			return 0;
		}
	}

	@Override
	public int getResultSetConcurrency() throws SQLException {
		return ResultSet.CONCUR_READ_ONLY;
	}

	@Override
	public int getResultSetType() throws SQLException {
		return ResultSet.TYPE_SCROLL_INSENSITIVE;
	}

	@Override
	public void addBatch(String sql) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void clearBatch() throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public Connection getConnection() throws SQLException {
		return connection;
	}

	@Override
	public boolean getMoreResults(int current) throws SQLException {
		return false;
	}

	@Override
	public ResultSet getGeneratedKeys() throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public int getResultSetHoldability() throws SQLException {
		return ResultSet.HOLD_CURSORS_OVER_COMMIT;
	}

	@Override
	public boolean isClosed() throws SQLException {
		return true;
	}

	@Override
	public void setPoolable(boolean poolable) throws SQLException {
	}

	@Override
	public boolean isPoolable() throws SQLException {
		return false;
	}

	@Override
	public void closeOnCompletion() throws SQLException {
	}

	@Override
	public boolean isCloseOnCompletion() throws SQLException {
		return false;
	}
}
