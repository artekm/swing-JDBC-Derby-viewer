import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;

import javax.sql.DataSource;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;

import org.apache.derby.jdbc.EmbeddedDataSource;
import org.apache.derby.jdbc.EmbeddedDataSourceInterface;

public class DatabaseService {
	private static Connection connection;
	private static Statement statement;
	private static String dbName;
//	private static final String dbPrefix = "jdbc:derby://localhost:1527/";
//	private static final String dbPrefix = "jdbc:derby:";

	private static final String getTableNamesQuery = "SELECT st.tablename FROM sys.systables st LEFT OUTER JOIN sys.sysschemas ss ON (st.schemaid = ss.schemaid) WHERE ss.schemaname ='APP'";
	private static final String getColumnNamesQuery = "SELECT columnname,columndatatype FROM sys.syscolumns WHERE referenceid = (select tableid FROM sys.systables WHERE tablename = ?) ORDER BY columnnumber";

	public static DataSource getDS(String database, String user, String password) throws SQLException {

		EmbeddedDataSourceInterface ds = new EmbeddedDataSource();

		ds.setDatabaseName(database);
		return ds;
	}

	public class StructureLoader extends SwingWorker<Void, String> {

		private JTextArea textStructure;

		StructureLoader(JTextArea textStructure) {
			this.textStructure = textStructure;
		}

		@Override
		protected Void doInBackground() throws Exception {
			textStructure.setText("");
			ResultSet queryResult = statement.executeQuery(getTableNamesQuery);
			List<String> tablesNames = new LinkedList<>();
			while (queryResult.next()) {
				tablesNames.add(queryResult.getString(1).toUpperCase());
			}
			PreparedStatement preparedStatement = connection.prepareStatement(getColumnNamesQuery);
			for (String tableName : tablesNames) {
				preparedStatement.setString(1, tableName);
				queryResult = preparedStatement.executeQuery();
				StringBuilder tableStruct = new StringBuilder("");
				tableStruct.append(tableName).append("\n");
				while (queryResult.next()) {
					String type = queryResult.getString(2);
					if (type.contains("INTEGER") || type.contains("DECIMAL"))
						tableStruct.append(".[n]");
					else if (type.contains("CHAR"))
						tableStruct.append(".[s]");
					else if (type.contains("DATE"))
						tableStruct.append(".[d]");
					else if (type.contains("TIME"))
						tableStruct.append(".[t]");
					else if (type.contains("BOOLEAN"))
						tableStruct.append(".[b]");
					else
						tableStruct.append(".[?]");
					tableStruct.append(".").append(queryResult.getString(1).toLowerCase()).append("\n");
				}
				tableStruct.append("\n");
				publish(tableStruct.toString());
			}
			return null;
		}

		@Override
		protected void process(List<String> list) {
			for (String line : list)
				textStructure.append(line);
		}
	}

	public void loadStructure(JTextArea forResult) {
		new StructureLoader(forResult).execute();
	}

	public String executeDB(String command) {
		ResultSetPrinter printer = new ResultSetPrinter();
		try {
			boolean isRS = statement.execute(command);
			if (isRS) {
				ResultSet queryResult = statement.getResultSet();
				return (printer.formatAll(queryResult));
			} else {
				int numUpdated = statement.getUpdateCount();
				return ("Zaktualizowano " + numUpdated + " wierszy tabeli");
			}
		} catch (SQLException e) {
			return getExceptionText(e);
		}
	}

	private String breakLine(String text, int breakPos) {
		if (text.length() <= breakPos)
			return text + "\n";
		else
			return text.substring(0, breakPos) + "\n" + breakLine(text.substring(breakPos), breakPos);
	}

	private String getExceptionText(SQLException e) {
		StringBuilder sb = new StringBuilder();
		while (e != null) {
			sb = sb.append(breakLine(e.getMessage(), 80));
			e = e.getNextException();
		}
		return sb.toString();
	}

	public String changeDB(String newName) {
		try {
			disconnectDB();
			connectDB(newName);
			dbName = newName;
			return "Baza " + newName + " podłączona";
		} catch (SQLException e) {
			try {
				connectDB(dbName);
			} catch (SQLException ignored) {
			}
			return getExceptionText(e);
		}
	}

	public String getDbName() {
		return dbName;
	}
	
	public void connectDB(String dbName) throws SQLException {
		if (dbName != null) {
			DataSource ds = getDS(dbName, null, null);
			connection = ds.getConnection();
			statement = connection.createStatement();
		}
	}

	public void disconnectDB() {
		try {
			if (connection != null)
				connection.close();
		} catch (SQLException ignored) {
		}
	}
}
