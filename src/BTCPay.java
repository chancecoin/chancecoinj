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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.bitcoin.core.Transaction;
import com.google.common.primitives.Ints;

public class BTCPay {
    static Logger logger = LoggerFactory.getLogger(Bet.class);
	public static Integer length = 32+32;
	public static Integer id = 11;
	
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
				
				ResultSet rsCheck = db.executeQuery("select * from btcpays where tx_index='"+txIndex.toString()+"'");
				if (rsCheck.next()) return;

				if (message.size() == length) {
					String tx0Hash = new BigInteger(1, Util.toByteArray(message.subList(0, 32))).toString(16);
					String tx1Hash = new BigInteger(1, Util.toByteArray(message.subList(32, 64))).toString(16);
					String orderMatchId = tx0Hash + tx1Hash;
					ResultSet rsOrderMatch = db.executeQuery("select * from order_matches where validity='pending' and id='"+orderMatchId+"';");
					
					String validity = "invalid";
					if (rsOrderMatch.next()) {
						String orderMatchValidity = rsOrderMatch.getString("validity");
						String orderMatchTx0Address = rsOrderMatch.getString("tx0_address");
						String orderMatchTx1Address = rsOrderMatch.getString("tx1_address");
						String orderMatchForwardAsset = rsOrderMatch.getString("forward_asset");
						String orderMatchBackwardAsset = rsOrderMatch.getString("backward_asset");
						BigInteger orderMatchForwardAmount = BigInteger.valueOf(rsOrderMatch.getLong("forward_amount"));
						BigInteger orderMatchBackwardAmount = BigInteger.valueOf(rsOrderMatch.getLong("backward_amount"));
						if (orderMatchValidity.equals("pending")) {
							Boolean update = false;
							if (orderMatchTx0Address.equals(source) && btcAmount.compareTo(orderMatchForwardAmount)>=0) {
								update = true;
								if (!orderMatchBackwardAsset.equals("BTC")) {
									Util.credit(source, orderMatchBackwardAsset, orderMatchBackwardAmount, "BtcPay", txHash, blockIndex);
								}
								validity = "valid";
							}
							if (orderMatchTx1Address.equals(source) && btcAmount.compareTo(orderMatchBackwardAmount)>=0) {
								update = true;
								if (!orderMatchForwardAsset.equals("BTC")) {
									Util.credit(source, orderMatchForwardAsset, orderMatchForwardAmount, "BtcPay", txHash, blockIndex);
								}
								validity = "valid";
							}
							if (update) {
								db.executeUpdate("update order_matches set validity='"+validity+"' where id='"+orderMatchId+"';");
							}
						}
					}
					db.executeUpdate("insert into btcpays(tx_index, tx_hash, block_index, source, destination, btc_amount, order_match_id, validity) values('"+txIndex.toString()+"','"+txHash+"','"+blockIndex.toString()+"','"+source+"','"+destination+"', '"+btcAmount.toString()+"', '"+orderMatchId+"', '"+validity+"');");												
				}				
			}
		} catch (SQLException e) {	
		}
	}
	public static Transaction create(String orderMatchId) {
		Database db = Database.getInstance();
		String tx0Hash = orderMatchId.substring(0, 64);
		String tx1Hash = orderMatchId.substring(64, 128);
		ResultSet rsOrderMatch = db.executeQuery("select * from order_matches where id='"+orderMatchId+"';");
		String source = "";
		String destination = "";
		BigInteger btcAmount = BigInteger.ZERO;
		try {
			if (rsOrderMatch.next()) {
				String orderMatchValidity = rsOrderMatch.getString("validity");
				String orderMatchTx0Address = rsOrderMatch.getString("tx0_address");
				String orderMatchTx1Address = rsOrderMatch.getString("tx1_address");
				String orderMatchForwardAsset = rsOrderMatch.getString("forward_asset");
				String orderMatchBackwardAsset = rsOrderMatch.getString("backward_asset");
				BigInteger orderMatchForwardAmount = BigInteger.valueOf(rsOrderMatch.getLong("forward_amount"));
				BigInteger orderMatchBackwardAmount = BigInteger.valueOf(rsOrderMatch.getLong("backward_amount"));
				if (orderMatchValidity.equals("pending")) {
					if (orderMatchBackwardAsset.equals("BTC")) {
						source = orderMatchTx1Address;
						destination = orderMatchTx0Address;
						btcAmount = orderMatchBackwardAmount;
					} else {
						source = orderMatchTx0Address;
						destination = orderMatchTx1Address;
						btcAmount = orderMatchForwardAmount;					
					}

					byte[] tx0HashBytes = DatatypeConverter.parseHexBinary(tx0Hash);
					byte[] tx1HashBytes = DatatypeConverter.parseHexBinary(tx1Hash);
					Blocks blocks = Blocks.getInstance();
					ByteBuffer byteBuffer = ByteBuffer.allocate(length+4);
					byteBuffer.putInt(0, id);
					for (int i = 0; i<tx0HashBytes.length; i++) byteBuffer.put(0+4+i, tx0HashBytes[i]);
					for (int i = 0; i<tx1HashBytes.length; i++) byteBuffer.put(32+4+i, tx1HashBytes[i]);
					List<Byte> dataArrayList = Util.toByteArrayList(byteBuffer.array());
					dataArrayList.addAll(0, Util.toByteArrayList(Config.prefix.getBytes()));
					byte[] data = Util.toByteArray(dataArrayList);
	
					String dataString = "";
					try {
						dataString = new String(data,"ISO-8859-1");
					} catch (UnsupportedEncodingException e) {
					}
					btcAmount = btcAmount.max(BigInteger.valueOf(Config.dustSize)); //must sent at least dust size
					Transaction tx = blocks.transaction(source, destination, btcAmount, BigInteger.valueOf(Config.minFee), dataString);
					return tx;
					
				}
			}
		} catch (SQLException e) {
		}
		return null;
	}
}
