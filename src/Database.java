import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Database {
	//uses https://bitbucket.org/xerial/sqlite-jdbc
	static Logger logger = LoggerFactory.getLogger(Database.class);
	Connection connection = null;
	Statement statement = null;
	public static String dbFile = "./resources/db/" + Config.appName.toLowerCase()+"-"+Config.majorVersionDB.toString()+".db";	
	private static Database instance = null;

	public static Database getInstance() {
		if(instance == null) {
			instance = new Database();
		}
		return instance;
	}

	private Database() {
		Boolean dbExists = true;
		if (!(new File(dbFile)).exists()) {
			dbExists = false;
		}
		init();
		if (!dbExists) {
			Blocks blocks = Blocks.getInstance();
			blocks.createTables();
		}
	}

	public void init() {
		try {
			Class.forName("org.sqlite.JDBC");
		} catch (Exception e) {
		}
		try {
			connection = DriverManager.getConnection("jdbc:sqlite:"+dbFile);
			statement = connection.createStatement();
			statement.setQueryTimeout(30);
		} catch (SQLException e) {
		}
	}

	public void executeUpdate(String query) {
		try {
			(connection.createStatement()).executeUpdate(query);
			logger.info("Update/Insert query: "+query);
		} catch (Exception e) {
			logger.error(e.toString());
			logger.error("Offending query: "+query);
			//System.exit(0);						
		}
	}

	public ResultSet executeQuery(String query) {
		try {
			ResultSet rs = (connection.createStatement()).executeQuery(query);
			logger.info("Select query: "+query);
			return rs;
		} catch (SQLException e) {
			logger.error(e.toString());
			logger.error("Offending query: "+query);
			//System.exit(0);						
		}
		return null;
	}

}
