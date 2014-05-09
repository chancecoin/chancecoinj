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

public class Send {
    static Logger logger = LoggerFactory.getLogger(Send.class);
	public static Integer length = 8+8;
	public static Integer id = 0;
	
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

				ResultSet rsCheck = db.executeQuery("select * from sends where tx_index='"+txIndex.toString()+"'");
				if (rsCheck.next()) return;

				if (message.size() == length) {
					ByteBuffer byteBuffer = ByteBuffer.allocate(length);
					for (byte b : message) {
						byteBuffer.put(b);
					}			
					Integer assetId = Ints.checkedCast(byteBuffer.getLong(0));
					BigInteger amount = BigInteger.valueOf(byteBuffer.getLong(8));
					String asset = Util.getAssetName(assetId);
					String validity = "invalid";
					BigInteger amountToTransfer = BigInteger.ZERO;
					if (!source.equals("") && !destination.equals("") && asset.equals("CHA")) {
						BigInteger sourceBalance = Util.getBalance(source, asset);
						amountToTransfer = amount.min(sourceBalance);
						validity = "valid";
						if (amountToTransfer.compareTo(BigInteger.ZERO)>0) {
							Util.debit(source, asset, amountToTransfer, "Send", txHash, blockIndex);								
							Util.credit(destination, asset, amountToTransfer, "Send", txHash, blockIndex);								
						}
					}
					db.executeUpdate("insert into sends(tx_index, tx_hash, block_index, source, destination, asset, amount, validity) values('"+txIndex.toString()+"','"+txHash+"','"+blockIndex.toString()+"','"+source+"','"+destination+"','"+asset+"','"+amountToTransfer.toString()+"','"+validity+"')");					
				}				
			}
		} catch (SQLException e) {	
		}
	}
	public static Transaction create(String source, String destination, String asset, BigInteger amount) throws Exception {
		if (!source.equals("") && !destination.equals("") && asset.equals("CHA")) {
			BigInteger sourceBalance = Util.getBalance(source, asset);
			Integer assetId = Util.getAssetId(asset);
			if (sourceBalance.compareTo(amount)>=0) {
				Blocks blocks = Blocks.getInstance();
				ByteBuffer byteBuffer = ByteBuffer.allocate(length+4);
				byteBuffer.putInt(0, id);
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
				Transaction tx = blocks.transaction(source, destination, BigInteger.valueOf(Config.dustSize), BigInteger.valueOf(Config.minFee), dataString);
				return tx;
			} else {
				throw new Exception("Please send less than your balance.");
			}
		} else {
			throw new Exception("Please specify a source address and destination address, and only send CHA.");
		}
	}
}
