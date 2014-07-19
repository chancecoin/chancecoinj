//import java.io.IOException;
//import java.io.UnsupportedEncodingException;
//import java.math.BigDecimal;
//import java.math.BigInteger;
//import java.nio.ByteBuffer;
//import java.sql.ResultSet;
//import java.sql.SQLException;
//import java.text.SimpleDateFormat;
//import java.util.Calendar;
//import java.util.Date;
//import java.util.List;
//import java.util.Map;
//import java.util.TimeZone;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import com.fasterxml.jackson.core.JsonParseException;
//import com.fasterxml.jackson.core.type.TypeReference;
//import com.fasterxml.jackson.databind.DeserializationFeature;
//import com.fasterxml.jackson.databind.JsonMappingException;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.google.bitcoin.core.Transaction;
//import com.google.common.primitives.Ints;
//
//public class Quote {
//	static Logger logger = LoggerFactory.getLogger(Order.class);
//	public static Integer length = 8+8+8+2;
//	public static Integer id = 12;
//
//	public static void parse(Integer txIndex, List<Byte> message) {
//		Database db = Database.getInstance();
//		ResultSet rs = db.executeQuery("select * from transactions where tx_index="+txIndex.toString());
//		try {
//			if (rs.next()) {
//				String source = rs.getString("source");
//				String destination = rs.getString("destination");
//				BigInteger btcAmount = BigInteger.valueOf(rs.getLong("btc_amount"));
//				BigInteger fee = BigInteger.valueOf(rs.getLong("fee"));
//				Integer blockIndex = rs.getInt("block_index");
//				String txHash = rs.getString("tx_hash");
//
//				ResultSet rsCheck = db.executeQuery("select * from quotes where tx_index='"+txIndex.toString()+"'");
//				if (rsCheck.next()) return;
//
//				if (message.size() == length) {
//					ByteBuffer byteBuffer = ByteBuffer.allocate(length);
//					for (byte b : message) {
//						byteBuffer.put(b);
//					}
//					BigInteger btcQuote = BigInteger.valueOf(byteBuffer.getLong(0));
//					BigInteger chaQuote = BigInteger.valueOf(byteBuffer.getLong(8));
//					Double width = byteBuffer.getDouble(16);
//					BigInteger expiration = BigInteger.valueOf(byteBuffer.getShort(24));
//					String validity = "invalid";
//					BigInteger expirationIndex = expiration.add(BigInteger.valueOf(blockIndex));
//					if (!source.equals("") && width<Config.maxWidth) {
//						BigInteger chaBalance = Util.getBalance(source, "CHA");
//						Double price = btcQuote.doubleValue() / chaQuote.doubleValue();
//						if (btcQuote.compareTo(btcAmount)>=0 && chaQuote.compareTo(chaBalance)>=0 && !destination.equals("") && expiration.compareTo(BigInteger.valueOf(Config.maxExpiration))<=0) {
//							validity = "valid";
//							Util.debit(source, "CHA", chaQuote, "Debit to cover quote", txHash, blockIndex);								
//							Util.credit(destination, "CHA", chaQuote, "Credit to cover quote", txHash, blockIndex);								
//						}
//					}
//					db.executeUpdate("insert into quotes(tx_index, tx_hash, block_index, source, destination, btc_amount, cha_amount, btc_remaining, cha_remaining, width, expiration, expire_index, validity) values('"+txIndex.toString()+"','"+txHash+"','"+blockIndex.toString()+"','"+source+"','"+destination+"','"+btcQuote.toString()+"','"+chaQuote.toString()+"','"+btcQuote.toString()+"','"+chaQuote.toString()+"','"+width.toString()+"','"+expiration.toString()+"','"+expirationIndex.toString()+"','"+validity+"')");
//					expire(blockIndex);
//				}				
//			}
//		} catch (SQLException e) {	
//		}
//	}
//	public static Transaction create(String source, String destination, BigInteger btcQuote, BigInteger chaQuote, Double width, BigInteger expiration) throws Exception {
//		if (!source.equals("")) {
//			BigInteger chaBalance = Util.getBalance(source, "CHA");
//			BigInteger btcBalance = Util.getBalance(source, "BTC");
//			if (chaBalance.compareTo(chaQuote)>=0 && btcBalance.compareTo(btcQuote)>=0) {
//				if (width<Config.maxWidth) {
//					Blocks blocks = Blocks.getInstance();
//					ByteBuffer byteBuffer = ByteBuffer.allocate(length+4);
//					byteBuffer.putInt(0, id);
//					byteBuffer.putLong(0+4, btcQuote.longValue());
//					byteBuffer.putLong(8+4, chaQuote.longValue());
//					byteBuffer.putDouble(16+4, width);
//					byteBuffer.putShort(24+4, expiration.shortValue());
//					List<Byte> dataArrayList = Util.toByteArrayList(byteBuffer.array());
//					dataArrayList.addAll(0, Util.toByteArrayList(Config.prefix.getBytes()));
//					byte[] data = Util.toByteArray(dataArrayList);
//
//					String dataString = "";
//					try {
//						dataString = new String(data,"ISO-8859-1");
//					} catch (UnsupportedEncodingException e) {
//					}
//					if (destination.equals("")) {
//						destination = Config.marketMakingAddress;
//					}
//					Transaction tx = blocks.transaction(source, destination, BigInteger.ZERO, BigInteger.valueOf(Config.minFee), dataString);
//					return tx;
//				} else {
//					throw new Exception("Your quote must be less than "+(Config.maxWidth*100)+"% wide.");					
//				}
//			} else {
//				throw new Exception("Please make your quote smaller than your balance.");
//			}
//		} else {
//			throw new Exception("Please specify a source address.");
//		}
//	}
//
//	public static void expire() {
//		expire(Util.getLastBlock());
//	}
//
//	public static void expire(Integer lastBlock) {
//		logger.info("Expiring quotes");
//
//		Database db = Database.getInstance();
//		ResultSet rs = db.executeQuery("select * from quotes where validity='valid' and expire_index<"+lastBlock.toString());
//		try {
//			while (rs.next()) {
//				String txIndex = rs.getString("tx_index");
//				Integer blockIndex = rs.getInt("block_index");
//				String txHash = rs.getString("tx_hash");
//				String source = rs.getString("source");
//				String destination = rs.getString("destintion");
//				BigInteger chaRemaining = BigInteger.valueOf(rs.getLong("cha_remaining"));
//				BigInteger btcRemaining = BigInteger.valueOf(rs.getLong("btc_remaining"));
//				db.executeUpdate("update quotes set validity = 'invalid: expired' where tx_index = '"+txIndex+"'");
//				Util.credit(source, "CHA", chaRemaining, "Expired quote credit", txHash, blockIndex);
//				Util.debit(destination, "CHA", chaRemaining, "Expired quote debit", txHash, blockIndex);
//				db.executeUpdate("insert into quotepays (id,bet_tx_hash,quote_tx_hash,buysell,source,destination,btc_amount,validity) values('"+txHash+"','','"+txHash+"','0','"+destination+"','"+source+"','"+btcRemaining+"','pending')");
//			}
//		} catch (SQLException e) {	
//		}		
//	}
//}
