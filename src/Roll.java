import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;
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

public class Roll {
	static Logger logger = LoggerFactory.getLogger(Roll.class);
	public static Integer length = 32+8;
	public static Integer id = 14;

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

				ResultSet rsCheck = db.executeQuery("select * from rolls where tx_index='"+txIndex.toString()+"'");
				if (rsCheck.next()) return;

				if (message.size() == length) {
					String rollTxHash = new BigInteger(1, Util.toByteArray(message.subList(0, 32))).toString(16);
					ByteBuffer byteBuffer = ByteBuffer.allocate(length);
					for (byte b : message) {
						byteBuffer.put(b);
					}
					Double roll = byteBuffer.getDouble(32);

					String validity = "invalid";
					if (source.equals(Config.feeAddress)) {
						validity = "valid";
					}
					db.executeUpdate("insert into rolls(tx_index, tx_hash, block_index, source, destination, roll_tx_hash, roll, validity) values('"+txIndex.toString()+"','"+txHash+"','"+blockIndex.toString()+"','"+source+"','"+destination+"','"+rollTxHash.toString()+"','"+roll.toString()+"','"+validity+"')");
				}				
			}
		} catch (SQLException e) {	
		}
	}
	
	public static List<RollRequestInfo> getPendingRollRequests(String source) {
		Database db = Database.getInstance();
		ResultSet rs = db.executeQuery("select * from transactions where block_index<0 and source='"+source+"' order by tx_index desc;");
		List<RollRequestInfo> rolls = new ArrayList<RollRequestInfo>();
		Blocks blocks = Blocks.getInstance();
		try {
			while (rs.next()) {
				String destination = rs.getString("destination");
				BigInteger btcAmount = BigInteger.valueOf(rs.getLong("btc_amount"));
				BigInteger fee = BigInteger.valueOf(rs.getLong("fee"));
				Integer blockIndex = rs.getInt("block_index");
				String txHash = rs.getString("tx_hash");
				Integer txIndex = rs.getInt("tx_index");
				String dataString = rs.getString("data");

				ResultSet rsCheck = db.executeQuery("select * from rolls where tx_index='"+txIndex.toString()+"'");
				if (!rsCheck.next()) {
					List<Byte> messageType = blocks.getMessageTypeFromTransaction(dataString);
					List<Byte> message = blocks.getMessageFromTransaction(dataString);

					if (messageType.get(3)==Roll.id.byteValue() && message.size() == length) {
						ByteBuffer byteBuffer = ByteBuffer.allocate(length);
						for (byte b : message) {
							byteBuffer.put(b);
						}		
						String rollTxHash = new BigInteger(1, Util.toByteArray(message.subList(0, 32))).toString(16);
						Double roll = byteBuffer.getDouble(32);
						RollRequestInfo rollInfo = new RollRequestInfo();
						rollInfo.roll = roll;
						rollInfo.rollTxHash = rollTxHash;
						rollInfo.source = source;
						rolls.add(rollInfo);
					}
				}
			}
		} catch (SQLException e) {	
		}	
		return rolls;
	}
	
	public static Transaction create(String source, String destination, BigInteger btcAmount, String rollTxHash) throws Exception {
		Database db = Database.getInstance();
		rollTxHash = rollTxHash.substring(0, 64);
		byte[] rollTxHashBytes = DatatypeConverter.parseHexBinary(rollTxHash);
		Blocks blocks = Blocks.getInstance();
		ByteBuffer byteBuffer = ByteBuffer.allocate(length+4);
		byteBuffer.putInt(0, id);
		for (int i = 0; i<rollTxHashBytes.length; i++) byteBuffer.put(0+4+i, rollTxHashBytes[i]);
		Random random = new Random();
		byteBuffer.putDouble(32+4, random.nextDouble()); //random double on (0,1)
		List<Byte> dataArrayList = Util.toByteArrayList(byteBuffer.array());
		dataArrayList.addAll(0, Util.toByteArrayList(Config.prefix.getBytes()));
		byte[] data = Util.toByteArray(dataArrayList);

		String dataString = "";
		try {
			dataString = new String(data,"ISO-8859-1");
		} catch (UnsupportedEncodingException e) {
		}
		Transaction tx = blocks.transaction(source, destination, btcAmount, BigInteger.valueOf(Config.minFee), dataString);
		return tx;
	}
	
	public static void serviceRollRequests() {
		if (Util.isOwnAddress(Config.feeAddress)) {
			List<RollRequestInfo> rollsPending = getPendingRollRequests(Config.feeAddress);
			Blocks blocks = Blocks.getInstance();
			Database db = Database.getInstance();
			List<UnspentOutput> unspents = Util.getUnspents(Config.feeAddress);
			for (UnspentOutput unspent : unspents) {
				if (unspent.confirmations.equals(0) && unspent.type.equals("pubkeyhash")) {
					BigInteger amount = new BigDecimal(unspent.amount*Config.unit).toBigInteger();
					String rollTxHash = unspent.txid;
					try {
						ResultSet rs = db.executeQuery("select * from rolls where roll_tx_hash='"+rollTxHash+"';");
						Boolean pending = false;
						for (RollRequestInfo rollInfo : rollsPending) {
							if (rollInfo.rollTxHash.equals(rollTxHash)) {
								pending = true;
							}
						}
						if (!pending && !rs.next()) {
							try {
								String destination = Config.donationAddress;
								Transaction tx = Roll.create(Config.feeAddress, destination, amount.subtract(BigInteger.valueOf(Config.minFee)).subtract(BigInteger.valueOf(Config.dustSize*2)), rollTxHash);
								blocks.sendTransaction(destination, tx);
							} catch (Exception e) {
							}
						}
					} catch (SQLException e) {
					}
				}
			}
		}
	}

}

class RollRequestInfo {
	public String source;
	public String rollTxHash;
	public Double roll;
}
