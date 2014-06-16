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
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.bitcoin.core.Transaction;

public class PokerBet {
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

				ResultSet rsCheck = db.executeQuery("select * from bets where tx_index='"+txIndex.toString()+"'");
				if (rsCheck.next()) return;

				if (message.size() == length) {
					ByteBuffer byteBuffer = ByteBuffer.allocate(length);
					for (byte b : message) {
						byteBuffer.put(b);
					}			
					BigInteger bet = BigInteger.valueOf(byteBuffer.getLong(0));
					Double chance = byteBuffer.getDouble(8);
					Double payout = byteBuffer.getDouble(16);
					Double houseEdge = Config.houseEdge;
					//PROTOCOL CHANGE
					Double oldHouseEdge = 0.02;
					Boolean payoutChanceCongruent = Util.roundOff(chance,6)==Util.roundOff(100.0/(payout/(1.0-houseEdge)),6) || Util.roundOff(chance,6)==Util.roundOff(100.0/(payout/(1.0-oldHouseEdge)),6);
					BigInteger chaSupply = Util.chaSupplyForBetting();
					String validity = "invalid";
					if (!source.equals("") && bet.compareTo(BigInteger.ZERO)>0 && chance>0.0 && chance<100.0 && payout>1.0 && payoutChanceCongruent) {
						if (bet.compareTo(Util.getBalance(source, "CHA"))<=0) {
							if ((payout-1.0)*bet.doubleValue()<chaSupply.doubleValue()*Config.maxProfit) {
								validity = "valid";
								Util.debit(source, "CHA", bet, "Debit bet amount", txHash, blockIndex);								
							}
						}
					}
					db.executeUpdate("insert into bets(tx_index, tx_hash, block_index, source, bet, chance, payout, profit, cha_supply, validity) values('"+txIndex.toString()+"','"+txHash+"','"+blockIndex.toString()+"','"+source+"','"+bet.toString()+"','"+chance.toString()+"','"+payout.toString()+"','0','"+chaSupply.toString()+"','"+validity+"')");					
				}				
			}
		} catch (SQLException e) {	
		}
	}

	public static List<BetInfo> getPending(String source) {
		Database db = Database.getInstance();
		ResultSet rs = db.executeQuery("select * from transactions where block_index<0 and source='"+source+"' order by tx_index desc;");
		List<BetInfo> bets = new ArrayList<BetInfo>();
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

				ResultSet rsCheck = db.executeQuery("select * from bets where tx_index='"+txIndex.toString()+"'");
				if (!rsCheck.next()) {
					List<Byte> messageType = blocks.getMessageTypeFromTransaction(dataString);
					List<Byte> message = blocks.getMessageFromTransaction(dataString);
					if (messageType.get(3)==Bet.id.byteValue() && message.size() == length) {
						ByteBuffer byteBuffer = ByteBuffer.allocate(length);
						for (byte b : message) {
							byteBuffer.put(b);
						}			
						BigInteger bet = BigInteger.valueOf(byteBuffer.getLong(0));
						Double chance = byteBuffer.getDouble(8);
						Double payout = byteBuffer.getDouble(16);
						Double houseEdge = Config.houseEdge;
						//PROTOCOL CHANGE
						Double oldHouseEdge = 0.02;
						Boolean payoutChanceCongruent = Util.roundOff(chance,6)==Util.roundOff(100.0/(payout/(1.0-houseEdge)),6) || Util.roundOff(chance,6)==Util.roundOff(100.0/(payout/(1.0-oldHouseEdge)),6);
						BigInteger chaSupply = Util.chaSupplyForBetting();
						String validity = "invalid";
						if (!source.equals("") && bet.compareTo(BigInteger.ZERO)>0 && chance>0.0 && chance<100.0 && payout>1.0 && payoutChanceCongruent) {
							if (bet.compareTo(Util.getBalance(source, "CHA"))<=0) {
								if ((payout-1.0)*bet.doubleValue()<chaSupply.doubleValue()*Config.maxProfit) {
									BetInfo betInfo = new BetInfo();
									betInfo.bet = bet;
									betInfo.chance = chance;
									betInfo.payout = payout;
									betInfo.source = source;
									betInfo.txHash = txHash;
									bets.add(betInfo);
								}
							}
						}
					}	
				}
			}
		} catch (SQLException e) {	
		}	
		return bets;
	}

	public static Transaction createBet(String source, BigInteger bet, List<String> playerCards, List<String> boardCards, List<String> opponentCards) throws Exception {
		//cards should be like 2C or 10S, or ?? if they are random
		BigInteger chaSupply = Util.chaSupplyForBetting();
		if (playerCards.size()!=2) throw new Exception("Please specify exactly two player cards."); 
		if (opponentCards.size()!=2) throw new Exception("Please specify exactly two opponent cards."); 
		if (boardCards.size()!=5) throw new Exception("Please specify exactly five board cards.");
		Deck deal = new Deck();
		deal.cards.clear();
		for (String cardString : playerCards) {
			Card card = new Card(cardString);
			if (card.suit>=0 && card.card>=0) {
				if (deal.cards.contains(card)) throw new Exception("Please make sure each card is unique.");
				deal.cards.add(card);				
			} else {
				throw new Exception("Please specify a complete player hand.");
			}
		}
		for (String cardString : opponentCards) {
			Card card = new Card(cardString);
			if (card.suit>=0 && card.card>=0) {
				if (deal.cards.contains(card)) throw new Exception("Please make sure each card is unique.");
				deal.cards.add(card);				
			} else {
				throw new Exception("Please specify a complete opponent hand.");
			}
		}
		for (String cardString : boardCards) {
			Card card = new Card(cardString);
			if (card.suit>=0 && card.card>=0) {
				if (deal.cards.contains(card)) throw new Exception("Please make sure each card is unique.");
				deal.cards.add(card);				
			} else {
				deal.cards.add(null);
			}
		}
		Double chance = Deck.chanceOfWinning(deal.cards);
		Double payout = 100.0/chance*(1-Config.houseEdge);
		
		Double field1 = 0.0;
		for (Card card : deal.cards) {
			field1 += card.cardValue();
			field1 /= 100.0;
		}
		field1 += 1.0;
		Double field2 = 0.0;
		
		if (source.equals("")) throw new Exception("Please specify a source address.");
		if (!(bet.compareTo(BigInteger.ZERO)>0)) throw new Exception("Please bet more than zero.");
		if (!(chance>0.0 && chance<100.0)) throw new Exception("Please specify a chance between 0 and 100.");
		if (!(payout>1.0)) throw new Exception("Please specify a payout greater than 1.");
		if (!(Util.roundOff(chance,6)==Util.roundOff(100.0/(payout/(1.0-Config.houseEdge)),6))) throw new Exception("Please specify a chance and payout that are congruent."); 
		if (!(bet.compareTo(Util.getBalance(source, "CHA"))<=0)) throw new Exception("Please specify a bet that is smaller than your CHA balance.");
		if (!((payout-1.0)*bet.doubleValue()<chaSupply.doubleValue()*Config.maxProfit)) throw new Exception("Please specify a bet with a payout less than the maximum percentage of the house bankroll you can win.");

		Blocks blocks = Blocks.getInstance();
		ByteBuffer byteBuffer = ByteBuffer.allocate(length+4);
		byteBuffer.putInt(0, id);
		byteBuffer.putLong(0+4, bet.longValue());
		byteBuffer.putDouble(8+4, field1);
		byteBuffer.putDouble(16+4, field2);
		List<Byte> dataArrayList = Util.toByteArrayList(byteBuffer.array());
		dataArrayList.addAll(0, Util.toByteArrayList(Config.prefix.getBytes()));
		byte[] data = Util.toByteArray(dataArrayList);

		String dataString = "";
		try {
			dataString = new String(data,"ISO-8859-1");
		} catch (UnsupportedEncodingException e) {
		}

		Transaction tx = blocks.transaction(source, "", BigInteger.ZERO, BigInteger.valueOf(Config.minFee), dataString);
		return tx;
	}
	
	public static void resolve() {
		//resolve bets

		logger.info("Resolving bets");

		Database db = Database.getInstance();
		ResultSet rs = db.executeQuery("select block_time,blocks.block_index as block_index,tx_index,tx_hash,source,bet,payout,chance,cha_supply from bets,blocks where bets.block_index=blocks.block_index and bets.validity='valid' and bets.resolved IS NOT 'true';");
		//if (true) return;

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
				Integer blockIndex = rs.getInt("block_index");
				BigInteger bet = BigInteger.valueOf(rs.getLong("bet"));
				Double payout = rs.getDouble("payout");
				Double chance = rs.getDouble("chance");
				BigInteger chaSupply = BigInteger.valueOf(rs.getLong("cha_supply"));
				Date blockTime = new Date((long)rs.getLong("block_time")*1000);

				logger.info("Attempting to resolve bet "+txHash);

				String lottoDate = dateFormatDateLotto.format(blockTime);
				Integer hour = Integer.parseInt(dateFormatHour.format(blockTime));
				Integer minute = Integer.parseInt(dateFormatMinute.format(blockTime));
				if (hour>=23 && minute>=56) {
					lottoDate = dateFormatDateLotto.format(Util.addDays(blockTime, 1));
				}
				String lottoUrl = "http://nylottery.ny.gov/wps/PA_NYSLNumberCruncher/NumbersServlet?game=quick&action=winningnumbers&startSearchDate="+lottoDate+"&endSearchDate=&pageNo=&last=&perPage=999&sort=";
				String lottoPage = Util.getPage(lottoUrl);
				logger.info("Getting lottery numbers "+lottoUrl);
				try {
					ObjectMapper objectMapper = new ObjectMapper();
					objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
					LottoResult lottoMap = objectMapper.readValue(lottoPage, LottoResult.class);

					for (LottoDraw draw : lottoMap.draw) {
						Date time = dateFormatDateTimeLotto.parse(draw.date);
						if (time.after(blockTime)) {
							logger.info("Found lottery numbers we can use to resolve bet");
							BigInteger denominator = Util.combinations(BigInteger.valueOf(80),BigInteger.valueOf(20)).subtract(BigInteger.ONE);
							BigInteger n = BigInteger.ZERO;
							BigInteger i = BigInteger.ONE;
							for (BigInteger number : draw.numbersDrawn) {
								n = n.add(Util.combinations(number.subtract(BigInteger.ONE),i));
								i = i.add(BigInteger.ONE);
							}
							Double rollA = n.doubleValue() / (denominator.doubleValue());
							Double rollB = (new BigInteger(txHash.substring(10, txHash.length()),16)).mod(BigInteger.valueOf(1000000000)).doubleValue()/1000000000.0;
							Double roll = (rollA + rollB) % 1.0;
							roll = roll * 100.0;

							logger.info("Roll = "+roll.toString()+", chance = "+chance.toString());

							BigInteger profit = BigInteger.ZERO;
							if (roll<chance) {
								logger.info("The bet is a winner");
								//win
								profit = new BigDecimal(bet.doubleValue()*(payout.doubleValue()-1.0)*chaSupply.doubleValue()/(chaSupply.doubleValue()-bet.doubleValue()*payout.doubleValue())).toBigInteger();
								BigInteger credit = profit.add(bet);
								Util.credit(source, "CHA", credit, "Bet won", txHash, blockIndex);
							} else {
								logger.info("The bet is a loser");
								//lose
								profit = bet.multiply(BigInteger.valueOf(-1));
							}
							db.executeUpdate("update bets set profit='"+profit.toString()+"', rolla='"+(rollA*100.0)+"', rollb='"+(rollB*100.0)+"', roll='"+roll+"', resolved='true' where tx_index='"+txIndex+"';");
							break;
						}
					}
				} catch (Exception e) {
					logger.error(e.toString());
				}
			}
		} catch (SQLException e) {
			logger.error(e.toString());
		}
	}
}