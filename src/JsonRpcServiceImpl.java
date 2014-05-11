import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.bitcoin.core.Transaction;

public class JsonRpcServiceImpl implements JsonRpcService {
    static Logger logger = LoggerFactory.getLogger(JsonRpcServiceImpl.class);
	
	public String getBalance(String address) {
		BigInteger balance = Util.getBalance(address, "CHA");
		return Double.toString(balance.doubleValue() / Config.unit);
	}
	
	public String send(String source, String destination, Double amount) {
		Blocks blocks = Blocks.getInstance();
		BigInteger quantity = new BigDecimal(amount*Config.unit).toBigInteger();
		try {
			Transaction tx = Send.create(source, destination, "CHA", quantity);
			blocks.sendTransaction(tx);
			System.out.println("Success! You sent "+amount+" CHA to "+destination+".");
			return "success";
		} catch (Exception e) {
			System.out.println("Error! There was a problem with your transaction: "+e.getMessage());						
		}
		return "failure";
	}
	
	public String getSends(String address) {
		Database db = Database.getInstance();
		//get my sends
		ResultSet rs = db.executeQuery("select * from sends where (source='"+address+"') and asset='CHA' and validity='valid' order by block_index desc, tx_index desc;");
		JSONArray jsonArray = new JSONArray();
		try {
			while (rs.next()) {
				HashMap<String,Object> map = new HashMap<String,Object>();
				map.put("amount", BigInteger.valueOf(rs.getLong("amount")).doubleValue()/Config.unit.doubleValue());
				map.put("tx_hash", rs.getString("tx_hash"));
				map.put("source", rs.getString("source"));
				map.put("destination", rs.getString("destination"));
				jsonArray.put(map);
			}
		} catch (SQLException e) {
		}
		return jsonArray.toString();								
	}
	
	public String getReceives(String address) {
		Database db = Database.getInstance();
		//get my receives
		ResultSet rs = db.executeQuery("select * from sends where (destination='"+address+"') and asset='CHA' and validity='valid' order by block_index desc, tx_index desc;");
		JSONArray jsonArray = new JSONArray();
		try {
			while (rs.next()) {
				HashMap<String,Object> map = new HashMap<String,Object>();
				map.put("amount", BigInteger.valueOf(rs.getLong("amount")).doubleValue()/Config.unit.doubleValue());
				map.put("tx_hash", rs.getString("tx_hash"));
				map.put("source", rs.getString("source"));
				map.put("destination", rs.getString("destination"));
				jsonArray.put(map);
				
			}
		} catch (SQLException e) {
		}
		return jsonArray.toString();									
	}
	
	
}