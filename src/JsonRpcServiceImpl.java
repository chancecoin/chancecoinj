import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.bitcoin.core.Transaction;


public class JsonRpcServiceImpl implements JsonRpcService {
	
	public Double getChancecoinBalance(String address) {
		Database db = Database.getInstance();		
		ResultSet rs = db.executeQuery("select sum(amount) as amount from balances where address='"+address+"' and asset='CHA';");
		try {
			if (rs.next()) {
				return rs.getLong("amount") / Config.unit.doubleValue();
			}
		} catch (SQLException e) {
		}
		
		return 0.0;
	}
	
	public Transaction sendChancecoin(String source, String destination, BigInteger amount) {
		String asset = "CHA";
		Transaction tx = null;
		Blocks blocks = Blocks.getInstance();
		if (!source.equals("") && !destination.equals("")) {
			BigInteger sourceBalance = Util.getBalance(source, asset);
			Integer assetId = Util.getAssetId(asset);
			if (sourceBalance.compareTo(amount)>=0) {
				ByteBuffer byteBuffer = ByteBuffer.allocate(20);
				byteBuffer.putInt(0, 0);
				byteBuffer.putLong(0+4, assetId);
				byteBuffer.putLong(8+4, amount.longValue());
				List<Byte> dataArrayList = Util.toByteArrayList(byteBuffer.array());
				dataArrayList.addAll(0, Util.toByteArrayList(Config.prefix.getBytes()));
				byte[] data = Util.toByteArray(dataArrayList);

				String dataString = "";
				try {
					dataString = new String(data,"ISO-8859-1");
				} catch (UnsupportedEncodingException e) {
				}
				try {
					tx = blocks.transaction(source, destination, BigInteger.valueOf(Config.dustSize), BigInteger.valueOf(Config.minFee), dataString);
				} catch (Exception e) {
				}
				return tx;
			}
		}
		
		if (tx!=null) {
			if (blocks.sendTransaction(tx)) {
				System.out.println("Success! You sent "+amount+" CHA to "+destination+".");
			} else {
				System.out.println("Error! Your transaction timed out and was not received by the Bitcoin network. Please try again.");							
			}
		}else{
			System.out.println("Error! There was a problem with your transaction.");						
		}
		return null;
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