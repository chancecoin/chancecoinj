import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Burn {
    static Logger logger = LoggerFactory.getLogger(Burn.class);

	public static void parse(Integer txIndex) {
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
				
				ResultSet rsCheck = db.executeQuery("select * from burns where tx_index='"+txIndex.toString()+"'");
				if (rsCheck.next()) return;

				ResultSet rsBurns = db.executeQuery("select sum(earned) as earned from burns where validity='valid';");
				BigInteger totalBurns = BigInteger.ZERO;
				BigInteger maxBurns = BigInteger.ZERO;
				if (rsBurns.next()) {
					totalBurns = BigInteger.valueOf(rsBurns.getInt("earned"));
				}
				maxBurns = BigInteger.valueOf(Config.maxBurn).multiply(BigInteger.valueOf(Config.unit));
				Integer totalTime = Config.endBlock - Config.startBlock;
				Integer partialTime = Config.endBlock - blockIndex;
				Double multiplier = Config.multiplier.doubleValue() + (partialTime.doubleValue()/totalTime.doubleValue())*(Config.multiplierInitial.doubleValue() - Config.multiplier.doubleValue());
				Double earnedUnrounded = btcAmount.doubleValue() * multiplier;
				BigInteger earned = new BigDecimal(earnedUnrounded).toBigInteger();
				if (!source.equals("") && blockIndex>=Config.startBlock && blockIndex<=Config.endBlock && totalBurns.add(earned).compareTo(maxBurns)<0) {
					db.executeUpdate("insert into burns(tx_index, tx_hash, block_index, source, burned, earned, validity) values('"+txIndex.toString()+"','"+txHash+"','"+blockIndex.toString()+"','"+source+"','"+btcAmount.toString()+"','"+earned+"','valid')");
					Util.credit(source, "CHA", earned, "BTC burned", txHash, blockIndex);
				}
			}
		} catch (SQLException e) {
		}
	}
}
