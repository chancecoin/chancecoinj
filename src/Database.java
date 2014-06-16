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
		createTables();
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
	
	public void createTables() {
		try {
			// Blocks
			executeUpdate("CREATE TABLE IF NOT EXISTS blocks(block_index INTEGER PRIMARY KEY, block_hash TEXT UNIQUE, block_time INTEGER)");
			executeUpdate("CREATE INDEX IF NOT EXISTS blocks_block_index_idx ON blocks (block_index)");

			// Transactions
			executeUpdate("CREATE TABLE IF NOT EXISTS transactions(tx_index INTEGER PRIMARY KEY, tx_hash TEXT UNIQUE, block_index INTEGER, block_time INTEGER, source TEXT, destination TEXT, btc_amount INTEGER, fee INTEGER, data BLOB, supported BOOL DEFAULT 1)");
			executeUpdate("CREATE INDEX IF NOT EXISTS transactions_block_index_idx ON transactions (block_index)");
			executeUpdate("CREATE INDEX IF NOT EXISTS transactions_tx_index_idx ON transactions (tx_index)");
			executeUpdate("CREATE INDEX IF NOT EXISTS transactions_tx_hash_idx ON transactions (tx_hash)");

			// (Valid) debits
			executeUpdate("CREATE TABLE IF NOT EXISTS debits(block_index INTEGER, address TEXT, asset TEXT, amount INTEGER, calling_function TEXT, event TEXT)");
			executeUpdate("CREATE INDEX IF NOT EXISTS debits_address_idx ON debits (address)");

			// (Valid) credits
			executeUpdate("CREATE TABLE IF NOT EXISTS credits(block_index INTEGER, address TEXT, asset TEXT, amount INTEGER, calling_function TEXT, event TEXT)");
			executeUpdate("CREATE INDEX IF NOT EXISTS credits_address_idx ON credits (address)");

			// Balances
			executeUpdate("CREATE TABLE IF NOT EXISTS balances(address TEXT, asset TEXT, amount INTEGER)");
			executeUpdate("CREATE INDEX IF NOT EXISTS address_idx ON balances (address)");
			executeUpdate("CREATE INDEX IF NOT EXISTS asset_idx ON balances (asset)");

			// Sends
			executeUpdate("CREATE TABLE IF NOT EXISTS sends(tx_index INTEGER PRIMARY KEY, tx_hash TEXT UNIQUE, block_index INTEGER, source TEXT, destination TEXT, asset TEXT, amount INTEGER, validity TEXT)");
			executeUpdate("CREATE INDEX IF NOT EXISTS sends_block_index_idx ON sends (block_index)");

			// Orders
			executeUpdate("CREATE TABLE IF NOT EXISTS orders(tx_index INTEGER PRIMARY KEY, tx_hash TEXT UNIQUE, block_index INTEGER, source TEXT, give_asset TEXT, give_amount INTEGER, give_remaining INTEGER, get_asset TEXT, get_amount INTEGER, get_remaining INTEGER, expiration INTEGER, expire_index INTEGER, fee_required INTEGER, fee_provided INTEGER, fee_remaining INTEGER, validity TEXT)");
			executeUpdate("CREATE INDEX IF NOT EXISTS block_index_idx ON orders (block_index)");
			executeUpdate("CREATE INDEX IF NOT EXISTS expire_index_idx ON orders (expire_index)");

			// Order Matches
			executeUpdate("CREATE TABLE IF NOT EXISTS order_matches(id TEXT PRIMARY KEY, tx0_index INTEGER, tx0_hash TEXT, tx0_address TEXT, tx1_index INTEGER, tx1_hash TEXT, tx1_address TEXT, forward_asset TEXT, forward_amount INTEGER, backward_asset TEXT, backward_amount INTEGER, tx0_block_index INTEGER, tx1_block_index INTEGER, tx0_expiration INTEGER, tx1_expiration INTEGER, match_expire_index INTEGER, validity TEXT)");
			executeUpdate("CREATE INDEX IF NOT EXISTS match_expire_index_idx ON order_matches (match_expire_index)");

			// BTCpays
			executeUpdate("CREATE TABLE IF NOT EXISTS btcpays(tx_index INTEGER PRIMARY KEY, tx_hash TEXT UNIQUE, block_index INTEGER, source TEXT, destination TEXT, btc_amount INTEGER, order_match_id TEXT, validity TEXT)");
			executeUpdate("CREATE INDEX IF NOT EXISTS block_index_idx ON btcpays (block_index)");

			// Bets (dice)
			executeUpdate("CREATE TABLE IF NOT EXISTS bets(tx_index INTEGER PRIMARY KEY, tx_hash TEXT UNIQUE, block_index INTEGER, source TEXT, bet INTEGER, chance REAL, payout REAL, profit INTEGER, cha_supply INTEGER, rolla REAL, rollb REAL, roll REAL, resolved TEXT, validity TEXT)");
			executeUpdate("CREATE INDEX IF NOT EXISTS block_index_idx ON bets (block_index)");

			// Bets (poker)
			executeUpdate("CREATE TABLE IF NOT EXISTS bets_poker(tx_index INTEGER PRIMARY KEY, tx_hash TEXT UNIQUE, block_index INTEGER, source TEXT, bet INTEGER, chance REAL, payout REAL, profit INTEGER, cha_supply INTEGER, rolla REAL, rollb REAL, roll REAL, cards TEXT, resolved TEXT, validity TEXT)");
			executeUpdate("CREATE INDEX IF NOT EXISTS block_index_idx ON bets_poker (block_index)");

			// Burns
			executeUpdate("CREATE TABLE IF NOT EXISTS burns(tx_index INTEGER PRIMARY KEY, tx_hash TEXT UNIQUE, block_index INTEGER, source TEXT, burned INTEGER, earned INTEGER, validity TEXT)");
			executeUpdate("CREATE INDEX IF NOT EXISTS validity_idx ON burns (validity)");
			executeUpdate("CREATE INDEX IF NOT EXISTS address_idx ON burns (address)");

			// Cancels
			executeUpdate("CREATE TABLE IF NOT EXISTS cancels(tx_index INTEGER PRIMARY KEY, tx_hash TEXT UNIQUE, block_index INTEGER, source TEXT, offer_hash TEXT, validity TEXT)");
			executeUpdate("CREATE INDEX IF NOT EXISTS cancels_block_index_idx ON cancels (block_index)");

			// Order Expirations
			executeUpdate("CREATE TABLE IF NOT EXISTS order_expirations(order_index INTEGER PRIMARY KEY, order_hash TEXT UNIQUE, source TEXT, block_index INTEGER)");
			executeUpdate("CREATE INDEX IF NOT EXISTS block_index_idx ON order_expirations (block_index)");

			// Order Match Expirations
			executeUpdate("CREATE TABLE IF NOT EXISTS order_match_expirations(order_match_id TEXT PRIMARY KEY, tx0_address TEXT, tx1_address TEXT, block_index INTEGER)");
			executeUpdate("CREATE INDEX IF NOT EXISTS block_index_idx ON order_match_expirations (block_index)");

			// Messages
			executeUpdate("CREATE TABLE IF NOT EXISTS messages(message_index INTEGER PRIMARY KEY, block_index INTEGER, command TEXT, category TEXT, bindings TEXT)");
			executeUpdate("CREATE INDEX IF NOT EXISTS block_index_idx ON messages (block_index)");

			updateMinorVersion();
		} catch (Exception e) {
			logger.error("Error during create tables: "+e.toString());
			e.printStackTrace();
		}
	}
	
	public void updateMinorVersion() {
		// Update minor version
		executeUpdate("PRAGMA user_version = "+Config.minorVersionDB.toString());
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
