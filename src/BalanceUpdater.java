
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.sql.ResultSet;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class BalanceUpdater {
	public static Logger logger = LoggerFactory.getLogger(Blocks.class);

	public static void main(String[] args) {
		Config.loadUserDefined();
		Blocks blocks = Blocks.getInstanceAndWait();
		Thread blocksThread = new Thread(blocks);
		blocksThread.setDaemon(true);
		blocksThread.start(); 
		Config.loadUserDefined();
		Database db = Database.getInstance();
		
		JSONObject attributes = null;
		JSONObject attributesSaved = null;
		while (true) {
			attributes = new JSONObject();
			ResultSet rs = db.executeQuery("select address,amount as balance from balances where asset='CHA' group by address order by amount desc;");
			JSONObject balances = new JSONObject();
			try {
				while (rs.next()) {
					balances.put(rs.getString("address"), BigInteger.valueOf(rs.getLong("balance")).doubleValue()/Config.unit.doubleValue());
				}
			} catch (Exception e) {
			}
			try {
				attributes.put("balances", balances);
				attributes.put("height", Util.getLastBlock());
				
				if (attributesSaved == null || attributes.getDouble("height") > attributesSaved.getDouble("height")) {
					attributesSaved = attributes;
					PrintWriter out = new PrintWriter("balances.txt");
					out.print(attributes.toString());
					out.close();
					try {
						//Git Commit the change and print out any output and errors in the process
						Runtime rt = Runtime.getRuntime();
						String[] commands = {"sh", "git_commit.sh"};
						Process proc = rt.exec(commands);
						BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
						BufferedReader stdError = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
						// read the output from the command
						String s = null;
						while ((s = stdInput.readLine()) != null) {
						    System.out.println(s);
						}
						// read any errors from the attempted command
						while ((s = stdError.readLine()) != null) {
						    System.out.println(s);
						}
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
			} catch (Exception e) {
			}

			try {
				Thread.sleep(1000*60); 
			} catch (InterruptedException e) {
				logger.error("Error during loop: "+e.toString());
			}
		}		
	}
}
