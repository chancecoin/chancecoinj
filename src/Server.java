import static spark.Spark.get;
import static spark.Spark.post;
import static spark.Spark.setPort;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.security.CodeSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.Route;
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
	
	public Map<String, Object> updateChatStatus(Request request, Map<String, Object> attributes) {
		if (request.session().attributes().contains("chat_open")) {
			attributes.put("chat_open", request.session().attribute("chat_open"));
		} else {
			attributes.put("chat_open", 1);
		}
		return attributes;
	}
	
	public void init() {
		//start Blocks thread
		Blocks blocks = Blocks.getInstance();
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

		get(new Route("/supply") {
			@Override
			public Object handle(Request request, Response response) {
				return String.format("%.8f", Util.chaSupply().doubleValue() / Config.unit);
			}
		});
		get(new Route("/chat_status_update") {
			@Override
			public Object handle(Request request, Response response) {
				request.session(true);
				if (request.queryParams().contains("chat_open")) {
					request.session().attribute("chat_open", request.queryParams("chat_open"));	
				}
				return request.session().attribute("chat_open");
			}
		});
		post(new Route("/process_bet") {
			@Override
			public Object handle(Request request, Response response) {
				request.session(true);
				JSONObject results = new JSONObject();
				Blocks blocks = Blocks.getInstance();
				if (request.queryParams().contains("form") && request.queryParams("form").equals("bet")) {
					String source = request.queryParams("source");
					Double rawBet = Double.parseDouble(request.queryParams("bet"));
					Double chance = Double.parseDouble(request.queryParams("chance"));
					Double payout = Double.parseDouble(request.queryParams("payout"));
					BigInteger bet = new BigDecimal(rawBet*Config.unit).toBigInteger();
					try {
						Transaction tx = Bet.create(source, bet, chance, payout);
						blocks.sendTransaction(source, tx);
						results.put("message", "Thank you for betting!");
					} catch (Exception e) {
						try {
							results.put("message", e.getMessage());
						} catch (JSONException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}
				}
				return results.toString();
			}
		});
		post(new Route("/process_import_private_key") {
			@Override
			public Object handle(Request request, Response response) {
				request.session(true);
				JSONObject results = new JSONObject();
				if (request.queryParams().contains("form") && request.queryParams("form").equals("import")) {
					String privateKey = request.queryParams("privatekey");
					try {
						String address = Blocks.getInstance().importPrivateKey(privateKey);
						request.session().attribute("address", address);
						results.put("address", address);				
						results.put("message", "Your private key has been imported.");
					} catch (Exception e) {
						try {
							results.put("message", "Error when importing private key: "+e.getMessage());
						} catch (JSONException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}
				}
				return results.toString();
			}
		});
		post(new Route("/process_send") {
			@Override
			public Object handle(Request request, Response response) {
				request.session(true);
				JSONObject results = new JSONObject();
				Blocks blocks = Blocks.getInstance();
				if (request.queryParams().contains("form") && request.queryParams("form").equals("send")) {
					String source = request.queryParams("source");
					String destination = request.queryParams("destination");
					String asset = request.queryParams("asset");
					Double rawQuantity = Double.parseDouble(request.queryParams("quantity"));
					BigInteger quantity = new BigDecimal(rawQuantity*Config.unit).toBigInteger();
					try {
						Transaction tx = Send.create(source, destination, asset, quantity);
						blocks.sendTransaction(source, tx);
						results.put("message", "You sent "+asset+" successfully.");
					} catch (Exception e) {
						try {
							results.put("message", e.getMessage());
						} catch (JSONException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}
				}
				return results.toString();
			}
		});
		get(new FreeMarkerRoute("/") {
			@Override
			public ModelAndView handle(Request request, Response response) {
				setConfiguration(configuration);
				Map<String, Object> attributes = new HashMap<String, Object>();
				request.session(true);
				attributes = updateChatStatus(request, attributes);
				attributes.put("title", "A coin for betting in a decentralized casino");

				Blocks blocks = Blocks.getInstance();
				
				if (request.queryParams().contains("reparse")) {
					blocks.reparse();
				}
				
				attributes.put("price_BTC", blocks.priceBTC);
				attributes.put("price_CHA", blocks.priceCHA);
				attributes.put("supply", Util.chaSupply().floatValue() / Config.unit.floatValue());
				attributes.put("blocksBTC", blocks.bitcoinBlock);
				attributes.put("blocksCHA", blocks.chancecoinBlock);
				attributes.put("version", Config.version);
				attributes.put("min_version", Util.getMinVersion());
				attributes.put("min_version_major", Util.getMinMajorVersion());
				attributes.put("min_version_minor", Util.getMinMinorVersion());
				attributes.put("version_major", Config.majorVersion);
				attributes.put("version_minor", Config.minorVersion);
				blocks.versionCheck();
				if (Blocks.getInstance().parsing) attributes.put("parsing", Blocks.getInstance().parsingBlock);
					
				String address = Util.getAddresses().get(0);
				
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
				
				//get top winners
				rs = db.executeQuery("select source, count(bet) as bet_count, avg(bet) as avg_bet, avg(chance) as avg_chance, sum(profit) as sum_profit from bets where validity='valid' group by source order by sum(profit) desc limit 10;");
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
				request.session(true);
				attributes = updateChatStatus(request, attributes);
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
				request.session(true);
				attributes = updateChatStatus(request, attributes);
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
				request.session(true);
				attributes = updateChatStatus(request, attributes);
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
				request.session(true);
				attributes = updateChatStatus(request, attributes);
				attributes.put("title", "Exchange");
				
				Blocks blocks = Blocks.getInstance();
				attributes.put("price_BTC", blocks.priceBTC);
				attributes.put("price_CHA", blocks.priceCHA);
				attributes.put("supply", Util.chaSupply().floatValue() / Config.unit.floatValue());
				attributes.put("blocksBTC", blocks.bitcoinBlock);
				attributes.put("blocksCHA", blocks.chancecoinBlock);
				attributes.put("version", Config.version);
				attributes.put("min_version", Util.getMinVersion());
				attributes.put("min_version_major", Util.getMinMajorVersion());
				attributes.put("min_version_minor", Util.getMinMinorVersion());
				attributes.put("version_major", Config.majorVersion);
				attributes.put("version_minor", Config.minorVersion);
				Blocks.getInstance().versionCheck();
				if (Blocks.getInstance().parsing) attributes.put("parsing", Blocks.getInstance().parsingBlock);
				
				String address = Util.getAddresses().get(0);
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
				for (ECKey key : blocks.wallet.getKeys()) {
					if (key.toAddress(blocks.params).toString().equals(address)) {
						attributes.put("own", true);
					}
				}
				
				if (request.queryParams().contains("form") && request.queryParams("form").equals("cancel")) {
					String txHash = request.queryParams("tx_hash");
					try {
						Transaction tx = Cancel.create(txHash);
						blocks.sendTransaction(address,tx);
						attributes.put("success", "Your order has been cancelled.");
					} catch (Exception e) {
						attributes.put("error", e.getMessage());
					}
				}
				if (request.queryParams().contains("form") && request.queryParams("form").equals("btcpay")) {
					String orderMatchId = request.queryParams("order_match_id");
					try {
						Transaction tx = BTCPay.create(orderMatchId);
						blocks.sendTransaction(address,tx);
						attributes.put("success", "Your payment was successful.");
					} catch (Exception e) {
						attributes.put("error", e.getMessage());
					}
				}
				if (request.queryParams().contains("form") && request.queryParams("form").equals("buy")) {
					String source = request.queryParams("source");
					Double price = Double.parseDouble(request.queryParams("price"));
					Double rawQuantity = Double.parseDouble(request.queryParams("quantity"));
					BigInteger quantity = new BigDecimal(rawQuantity*Config.unit).toBigInteger();
					BigInteger btcQuantity = new BigDecimal(quantity.doubleValue() * price).toBigInteger();
					BigInteger expiration = BigInteger.valueOf(Long.parseLong(request.queryParams("expiration")));
					try {
						Transaction tx = Order.create(source, "BTC", btcQuantity, "CHA", quantity, expiration, BigInteger.ZERO, BigInteger.ZERO);
						blocks.sendTransaction(source,tx);
						attributes.put("success", "Your order was successful.");
					} catch (Exception e) {
						attributes.put("error", e.getMessage());
					}					
				}
				if (request.queryParams().contains("form") && request.queryParams("form").equals("sell")) {
					String source = request.queryParams("source");
					Double price = Double.parseDouble(request.queryParams("price"));
					Double rawQuantity = Double.parseDouble(request.queryParams("quantity"));
					BigInteger quantity = new BigDecimal(rawQuantity*Config.unit).toBigInteger();
					BigInteger btcQuantity = new BigDecimal(quantity.doubleValue() * price).toBigInteger();
					BigInteger expiration = BigInteger.valueOf(Long.parseLong(request.queryParams("expiration")));
					try {
						Transaction tx = Order.create(source, "CHA", quantity, "BTC", btcQuantity, expiration, BigInteger.ZERO, BigInteger.ZERO);
						blocks.sendTransaction(source,tx);
						attributes.put("success", "Your order was successful.");
					} catch (Exception e) {
						attributes.put("error", e.getMessage());
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
				request.session(true);
				attributes = updateChatStatus(request, attributes);
				attributes.put("title", "Exchange");
				
				Blocks blocks = Blocks.getInstance();
				attributes.put("price_BTC", blocks.priceBTC);
				attributes.put("price_CHA", blocks.priceCHA);
				attributes.put("supply", Util.chaSupply().floatValue() / Config.unit.floatValue());
				attributes.put("blocksBTC", blocks.bitcoinBlock);
				attributes.put("blocksCHA", blocks.chancecoinBlock);
				attributes.put("version", Config.version);
				attributes.put("min_version", Util.getMinVersion());
				attributes.put("min_version_major", Util.getMinMajorVersion());
				attributes.put("min_version_minor", Util.getMinMinorVersion());
				attributes.put("version_major", Config.majorVersion);
				attributes.put("version_minor", Config.minorVersion);
				Blocks.getInstance().versionCheck();
				if (Blocks.getInstance().parsing) attributes.put("parsing", Blocks.getInstance().parsingBlock);
				
				String address = Util.getAddresses().get(0);
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
				for (ECKey key : blocks.wallet.getKeys()) {
					if (key.toAddress(blocks.params).toString().equals(address)) {
						attributes.put("own", true);
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
		get(new FreeMarkerRoute("/unspents") {
			@Override
			public ModelAndView handle(Request request, Response response) {
				setConfiguration(configuration);
				Map<String, Object> attributes = new HashMap<String, Object>();
				request.session(true);
				attributes = updateChatStatus(request, attributes);
				attributes.put("title", "Unspents");
				
				Blocks blocks = Blocks.getInstance();
				blocks.deletePending();
				attributes.put("price_BTC", blocks.priceBTC);
				attributes.put("price_CHA", blocks.priceCHA);
				attributes.put("supply", Util.chaSupply().floatValue() / Config.unit.floatValue());
				attributes.put("blocksBTC", blocks.bitcoinBlock);
				attributes.put("blocksCHA", blocks.chancecoinBlock);
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
				for (ECKey key : blocks.wallet.getKeys()) {
					if (key.toAddress(blocks.params).toString().equals(address)) {
						attributes.put("own", true);
					}
				}
				
				Double unspentTotal = 0.0;
				List<UnspentOutput> unspentOutputs = Util.getUnspents(address);
				ArrayList<HashMap<String, Object>> unspents = new ArrayList<HashMap<String, Object>>();
				for (UnspentOutput unspent : unspentOutputs) {
					HashMap<String,Object> map = new HashMap<String,Object>();	
					map.put("amount", unspent.amount);
					map.put("tx_hash", unspent.txid);
					map.put("vout", unspent.vout);
					map.put("type", unspent.type);
					map.put("confirmations", unspent.confirmations);
					unspentTotal += unspent.amount;
					unspents.add(map);
				}
				attributes.put("unspents", unspents);
				attributes.put("unspent_address", Util.unspentAddress(address));
				attributes.put("unspent_total", unspentTotal);

				return modelAndView(attributes, "unspents.html");
			}
		});			
		post(new FreeMarkerRoute("/wallet") {
			@Override
			public ModelAndView handle(Request request, Response response) {
				setConfiguration(configuration);
				Map<String, Object> attributes = new HashMap<String, Object>();
				request.session(true);
				attributes = updateChatStatus(request, attributes);
				attributes.put("title", "Wallet");
				
				Blocks blocks = Blocks.getInstance();
				attributes.put("price_BTC", blocks.priceBTC);
				attributes.put("price_CHA", blocks.priceCHA);
				attributes.put("supply", Util.chaSupply().floatValue() / Config.unit.floatValue());
				attributes.put("blocksBTC", blocks.bitcoinBlock);
				attributes.put("blocksCHA", blocks.chancecoinBlock);
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
						attributes.put("success", "Your private key has been deleted. You can no longer transact from this address.");							
						if (blocks.wallet.getKeys().size()<=0) {
							ECKey newKey = new ECKey();
							blocks.wallet.addKey(newKey);
						}
					} 
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
						try {
							blocks.importPrivateKey(importKey);
							attributes.put("success", "Your transactions have been reimported.");
						} catch (Exception e) {
							attributes.put("error", "Error when reimporting transactions: "+e.getMessage());
						}
					}
				}
								
				if (request.queryParams().contains("form") && request.queryParams("form").equals("send")) {
					String source = request.queryParams("source");
					String destination = request.queryParams("destination");
					String asset = request.queryParams("asset");
					Double rawQuantity = Double.parseDouble(request.queryParams("quantity"));
					BigInteger quantity = new BigDecimal(rawQuantity*Config.unit).toBigInteger();
					try {
						Transaction tx = Send.create(source, destination, asset, quantity);
						blocks.sendTransaction(source,tx);
						attributes.put("success", "You sent "+asset+" successfully.");
					} catch (Exception e) {
						attributes.put("error", e.getMessage());
					}
				}
				
				String address = Util.getAddresses().get(0);
				if (request.session().attributes().contains("address")) {
					address = request.session().attribute("address");
				}
				if (request.queryParams().contains("address")) {
					address = request.queryParams("address");
					request.session().attribute("address", address);
				}
				attributes.put("address", address);		
				for (ECKey key : blocks.wallet.getKeys()) {
					if (key.toAddress(blocks.params).toString().equals(address)) {
						attributes.put("own", true);
					}
				}

				if (request.queryParams().contains("form") && request.queryParams("form").equals("import")) {
					String privateKey = request.queryParams("privatekey");
					try {
						address = Blocks.getInstance().importPrivateKey(privateKey);
						request.session().attribute("address", address);
						attributes.put("address", address);				
						attributes.put("success", "Your private key has been imported.");
					} catch (Exception e) {
						attributes.put("error", "Error when importing private key: "+e.getMessage());
					}
				}

				ArrayList<HashMap<String, Object>> addresses = new ArrayList<HashMap<String, Object>>();
				for (String addr : Util.getAddresses()) {
					HashMap<String,Object> map = new HashMap<String,Object>();	
					map.put("address", addr);
					map.put("balance_CHA", Util.getBalance(addr, "CHA").floatValue() / Config.unit.floatValue());
					addresses.add(map);
				}
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

				//save wallet file
				try {
					blocks.wallet.saveToFile(new File(blocks.walletFile));
				} catch (IOException e) {
				}
				
				return modelAndView(attributes, "wallet.html");
			}
		});	
		get(new FreeMarkerRoute("/wallet") {
			@Override
			public ModelAndView handle(Request request, Response response) {
				setConfiguration(configuration);
				Map<String, Object> attributes = new HashMap<String, Object>();
				request.session(true);
				attributes = updateChatStatus(request, attributes);
				attributes.put("title", "Wallet");
				
				Blocks blocks = Blocks.getInstance();
				attributes.put("price_BTC", blocks.priceBTC);
				attributes.put("price_CHA", blocks.priceCHA);
				attributes.put("supply", Util.chaSupply().floatValue() / Config.unit.floatValue());
				attributes.put("blocksBTC", blocks.bitcoinBlock);
				attributes.put("blocksCHA", blocks.chancecoinBlock);
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
				for (ECKey key : blocks.wallet.getKeys()) {
					if (key.toAddress(blocks.params).toString().equals(address)) {
						attributes.put("own", true);
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
		post(new FreeMarkerRoute("/casino") {
			@Override
			public ModelAndView handle(Request request, Response response) {
				setConfiguration(configuration);
				Map<String, Object> attributes = new HashMap<String, Object>();
				request.session(true);
				attributes = updateChatStatus(request, attributes);
				attributes.put("title", "Casino");
				
				Blocks blocks = Blocks.getInstance();
				attributes.put("price_BTC", blocks.priceBTC);
				attributes.put("price_CHA", blocks.priceCHA);
				attributes.put("supply", Util.chaSupply().floatValue() / Config.unit.floatValue());
				attributes.put("blocksBTC", blocks.bitcoinBlock);
				attributes.put("blocksCHA", blocks.chancecoinBlock);
				attributes.put("version", Config.version);
				attributes.put("min_version", Util.getMinVersion());
				attributes.put("min_version_major", Util.getMinMajorVersion());
				attributes.put("min_version_minor", Util.getMinMinorVersion());
				attributes.put("version_major", Config.majorVersion);
				attributes.put("version_minor", Config.minorVersion);
				Blocks.getInstance().versionCheck();
				if (Blocks.getInstance().parsing) attributes.put("parsing", Blocks.getInstance().parsingBlock);

				String address = Util.getAddresses().get(0);
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
				for (ECKey key : blocks.wallet.getKeys()) {
					if (key.toAddress(blocks.params).toString().equals(address)) {
						attributes.put("own", true);
					}
				}
				
				attributes.put("max_profit", Util.chaSupply().floatValue() / Config.unit.floatValue() * Config.maxProfit);
				attributes.put("max_profit_percentage", Config.maxProfit);
				attributes.put("house_edge", Config.houseEdge);

				if (request.queryParams().contains("form") && request.queryParams("form").equals("bet")) {
					String source = request.queryParams("source");
					Double rawBet = Double.parseDouble(request.queryParams("bet"));
					Double chance = Double.parseDouble(request.queryParams("chance"));
					Double payout = Double.parseDouble(request.queryParams("payout"));
					BigInteger bet = new BigDecimal(rawBet*Config.unit).toBigInteger();
					try {
						Transaction tx = Bet.create(source, bet, chance, payout);
						blocks.sendTransaction(source,tx);
						attributes.put("success", "Thank you for betting!");
					} catch (Exception e) {
						attributes.put("error", e.getMessage());
					}
				}
				
				Database db = Database.getInstance();
				
				//get profit by time
				ResultSet rs = db.executeQuery("select blocks.block_time as block_time, (select -sum(bets2.profit) from bets bets2 where bets2.block_index <= bets1.block_index ) as profit, (select sum((1-bets2.payout*bets2.chance/100.0)*bets2.bet) from bets bets2 where bets2.block_index <= bets1.block_index ) as expected_profit, (select sum(bets2.bet) from bets bets2 where bets2.block_index <= bets1.block_index ) as volume, (select count(bets2.bet) from bets bets2 where bets2.block_index <= bets1.block_index ) as nbets from bets bets1, blocks blocks where blocks.block_index=bets1.block_index order by blocks.block_index asc;");
				ArrayList<HashMap<String, Object>> vstimes = new ArrayList<HashMap<String, Object>>();
				try {
					while (rs.next()) {
						HashMap<String,Object> map = new HashMap<String,Object>();
						map.put("profit", BigInteger.valueOf(rs.getLong("profit")).doubleValue()/Config.unit.doubleValue());
						map.put("expected_profit", BigInteger.valueOf(rs.getLong("expected_profit")).doubleValue()/Config.unit.doubleValue());
						map.put("volume", BigInteger.valueOf(rs.getLong("volume")).doubleValue()/Config.unit.doubleValue());
						map.put("bets", rs.getLong("nbets"));
						map.put("block_time", rs.getInt("block_time"));
						vstimes.add(map);
					}
				} catch (SQLException e) {
				}
				attributes.put("vstimes", vstimes);
				
				//get my profit by time
				rs = db.executeQuery("select blocks.block_time as block_time, (select sum(bets2.profit) from bets bets2 where bets2.block_index <= bets1.block_index and bets2.source='"+address+"' ) as profit, (select sum((1-bets2.payout*bets2.chance/100.0)*bets2.bet) from bets bets2 where bets2.block_index <= bets1.block_index and bets2.source='"+address+"' ) as expected_profit, (select sum(bets2.bet) from bets bets2 where bets2.block_index <= bets1.block_index and bets2.source='"+address+"' ) as volume, (select count(bets2.bet) from bets bets2 where bets2.block_index <= bets1.block_index and bets2.source='"+address+"' ) as nbets from bets bets1, blocks blocks where blocks.block_index=bets1.block_index and bets1.source='"+address+"' order by blocks.block_index asc;");
				ArrayList<HashMap<String, Object>> my_vstimes = new ArrayList<HashMap<String, Object>>();
				try {
					while (rs.next()) {
						HashMap<String,Object> map = new HashMap<String,Object>();
						map.put("profit", BigInteger.valueOf(rs.getLong("profit")).doubleValue()/Config.unit.doubleValue());
						map.put("expected_profit", BigInteger.valueOf(rs.getLong("expected_profit")).doubleValue()/Config.unit.doubleValue());
						map.put("volume", BigInteger.valueOf(rs.getLong("volume")).doubleValue()/Config.unit.doubleValue());
						map.put("bets", rs.getLong("nbets"));
						map.put("block_time", rs.getInt("block_time"));
						my_vstimes.add(map);
					}
				} catch (SQLException e) {
				}
				attributes.put("my_vstimes", my_vstimes);				
				
				//get top winners
				rs = db.executeQuery("select source, count(bet) as bet_count, avg(bet) as avg_bet, avg(chance) as avg_chance, sum(profit) as sum_profit from bets where validity='valid' group by source order by sum(profit) desc limit 10;");
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
				
				//get top 10 highest rollers
				rs = db.executeQuery("select source, count(bet) as bet_count, sum(bet) as sum_bet, avg(chance) as avg_chance, sum(profit) as sum_profit from bets where validity='valid' group by source order by sum(bet) desc limit 10;");
				ArrayList<HashMap<String, Object>> highRollers = new ArrayList<HashMap<String, Object>>();
				try {
					while (rs.next()) {
						HashMap<String,Object> map = new HashMap<String,Object>();
						map.put("source", rs.getString("source"));
						map.put("bet_count", rs.getDouble("bet_count"));
						map.put("sum_bet", BigInteger.valueOf(rs.getLong("sum_bet")).doubleValue()/Config.unit.doubleValue());
						map.put("avg_chance", rs.getDouble("avg_chance"));
						map.put("sum_profit", BigInteger.valueOf(rs.getLong("sum_profit")).doubleValue()/Config.unit.doubleValue());
						highRollers.add(map);
					}
				} catch (SQLException e) {
				}
				attributes.put("high_rollers", highRollers);	
				
				//get top 10 largest bets
				rs = db.executeQuery("select bets.source as source,bet,chance,payout,profit,bets.tx_hash as tx_hash,rolla,rollb,roll,resolved,bets.tx_index as tx_index,block_time from bets,transactions where bets.validity='valid' and bets.tx_index=transactions.tx_index order by bets.bet desc limit 10;");
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
				attributes.put("largest_bets", bets);

				//get last 200 bets
				rs = db.executeQuery("select bets.source as source,bet,chance,payout,profit,bets.tx_hash as tx_hash,rolla,rollb,roll,resolved,bets.tx_index as tx_index,block_time from bets,transactions where bets.validity='valid' and bets.tx_index=transactions.tx_index order by bets.block_index desc, bets.tx_index desc limit 200;");
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
				attributes.put("bets", bets);
				
				//get my bets
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
				
				List<BetInfo> betsPending = Bet.getPending(address);
				bets = new ArrayList<HashMap<String, Object>>();
				for (BetInfo betInfo : betsPending) {
					HashMap<String,Object> map = new HashMap<String,Object>();
					map.put("source", betInfo.source);
					map.put("bet", betInfo.bet.doubleValue()/Config.unit.doubleValue());
					map.put("chance", betInfo.chance);
					map.put("payout", betInfo.payout);
					map.put("tx_hash", betInfo.txHash);
					bets.add(map);
				}
				attributes.put("my_bets_pending", bets);
				
				return modelAndView(attributes, "casino.html");
			}
		});
		get(new FreeMarkerRoute("/casino") {
			@Override
			public ModelAndView handle(Request request, Response response) {
				setConfiguration(configuration);
				Map<String, Object> attributes = new HashMap<String, Object>();
				request.session(true);
				attributes = updateChatStatus(request, attributes);
				attributes.put("title", "Casino");
				
				Blocks blocks = Blocks.getInstance();
				attributes.put("price_BTC", blocks.priceBTC);
				attributes.put("price_CHA", blocks.priceCHA);
				attributes.put("supply", Util.chaSupply().floatValue() / Config.unit.floatValue());
				attributes.put("blocksBTC", blocks.bitcoinBlock);
				attributes.put("blocksCHA", blocks.chancecoinBlock);
				attributes.put("version", Config.version);
				attributes.put("min_version", Util.getMinVersion());
				attributes.put("min_version_major", Util.getMinMajorVersion());
				attributes.put("min_version_minor", Util.getMinMinorVersion());
				attributes.put("version_major", Config.majorVersion);
				attributes.put("version_minor", Config.minorVersion);
				Blocks.getInstance().versionCheck();
				if (Blocks.getInstance().parsing) attributes.put("parsing", Blocks.getInstance().parsingBlock);
				
				String address = Util.getAddresses().get(0);
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
				for (ECKey key : blocks.wallet.getKeys()) {
					if (key.toAddress(blocks.params).toString().equals(address)) {
						attributes.put("own", true);
					}
				}
				
				attributes.put("supply", Util.chaSupply().floatValue() / Config.unit.floatValue());
				attributes.put("max_profit", Util.chaSupply().floatValue() / Config.unit.floatValue() * Config.maxProfit);
				attributes.put("max_profit_percentage", Config.maxProfit);
				attributes.put("house_edge", Config.houseEdge);
				Database db = Database.getInstance();
				
				//get profit by time
				ResultSet rs = db.executeQuery("select blocks.block_time as block_time, (select -sum(bets2.profit) from bets bets2 where bets2.block_index <= bets1.block_index ) as profit, (select sum((1-bets2.payout*bets2.chance/100.0)*bets2.bet) from bets bets2 where bets2.block_index <= bets1.block_index ) as expected_profit, (select sum(bets2.bet) from bets bets2 where bets2.block_index <= bets1.block_index ) as volume, (select count(bets2.bet) from bets bets2 where bets2.block_index <= bets1.block_index ) as nbets from bets bets1, blocks blocks where blocks.block_index=bets1.block_index order by blocks.block_index asc;");
				ArrayList<HashMap<String, Object>> vstimes = new ArrayList<HashMap<String, Object>>();
				try {
					while (rs.next()) {
						HashMap<String,Object> map = new HashMap<String,Object>();
						map.put("profit", BigInteger.valueOf(rs.getLong("profit")).doubleValue()/Config.unit.doubleValue());
						map.put("expected_profit", BigInteger.valueOf(rs.getLong("expected_profit")).doubleValue()/Config.unit.doubleValue());
						map.put("volume", BigInteger.valueOf(rs.getLong("volume")).doubleValue()/Config.unit.doubleValue());
						map.put("bets", rs.getLong("nbets"));
						map.put("block_time", rs.getInt("block_time"));
						vstimes.add(map);
					}
				} catch (SQLException e) {
				}
				attributes.put("vstimes", vstimes);
				
				//get my profit by time
				rs = db.executeQuery("select blocks.block_time as block_time, (select sum(bets2.profit) from bets bets2 where bets2.block_index <= bets1.block_index and bets2.source='"+address+"' ) as profit, (select sum((1-bets2.payout*bets2.chance/100.0)*bets2.bet) from bets bets2 where bets2.block_index <= bets1.block_index and bets2.source='"+address+"' ) as expected_profit, (select sum(bets2.bet) from bets bets2 where bets2.block_index <= bets1.block_index and bets2.source='"+address+"' ) as volume, (select count(bets2.bet) from bets bets2 where bets2.block_index <= bets1.block_index and bets2.source='"+address+"' ) as nbets from bets bets1, blocks blocks where blocks.block_index=bets1.block_index and bets1.source='"+address+"' order by blocks.block_index asc;");
				ArrayList<HashMap<String, Object>> my_vstimes = new ArrayList<HashMap<String, Object>>();
				try {
					while (rs.next()) {
						HashMap<String,Object> map = new HashMap<String,Object>();
						map.put("profit", BigInteger.valueOf(rs.getLong("profit")).doubleValue()/Config.unit.doubleValue());
						map.put("expected_profit", BigInteger.valueOf(rs.getLong("expected_profit")).doubleValue()/Config.unit.doubleValue());
						map.put("volume", BigInteger.valueOf(rs.getLong("volume")).doubleValue()/Config.unit.doubleValue());
						map.put("bets", rs.getLong("nbets"));
						map.put("block_time", rs.getInt("block_time"));
						my_vstimes.add(map);
					}
				} catch (SQLException e) {
				}
				attributes.put("my_vstimes", my_vstimes);
				
				//get top winners
				rs = db.executeQuery("select source, count(bet) as bet_count, avg(bet) as avg_bet, avg(chance) as avg_chance, sum(profit) as sum_profit from bets where validity='valid' group by source order by sum(profit) desc limit 10;");
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
				
				//get top 10 highest rollers
				rs = db.executeQuery("select source, count(bet) as bet_count, sum(bet) as sum_bet, avg(chance) as avg_chance, sum(profit) as sum_profit from bets where validity='valid' group by source order by sum(bet) desc limit 10;");
				ArrayList<HashMap<String, Object>> highRollers = new ArrayList<HashMap<String, Object>>();
				try {
					while (rs.next()) {
						HashMap<String,Object> map = new HashMap<String,Object>();
						map.put("source", rs.getString("source"));
						map.put("bet_count", rs.getDouble("bet_count"));
						map.put("sum_bet", BigInteger.valueOf(rs.getLong("sum_bet")).doubleValue()/Config.unit.doubleValue());
						map.put("avg_chance", rs.getDouble("avg_chance"));
						map.put("sum_profit", BigInteger.valueOf(rs.getLong("sum_profit")).doubleValue()/Config.unit.doubleValue());
						highRollers.add(map);
					}
				} catch (SQLException e) {
				}
				attributes.put("high_rollers", highRollers);	
				
				//get top 10 largest bets
				rs = db.executeQuery("select bets.source as source,bet,chance,payout,profit,bets.tx_hash as tx_hash,rolla,rollb,roll,resolved,bets.tx_index as tx_index,block_time from bets,transactions where bets.validity='valid' and bets.tx_index=transactions.tx_index order by bets.bet desc limit 10;");
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
				attributes.put("largest_bets", bets);

				//get last 200 bets
				rs = db.executeQuery("select bets.source as source,bet,chance,payout,profit,bets.tx_hash as tx_hash,rolla,rollb,roll,resolved,bets.tx_index as tx_index,block_time from bets,transactions where bets.validity='valid' and bets.tx_index=transactions.tx_index order by bets.block_index desc, bets.tx_index desc limit 200;");
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
				attributes.put("bets", bets);

				//get my bets
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
								
				List<BetInfo> betsPending = Bet.getPending(address);
				bets = new ArrayList<HashMap<String, Object>>();
				for (BetInfo betInfo : betsPending) {
					HashMap<String,Object> map = new HashMap<String,Object>();
					map.put("source", betInfo.source);
					map.put("bet", betInfo.bet.doubleValue()/Config.unit.doubleValue());
					map.put("chance", betInfo.chance);
					map.put("payout", betInfo.payout);
					map.put("tx_hash", betInfo.txHash);
					bets.add(map);
				}
				attributes.put("my_bets_pending", bets);

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