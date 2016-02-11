package ru.aplix.packline.jdbc;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;

public class EnclosureResultSetMetaData implements ResultSetMetaData {

	public EnclosureResultSetMetaData() {
	}

	@Override
	public int getColumnCount() throws SQLException {
		return 7;
	}

	@Override
	public boolean isAutoIncrement(int column) throws SQLException {
		return true;
	}

	@Override
	public boolean isCaseSensitive(int column) throws SQLException {
		return true;
	}

	@Override
	public boolean isSearchable(int column) throws SQLException {
		return false;
	}

	@Override
	public boolean isCurrency(int column) throws SQLException {
		return false;
	}

	@Override
	public int isNullable(int column) throws SQLException {
		switch (column) {
		case 1:
		case 3:
		case 4:
		case 5:
		case 6:
		case 7:
			return ResultSetMetaData.columnNoNulls;
		case 2:
			return ResultSetMetaData.columnNullable;
		default:
			throw new SQLException(new IndexOutOfBoundsException());
		}
	}

	@Override
	public boolean isSigned(int column) throws SQLException {
		return false;
	}

	@Override
	public int getColumnDisplaySize(int column) throws SQLException {
		return -1;
	}

	@Override
	public String getColumnLabel(int column) throws SQLException {
		switch (column) {
		case 1:
			return "INDEX";
		case 2:
			return "DESCRIPTION";
		case 3:
			return "COST";
		case 4:
			return "QUANTITY";
		case 5:
			return "TOTAL_COST";
		case 6:
			return "VAT";
		case 7:
			return "VAT_RATE";
		default:
			throw new SQLException(new IndexOutOfBoundsException());
		}
	}

	@Override
	public String getColumnName(int column) throws SQLException {
		return getColumnLabel(column);
	}

	@Override
	public String getSchemaName(int column) throws SQLException {
		return "";
	}

	@Override
	public int getPrecision(int column) throws SQLException {
		return 0;
	}

	@Override
	public int getScale(int column) throws SQLException {
		return 0;
	}

	@Override
	public String getTableName(int column) throws SQLException {
		return "";
	}

	@Override
	public String getCatalogName(int column) throws SQLException {
		return "";
	}

	@Override
	public int getColumnType(int column) throws SQLException {
		switch (column) {
		case 1:
			return java.sql.Types.INTEGER;
		case 2:
			return java.sql.Types.NVARCHAR;
		case 3:
			return java.sql.Types.FLOAT;
		case 4:
			return java.sql.Types.INTEGER;
		case 5:
			return java.sql.Types.FLOAT;
		case 6:
			return java.sql.Types.FLOAT;
		case 7:
			return java.sql.Types.NVARCHAR;
		default:
			throw new SQLException(new IndexOutOfBoundsException());
		}
	}

	@Override
	public String getColumnTypeName(int column) throws SQLException {
		switch (column) {
		case 1:
			return "INTEGER";
		case 2:
			return "NVARCHAR";
		case 3:
			return "FLOAT";
		case 4:
			return "INTEGER";
		case 5:
			return "FLOAT";
		case 6:
			return "FLOAT";
		case 7:
			return "NVARCHAR";
		default:
			throw new SQLException(new IndexOutOfBoundsException());
		}
	}

	@Override
	public boolean isReadOnly(int column) throws SQLException {
		return true;
	}

	@Override
	public boolean isWritable(int column) throws SQLException {
		return false;
	}

	@Override
	public boolean isDefinitelyWritable(int column) throws SQLException {
		return false;
	}

	@Override
	public String getColumnClassName(int column) throws SQLException {
		switch (column) {
		case 1:
			return Integer.class.getName();
		case 2:
			return String.class.getName();
		case 3:
			return Float.class.getName();
		case 4:
			return Integer.class.getName();
		case 5:
			return Float.class.getName();
		case 6:
			return Float.class.getName();
		case 7:
			return String.class.getName();
		default:
			throw new SQLException(new IndexOutOfBoundsException());
		}
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}
}
