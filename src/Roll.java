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
	public static Integer length2 = 32+8+8; //last parameter is amount of CHA given for bet made with BTC
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

				if (message.size() == length || message.size() == length2) {
					String rollTxHash = new BigInteger(1, Util.toByteArray(message.subList(0, 32))).toString(16);
					while (rollTxHash.length()<64) rollTxHash = "0"+rollTxHash;
					ByteBuffer byteBuffer = ByteBuffer.allocate(message.size());
					for (byte b : message) {
						byteBuffer.put(b);
					}
					Double roll = byteBuffer.getDouble(32) * 100.0;
					BigInteger chaAmount = BigInteger.ZERO;
					if (message.size() == length2) {
						chaAmount = BigInteger.valueOf(byteBuffer.getLong(32+8));
					}

					String validity = "invalid";
					if (source.equals(Config.feeAddress)) {
						validity = "valid";
					}
					if (chaAmount.compareTo(BigInteger.ZERO)>0) {
						Util.debit(source, "CHA", chaAmount, "Debit CHA amount from casino liquidity provider", txHash, blockIndex);
						db.executeUpdate("update bets set bet = '"+chaAmount.toString()+"' where tx_hash='"+rollTxHash.toString()+"';");
					}
					ResultSet rsBet = db.executeQuery("select * from bets where tx_hash='"+rollTxHash.toString()+"' and resolved='true';");
					if (rsBet.next()) {
						db.executeUpdate("update bets set resolved='', profit='0', roll='', rolla='', rollb='' where tx_hash='"+rollTxHash.toString()+"';");						
						db.executeUpdate("delete from credits where event='"+rollTxHash.toString()+"';");
					}
					db.executeUpdate("insert into rolls(tx_index, tx_hash, block_index, source, destination, roll_tx_hash, roll, cha_amount, validity) values('"+txIndex.toString()+"','"+txHash+"','"+blockIndex.toString()+"','"+source+"','"+destination+"','"+rollTxHash.toString()+"','"+roll.toString()+"','"+chaAmount.toString()+"','"+validity+"')");
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

					if (messageType.get(3)==Roll.id.byteValue() && (message.size() == length || message.size() == length2)) {
						ByteBuffer byteBuffer = ByteBuffer.allocate(length);
						for (byte b : message) {
							byteBuffer.put(b);
						}		
						String rollTxHash = new BigInteger(1, Util.toByteArray(message.subList(0, 32))).toString(16);
						while (rollTxHash.length()<64) rollTxHash = "0"+rollTxHash;
						Double roll = byteBuffer.getDouble(32);
						BigInteger chaAmount = BigInteger.ZERO;
						if (message.size() == length2) {
							chaAmount = BigInteger.valueOf(byteBuffer.getLong(32+8));
						}
						RollRequestInfo rollInfo = new RollRequestInfo();
						rollInfo.roll = roll;
						rollInfo.chaAmount = chaAmount;
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
	
	public static Transaction create(String source, String destination, BigInteger btcAmount, String rollTxHash, BigInteger chaAmount, String useUnspentTxHash, Integer useUnspentVout) throws Exception {
		List<String> destinations = new ArrayList<String>();
		destinations.add(destination);
		List<BigInteger> btcAmounts = new ArrayList<BigInteger>();
		btcAmounts.add(btcAmount);
		return create(source, destinations, btcAmounts, rollTxHash, chaAmount, useUnspentTxHash, useUnspentVout);
	}
	public static Transaction create(String source, List<String> destinations, List<BigInteger> btcAmounts, String rollTxHash, BigInteger chaAmount, String useUnspentTxHash, Integer useUnspentVout) throws Exception {
		Database db = Database.getInstance();
		rollTxHash = rollTxHash.substring(0, 64);
		byte[] rollTxHashBytes = DatatypeConverter.parseHexBinary(rollTxHash);
		Blocks blocks = Blocks.getInstance();
		ByteBuffer byteBuffer = null;			
		if (chaAmount.compareTo(BigInteger.ZERO)>0) {
			byteBuffer = ByteBuffer.allocate(length2+4);
		} else {
			byteBuffer = ByteBuffer.allocate(length+4);			
		}
		byteBuffer.putInt(0, id);
		for (int i = 0; i<rollTxHashBytes.length; i++) byteBuffer.put(0+4+i, rollTxHashBytes[i]);
		Random random = new Random();
		Double roll = random.nextDouble();
		byteBuffer.putDouble(32+4, roll); //random double on (0,1)
		if (chaAmount.compareTo(BigInteger.ZERO)>0) {
			byteBuffer.putLong(32+4+8, chaAmount.longValue());
		}
		List<Byte> dataArrayList = Util.toByteArrayList(byteBuffer.array());
		dataArrayList.addAll(0, Util.toByteArrayList(Config.prefix.getBytes()));
		byte[] data = Util.toByteArray(dataArrayList);

		String dataString = "";
		try {
			dataString = new String(data,"ISO-8859-1");
		} catch (UnsupportedEncodingException e) {
		}
		logger.info("Rolling the dice for "+rollTxHash+": "+roll);
		Transaction tx = blocks.transaction(source, destinations, btcAmounts, BigInteger.valueOf(Config.minFee), dataString, useUnspentTxHash, useUnspentVout);
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
					TransactionInfoInsight txInfo = Util.getTransactionInsight(unspent.txid);
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
								List<String> destinations = new ArrayList<String>();
								List<BigInteger> btcAmounts = new ArrayList<BigInteger>();
								destinations.add(Config.donationAddress);
								BigInteger feeAmount = BigInteger.valueOf(Config.feeAddressFee).subtract(BigInteger.valueOf(Config.minFee)).subtract(BigInteger.valueOf(Config.dustSize*2));
								btcAmounts.add(feeAmount);
								
								BigInteger convertToCha = amount.subtract(BigInteger.valueOf(Config.feeAddressFee));
								BigInteger chaAmount = BigInteger.ZERO;
								if (convertToCha.compareTo(BigInteger.ZERO)>0) {
									Double price = Util.buyBestOfferOnExchanges(convertToCha.doubleValue()/Config.unit.doubleValue());
									chaAmount = new BigDecimal(convertToCha.doubleValue() * price).toBigInteger();
								}
								Transaction tx = Roll.create(Config.feeAddress, destinations, btcAmounts, rollTxHash, chaAmount, unspent.txid, unspent.vout);
								blocks.sendTransaction(Config.feeAddress, tx);
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
	public BigInteger chaAmount;
}
