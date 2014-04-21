import static spark.Spark.*;

import java.io.File;
import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.google.bitcoin.store.BlockStoreException;

import freemarker.template.Configuration;
import spark.*;
import spark.template.freemarker.FreeMarkerRoute;

public class Server implements Runnable {

	public static void main(String[] args){
		Server server = new Server();
		server.run();
	}

	public void run() { 
		init(); 
	} 

	public void init() {
		Blocks blocks = Blocks.getInstance();
		setPort(8080);    
		externalStaticFileLocation("./static");

		final Configuration configuration = new Configuration();
		try {
			configuration.setDirectoryForTemplateLoading(new File("./templates"));
		} catch (Exception e) {
		}

		get(new FreeMarkerRoute("/") {
			@Override
			public ModelAndView handle(Request request, Response response) {
				setConfiguration(configuration);
				Map<String, Object> attributes = new HashMap<String, Object>();
				attributes.put("title", "A coin for decentralized dice betting");
				String address = Util.getAddresses().get(0);
				request.session(true);
				if (request.session().attributes().contains("address")) {
					address = request.session().attribute("address");
				}
				attributes.put("address", address);
				attributes.put("addresses", Util.getAddresses());				
				attributes.put("supply", Util.chaSupply().floatValue() / Config.unit.floatValue());
				attributes.put("max_profit", Util.chaSupply().floatValue() / Config.unit.floatValue() * Config.maxProfit);
				attributes.put("house_edge", Config.houseEdge);
				return modelAndView(attributes, "index.html");
			}
		});
		post(new FreeMarkerRoute("/") {
			@Override
			public ModelAndView handle(Request request, Response response) {
				setConfiguration(configuration);
				Map<String, Object> attributes = new HashMap<String, Object>();
				attributes.put("title", "A coin for decentralized dice betting");
				attributes.put("supply", Util.chaSupply().floatValue() / Config.unit.floatValue());
				attributes.put("max_profit", Util.chaSupply().floatValue() / Config.unit.floatValue() * Config.maxProfit);
				attributes.put("house_edge", Config.houseEdge);
				return modelAndView(attributes, "index2.html");
			}
		});
		get(new FreeMarkerRoute("/participate") {
			@Override
			public ModelAndView handle(Request request, Response response) {
				setConfiguration(configuration);
				Map<String, Object> attributes = new HashMap<String, Object>();
				attributes.put("title", "Participate");
				attributes.put("house_edge", Config.houseEdge);
				attributes.put("max_profit", Config.maxProfit);
				attributes.put("burn_address", Config.burnAddress);
				attributes.put("max_burn", Config.maxBurn);
				attributes.put("start_block", Config.startBlock);
				attributes.put("end_block", Config.endBlock);
				attributes.put("multiplier", Config.multiplier);
				attributes.put("multiplier_initial", Config.multiplierInitial);
				attributes.put("version", Config.version);
				return modelAndView(attributes, "participate.html");
			}
		});
		get(new FreeMarkerRoute("/technical") {
			@Override
			public ModelAndView handle(Request request, Response response) {
				setConfiguration(configuration);
				Map<String, Object> attributes = new HashMap<String, Object>();
				attributes.put("title", "Technical");
				attributes.put("house_edge", Config.houseEdge);
				return modelAndView(attributes, "technical.html");
			}
		});
		get(new FreeMarkerRoute("/balances") {
			@Override
			public ModelAndView handle(Request request, Response response) {
				setConfiguration(configuration);
				Map<String, Object> attributes = new HashMap<String, Object>();
				attributes.put("title", "Balances");
				Database db = Database.getInstance();
				ResultSet rs = db.executeQuery("select address,amount as balance,amount*100.0/(select sum(amount) from balances) as share from balances where asset='CHA' group by address order by amount desc;");
				ArrayList<HashMap<String, Object>> balances = new ArrayList<HashMap<String, Object>>();
				try {
					while (rs.next()) {
						HashMap<String,Object> map = new HashMap<String,Object>();
						map.put("address", rs.getString("address"));
						map.put("balance", BigInteger.valueOf(rs.getLong("balance")).doubleValue()/Config.unit.doubleValue());
						map.put("share", rs.getDouble("share"));
						balances.add(map);
					}
				} catch (SQLException e) {
				}
				attributes.put("balances", balances);
				return modelAndView(attributes, "balances.html");
			}
		});		
		post(new FreeMarkerRoute("/wallet") {
			@Override
			public ModelAndView handle(Request request, Response response) {
				setConfiguration(configuration);
				Map<String, Object> attributes = new HashMap<String, Object>();
				attributes.put("title", "Wallet");
				
				Blocks blocks = Blocks.getInstance();
				attributes.put("blocksBTC", blocks.getHeight());
				attributes.put("blocksCHA", Util.getLastBlock());
				attributes.put("version", Config.version);
				attributes.put("min_version", Util.getMinVersion());
				
				if (request.queryParams().contains("form") && request.queryParams("form").equals("import")) {
					String privateKey = request.queryParams("privatekey");
					Blocks.getInstance().importPrivateKey(privateKey);
					attributes.put("success", "Your private key has been imported.");
				}				
				String address = Util.getAddresses().get(0);
				request.session(true);
				if (request.session().attributes().contains("address")) {
					address = request.session().attribute("address");
				}
				if (request.queryParams().contains("address")) {
					address = request.queryParams("address");
					request.session().attribute("address", address);
				}
				attributes.put("address", address);
				attributes.put("addresses", Util.getAddresses());
				attributes.put("balanceCHA", Util.getBalance(address, "CHA").doubleValue() / Config.unit.doubleValue());
				attributes.put("balanceBTC", Util.getBalance(address, "BTC").doubleValue() / Config.unit.doubleValue());
				return modelAndView(attributes, "wallet.html");
			}
		});	
		get(new FreeMarkerRoute("/wallet") {
			@Override
			public ModelAndView handle(Request request, Response response) {
				setConfiguration(configuration);
				Map<String, Object> attributes = new HashMap<String, Object>();
				attributes.put("title", "Wallet");
				
				Blocks blocks = Blocks.getInstance();
				attributes.put("blocksBTC", blocks.getHeight());
				attributes.put("blocksCHA", Util.getLastBlock());
				attributes.put("version", Config.version);
				attributes.put("min_version", Util.getMinVersion());
				
				String address = Util.getAddresses().get(0);
				request.session(true);
				if (request.session().attributes().contains("address")) {
					address = request.session().attribute("address");
				}
				if (request.queryParams().contains("address")) {
					address = request.queryParams("address");
					request.session().attribute("address", address);
				}				
				attributes.put("address", address);
				attributes.put("addresses", Util.getAddresses());
				attributes.put("balanceCHA", Util.getBalance(address, "CHA").doubleValue() / Config.unit.doubleValue());
				attributes.put("balanceBTC", Util.getBalance(address, "BTC").doubleValue() / Config.unit.doubleValue());
				return modelAndView(attributes, "wallet.html");
			}
		});	
		post(new FreeMarkerRoute("/casino") {
			@Override
			public ModelAndView handle(Request request, Response response) {
				setConfiguration(configuration);
				Map<String, Object> attributes = new HashMap<String, Object>();
				attributes.put("title", "Casino");
				
				Blocks blocks = Blocks.getInstance();
				attributes.put("blocksBTC", blocks.getHeight());
				attributes.put("blocksCHA", Util.getLastBlock());
				attributes.put("version", Config.version);
				attributes.put("min_version", Util.getMinVersion());

				String address = Util.getAddresses().get(0);
				request.session(true);
				if (request.session().attributes().contains("address")) {
					address = request.session().attribute("address");
				}
				if (request.queryParams().contains("address")) {
					address = request.queryParams("address");
					request.session().attribute("address", address);
				}
				attributes.put("address", address);
				attributes.put("addresses", Util.getAddresses());				
				attributes.put("supply", Util.chaSupply().floatValue() / Config.unit.floatValue());
				attributes.put("max_profit", Util.chaSupply().floatValue() / Config.unit.floatValue() * Config.maxProfit);
				attributes.put("house_edge", Config.houseEdge);

				Database db = Database.getInstance();
				
				//get top winners
				ResultSet rs = db.executeQuery("select source, count(bet) as bet_count, avg(bet) as avg_bet, avg(chance) as avg_chance, sum(profit) as sum_profit from bets where validity='valid' group by source order by sum(profit) desc;");
				ArrayList<HashMap<String, Object>> winners = new ArrayList<HashMap<String, Object>>();
				try {
					while (rs.next()) {
						HashMap<String,Object> map = new HashMap<String,Object>();
						map.put("source", rs.getString("source"));
						map.put("bet_count", rs.getDouble("bet_count"));
						map.put("avg_bet", BigInteger.valueOf(rs.getLong("avg_bet")).doubleValue()/Config.unit.doubleValue());
						map.put("avg_chance", rs.getDouble("avg_chance"));
						map.put("sum_profit", BigInteger.valueOf(rs.getLong("sum_profit")).doubleValue()/Config.unit.doubleValue());
						winners.add(map);
					}
				} catch (SQLException e) {
				}
				attributes.put("winners", winners);				
				
				
				
				//get last 200 bets
				rs = db.executeQuery("select source,bet,chance,payout,profit,tx_hash,rolla,rollb,roll,resolved from bets where validity='valid' order by block_index desc limit 200;");
				ArrayList<HashMap<String, Object>> bets = new ArrayList<HashMap<String, Object>>();
				try {
					while (rs.next()) {
						HashMap<String,Object> map = new HashMap<String,Object>();
						map.put("source", rs.getString("source"));
						map.put("bet", BigInteger.valueOf(rs.getLong("bet")).doubleValue()/Config.unit.doubleValue());
						map.put("chance", rs.getDouble("chance"));
						map.put("payout", rs.getDouble("payout"));
						map.put("tx_hash", rs.getString("tx_hash"));
						map.put("roll", rs.getDouble("roll"));
						map.put("resolved", rs.getString("resolved"));
						map.put("profit", BigInteger.valueOf(rs.getLong("profit")).doubleValue()/Config.unit.doubleValue());
						bets.add(map);
					}
				} catch (SQLException e) {
				}
				attributes.put("bets", bets);
				
				rs = db.executeQuery("select source,bet,chance,payout,profit,tx_hash,rolla,rollb,roll,resolved from bets where validity='valid' and source='"+address+"' order by block_index desc;");
				bets = new ArrayList<HashMap<String, Object>>();
				try {
					while (rs.next()) {
						HashMap<String,Object> map = new HashMap<String,Object>();
						map.put("source", rs.getString("source"));
						map.put("bet", BigInteger.valueOf(rs.getLong("bet")).doubleValue()/Config.unit.doubleValue());
						map.put("chance", rs.getDouble("chance"));
						map.put("payout", rs.getDouble("payout"));
						map.put("tx_hash", rs.getString("tx_hash"));
						map.put("roll", rs.getDouble("roll"));
						map.put("resolved", rs.getString("resolved"));							
						map.put("profit", BigInteger.valueOf(rs.getLong("profit")).doubleValue()/Config.unit.doubleValue());
						bets.add(map);
					}
				} catch (SQLException e) {
				}
				attributes.put("my_bets", bets);
				
				return modelAndView(attributes, "casino.html");
			}
		});
		get(new FreeMarkerRoute("/casino") {
			@Override
			public ModelAndView handle(Request request, Response response) {
				setConfiguration(configuration);
				Map<String, Object> attributes = new HashMap<String, Object>();
				attributes.put("title", "Casino");
				
				Blocks blocks = Blocks.getInstance();
				attributes.put("blocksBTC", blocks.getHeight());
				attributes.put("blocksCHA", Util.getLastBlock());
				attributes.put("version", Config.version);
				attributes.put("min_version", Util.getMinVersion());
				
				String address = Util.getAddresses().get(0);
				request.session(true);
				if (request.session().attributes().contains("address")) {
					address = request.session().attribute("address");
				}
				if (request.queryParams().contains("address")) {
					address = request.queryParams("address");
					request.session().attribute("address", address);
				}
				attributes.put("address", address);
				attributes.put("addresses", Util.getAddresses());
				attributes.put("supply", Util.chaSupply().floatValue() / Config.unit.floatValue());
				attributes.put("max_profit", Util.chaSupply().floatValue() / Config.unit.floatValue() * Config.maxProfit);
				attributes.put("house_edge", Config.houseEdge);
				Database db = Database.getInstance();
				
				//get top winners
				ResultSet rs = db.executeQuery("select source, count(bet) as bet_count, avg(bet) as avg_bet, avg(chance) as avg_chance, sum(profit) as sum_profit from bets where validity='valid' group by source order by sum(profit) desc;");
				ArrayList<HashMap<String, Object>> winners = new ArrayList<HashMap<String, Object>>();
				try {
					while (rs.next()) {
						HashMap<String,Object> map = new HashMap<String,Object>();
						map.put("source", rs.getString("source"));
						map.put("bet_count", rs.getDouble("bet_count"));
						map.put("avg_bet", BigInteger.valueOf(rs.getLong("avg_bet")).doubleValue()/Config.unit.doubleValue());
						map.put("avg_chance", rs.getDouble("avg_chance"));
						map.put("sum_profit", BigInteger.valueOf(rs.getLong("sum_profit")).doubleValue()/Config.unit.doubleValue());
						winners.add(map);
					}
				} catch (SQLException e) {
				}
				attributes.put("winners", winners);				
				
				//get last 200 bets
				rs = db.executeQuery("select source,bet,chance,payout,profit,tx_hash,rolla,rollb,roll,resolved from bets where validity='valid' order by block_index desc limit 200;");
				ArrayList<HashMap<String, Object>> bets = new ArrayList<HashMap<String, Object>>();
				try {
					while (rs.next()) {
						HashMap<String,Object> map = new HashMap<String,Object>();
						map.put("source", rs.getString("source"));
						map.put("bet", BigInteger.valueOf(rs.getLong("bet")).doubleValue()/Config.unit.doubleValue());
						map.put("chance", rs.getDouble("chance"));
						map.put("payout", rs.getDouble("payout"));
						map.put("tx_hash", rs.getString("tx_hash"));
						map.put("roll", rs.getDouble("roll"));
						map.put("resolved", rs.getString("resolved"));
						map.put("profit", BigInteger.valueOf(rs.getLong("profit")).doubleValue()/Config.unit.doubleValue());
						bets.add(map);
					}
				} catch (SQLException e) {
				}
				attributes.put("bets", bets);

				rs = db.executeQuery("select source,bet,chance,payout,profit,tx_hash,rolla,rollb,roll,resolved from bets where validity='valid' and source='"+address+"' order by block_index desc;");
				bets = new ArrayList<HashMap<String, Object>>();
				try {
					while (rs.next()) {
						HashMap<String,Object> map = new HashMap<String,Object>();
						map.put("source", rs.getString("source"));
						map.put("bet", BigInteger.valueOf(rs.getLong("bet")).doubleValue()/Config.unit.doubleValue());
						map.put("chance", rs.getDouble("chance"));
						map.put("payout", rs.getDouble("payout"));
						map.put("tx_hash", rs.getString("tx_hash"));
						map.put("roll", rs.getDouble("roll"));
						map.put("resolved", rs.getString("resolved"));							
						map.put("profit", BigInteger.valueOf(rs.getLong("profit")).doubleValue()/Config.unit.doubleValue());
						bets.add(map);
					}
				} catch (SQLException e) {
				}
				attributes.put("my_bets", bets);
								
				return modelAndView(attributes, "casino.html");
			}
		});
		get(new FreeMarkerRoute("/error") {
			@Override
			public ModelAndView handle(Request request, Response response) {
				setConfiguration(configuration);
				Map<String, Object> attributes = new HashMap<String, Object>();
				attributes.put("title", "Error");
				return modelAndView(attributes, "error.html");
			}
		});
	}

}