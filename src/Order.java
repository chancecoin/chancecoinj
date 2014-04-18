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

public class Order {
    static Logger logger = LoggerFactory.getLogger(Bet.class);
	public static Integer length = 8+8+8+8+2+8;
	public static Integer id = 10;
	
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
					for (byte b : message) {
						byteBuffer.put(b);
					}			
					Integer giveId = Ints.checkedCast(byteBuffer.getLong(0));
					BigInteger giveAmount = BigInteger.valueOf(byteBuffer.getLong(8));
					Integer getId = Ints.checkedCast(byteBuffer.getLong(16));
					BigInteger getAmount = BigInteger.valueOf(byteBuffer.getLong(24));
					BigInteger expiration = BigInteger.valueOf(byteBuffer.getShort(32));
					BigInteger feeRequired = BigInteger.valueOf(byteBuffer.getLong(34));
					String giveAsset = Util.getAssetName(giveId);
					String getAsset = Util.getAssetName(getId);
					if (!source.equals("") && !destination.equals("")) {
						BigInteger sourceBalance = Util.getBalance(source, giveAsset);
						Double price = getAmount.doubleValue() / giveAmount.doubleValue();
						if (!giveAsset.equals("BTC")) {
							giveAmount = giveAmount.min(sourceBalance);
							getAmount = new BigDecimal(price * giveAmount.doubleValue()).toBigInteger();
						}
						if (!giveAsset.equals(getAsset) && giveAmount.compareTo(BigInteger.ZERO)>=0 && getAmount.compareTo(BigInteger.ZERO)>=0 && expiration.compareTo(BigInteger.valueOf(Config.maxExpiration))<=0) {
							BigInteger expirationIndex = expiration.add(BigInteger.valueOf(blockIndex));
							db.executeUpdate("insert into transactions(tx_index, tx_hash, block_index, source, give_asset, give_amount, give_remaining, get_asset, get_amount, get_remaining, expiration, expire_index, fee_required, fee_provided, fee_remaining, validity) values('"+txIndex.toString()+"','"+txHash+"','"+blockIndex.toString()+"','"+source+"','"+giveAsset+"','"+giveAmount.toString()+"','"+giveAmount.toString()+"','"+getAsset+"','"+getAmount.toString()+"','"+getAmount.toString()+"','"+expiration.toString()+"','"+expirationIndex.toString()+"','"+feeRequired.toString()+"','"+fee.toString()+"','"+fee.toString()+"','valid')");
							if (!giveAsset.equals("BTC")) {
								Util.debit(source, giveAsset, giveAmount);								
							}
							match(txIndex);
						}
					}
				}				
			}
		} catch (SQLException e) {	
		}
	}
	public static Transaction create(String source, String giveAsset, BigInteger giveAmount, String getAsset, BigInteger getAmount, BigInteger expiration, BigInteger feeRequired, BigInteger feeProvided) {
		if (!source.equals("")) {
			BigInteger sourceBalance = Util.getBalance(source, giveAsset);
			Integer giveId = Util.getAssetId(giveAsset);
			Integer getId = Util.getAssetId(getAsset);
			if (sourceBalance.compareTo(giveAmount)>=0) {
				Blocks blocks = Blocks.getInstance();
				ByteBuffer byteBuffer = ByteBuffer.allocate(length+4);
				byteBuffer.putInt(0, id);
				byteBuffer.putLong(0+4, giveId);
				byteBuffer.putLong(8+4, giveAmount.longValue());
				byteBuffer.putLong(16+4, getId);
				byteBuffer.putLong(24+4, getAmount.longValue());
				byteBuffer.putShort(32+4, expiration.shortValue());
				byteBuffer.putLong(34+4, feeRequired.longValue());
				List<Byte> dataArrayList = Util.toByteArrayList(byteBuffer.array());
				dataArrayList.addAll(0, Util.toByteArrayList(Config.prefix.getBytes()));
				byte[] data = Util.toByteArray(dataArrayList);

				String dataString = "";
				try {
					dataString = new String(data,"ISO-8859-1");
				} catch (UnsupportedEncodingException e) {
				}
				String destination = source;
				Transaction tx = blocks.transaction(source, destination, BigInteger.valueOf(Config.dustSize), BigInteger.valueOf(Config.minFee), dataString);
				return tx;
			}
		}
		return null;
	}
	public static void match(Integer txIndex) {
		Database db = Database.getInstance();
		ResultSet rsOrder = db.executeQuery("select * from orders where tx_index="+txIndex.toString());
		try {
			if (rsOrder.next()) {
				String giveAsset = rsOrder.getString("give_asset");
				String getAsset = rsOrder.getString("get_asset");
				ResultSet rsMatches = db.executeQuery("select * from orders where give_asset='"+getAsset+"' and get_asset='"+giveAsset+"' and validity='valid';");
				
			}
		} catch (SQLException e) {	
		}
	}
	public static void expire() {
		
	}
}
