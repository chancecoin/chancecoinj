import java.io.BufferedInputStream;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLConnection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.TransactionOutput;
import com.google.bitcoin.script.Script;


public class Util {

	public static String getPage(String url_string) {
		return getPage(url_string, 1);

	}

	public static String getPage(String url_string, int retries) {
		URL url;
		String text = null;
		try {
			url = new URL(url_string);
			URLConnection urlc = url.openConnection();

			BufferedInputStream buffer = new BufferedInputStream(urlc.getInputStream());

			StringBuilder builder = new StringBuilder();

			int byteRead;

			while ((byteRead = buffer.read()) != -1) {
			    builder.append((char) byteRead);
			}

			buffer.close();

			text=builder.toString();
			
		} catch (Exception e) {
			if (retries != 0) {
				return getPage(url_string, retries-1);	
			} else {
				e.printStackTrace();
			}
		}
		return text;
	}	
	
	public static Map<String,Object> infoGetBlock(Integer blockNumber) {
		String result = getPage("https://blockchain.info/block-index/"+Integer.toString(blockNumber)+"?format=json");
	    try {
			Map<String, Object> map = (new ObjectMapper()).readValue(result, new TypeReference<Map<String,Object>>() { });
			return map;
		} catch (Exception e) {
		}
	    return null;
	}
	
	public static Map<String,Object> infoGetBlockByHash(String blockHash) {
		String result = getPage("https://blockchain.info/rawblock/"+blockHash);
	    try {
			Map<String, Object> map = (new ObjectMapper()).readValue(result, new TypeReference<Map<String,Object>>() { });
			return map;
		} catch (Exception e) {
		}
	    return null;
	}

	public static String format(Double input) {
		return format(input, "#.00");
	}
	
	public static String format(Double input, String format) {
		return (new DecimalFormat(format)).format(input);
	}
	
    static float roundOff(Double x, int position)
    {
        float a = x.floatValue();
        double temp = Math.pow(10.0, position);
        a *= temp;
        a = Math.round(a);
        return (a / (float)temp);
    }
		
	public static Integer getLastBlock() {
		Database db = Database.getInstance();
		ResultSet rs = db.executeQuery("select * from blocks order by block_index desc limit 1;");
		try {
			while(rs.next()) {
			    return rs.getInt("block_index");
			}
		} catch (SQLException e) {
		}	
		return 0;
	}

	public static Integer getLastTxIndex() {
		Database db = Database.getInstance();
		ResultSet rs = db.executeQuery("SELECT * FROM transactions WHERE tx_index = (SELECT MAX(tx_index) from transactions);");
		try {
			while(rs.next()) {
			    return rs.getInt("tx_index");
			}
		} catch (SQLException e) {
		}	
		return 0;
	}	
	
	public static void debit(String address, String asset, BigInteger amount) {
		Database db = Database.getInstance();
		if (hasBalance(address, asset)) {
			BigInteger existingAmount = getBalance(address,asset);
			BigInteger newAmount = existingAmount.subtract(amount);
			if (newAmount.compareTo(BigInteger.ZERO)>0) {
				db.executeUpdate("update balances set amount='"+newAmount.toString()+"' where address='"+address+"' and asset='"+asset+"';");
			    db.executeUpdate("insert into debits(address, asset, amount) values('"+address+"','"+asset+"','"+amount.toString()+"');");
			}
		}
	}
	public static void credit(String address, String asset, BigInteger amount) {
		Database db = Database.getInstance();
		if (hasBalance(address, asset)) {
			BigInteger existingAmount = getBalance(address,asset);
			BigInteger newAmount = existingAmount.add(amount);
			db.executeUpdate("update balances set amount='"+newAmount.toString()+"' where address='"+address+"' and asset='"+asset+"';");
		} else {
			db.executeUpdate("insert into balances(address, asset, amount) values('"+address+"','"+asset+"','"+amount.toString()+"');");				
		}
	    db.executeUpdate("insert into credits(address, asset, amount) values('"+address+"','"+asset+"','"+amount.toString()+"');");
	}
	public static Boolean hasBalance(String address, String asset) {
		Database db = Database.getInstance();
		ResultSet rs = db.executeQuery("select amount from balances where address='"+address+"' and asset='"+asset+"';");
		try {
			if (rs.next()) {
				return true;
			}
		} catch (SQLException e) {
		}
		return false;
	}
	public static BigInteger getBalance(String address, String asset) {
		Database db = Database.getInstance();
		Blocks blocks = Blocks.getInstance();
		if (asset.equals("BTC")) {
			LinkedList<TransactionOutput> unspentOutputs = blocks.wallet.calculateAllSpendCandidates(true);
			BigInteger totalBalance = BigInteger.ZERO;
			for (TransactionOutput out : unspentOutputs) {
				Script script = out.getScriptPubKey();
				if (script.getToAddress(blocks.params).toString().equals(address)) {
					totalBalance = totalBalance.add(out.getValue());
				}
			}			
			return totalBalance;
		} else {
			ResultSet rs = db.executeQuery("select sum(amount) as amount from balances where address='"+address+"' and asset='"+asset+"';");
			try {
				if (rs.next()) {
					return BigInteger.valueOf(rs.getLong("amount"));
				}
			} catch (SQLException e) {
			}
		}
		return BigInteger.ZERO;
	}
	public static BigInteger chaSupply() {
		Database db = Database.getInstance();
		ResultSet rs = db.executeQuery("select sum(amount) as amount from balances where asset='CHA';");
		try {
			if (rs.next()) {
				return BigInteger.valueOf(rs.getLong("amount"));
			}
		} catch (SQLException e) {
		}
		return BigInteger.ZERO;
	}
	
	public static List<String> getAddresses() {
		Blocks blocks = Blocks.getInstance();
		List<ECKey> keys = blocks.wallet.getKeys();
		List<String> addresses = new ArrayList<String>();
		for(ECKey key : keys) {
			addresses.add(key.toAddress(blocks.params).toString());
		}
		return addresses;
	}
	
	public static Integer getAssetId(String asset) {
		if (asset.equals("BTC")) {
			return 0;
		} else if (asset.equals("CHA")) {
			return 1;
		} else {
			return null;
		}
	}
	public static String getAssetName(Integer assetId) {
		if (assetId==0) {
			return "BTC";
		} else if (assetId==1) {
			return "CHA";
		} else {
			return null;
		}
	}
	
	public static byte[] toByteArray(List<Byte> in) {
	    final int n = in.size();
	    byte ret[] = new byte[n];
	    for (int i = 0; i < n; i++) {
	        ret[i] = in.get(i);
	    }
	    return ret;
	}	
	public static List<Byte> toByteArrayList(byte[] in) {
		List<Byte> arrayList = new ArrayList<Byte>();
		
		for (byte b : in) {
			arrayList.add(b);
		}
		return arrayList;
	}	
}
