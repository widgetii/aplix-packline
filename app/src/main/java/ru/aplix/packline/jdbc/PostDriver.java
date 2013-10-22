package ru.aplix.packline.jdbc;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class PostDriver implements Driver {

	private static final Log LOG;

	static {
		LOG = LogFactory.getLog(PostDriver.class);
		try {
			DriverManager.registerDriver(new PostDriver());
		} catch (Exception e) {
			if (LOG != null)
				LOG.error("Could not register with driver manager", e);
		}
	}

	@Override
	public Connection connect(String url, Properties info) throws SQLException {
		if (acceptsURL(url)) {
			return new PostConnection(url, info);
		} else {
			throw new SQLException("Invalid URL");
		}
	}

	@Override
	public boolean acceptsURL(String url) throws SQLException {
		return url.startsWith("http://") || url.startsWith("https://");
	}

	@Override
	public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
		return new DriverPropertyInfo[0];
	}

	@Override
	public int getMajorVersion() {
		return 1;
	}

	@Override
	public int getMinorVersion() {
		return 0;
	}

	@Override
	public boolean jdbcCompliant() {
		return false;
	}

	@Override
	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		throw new SQLFeatureNotSupportedException();
	}
}
