import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import com.google.bitcoin.core.Transaction;


public class JsonRpcServiceImpl implements JsonRpcService {

	public String getName() {
		return "BLAH";
	}
	
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
		if (!source.equals("") && !destination.equals("")) {
			BigInteger sourceBalance = Util.getBalance(source, asset);
			Integer assetId = Util.getAssetId(asset);
			if (sourceBalance.compareTo(amount)>=0) {
				Blocks blocks = Blocks.getInstance();
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
				Transaction tx = blocks.transaction(source, destination, BigInteger.valueOf(Config.dustSize), BigInteger.valueOf(Config.minFee), dataString);
				return tx;
			}
		}
		return null;
	}
}