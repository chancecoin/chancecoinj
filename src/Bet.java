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

public class Bet {
    static Logger logger = LoggerFactory.getLogger(Bet.class);
	public static Integer length = 8+8+8;
	public static Integer id = 40;
	
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
					BigInteger bet = BigInteger.valueOf(byteBuffer.getLong(0));
					Double chance = byteBuffer.getDouble(8);
					Double payout = byteBuffer.getDouble(16);
					if (!source.equals("") && bet.compareTo(BigInteger.ZERO)>0 && chance>0.0 && chance<100.0 && payout>1.0 && chance==100.0/(payout/(1.0-Config.houseEdge))) {
						if (bet.compareTo(Util.getBalance(source, "CHA"))<=0) {
							BigInteger chaSupply = Util.chaSupply();
							if ((payout-1.0)*bet.doubleValue()<chaSupply.doubleValue()*Config.maxProfit) {
								db.executeUpdate("insert into bets(tx_index, tx_hash, block_index, source, bet, chance, payout, profit, cha_supply, validity) values('"+txIndex.toString()+"','"+txHash+"','"+blockIndex.toString()+"','"+source+"','"+bet.toString()+"','"+chance.toString()+"','"+payout.toString()+"','0','"+chaSupply.toString()+"','valid')");
								Util.debit(source, "CHA", bet);								
							}
						}
					}
				}				
			}
		} catch (SQLException e) {	
		}
	}
	public static Transaction createBet(String source, BigInteger bet, Double chance, Double payout) {
		if (!source.equals("") && bet.compareTo(BigInteger.ZERO)>0 && chance>0.0 && chance<100.0 && payout>1.0 && chance==100.0/(payout/(1.0-Config.houseEdge))) {
			if (bet.compareTo(Util.getBalance(source, "CHA"))<=0) {
				BigInteger chaSupply = Util.chaSupply();
				if ((payout-1.0)*bet.doubleValue()<chaSupply.doubleValue()*Config.maxProfit) {
					Blocks blocks = Blocks.getInstance();
					ByteBuffer byteBuffer = ByteBuffer.allocate(length);
					byteBuffer.putLong(0, bet.longValue());
					byteBuffer.putDouble(8, chance);
					byteBuffer.putDouble(16, payout);
					byte[] data = byteBuffer.array();

					String dataString = "";
					try {
						dataString = new String(data,"ISO-8859-1");
					} catch (UnsupportedEncodingException e) {
					}
					Transaction tx = blocks.transaction(source, source, BigInteger.valueOf(Config.dustSize), BigInteger.valueOf(Config.minFee), dataString);
					return tx;
				}
			}
		}
		return null;
	}
	public static BigInteger factorial(BigInteger n) {
	    BigInteger result = BigInteger.ONE;

	    while (!n.equals(BigInteger.ZERO)) {
	        result = result.multiply(n);
	        n = n.subtract(BigInteger.ONE);
	    }

	    return result;
	}
	public static BigInteger combinations(BigInteger n, BigInteger k) {
		if (k.compareTo(n)>0) {
			return BigInteger.ZERO;
		}else{
			return factorial(n).divide(factorial(k)).divide(factorial(n.subtract(k)));
		}
	}
    public static Date addDays(Date date, int days)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DATE, days); //minus number would decrement the days
        return cal.getTime();
    }
	public static void resolve() {
		//resolve bets
		Database db = Database.getInstance();
		ResultSet rs = db.executeQuery("select block_time,tx_index,tx_hash,source,bet,payout,chance,cha_supply from bets,blocks where bets.block_index=blocks.block_index and profit=0;");

		SimpleDateFormat dateFormatStandard = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
		SimpleDateFormat dateFormatDateTimeLotto = new SimpleDateFormat("MM/dd/yyyy HH:mm");
		SimpleDateFormat dateFormatDateLotto = new SimpleDateFormat("dd/MM/yyyy");
		SimpleDateFormat dateFormatHour = new SimpleDateFormat("HH");
		SimpleDateFormat dateFormatMinute = new SimpleDateFormat("mm");
		TimeZone newYork = TimeZone.getTimeZone("America/New_York");
		TimeZone UTC = TimeZone.getTimeZone("UTC");
		dateFormatStandard.setTimeZone(UTC);
		dateFormatDateLotto.setTimeZone(newYork);
		dateFormatDateTimeLotto.setTimeZone(newYork);
		dateFormatHour.setTimeZone(newYork);
		dateFormatMinute.setTimeZone(newYork);
	    
	    try {
			while (rs.next()) {
				String source = rs.getString("source");
				String txHash = rs.getString("tx_hash");
				Integer txIndex = rs.getInt("tx_index");
				BigInteger bet = BigInteger.valueOf(rs.getLong("bet"));
				Double payout = rs.getDouble("payout");
				Double chance = rs.getDouble("chance");
				BigInteger chaSupply = BigInteger.valueOf(rs.getLong("cha_supply"));
				Date blockTime = new Date((long)rs.getLong("block_time")*1000);
				
				String lottoDate = dateFormatDateLotto.format(blockTime);
				Integer hour = Integer.parseInt(dateFormatHour.format(blockTime));
				Integer minute = Integer.parseInt(dateFormatMinute.format(blockTime));
				if (hour>=23 && minute>=56) {
					lottoDate = dateFormatDateLotto.format(addDays(blockTime, 1));
				}
				String lottoUrl = "http://nylottery.ny.gov/wps/PA_NYSLNumberCruncher/NumbersServlet?game=quick&action=winningnumbers&startSearchDate="+lottoDate+"&endSearchDate=&pageNo=&last=&perPage=999&sort=";
				String lottoPage = Util.getPage(lottoUrl);
				try {
					ObjectMapper objectMapper = new ObjectMapper();
					objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
					LottoResult lottoMap = objectMapper.readValue(lottoPage, LottoResult.class);
					
					for (LottoDraw draw : lottoMap.draw) {
						Date time = dateFormatDateTimeLotto.parse(draw.date);
						if (time.after(blockTime)) {
							BigInteger denominator = combinations(BigInteger.valueOf(80),BigInteger.valueOf(20)).subtract(BigInteger.ONE);
							BigInteger n = BigInteger.ZERO;
							BigInteger i = BigInteger.ONE;
							for (BigInteger number : draw.numbersDrawn) {
								n = n.add(combinations(number.subtract(BigInteger.ONE),i));
								i = i.add(BigInteger.ONE);
							}
							Double roll1 = n.doubleValue() / (denominator.doubleValue());
							Double roll2 = (new BigInteger(txHash.substring(10, txHash.length()),16)).mod(BigInteger.valueOf(1000000000)).doubleValue()/1000000000.0;
							Double roll = (roll1 + roll2) % 1.0;
							roll = roll * 100.0;
							
							BigInteger profit = BigInteger.ZERO;
							if (roll<chance) {
								//win
								profit = new BigDecimal(bet.doubleValue()*(payout.doubleValue()-1.0)*chaSupply.doubleValue()/(chaSupply.doubleValue()-bet.doubleValue()*payout.doubleValue())).toBigInteger();
								BigInteger credit = profit.add(bet);
								Util.credit(source, "CHA", credit);
							} else {
								//lose
								profit = bet.multiply(BigInteger.valueOf(-1));
							}
							db.executeUpdate("update bets set profit='"+profit.toString()+"' where tx_index='"+txIndex+"';");
							break;
						}
					}
				} catch (Exception e) {
					logger.error(e.toString());
					System.exit(0);
				}
			}
		} catch (SQLException e) {
			logger.error(e.toString());
			System.exit(0);
		}
	}
}

class LottoResult {
	public List<LottoDraw> draw;
	public String jsonSearch;
}
class LottoDraw {
	public String date;
	public List<BigInteger> numbersDrawn;
}