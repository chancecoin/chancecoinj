import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.xml.bind.DatatypeConverter;

import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.bitcoin.core.Transaction;
import com.google.common.primitives.Ints;

public class QuotePay {
    static Logger logger = LoggerFactory.getLogger(QuotePay.class);
	public static Integer length = 32+32;
	public static Integer id = 13;
	
	public static void parse(Integer txIndex, List<Byte> message) {
		Database db = Database.getInstance();
		ResultSet rs = db.executeQuery("select * from transactions where tx_index="+txIndex.toString());
		try {
			if (rs.next()) {
				String source = rs.getString("source");
				String destination = rs.getString("destination");
				BigInteger btcAmount = BigInteger.valueOf(rs.getLong("btc_amount"));
				BigInteger fee = BigInteger.valueOf(rs.getLong("fee"));
				Integer blockIndex = rs.getInt("block_index");
				String txHash = rs.getString("tx_hash");
				
				ResultSet rsCheck = db.executeQuery("select * from quotepays where tx_index='"+txIndex.toString()+"'");
				if (rsCheck.next()) return;

				if (message.size() == length) {
					String txHashBuy = new BigInteger(1, Util.toByteArray(message.subList(0, 32))).toString(16);
					String txHashSell = new BigInteger(1, Util.toByteArray(message.subList(32, 64))).toString(16);
					String quotePayId = txHashBuy + txHashSell;
					ResultSet rsQuotePay = db.executeQuery("select * from quotepays where validity='pending' and id='"+quotePayId+"';");
					
					String validity = "invalid";
					if (rsQuotePay.next()) {
						String quotePayValidity = rsQuotePay.getString("validity");
						String quotePaySource = rsQuotePay.getString("source");
						String quotePayDestination = rsQuotePay.getString("destination");
						BigInteger btcAmountNeeded = BigInteger.valueOf(rsQuotePay.getLong("btc_amount"));
						if (quotePayValidity.equals("pending") && source.equals(quotePaySource) && destination.equals(quotePayDestination)) {
							validity = "valid";							
							db.executeUpdate("update quotepays set validity='"+validity+"', tx_hash='"+txHash+"', block_index='"+blockIndex.toString()+"' where id='"+quotePayId+"';");
						}
					}
				}				
			}
		} catch (SQLException e) {	
		}
	}
	public static Transaction create(String quotePayId) throws Exception {
		Database db = Database.getInstance();
		String txBuyHash = quotePayId.substring(0, 64);
		String txSellHash = quotePayId.substring(64, 128);
		ResultSet rsQuotePay = db.executeQuery("select * from quotepays where id='"+quotePayId+"';");
		try {
			if (rsQuotePay.next()) {
				String validity = rsQuotePay.getString("validity");
				String source = rsQuotePay.getString("source");
				String destination = rsQuotePay.getString("destination");
				BigInteger btcAmount = BigInteger.valueOf(rsQuotePay.getLong("btc_amount"));
				if (validity.equals("pending")) {
					byte[] txBuyHashBytes = DatatypeConverter.parseHexBinary(txBuyHash);
					byte[] txSellHashBytes = DatatypeConverter.parseHexBinary(txSellHash);
					Blocks blocks = Blocks.getInstance();
					ByteBuffer byteBuffer = ByteBuffer.allocate(length+4);
					byteBuffer.putInt(0, id);
					for (int i = 0; i<txBuyHashBytes.length; i++) byteBuffer.put(0+4+i, txBuyHashBytes[i]);
					for (int i = 0; i<txSellHashBytes.length; i++) byteBuffer.put(32+4+i, txSellHashBytes[i]);
					List<Byte> dataArrayList = Util.toByteArrayList(byteBuffer.array());
					dataArrayList.addAll(0, Util.toByteArrayList(Config.prefix.getBytes()));
					byte[] data = Util.toByteArray(dataArrayList);
	
					String dataString = "";
					try {
						dataString = new String(data,"ISO-8859-1");
					} catch (UnsupportedEncodingException e) {
					}
					btcAmount = btcAmount.max(BigInteger.valueOf(Config.dustSize)); //must send at least dust size
					Transaction tx = blocks.transaction(source, destination, btcAmount, BigInteger.valueOf(Config.minFee), dataString);
					return tx;
				} else {
					throw new Exception("Please specify a quote that is awaiting payment.");					
				}
			} else {
				throw new Exception("Please specify a valid quote to pay for.");
			}
		} catch (SQLException e) {
			throw new Exception(e.getMessage());
		}
	}
	
	public static void pay() {
		Database db = Database.getInstance();
		Blocks blocks = Blocks.getInstance();
		ResultSet rs = db.executeQuery("select * from quotepays where validity='pending'");
		try {
			while (rs.next()) {
				String quotePayId = rs.getString("id");
				String source = rs.getString("source");
				String destination = rs.getString("destination");
				BigInteger btcAmount = BigInteger.valueOf(rs.getLong("btc_amount"));
				if (Util.isOwnAddress(source)) {
					BigInteger balance = Util.getBalance(source, "BTC");
					if (btcAmount.compareTo(balance)<=0) {
						try {
							Transaction tx = QuotePay.create(quotePayId);
							logger.info("Sending quote pay from "+source+" to "+destination+ " in the amount of "+btcAmount.toString());
							blocks.sendTransaction(source, tx);
						} catch (Exception e) {
							logger.error(e.getMessage());
						}
					}
				}
			}
		} catch (SQLException e) {	
		}		
	}
}
