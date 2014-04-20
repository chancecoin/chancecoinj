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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.bitcoin.core.Transaction;
import com.google.common.primitives.Ints;

public class Cancel {
    static Logger logger = LoggerFactory.getLogger(Bet.class);
	public static Integer length = 32;
	public static Integer id = 70;
	
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

				if (message.size() == length) {
					ByteBuffer byteBuffer = ByteBuffer.allocate(length);
					String offerHash = new BigInteger(1, Util.toByteArray(message.subList(0, 32))).toString(16);
					ResultSet rsOrder = db.executeQuery("select * from orders where tx_hash='"+offerHash+"' and source='"+source+"' and validity='valid';");
					String validity = "invalid";
					if (rsOrder.next()) {
						String orderTxHash = rsOrder.getString("tx_hash");
						String orderGiveAsset = rsOrder.getString("give_asset");
						BigInteger orderGiveRemaining = BigInteger.valueOf(rsOrder.getLong("get_remaining"));
						db.executeUpdate("update orders set validity='cancelled' where tx_hash='"+orderTxHash+"';");
						if (!orderGiveAsset.equals("BTC")) {
							Util.credit(source, orderGiveAsset, orderGiveRemaining);
						}
						validity = "valid";
					}
					db.executeUpdate("insert into cancels(tx_index, tx_hash, block_index, source, offer_hash, validity) values('"+txIndex.toString()+"','"+txHash+"','"+blockIndex.toString()+"','"+source+"','"+offerHash+"','"+validity+"')");
				}				
			}
		} catch (SQLException e) {	
		}
	}
	public static Transaction create(String offerHash) {
		Database db = Database.getInstance();
		ResultSet rsOrderMatch = db.executeQuery("select * from orders where tx_hash='"+offerHash+"';");
		String source = "";
		String destination = "";
		BigInteger btcAmount = BigInteger.ZERO;
		try {
			if (rsOrderMatch.next()) {
				String orderMatchValidity = rsOrderMatch.getString("validity");
				String orderMatchSource = rsOrderMatch.getString("source");
				if (orderMatchValidity.equals("pending")) {
					byte[] offerHashBytes = null;
					try {
						offerHashBytes = offerHash.getBytes("UTF-8");
					} catch (UnsupportedEncodingException e1) {
					}
					Blocks blocks = Blocks.getInstance();
					ByteBuffer byteBuffer = ByteBuffer.allocate(length+4);
					byteBuffer.putInt(0, id);
					byteBuffer.put(offerHashBytes, 0+4, 32);
					List<Byte> dataArrayList = Util.toByteArrayList(byteBuffer.array());
					dataArrayList.addAll(0, Util.toByteArrayList(Config.prefix.getBytes()));
					byte[] data = Util.toByteArray(dataArrayList);
	
					String dataString = "";
					try {
						dataString = new String(data,"ISO-8859-1");
					} catch (UnsupportedEncodingException e) {
					}
					source = orderMatchSource;
					Transaction tx = blocks.transaction(source, "", BigInteger.ZERO, BigInteger.valueOf(Config.minFee), dataString);
					return tx;
				}
			}
		} catch (SQLException e) {
		}
		return null;
	}
}
