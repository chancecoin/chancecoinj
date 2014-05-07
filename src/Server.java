import static spark.Spark.externalStaticFileLocation;
import static spark.Spark.get;
import static spark.Spark.post;
import static spark.Spark.setPort;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.security.CodeSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.Spark;
import spark.template.freemarker.FreeMarkerRoute;

import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.Transaction;

import freemarker.template.Configuration;

public class Server implements Runnable {
	public Logger logger = LoggerFactory.getLogger(Server.class);

	public static void main(String[] args){
		Server server = new Server();
		server.run();
	}

	public void run() { 
		init(); 
	} 

	public void init() {
		//start Blocks thread
		Blocks blocks = new Blocks();
		Thread blocksThread = new Thread(blocks);
		blocksThread.setDaemon(true);
		blocksThread.start(); 
		
		boolean inJar = false;
		try {
			CodeSource cs = this.getClass().getProtectionDomain().getCodeSource();
			inJar = cs.getLocation().toURI().getPath().endsWith(".jar");
		}
		catch (URISyntaxException e) {
			e.printStackTrace();
		}
		
		setPort(8080);    
		
		final Configuration configuration = new Configuration();
		try {
			if (inJar) {
				Spark.externalStaticFileLocation("resources/static/");
				configuration.setClassForTemplateLoading(this.getClass(), "resources/templates/");
			} else {
				Spark.externalStaticFileLocation("./resources/static/");
				configuration.setDirectoryForTemplateLoading(new File("./resources/templates/"));	
			}
		} catch (Exception e) {
		}

		get(new FreeMarkerRoute("/") {
			@Override
			public ModelAndView handle(Request request, Response response) {
				setConfiguration(configuration);
				Map<String, Object> attributes = new HashMap<String, Object>();
				attributes.put("title", "A coin for betting in a decentralized casino");
				
				Blocks blocks = Blocks.getInstance();
				
				if (request.queryParams().contains("reparse")) {
					blocks.reparse();
				}
				
				attributes.put("blocksBTC", blocks.getHeight());
				attributes.put("blocksCHA", Util.getLastBlock());
				attributes.put("version", Config.version);
				attributes.put("min_version", Util.getMinVersion());
				attributes.put("min_version_major", Util.getMinMajorVersion());
				attributes.put("min_version_minor", Util.getMinMinorVersion());
				attributes.put("version_major", Config.majorVersion);
				attributes.put("version_minor", Config.minorVersion);
				blocks.versionCheck();
				if (Blocks.getInstance().parsing) attributes.put("parsing", Blocks.getInstance().parsingBlock);
					
				String address = Util.getAddresses().get(0);
				request.session(true);
				if (request.session().attributes().contains("address")) {
					address = request.session().attribute("address");
				}
				if (request.queryParams().contains("address")) {
					address = request.queryParams("address");
					request.session().attribute("address", address);
				}
				ArrayList<HashMap<String, Object>> addresses = new ArrayList<HashMap<String, Object>>();
				for (String addr : Util.getAddresses()) {
					HashMap<String,Object> map = new HashMap<String,Object>();	
					map.put("address", addr);
					map.put("balance_CHA", Util.getBalance(addr, "CHA").floatValue() / Config.unit.floatValue());
					addresses.add(map);
				}
				attributes.put("address", address);				
				attributes.put("addresses", addresses);
				
				Database db = Database.getInstance();
				ResultSet rs = db.executeQuery("select address,amount as balance,amount*100.0/(select sum(amount) from balances) as share from balances where asset='CHA' group by address order by amount desc limit 10;");
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
				
				rs = db.executeQuery("select bets.source as source,bet,chance,payout,profit,bets.tx_hash as tx_hash,rolla,rollb,roll,resolved,bets.tx_index as tx_index,block_time from bets,transactions where bets.validity='valid' and bets.tx_index=transactions.tx_index and bets.profit!=0 order by bets.block_index desc, bets.tx_index desc limit 10;");
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
						map.put("block_time", Util.timeFormat(rs.getInt("block_time")));
						map.put("profit", BigInteger.valueOf(rs.getLong("profit")).doubleValue()/Config.unit.doubleValue());
						bets.add(map);
					}
				} catch (SQLException e) {
				}
				attributes.put("bets", bets);
								
				attributes.put("supply", Util.chaSupply().floatValue() / Config.unit.floatValue());
				attributes.put("max_profit", Util.chaSupply().floatValue() / Config.unit.floatValue() * Config.maxProfit);
				attributes.put("house_edge", Config.houseEdge);
				return modelAndView(attributes, "index.html");
			}
		});
		get(new FreeMarkerRoute("/participate") {
			@Override
			public ModelAndView handle(Request request, Response response) {
				setConfiguration(configuration);
				Map<String, Object> attributes = new HashMap<String, Object>();
				attributes.put("title", "Participate");
				attributes.put("version", Config.version);
				attributes.put("min_version", Util.getMinVersion());
				attributes.put("min_version_major", Util.getMinMajorVersion());
				attributes.put("min_version_minor", Util.getMinMinorVersion());
				attributes.put("version_major", Config.majorVersion);
				attributes.put("version_minor", Config.minorVersion);
				Blocks.getInstance().versionCheck();
				if (Blocks.getInstance().parsing) attributes.put("parsing", Blocks.getInstance().parsingBlock);
				return modelAndView(attributes, "participate.html");
			}
		});
		get(new FreeMarkerRoute("/technical") {
			@Override
			public ModelAndView handle(Request request, Response response) {
				setConfiguration(configuration);
				Map<String, Object> attributes = new HashMap<String, Object>();
				attributes.put("title", "Technical");
				attributes.put("version", Config.version);
				attributes.put("min_version", Util.getMinVersion());	
				attributes.put("min_version_major", Util.getMinMajorVersion());
				attributes.put("min_version_minor", Util.getMinMinorVersion());
				attributes.put("version_major", Config.majorVersion);
				attributes.put("version_minor", Config.minorVersion);
				Blocks.getInstance().versionCheck();
				if (Blocks.getInstance().parsing) attributes.put("parsing", Blocks.getInstance().parsingBlock);
				attributes.put("house_edge", Config.houseEdge);
				attributes.put("max_profit", Config.maxProfit);
				attributes.put("burn_address", Config.burnAddress);
				attributes.put("max_burn", Config.maxBurn);
				attributes.put("start_block", Config.startBlock);
				attributes.put("end_block", Config.endBlock);
				attributes.put("multiplier", Config.multiplier);
				attributes.put("multiplier_initial", Config.multiplierInitial);
				attributes.put("burned_BTC", Util.btcBurned().doubleValue()/Config.unit.doubleValue());
				attributes.put("burned_CHA", Util.chaBurned().doubleValue()/Config.unit.doubleValue());
				return modelAndView(attributes, "technical.html");
			}
		});
		get(new FreeMarkerRoute("/balances") {
			@Override
			public ModelAndView handle(Request request, Response response) {
				setConfiguration(configuration);
				Map<String, Object> attributes = new HashMap<String, Object>();
				attributes.put("title", "Balances");
				attributes.put("version", Config.version);
				attributes.put("min_version", Util.getMinVersion());
				attributes.put("min_version_major", Util.getMinMajorVersion());
				attributes.put("min_version_minor", Util.getMinMinorVersion());
				attributes.put("version_major", Config.majorVersion);
				attributes.put("version_minor", Config.minorVersion);
				Blocks.getInstance().versionCheck();
				if (Blocks.getInstance().parsing) attributes.put("parsing", Blocks.getInstance().parsingBlock);
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
		post(new FreeMarkerRoute("/exchange") {
			@Override
			public ModelAndView handle(Request request, Response response) {
				setConfiguration(configuration);
				Map<String, Object> attributes = new HashMap<String, Object>();
				attributes.put("title", "Exchange");
				
				Blocks blocks = Blocks.getInstance();
				attributes.put("blocksBTC", blocks.getHeight());
				attributes.put("blocksCHA", Util.getLastBlock());
				attributes.put("version", Config.version);
				attributes.put("min_version", Util.getMinVersion());
				attributes.put("min_version_major", Util.getMinMajorVersion());
				attributes.put("min_version_minor", Util.getMinMinorVersion());
				attributes.put("version_major", Config.majorVersion);
				attributes.put("version_minor", Config.minorVersion);
				Blocks.getInstance().versionCheck();
				if (Blocks.getInstance().parsing) attributes.put("parsing", Blocks.getInstance().parsingBlock);
				
				String address = Util.getAddresses().get(0);
				request.session(true);
				if (request.session().attributes().contains("address")) {
					address = request.session().attribute("address");
				}
				if (request.queryParams().contains("address")) {
					address = request.queryParams("address");
					request.session().attribute("address", address);
				}
				ArrayList<HashMap<String, Object>> addresses = new ArrayList<HashMap<String, Object>>();
				for (String addr : Util.getAddresses()) {
					HashMap<String,Object> map = new HashMap<String,Object>();	
					map.put("address", addr);
					map.put("balance_CHA", Util.getBalance(addr, "CHA").floatValue() / Config.unit.floatValue());
					addresses.add(map);
				}
				attributes.put("address", address);				
				attributes.put("addresses", addresses);				
				
				if (request.queryParams().contains("form") && request.queryParams("form").equals("cancel")) {
					String txHash = request.queryParams("tx_hash");
					Transaction tx = Cancel.create(txHash);
					if (tx!=null) {
						if (blocks.sendTransaction(tx)) {
							attributes.put("success", "Your order has been cancelled.");
						} else {
							attributes.put("error", "Your transaction timed out and was not received by the Bitcoin network. Please try again.");							
						}
					}else{
						attributes.put("error", "There was a problem with your transaction.");						
					}
				}
				if (request.queryParams().contains("form") && request.queryParams("form").equals("btcpay")) {
					String orderMatchId = request.queryParams("order_match_id");
					Transaction tx = BTCPay.create(orderMatchId);
					if (tx!=null) {
						if (blocks.sendTransaction(tx)) {
							attributes.put("success", "Your BTC payment was successful.");
						} else {
							attributes.put("error", "Your transaction timed out and was not received by the Bitcoin network. Please try again.");							
						}
					}else{
						attributes.put("error", "There was a problem with your transaction.");						
					}
				}
				if (request.queryParams().contains("form") && request.queryParams("form").equals("buy")) {
					String source = request.queryParams("source");
					Double price = Double.parseDouble(request.queryParams("price"));
					Double rawQuantity = Double.parseDouble(request.queryParams("quantity"));
					BigInteger quantity = new BigDecimal(rawQuantity*Config.unit).toBigInteger();
					BigInteger btcQuantity = new BigDecimal(quantity.doubleValue() * price).toBigInteger();
					BigInteger expiration = BigInteger.valueOf(Long.parseLong(request.queryParams("expiration")));
					Transaction tx = Order.create(source, "BTC", btcQuantity, "CHA", quantity, expiration, BigInteger.ZERO, BigInteger.ZERO);
					if (tx!=null) {
						if (blocks.sendTransaction(tx)) {
							attributes.put("success", "Thank you for your order.");
						} else {
							attributes.put("error", "Your transaction timed out and was not received by the Bitcoin network. Please try again.");							
						}
					}else{
						attributes.put("error", "There was a problem with your transaction.");						
					}
				}
				if (request.queryParams().contains("form") && request.queryParams("form").equals("sell")) {
					String source = request.queryParams("source");
					Double price = Double.parseDouble(request.queryParams("price"));
					Double rawQuantity = Double.parseDouble(request.queryParams("quantity"));
					BigInteger quantity = new BigDecimal(rawQuantity*Config.unit).toBigInteger();
					BigInteger btcQuantity = new BigDecimal(quantity.doubleValue() * price).toBigInteger();
					BigInteger expiration = BigInteger.valueOf(Long.parseLong(request.queryParams("expiration")));
					Transaction tx = Order.create(source, "CHA", quantity, "BTC", btcQuantity, expiration, BigInteger.ZERO, BigInteger.ZERO);
					if (tx!=null) {
						if (blocks.sendTransaction(tx)) {
							attributes.put("success", "Thank you for your order.");
						} else {
							attributes.put("error", "Your transaction timed out and was not received by the Bitcoin network. Please try again.");							
						}
					}else{
						attributes.put("error", "There was a problem with your transaction.");						
					}
				}

				attributes.put("balanceCHA", Util.getBalance(address, "CHA").doubleValue() / Config.unit.doubleValue());
				attributes.put("balanceBTC", Util.getBalance(address, "BTC").doubleValue() / Config.unit.doubleValue());
				
				Database db = Database.getInstance();
				
				//get buy orders
				ResultSet rs = db.executeQuery("select 1.0*give_amount/get_amount as price, get_remaining as quantity,tx_hash from orders where get_asset='CHA' and give_asset='BTC' and validity='valid' and give_remaining>0 and get_remaining>0 order by price desc, quantity desc;");
				ArrayList<HashMap<String, Object>> ordersBuy = new ArrayList<HashMap<String, Object>>();
				try {
					while (rs.next()) {
						HashMap<String,Object> map = new HashMap<String,Object>();
						map.put("quantity", BigInteger.valueOf(rs.getLong("quantity")).doubleValue()/Config.unit.doubleValue());
						map.put("price", rs.getDouble("price"));
						map.put("tx_hash", rs.getString("tx_hash"));
						ordersBuy.add(map);
					}
				} catch (SQLException e) {
				}
				attributes.put("orders_buy", ordersBuy);				
				
				//get sell orders
				rs = db.executeQuery("select 1.0*get_amount/give_amount as price, give_remaining as quantity,tx_hash from orders where give_asset='CHA' and get_asset='BTC' and validity='valid' and give_remaining>0 and get_remaining>0 order by price desc, quantity asc;");
				ArrayList<HashMap<String, Object>> ordersSell = new ArrayList<HashMap<String, Object>>();
				try {
					while (rs.next()) {
						HashMap<String,Object> map = new HashMap<String,Object>();
						map.put("quantity", BigInteger.valueOf(rs.getLong("quantity")).doubleValue()/Config.unit.doubleValue());
						map.put("price", rs.getDouble("price"));
						map.put("tx_hash", rs.getString("tx_hash"));
						ordersSell.add(map);
					}
				} catch (SQLException e) {
				}
				attributes.put("orders_sell", ordersSell);				

				//get my orders
				rs = db.executeQuery("select * from orders where source='"+address+"' order by block_index desc, tx_index desc;");
				ArrayList<HashMap<String, Object>> myOrders = new ArrayList<HashMap<String, Object>>();
				try {
					while (rs.next()) {
						HashMap<String,Object> map = new HashMap<String,Object>();
						if (rs.getString("get_asset").equals("CHA")) {
							map.put("buysell", "Buy");
							map.put("price", rs.getDouble("give_amount")/rs.getDouble("get_amount"));
							map.put("quantity_cha", BigInteger.valueOf(rs.getLong("get_amount")).doubleValue()/Config.unit.doubleValue());
							map.put("quantity_btc", BigInteger.valueOf(rs.getLong("give_amount")).doubleValue()/Config.unit.doubleValue());
							map.put("quantity_remaining_cha", BigInteger.valueOf(rs.getLong("get_remaining")).doubleValue()/Config.unit.doubleValue());
							map.put("quantity_remaining_btc", BigInteger.valueOf(rs.getLong("give_remaining")).doubleValue()/Config.unit.doubleValue());
						} else {
							map.put("buysell", "Sell");
							map.put("price", rs.getDouble("get_amount")/rs.getDouble("give_amount"));
							map.put("quantity_cha", BigInteger.valueOf(rs.getLong("give_amount")).doubleValue()/Config.unit.doubleValue());
							map.put("quantity_btc", BigInteger.valueOf(rs.getLong("get_amount")).doubleValue()/Config.unit.doubleValue());
							map.put("quantity_remaining_cha", BigInteger.valueOf(rs.getLong("give_remaining")).doubleValue()/Config.unit.doubleValue());
							map.put("quantity_remaining_btc", BigInteger.valueOf(rs.getLong("get_remaining")).doubleValue()/Config.unit.doubleValue());
						}
						map.put("tx_hash", rs.getString("tx_hash"));
						map.put("validity", rs.getString("validity"));
						myOrders.add(map);
					}
				} catch (SQLException e) {
				}
				attributes.put("my_orders", myOrders);				

				//get my order matches
				rs = db.executeQuery("select * from order_matches where ((tx0_address='"+address+"' and forward_asset='BTC') or (tx1_address='"+address+"' and backward_asset='BTC')) and validity='pending' order by tx0_block_index desc, tx0_index desc;");
				ArrayList<HashMap<String, Object>> myOrderMatches = new ArrayList<HashMap<String, Object>>();
				try {
					while (rs.next()) {
						HashMap<String,Object> map = new HashMap<String,Object>();
						if (rs.getString("forward_asset").equals("BTC")) {
							map.put("btc_owed", BigInteger.valueOf(rs.getLong("forward_amount")).doubleValue()/Config.unit.doubleValue());
							map.put("cha_return", BigInteger.valueOf(rs.getLong("backward_amount")).doubleValue()/Config.unit.doubleValue());
						} else {
							map.put("cha_return", BigInteger.valueOf(rs.getLong("forward_amount")).doubleValue()/Config.unit.doubleValue());
							map.put("btc_owed", BigInteger.valueOf(rs.getLong("backward_amount")).doubleValue()/Config.unit.doubleValue());
						}
						map.put("order_match_id", rs.getString("id"));
						myOrderMatches.add(map);
					}
				} catch (SQLException e) {
				}
				attributes.put("my_order_matches", myOrderMatches);				
				
				return modelAndView(attributes, "exchange.html");
			}
		});	
		get(new FreeMarkerRoute("/exchange") {
			@Override
			public ModelAndView handle(Request request, Response response) {
				setConfiguration(configuration);
				Map<String, Object> attributes = new HashMap<String, Object>();
				attributes.put("title", "Exchange");
				
				Blocks blocks = Blocks.getInstance();
				attributes.put("blocksBTC", blocks.getHeight());
				attributes.put("blocksCHA", Util.getLastBlock());
				attributes.put("version", Config.version);
				attributes.put("min_version", Util.getMinVersion());
				attributes.put("min_version_major", Util.getMinMajorVersion());
				attributes.put("min_version_minor", Util.getMinMinorVersion());
				attributes.put("version_major", Config.majorVersion);
				attributes.put("version_minor", Config.minorVersion);
				Blocks.getInstance().versionCheck();
				if (Blocks.getInstance().parsing) attributes.put("parsing", Blocks.getInstance().parsingBlock);
				
				String address = Util.getAddresses().get(0);
				request.session(true);
				if (request.session().attributes().contains("address")) {
					address = request.session().attribute("address");
				}
				if (request.queryParams().contains("address")) {
					address = request.queryParams("address");
					request.session().attribute("address", address);
				}
				ArrayList<HashMap<String, Object>> addresses = new ArrayList<HashMap<String, Object>>();
				for (String addr : Util.getAddresses()) {
					HashMap<String,Object> map = new HashMap<String,Object>();	
					map.put("address", addr);
					map.put("balance_CHA", Util.getBalance(addr, "CHA").floatValue() / Config.unit.floatValue());
					addresses.add(map);
				}
				attributes.put("address", address);				
				attributes.put("addresses", addresses);
				
				attributes.put("balanceCHA", Util.getBalance(address, "CHA").doubleValue() / Config.unit.doubleValue());
				attributes.put("balanceBTC", Util.getBalance(address, "BTC").doubleValue() / Config.unit.doubleValue());
				
				Database db = Database.getInstance();
				
				//get buy orders
				ResultSet rs = db.executeQuery("select 1.0*give_amount/get_amount as price, get_remaining as quantity,tx_hash from orders where get_asset='CHA' and give_asset='BTC' and validity='valid' and give_remaining>0 and get_remaining>0 order by price desc, quantity desc;");
				ArrayList<HashMap<String, Object>> ordersBuy = new ArrayList<HashMap<String, Object>>();
				try {
					while (rs.next()) {
						HashMap<String,Object> map = new HashMap<String,Object>();
						map.put("quantity", BigInteger.valueOf(rs.getLong("quantity")).doubleValue()/Config.unit.doubleValue());
						map.put("price", rs.getDouble("price"));
						map.put("tx_hash", rs.getString("tx_hash"));
						ordersBuy.add(map);
					}
				} catch (SQLException e) {
				}
				attributes.put("orders_buy", ordersBuy);				
				
				//get sell orders
				rs = db.executeQuery("select 1.0*get_amount/give_amount as price, give_remaining as quantity,tx_hash from orders where give_asset='CHA' and get_asset='BTC' and validity='valid' and give_remaining>0 and get_remaining>0 order by price desc, quantity asc;");
				ArrayList<HashMap<String, Object>> ordersSell = new ArrayList<HashMap<String, Object>>();
				try {
					while (rs.next()) {
						HashMap<String,Object> map = new HashMap<String,Object>();
						map.put("quantity", BigInteger.valueOf(rs.getLong("quantity")).doubleValue()/Config.unit.doubleValue());
						map.put("price", rs.getDouble("price"));
						map.put("tx_hash", rs.getString("tx_hash"));
						ordersSell.add(map);
					}
				} catch (SQLException e) {
				}
				attributes.put("orders_sell", ordersSell);				

				//get my orders
				rs = db.executeQuery("select * from orders where source='"+address+"' order by block_index desc, tx_index desc;");
				ArrayList<HashMap<String, Object>> myOrders = new ArrayList<HashMap<String, Object>>();
				try {
					while (rs.next()) {
						HashMap<String,Object> map = new HashMap<String,Object>();
						if (rs.getString("get_asset").equals("CHA")) {
							map.put("buysell", "Buy");
							map.put("price", rs.getDouble("give_amount")/rs.getDouble("get_amount"));
							map.put("quantity_cha", BigInteger.valueOf(rs.getLong("get_amount")).doubleValue()/Config.unit.doubleValue());
							map.put("quantity_btc", BigInteger.valueOf(rs.getLong("give_amount")).doubleValue()/Config.unit.doubleValue());
							map.put("quantity_remaining_cha", BigInteger.valueOf(rs.getLong("get_remaining")).doubleValue()/Config.unit.doubleValue());
							map.put("quantity_remaining_btc", BigInteger.valueOf(rs.getLong("give_remaining")).doubleValue()/Config.unit.doubleValue());
						} else {
							map.put("buysell", "Sell");
							map.put("price", rs.getDouble("get_amount")/rs.getDouble("give_amount"));
							map.put("quantity_cha", BigInteger.valueOf(rs.getLong("give_amount")).doubleValue()/Config.unit.doubleValue());
							map.put("quantity_btc", BigInteger.valueOf(rs.getLong("get_amount")).doubleValue()/Config.unit.doubleValue());
							map.put("quantity_remaining_cha", BigInteger.valueOf(rs.getLong("give_remaining")).doubleValue()/Config.unit.doubleValue());
							map.put("quantity_remaining_btc", BigInteger.valueOf(rs.getLong("get_remaining")).doubleValue()/Config.unit.doubleValue());
						}
						map.put("tx_hash", rs.getString("tx_hash"));
						map.put("validity", rs.getString("validity"));
						myOrders.add(map);
					}
				} catch (SQLException e) {
				}
				attributes.put("my_orders", myOrders);				

				//get my order matches
				rs = db.executeQuery("select * from order_matches where ((tx0_address='"+address+"' and forward_asset='BTC') or (tx1_address='"+address+"' and backward_asset='BTC')) and validity='pending' order by tx0_block_index desc, tx0_index desc;");
				ArrayList<HashMap<String, Object>> myOrderMatches = new ArrayList<HashMap<String, Object>>();
				try {
					while (rs.next()) {
						HashMap<String,Object> map = new HashMap<String,Object>();
						if (rs.getString("forward_asset").equals("BTC")) {
							map.put("btc_owed", BigInteger.valueOf(rs.getLong("forward_amount")).doubleValue()/Config.unit.doubleValue());
							map.put("cha_return", BigInteger.valueOf(rs.getLong("backward_amount")).doubleValue()/Config.unit.doubleValue());
						} else {
							map.put("cha_return", BigInteger.valueOf(rs.getLong("forward_amount")).doubleValue()/Config.unit.doubleValue());
							map.put("btc_owed", BigInteger.valueOf(rs.getLong("backward_amount")).doubleValue()/Config.unit.doubleValue());
						}
						map.put("order_match_id", rs.getString("id"));
						myOrderMatches.add(map);
					}
				} catch (SQLException e) {
				}
				attributes.put("my_order_matches", myOrderMatches);				
				
				return modelAndView(attributes, "exchange.html");
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
				attributes.put("min_version_major", Util.getMinMajorVersion());
				attributes.put("min_version_minor", Util.getMinMinorVersion());
				attributes.put("version_major", Config.majorVersion);
				attributes.put("version_minor", Config.minorVersion);
				Blocks.getInstance().versionCheck();
				if (Blocks.getInstance().parsing) attributes.put("parsing", Blocks.getInstance().parsingBlock);
				
				if (request.queryParams().contains("form") && request.queryParams("form").equals("delete")) {
					ECKey deleteKey = null;
					String deleteAddress = request.queryParams("address");
					for (ECKey key : blocks.wallet.getKeys()) {
						if (key.toAddress(blocks.params).toString().equals(deleteAddress)) {
							deleteKey = key;
						}
					}
					if (deleteKey != null) {
						logger.info("Deleting private key");
						blocks.wallet.removeKey(deleteKey);
					}
					attributes.put("success", "Your private key has been deleted. You can no longer transact from this address.");
				}
				if (request.queryParams().contains("form") && request.queryParams("form").equals("reimport")) {
					ECKey importKey = null;
					String deleteAddress = request.queryParams("address");
					for (ECKey key : blocks.wallet.getKeys()) {
						if (key.toAddress(blocks.params).toString().equals(deleteAddress)) {
							importKey = key;
						}
					}
					if (importKey != null) {
						logger.info("Reimporting private key transactions");
						blocks.importPrivateKey(importKey.toString());
					}
					attributes.put("success", "Your transactions have been reimported.");
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
				ArrayList<HashMap<String, Object>> addresses = new ArrayList<HashMap<String, Object>>();
				for (String addr : Util.getAddresses()) {
					HashMap<String,Object> map = new HashMap<String,Object>();	
					map.put("address", addr);
					map.put("balance_CHA", Util.getBalance(addr, "CHA").floatValue() / Config.unit.floatValue());
					addresses.add(map);
				}
				attributes.put("address", address);				
				attributes.put("addresses", addresses);				
				
				if (request.queryParams().contains("form") && request.queryParams("form").equals("import")) {
					String privateKey = request.queryParams("privatekey");
					address = Blocks.getInstance().importPrivateKey(privateKey);
					request.session().attribute("address", address);
					attributes.put("success", "Your private key has been imported.");
				}
				if (request.queryParams().contains("form") && request.queryParams("form").equals("send")) {
					String source = request.queryParams("source");
					String destination = request.queryParams("destination");
					Double rawQuantity = Double.parseDouble(request.queryParams("quantity"));
					BigInteger quantity = new BigDecimal(rawQuantity*Config.unit).toBigInteger();
					Transaction tx = Send.create(source, destination, "CHA", quantity);
					if (tx!=null) {
						if (blocks.sendTransaction(tx)) {
							attributes.put("success", "You sent "+rawQuantity.toString()+" CHA to "+destination+".");
						} else {
							attributes.put("error", "Your transaction timed out and was not received by the Bitcoin network. Please try again.");							
						}
					}else{
						attributes.put("error", "There was a problem with your transaction.");						
					}
				}

				attributes.put("balanceCHA", Util.getBalance(address, "CHA").doubleValue() / Config.unit.doubleValue());
				attributes.put("balanceBTC", Util.getBalance(address, "BTC").doubleValue() / Config.unit.doubleValue());
				
				Database db = Database.getInstance();
				
				//get my sends
				ResultSet rs = db.executeQuery("select * from sends where (source='"+address+"') and asset='CHA' and validity='valid' order by block_index desc, tx_index desc;");
				ArrayList<HashMap<String, Object>> mySends = new ArrayList<HashMap<String, Object>>();
				try {
					while (rs.next()) {
						HashMap<String,Object> map = new HashMap<String,Object>();
						map.put("amount", BigInteger.valueOf(rs.getLong("amount")).doubleValue()/Config.unit.doubleValue());
						map.put("tx_hash", rs.getString("tx_hash"));
						map.put("source", rs.getString("source"));
						map.put("destination", rs.getString("destination"));
						mySends.add(map);
					}
				} catch (SQLException e) {
				}
				attributes.put("my_sends", mySends);								

				//get my receives
				rs = db.executeQuery("select * from sends where (destination='"+address+"') and asset='CHA' and validity='valid' order by block_index desc, tx_index desc;");
				ArrayList<HashMap<String, Object>> myReceives = new ArrayList<HashMap<String, Object>>();
				try {
					while (rs.next()) {
						HashMap<String,Object> map = new HashMap<String,Object>();
						map.put("amount", BigInteger.valueOf(rs.getLong("amount")).doubleValue()/Config.unit.doubleValue());
						map.put("tx_hash", rs.getString("tx_hash"));
						map.put("source", rs.getString("source"));
						map.put("destination", rs.getString("destination"));
						myReceives.add(map);
					}
				} catch (SQLException e) {
				}
				attributes.put("my_receives", myReceives);								
				
				//get my burns
				rs = db.executeQuery("select * from burns where source='"+address+"' and validity='valid' order by block_index desc, tx_index desc;");
				ArrayList<HashMap<String, Object>> myBurns = new ArrayList<HashMap<String, Object>>();
				try {
					while (rs.next()) {
						HashMap<String,Object> map = new HashMap<String,Object>();
						map.put("burned", BigInteger.valueOf(rs.getLong("burned")).doubleValue()/Config.unit.doubleValue());
						map.put("earned", BigInteger.valueOf(rs.getLong("earned")).doubleValue()/Config.unit.doubleValue());
						map.put("tx_hash", rs.getString("tx_hash"));
						myBurns.add(map);
					}
				} catch (SQLException e) {
				}
				attributes.put("my_burns", myBurns);
				
				//get sends
				rs = db.executeQuery("select * from sends where asset='CHA' and validity='valid' order by block_index desc, tx_index desc limit 20;");
				ArrayList<HashMap<String, Object>> sends = new ArrayList<HashMap<String, Object>>();
				try {
					while (rs.next()) {
						HashMap<String,Object> map = new HashMap<String,Object>();
						map.put("amount", BigInteger.valueOf(rs.getLong("amount")).doubleValue()/Config.unit.doubleValue());
						map.put("tx_hash", rs.getString("tx_hash"));
						map.put("source", rs.getString("source"));
						map.put("destination", rs.getString("destination"));
						sends.add(map);
					}
				} catch (SQLException e) {
				}
				attributes.put("sends", sends);			
				
				//get burns
				rs = db.executeQuery("select * from burns where validity='valid' order by earned desc limit 20;");
				ArrayList<HashMap<String, Object>> burns = new ArrayList<HashMap<String, Object>>();
				try {
					while (rs.next()) {
						HashMap<String,Object> map = new HashMap<String,Object>();
						map.put("burned", BigInteger.valueOf(rs.getLong("burned")).doubleValue()/Config.unit.doubleValue());
						map.put("earned", BigInteger.valueOf(rs.getLong("earned")).doubleValue()/Config.unit.doubleValue());
						map.put("tx_hash", rs.getString("tx_hash"));
						map.put("source", rs.getString("source"));
						burns.add(map);
					}
				} catch (SQLException e) {
				}
				attributes.put("burns", burns);				

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
				attributes.put("min_version_major", Util.getMinMajorVersion());
				attributes.put("min_version_minor", Util.getMinMinorVersion());
				attributes.put("version_major", Config.majorVersion);
				attributes.put("version_minor", Config.minorVersion);
				Blocks.getInstance().versionCheck();
				if (Blocks.getInstance().parsing) attributes.put("parsing", Blocks.getInstance().parsingBlock);
				
				String address = Util.getAddresses().get(0);
				request.session(true);
				if (request.session().attributes().contains("address")) {
					address = request.session().attribute("address");
				}
				if (request.queryParams().contains("address")) {
					address = request.queryParams("address");
					request.session().attribute("address", address);
				}
				ArrayList<HashMap<String, Object>> addresses = new ArrayList<HashMap<String, Object>>();
				for (String addr : Util.getAddresses()) {
					HashMap<String,Object> map = new HashMap<String,Object>();	
					map.put("address", addr);
					map.put("balance_CHA", Util.getBalance(addr, "CHA").floatValue() / Config.unit.floatValue());
					addresses.add(map);
				}
				attributes.put("address", address);				
				attributes.put("addresses", addresses);
				
				attributes.put("balanceCHA", Util.getBalance(address, "CHA").doubleValue() / Config.unit.doubleValue());
				attributes.put("balanceBTC", Util.getBalance(address, "BTC").doubleValue() / Config.unit.doubleValue());
				
				Database db = Database.getInstance();

				//get my sends
				ResultSet rs = db.executeQuery("select * from sends where (source='"+address+"') and asset='CHA' and validity='valid' order by block_index desc, tx_index desc;");
				ArrayList<HashMap<String, Object>> mySends = new ArrayList<HashMap<String, Object>>();
				try {
					while (rs.next()) {
						HashMap<String,Object> map = new HashMap<String,Object>();
						map.put("amount", BigInteger.valueOf(rs.getLong("amount")).doubleValue()/Config.unit.doubleValue());
						map.put("tx_hash", rs.getString("tx_hash"));
						map.put("source", rs.getString("source"));
						map.put("destination", rs.getString("destination"));
						mySends.add(map);
					}
				} catch (SQLException e) {
				}
				attributes.put("my_sends", mySends);								

				//get my receives
				rs = db.executeQuery("select * from sends where (destination='"+address+"') and asset='CHA' and validity='valid' order by block_index desc, tx_index desc;");
				ArrayList<HashMap<String, Object>> myReceives = new ArrayList<HashMap<String, Object>>();
				try {
					while (rs.next()) {
						HashMap<String,Object> map = new HashMap<String,Object>();
						map.put("amount", BigInteger.valueOf(rs.getLong("amount")).doubleValue()/Config.unit.doubleValue());
						map.put("tx_hash", rs.getString("tx_hash"));
						map.put("source", rs.getString("source"));
						map.put("destination", rs.getString("destination"));
						myReceives.add(map);
					}
				} catch (SQLException e) {
				}
				attributes.put("my_receives", myReceives);								
				
				//get my burns
				rs = db.executeQuery("select * from burns where source='"+address+"' and validity='valid' order by block_index desc, tx_index desc;");
				ArrayList<HashMap<String, Object>> myBurns = new ArrayList<HashMap<String, Object>>();
				try {
					while (rs.next()) {
						HashMap<String,Object> map = new HashMap<String,Object>();
						map.put("burned", BigInteger.valueOf(rs.getLong("burned")).doubleValue()/Config.unit.doubleValue());
						map.put("earned", BigInteger.valueOf(rs.getLong("earned")).doubleValue()/Config.unit.doubleValue());
						map.put("tx_hash", rs.getString("tx_hash"));
						myBurns.add(map);
					}
				} catch (SQLException e) {
				}
				attributes.put("my_burns", myBurns);	
				
				//get sends
				rs = db.executeQuery("select * from sends where asset='CHA' and validity='valid' order by block_index desc, tx_index desc limit 20;");
				ArrayList<HashMap<String, Object>> sends = new ArrayList<HashMap<String, Object>>();
				try {
					while (rs.next()) {
						HashMap<String,Object> map = new HashMap<String,Object>();
						map.put("amount", BigInteger.valueOf(rs.getLong("amount")).doubleValue()/Config.unit.doubleValue());
						map.put("tx_hash", rs.getString("tx_hash"));
						map.put("source", rs.getString("source"));
						map.put("destination", rs.getString("destination"));
						sends.add(map);
					}
				} catch (SQLException e) {
				}
				attributes.put("sends", sends);			
				
				//get burns
				rs = db.executeQuery("select * from burns where validity='valid' order by earned desc limit 20;");
				ArrayList<HashMap<String, Object>> burns = new ArrayList<HashMap<String, Object>>();
				try {
					while (rs.next()) {
						HashMap<String,Object> map = new HashMap<String,Object>();
						map.put("burned", BigInteger.valueOf(rs.getLong("burned")).doubleValue()/Config.unit.doubleValue());
						map.put("earned", BigInteger.valueOf(rs.getLong("earned")).doubleValue()/Config.unit.doubleValue());
						map.put("tx_hash", rs.getString("tx_hash"));
						map.put("source", rs.getString("source"));
						burns.add(map);
					}
				} catch (SQLException e) {
				}
				attributes.put("burns", burns);					

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
				attributes.put("min_version_major", Util.getMinMajorVersion());
				attributes.put("min_version_minor", Util.getMinMinorVersion());
				attributes.put("version_major", Config.majorVersion);
				attributes.put("version_minor", Config.minorVersion);
				Blocks.getInstance().versionCheck();
				if (Blocks.getInstance().parsing) attributes.put("parsing", Blocks.getInstance().parsingBlock);

				String address = Util.getAddresses().get(0);
				request.session(true);
				if (request.session().attributes().contains("address")) {
					address = request.session().attribute("address");
				}
				if (request.queryParams().contains("address")) {
					address = request.queryParams("address");
					request.session().attribute("address", address);
				}
				ArrayList<HashMap<String, Object>> addresses = new ArrayList<HashMap<String, Object>>();
				for (String addr : Util.getAddresses()) {
					HashMap<String,Object> map = new HashMap<String,Object>();	
					map.put("address", addr);
					map.put("balance_CHA", Util.getBalance(addr, "CHA").floatValue() / Config.unit.floatValue());
					addresses.add(map);
				}
				attributes.put("address", address);				
				attributes.put("addresses", addresses);
				
				attributes.put("supply", Util.chaSupply().floatValue() / Config.unit.floatValue());
				attributes.put("max_profit", Util.chaSupply().floatValue() / Config.unit.floatValue() * Config.maxProfit);
				attributes.put("house_edge", Config.houseEdge);

				if (request.queryParams().contains("form") && request.queryParams("form").equals("bet")) {
					String source = request.queryParams("source");
					Double rawBet = Double.parseDouble(request.queryParams("bet"));
					Double chance = Double.parseDouble(request.queryParams("chance"));
					Double payout = Double.parseDouble(request.queryParams("payout"));
					BigInteger bet = new BigDecimal(rawBet*Config.unit).toBigInteger();
					Transaction tx = Bet.create(source, bet, chance, payout);
					if (tx!=null) {
						if (blocks.sendTransaction(tx)) {
							attributes.put("success", "Thanks for betting!");
						} else {
							attributes.put("error", "Your transaction timed out and was not received by the Bitcoin network. Please try again.");							
						}
					}else{
						attributes.put("error", "There was a problem with your bet.");						
					}
				}
				
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
				rs = db.executeQuery("select bets.source as source,bet,chance,payout,profit,bets.tx_hash as tx_hash,rolla,rollb,roll,resolved,bets.tx_index as tx_index,block_time from bets,transactions where bets.validity='valid' and bets.tx_index=transactions.tx_index order by bets.block_index desc, bets.tx_index desc limit 200;");
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
						map.put("block_time", Util.timeFormat(rs.getInt("block_time")));
						map.put("profit", BigInteger.valueOf(rs.getLong("profit")).doubleValue()/Config.unit.doubleValue());
						bets.add(map);
					}
				} catch (SQLException e) {
				}
				attributes.put("bets", bets);
				
				rs = db.executeQuery("select bets.source as source,bet,chance,payout,profit,bets.tx_hash as tx_hash,rolla,rollb,roll,resolved,bets.tx_index as tx_index,block_time from bets,transactions where bets.validity='valid' and bets.source='"+address+"' and bets.tx_index=transactions.tx_index order by bets.block_index desc, bets.tx_index desc limit 200;");
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
						map.put("block_time", Util.timeFormat(rs.getInt("block_time")));
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
				attributes.put("min_version_major", Util.getMinMajorVersion());
				attributes.put("min_version_minor", Util.getMinMinorVersion());
				attributes.put("version_major", Config.majorVersion);
				attributes.put("version_minor", Config.minorVersion);
				Blocks.getInstance().versionCheck();
				if (Blocks.getInstance().parsing) attributes.put("parsing", Blocks.getInstance().parsingBlock);
				
				String address = Util.getAddresses().get(0);
				request.session(true);
				if (request.session().attributes().contains("address")) {
					address = request.session().attribute("address");
				}
				if (request.queryParams().contains("address")) {
					address = request.queryParams("address");
					request.session().attribute("address", address);
				}
				ArrayList<HashMap<String, Object>> addresses = new ArrayList<HashMap<String, Object>>();
				for (String addr : Util.getAddresses()) {
					HashMap<String,Object> map = new HashMap<String,Object>();	
					map.put("address", addr);
					map.put("balance_CHA", Util.getBalance(addr, "CHA").floatValue() / Config.unit.floatValue());
					addresses.add(map);
				}
				attributes.put("address", address);				
				attributes.put("addresses", addresses);
				
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
				rs = db.executeQuery("select bets.source as source,bet,chance,payout,profit,bets.tx_hash as tx_hash,rolla,rollb,roll,resolved,bets.tx_index as tx_index,block_time from bets,transactions where bets.validity='valid' and bets.tx_index=transactions.tx_index order by bets.block_index desc, bets.tx_index desc limit 200;");
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
						map.put("block_time", Util.timeFormat(rs.getInt("block_time")));
						map.put("profit", BigInteger.valueOf(rs.getLong("profit")).doubleValue()/Config.unit.doubleValue());
						bets.add(map);
					}
				} catch (SQLException e) {
				}
				attributes.put("bets", bets);

				rs = db.executeQuery("select bets.source as source,bet,chance,payout,profit,bets.tx_hash as tx_hash,rolla,rollb,roll,resolved,bets.tx_index as tx_index,block_time from bets,transactions where bets.validity='valid' and bets.source='"+address+"' and bets.tx_index=transactions.tx_index order by bets.block_index desc, bets.tx_index desc limit 200;");
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
						map.put("block_time", Util.timeFormat(rs.getInt("block_time")));
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