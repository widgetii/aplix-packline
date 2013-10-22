package ru.aplix.packline.jdbc;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.List;

import ru.aplix.packline.post.Field;

public class PostResultSetMetaData implements ResultSetMetaData {

	private List<Field> fields;

	public PostResultSetMetaData(List<Field> fields) {
		this.fields = fields;
	}

	@Override
	public int getColumnCount() throws SQLException {
		return fields.size();
	}

	private Field getColumn(int column) throws SQLException {
		try {
			return fields.get(column - 1);
		} catch (IndexOutOfBoundsException ioobe) {
			throw new SQLException(ioobe);
		}
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
		return getColumn(column).getValue() != null ? ResultSetMetaData.columnNoNulls : ResultSetMetaData.columnNullable;
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
		return getColumn(column).getName();
	}

	@Override
	public String getColumnName(int column) throws SQLException {
		return getColumn(column).getName();
	}

	@Override
	public String getSchemaName(int column) throws SQLException {
		return "";
	}

	@Override
	public int getPrecision(int column) throws SQLException {
		return getColumn(column).getValue().length();
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
		return java.sql.Types.NVARCHAR;
	}

	@Override
	public String getColumnTypeName(int column) throws SQLException {
		return "NVARCHAR";
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
		return String.class.getName();
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
