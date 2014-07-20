import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.TimeZone;

import javax.xml.bind.DatatypeConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.script.Script;
import com.google.bitcoin.script.ScriptBuilder;
import com.google.bitcoin.script.ScriptOpCodes;

public class Bet {
	static Logger logger = LoggerFactory.getLogger(Bet.class);
	public static Integer lengthDice = 8+8+8;
	public static Integer idDice = 40;
	public static Integer lengthPoker = 4+8+9*2;
	public static Integer idPoker = 41;	

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
				String dataString = rs.getString("data");
				List<Byte> messageType = Blocks.getMessageTypeFromTransaction(dataString);

				ResultSet rsCheck = db.executeQuery("select * from bets where tx_index='"+txIndex.toString()+"'");
				if (rsCheck.next()) return;

				BigInteger bet = BigInteger.ZERO;
				Double chance = 0.0;
				Double payout = 0.0;
				ByteBuffer byteBuffer = null;
				if (messageType.get(3) == idDice.byteValue() && message.size() == lengthDice) {
					byteBuffer = ByteBuffer.allocate(lengthDice);
					for (byte b : message) {
						byteBuffer.put(b);
					}			
					bet = BigInteger.valueOf(byteBuffer.getLong(0));
				} else if (messageType.get(3) == idPoker.byteValue() && message.size() == lengthPoker) {
					byteBuffer = ByteBuffer.allocate(lengthPoker);
					for (byte b : message) {
						byteBuffer.put(b);
					}			
					bet = BigInteger.valueOf(byteBuffer.getLong(0));
				}

				String asset = "CHA";
				String getBTCBack = "";
				//				if (!destination.equals("") && !destination.equals(Config.feeAddress)) {
				//					asset = "BTC";
				//					ResultSet rsAvailableCHAInBTC = db.executeQuery("select sum(btc_amount/cha_amount*(1+width/2)*cha_remaining) as available_cha_in_btc from quotes where validity='valid' and destination='"+destination+"' and cha_remaining>0 order by btc_amount/cha_amount*(1+width/2) asc, tx_index asc");
				//					if (rsAvailableCHAInBTC.next() && BigInteger.valueOf(rsAvailableCHAInBTC.getLong("available_cha_in_btc")).compareTo(btcAmount)>=0) {
				//						ResultSet rsQuotes = db.executeQuery("select * from quotes where validity='valid' and destination='"+destination+"' and cha_remaining>0 order by btc_amount/cha_amount*(1+width/2) asc, tx_index asc");
				//						BigInteger btcFilled = BigInteger.ZERO;
				//						BigInteger chaFilled = BigInteger.ZERO;
				//						try {
				//							while (rsQuotes.next()) {
				//								String quoteTxIndex = rsQuotes.getString("tx_index");
				//								String quoteTxHash = rsQuotes.getString("tx_hash");
				//								String quoteSource = rsQuotes.getString("source");
				//								String quoteDestination = rsQuotes.getString("destination");
				//								BigInteger chaRemaining = BigInteger.valueOf(rs.getLong("cha_remaining"));
				//								BigInteger btcQuote = BigInteger.valueOf(rs.getLong("btc_amount"));
				//								BigInteger chaQuote = BigInteger.valueOf(rs.getLong("cha_amount"));
				//								Double width = rs.getDouble("width");
				//								Double price = btcQuote.doubleValue() / chaQuote.doubleValue() * (1+width/2);
				//								BigInteger availableCHAInBTC = new BigDecimal(price * chaRemaining.doubleValue()).toBigInteger();
				//								if (availableCHAInBTC.compareTo(btcAmount.subtract(btcFilled))<0) {
				//									//we take down all of the quote
				//									BigInteger btcToFill = availableCHAInBTC;
				//									BigInteger chaToFill = chaRemaining;
				//									btcFilled = btcFilled.add(btcToFill);
				//									chaFilled = chaFilled.add(chaToFill);
				//									chaRemaining = BigInteger.ZERO;
				//									db.executeUpdate("update quotes set cha_available='"+chaRemaining+"' where tx_index = '"+quoteTxIndex+"'");
				//									Util.credit(source, "CHA", chaToFill, "Quote fill", txHash, blockIndex);
				//									Util.debit(quoteDestination, "CHA", chaToFill, "Quote fill", txHash, blockIndex);
				//									String quotePayId = txHash + quoteTxHash;
				//									db.executeUpdate("insert into quotepays (id,tx_hash_buy,tx_hash_sell,source,destination,btc_amount,validity) values('"+quotePayId+"','"+txHash+"','"+quoteTxHash+"','"+destination+"','"+quoteSource+"','"+btcToFill+"','pending')");
				//								} else {
				//									//we take down a fraction of the quote, and we are done
				//									BigInteger btcToFill = btcAmount.subtract(btcFilled);
				//									BigInteger chaToFill = new BigDecimal(btcFilled.doubleValue() / price).toBigInteger();
				//									btcFilled = btcFilled.add(btcToFill);
				//									chaFilled = chaFilled.add(chaToFill);
				//									chaRemaining = chaRemaining.subtract(chaToFill);
				//									db.executeUpdate("update quotes set cha_available='"+chaRemaining+"' where tx_index = '"+quoteTxIndex+"'");
				//									Util.credit(source, "CHA", chaToFill, "Quote fill", txHash, blockIndex);
				//									Util.debit(quoteDestination, "CHA", chaToFill, "Quote fill", txHash, blockIndex);
				//									String quotePayId = txHash + quoteTxHash;
				//									db.executeUpdate("insert into quotepays (id,tx_hash_buy,tx_hash_sell,source,destination,btc_amount,validity) values('"+quotePayId+"','"+txHash+"','"+quoteTxHash+"','"+destination+"','"+quoteSource+"','"+btcToFill+"','pending')");
				//									break;
				//								}
				//							}
				//						} catch (SQLException e) {	
				//						}
				//						if (bet.compareTo(BigInteger.ZERO)<=0) {
				//							getBTCBack = "true";
				//						}
				//						//convert the BTC bet to CHA
				//						bet = chaFilled;
				//					} else {
				//						//not enough available CHA on quotes, must return BTC
				//						db.executeUpdate("insert into quotepays (id,tx_hash_buy,tx_hash_sell,source,destination,btc_amount,validity) values('"+txHash+txHash+"','"+txHash+"','"+txHash+"','"+destination+"','"+source+"','"+btcAmount+"','pending')");
				//						return;
				//					}
				//				}

				if (messageType.get(3) == idDice.byteValue() && message.size() == lengthDice) {
					chance = byteBuffer.getDouble(8);
					payout = byteBuffer.getDouble(16);

					//PROTOCOL CHANGE
					Double oldHouseEdge = 0.02;
					Boolean payoutChanceCongruent = Util.roundOff(chance,6)==Util.roundOff(100.0/(payout/(1.0-Config.houseEdge)),6) || Util.roundOff(chance,6)==Util.roundOff(100.0/(payout/(1.0-oldHouseEdge)),6);
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
					db.executeUpdate("insert into bets(tx_index, tx_hash, block_index, source, bet, chance, payout, profit, cha_supply, validity, get_btc_back) values('"+txIndex.toString()+"','"+txHash+"','"+blockIndex.toString()+"','"+source+"','"+bet.toString()+"','"+chance.toString()+"','"+payout.toString()+"','0','"+chaSupply.toString()+"','"+validity+"','"+getBTCBack+"')");					
				} else if (messageType.get(3) == idPoker.byteValue() && message.size() == lengthPoker) {
					Deck deal = new Deck();
					deal.cards.clear();
					for (int i = 0; i < 9; i++) {
						Card card = new Card(byteBuffer.getShort(8+2*i));
						if (card!=null && !card.toString().equals("??") && deal.cards.contains(card)) {
							return; //duplicate cards; this hand is invalid
						}
						deal.cards.add(card);
					}

					chance = Deck.chanceOfWinning(deal.cards)*100.0;
					payout = 100.0/chance*(1-Config.houseEdge);
					Boolean payoutChanceCongruent = Util.roundOff(chance,6)==Util.roundOff(100.0/(payout/(1.0-Config.houseEdge)),6);
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
					db.executeUpdate("insert into bets(tx_index, tx_hash, block_index, source, destination, bet, chance, payout, profit, cha_supply, validity, cards, get_btc_back) values('"+txIndex.toString()+"','"+txHash+"','"+blockIndex.toString()+"','"+source+"','"+destination+"','"+bet.toString()+"','"+chance.toString()+"','"+payout.toString()+"','0','"+chaSupply.toString()+"','"+validity+"','"+deal.toString()+"','"+getBTCBack+"')");
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

					Double roll = null;
					//check for pending roll
					for (UnspentOutput unspent : Util.getUnspents(Config.donationAddress)) {
						if (unspent.confirmations.equals(0)) {
							TransactionInfoInsight txInfo = Util.getTransactionInsight(unspent.txid);
							List<Byte> dataArrayList = null;
							if (txInfo != null && txInfo.vout != null && txInfo.vout.size()>0) {
								for (Vout vout : txInfo.vout) {
									ScriptBuilder sb = new ScriptBuilder();
									String asm = vout.scriptPubKey.asm;
									for (String opcode : asm.split(" ")) {
										if (opcode.equals("1")) {
											sb.op(ScriptOpCodes.OP_1);							
										} else if (opcode.equals("2")) {
											sb.op(ScriptOpCodes.OP_2);							
										} else if (opcode.equals("OP_CHECKMULTISIG")) {
											sb.op(ScriptOpCodes.OP_CHECKMULTISIG);							
										} else {
											try {
												byte[] bytes = DatatypeConverter.parseHexBinary(opcode);
												sb.data(bytes);							
											} catch (Exception e) {								
											}
										}
									}
									Script script = sb.build();
									dataArrayList = blocks.scriptToDataArrayList(script, dataArrayList);
								}
							}
							if (dataArrayList!=null && dataArrayList.size()>0) {
								byte[] data = blocks.dataArrayListToDataArray(dataArrayList);
								String dataStringRoll = blocks.dataArrayToString(data);
								List<Byte> messageTypeRoll = blocks.getMessageTypeFromTransaction(dataStringRoll);
								List<Byte> messageRoll = blocks.getMessageFromTransaction(dataStringRoll);

								if (messageTypeRoll.get(3)==Roll.id.byteValue() && messageRoll.size() == Roll.length) {
									ByteBuffer byteBuffer = ByteBuffer.allocate(Roll.length);
									for (byte b : messageRoll) {
										byteBuffer.put(b);
									}	
									String rollTxHash = new BigInteger(1, Util.toByteArray(messageRoll.subList(0, 32))).toString(16);
									while (rollTxHash.length()<64) rollTxHash = "0"+rollTxHash;
									System.out.println(rollTxHash);
									System.out.println(txHash);
									if (rollTxHash.equals(txHash)) {
										roll = byteBuffer.getDouble(32) * 100.0;									
									}
								}
							}
						}
					}

					if (messageType.get(3)==Bet.idDice.byteValue() && message.size() == lengthDice) {
						ByteBuffer byteBuffer = ByteBuffer.allocate(lengthDice);
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
									betInfo.roll = null;
									betInfo.resolved = false;
									betInfo.profit = null;
									betInfo.betType = "dice";

									if (roll != null) {
										logger.info("Roll = "+roll.toString()+", chance = "+chance.toString());

										BigInteger profit = BigInteger.ZERO;
										if (roll<chance) {
											logger.info("The bet is a winner");
											//win
											profit = new BigDecimal(bet.doubleValue()*(payout.doubleValue()-1.0)*chaSupply.doubleValue()/(chaSupply.doubleValue()-bet.doubleValue()*payout.doubleValue())).toBigInteger();
										} else {
											logger.info("The bet is a loser");
											//lose
											profit = bet.multiply(BigInteger.valueOf(-1));
										}
										betInfo.profit = profit.doubleValue()/Config.unit.doubleValue();
										betInfo.roll = roll;
										betInfo.resolved = true;
									}
									bets.add(betInfo);
								}
							}
						}
					} else if (messageType.get(3)==Bet.idPoker.byteValue() && message.size() == lengthPoker) {
						ByteBuffer byteBuffer = ByteBuffer.allocate(lengthPoker);
						for (byte b : message) {
							byteBuffer.put(b);
						}			
						BigInteger bet = BigInteger.valueOf(byteBuffer.getLong(0));
						Deck deal = new Deck();
						deal.cards.clear();

						for (int i = 0; i < 9; i++) {
							deal.cards.add(new Card(byteBuffer.getShort(8+2*i)));
						}
						String cards = deal.toString();
						Double chance = Deck.chanceOfWinning(deal.cards) * 100.0;
						Double payout = 100.0/chance*(1-Config.houseEdge);
						Boolean payoutChanceCongruent = Util.roundOff(chance,6)==Util.roundOff(100.0/(payout/(1.0-Config.houseEdge)),6);
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
									betInfo.betType = "poker";
									betInfo.cards = cards;	
									betInfo.roll = null;
									betInfo.resolved = false;
									betInfo.profit = null;
									if (roll != null) {
										Deck dealtSoFar = new Deck();
										dealtSoFar.cards.clear();
										Deck removedCards = new Deck();
										removedCards.cards.clear();
										for (String c : cards.split(" ")) {
											Card card = new Card(c);
											if (!card.toString().equals("??")) {
												removedCards.cards.add(card);
											}
											dealtSoFar.cards.add(card);
										}
										Deck dealRandom = Deck.ShuffleAndDeal(roll / 100.0, removedCards.cards, dealtSoFar.cards.size()-removedCards.cards.size());
										for (int j = 0; j<dealtSoFar.cards.size(); j++) {
											if (dealtSoFar.cards.get(j).cardString().equals("??")) {
												dealtSoFar.cards.set(j, dealRandom.cards.remove(0));
											}
										}
										logger.info("Dealt cards = "+dealtSoFar.toString());

										BigInteger profit = BigInteger.ZERO;
										if (Deck.didWin(dealtSoFar.cards)) {
											logger.info("The bet is a winner");
											//win
											profit = new BigDecimal(bet.doubleValue()*(payout.doubleValue()-1.0)*chaSupply.doubleValue()/(chaSupply.doubleValue()-bet.doubleValue()*payout.doubleValue())).toBigInteger();
										} else {
											logger.info("The bet is a loser");
											//lose
											profit = bet.multiply(BigInteger.valueOf(-1));
										}
										betInfo.profit = profit.doubleValue()/Config.unit.doubleValue();
										betInfo.roll = roll;
										betInfo.cards = dealtSoFar.toString();
										betInfo.resolved = true;
									}
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

	public static Transaction createDiceBet(String source, String destination, String asset, String resolution, BigInteger bet, Double chance, Double payout) throws Exception {
		BigInteger chaSupply = Util.chaSupplyForBetting();
		if (source.equals("")) throw new Exception("Please specify a source address.");
		if (!(bet.compareTo(BigInteger.ZERO)>0)) throw new Exception("Please bet more than zero.");
		if (!(chance>0.0 && chance<100.0)) throw new Exception("Please specify a chance between 0 and 100.");
		if (!(payout>1.0)) throw new Exception("Please specify a payout greater than 1.");
		if (!(Util.roundOff(chance,6)==Util.roundOff(100.0/(payout/(1.0-Config.houseEdge)),6))) throw new Exception("Please specify a chance and payout that are congruent."); 
		if (!(bet.compareTo(Util.getBalance(source, "CHA"))<=0)) throw new Exception("Please specify a bet that is smaller than your CHA balance.");
		if (!((payout-1.0)*bet.doubleValue()<chaSupply.doubleValue()*Config.maxProfit)) throw new Exception("Please specify a bet with a payout less than the maximum percentage of the house bankroll you can win.");

		BigInteger btcAmount = BigInteger.ZERO;
		//		if (asset.equals("BTC") && !destination.equals("")) { // bet with BTC, expect return in CHA
		//			btcAmount = bet;
		//		} else if (asset.equals("BTC2") && !destination.equals("")) { // bet with BTC, expect return in BTC
		//			btcAmount = bet;
		//			bet = bet.negate();
		//		} else { // bet with CHA
		//			destination = "";
		//		}
		destination = "";

		if (destination.equals("") && resolution.equals("instant")) {
			destination = Config.feeAddress;
			btcAmount = BigInteger.valueOf(Config.feeAddressFee);
		}

		Blocks blocks = Blocks.getInstance();
		ByteBuffer byteBuffer = ByteBuffer.allocate(lengthDice+4);
		byteBuffer.putInt(0, idDice);
		byteBuffer.putLong(0+4, bet.longValue());
		byteBuffer.putDouble(8+4, chance);
		byteBuffer.putDouble(16+4, payout);
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

	public static Transaction createPokerBet(String source, String destination, String asset, String resolution, BigInteger bet, List<String> playerCards, List<String> boardCards, List<String> opponentCards) throws Exception {
		//cards should be like 2C or TS, or ?? if they are random

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
		for (String cardString : boardCards) {
			Card card = new Card(cardString);
			if (card.suit>=0 && card.card>=0) {
				if (deal.cards.contains(card)) throw new Exception("Please make sure each card is unique.");
				deal.cards.add(card);				
			} else {
				deal.cards.add(card);
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

		Double chance = Deck.chanceOfWinning(deal.cards) * 100.0;
		Double payout = 100.0/chance*(1-Config.houseEdge);

		if (source.equals("")) throw new Exception("Please specify a source address.");
		if (!(bet.compareTo(BigInteger.ZERO)>0)) throw new Exception("Please bet more than zero.");
		if (!(chance>0.0 && chance<100.0)) throw new Exception("Please specify a chance between 0 and 100.");
		if (!(payout>1.0)) throw new Exception("Please specify a payout greater than 1.");
		if (!(Util.roundOff(chance,6)==Util.roundOff(100.0/(payout/(1.0-Config.houseEdge)),6))) throw new Exception("Please specify a chance and payout that are congruent."); 
		if (!(bet.compareTo(Util.getBalance(source, "CHA"))<=0)) throw new Exception("Please specify a bet that is smaller than your CHA balance.");
		if (!((payout-1.0)*bet.doubleValue()<chaSupply.doubleValue()*Config.maxProfit)) throw new Exception("Please specify a bet with a payout less than the maximum percentage of the house bankroll you can win.");

		BigInteger btcAmount = BigInteger.ZERO;
		//		if (asset.equals("BTC") && !destination.equals("")) { // bet with BTC, expect return in CHA
		//			btcAmount = bet;
		//		} else if (asset.equals("BTC2") && !destination.equals("")) { // bet with BTC, expect return in BTC
		//			btcAmount = bet;
		//			bet = bet.negate();
		//		} else { // bet with CHA
		//			destination = "";
		//		}
		destination = "";

		if (destination.equals("") && resolution.equals("instant")) {
			destination = Config.feeAddress;
			btcAmount = BigInteger.valueOf(Config.feeAddressFee);
		}

		Blocks blocks = Blocks.getInstance();
		ByteBuffer byteBuffer = ByteBuffer.allocate(lengthPoker+4);

		byteBuffer.putInt(0, idPoker);
		byteBuffer.putLong(0+4, bet.longValue());
		for (int i = 0; i<deal.cards.size(); i++) {
			byteBuffer.putShort(i*2+12, Integer.valueOf(deal.cards.get(i).cardValue()).shortValue());
		}

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

	public static LottoResult getLottoResults(Date date) {
		SimpleDateFormat dateFormatStandard = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
		SimpleDateFormat dateFormatDateLotto = new SimpleDateFormat("dd/MM/yyyy");
		SimpleDateFormat dateFormatHour = new SimpleDateFormat("HH");
		SimpleDateFormat dateFormatMinute = new SimpleDateFormat("mm");
		SimpleDateFormat dateFormatDateTimeLotto = new SimpleDateFormat("MM/dd/yyyy HH:mm");
		TimeZone newYork = TimeZone.getTimeZone("America/New_York");
		TimeZone UTC = TimeZone.getTimeZone("UTC");
		dateFormatStandard.setTimeZone(UTC);
		dateFormatDateLotto.setTimeZone(newYork);
		dateFormatHour.setTimeZone(newYork);
		dateFormatMinute.setTimeZone(newYork);				
		dateFormatDateTimeLotto.setTimeZone(newYork);

		String lottoDate = dateFormatDateLotto.format(date);
		Integer hour = Integer.parseInt(dateFormatHour.format(date));
		Integer minute = Integer.parseInt(dateFormatMinute.format(date));
		if (hour>=23 && minute>=56) {
			lottoDate = dateFormatDateLotto.format(Util.addDays(date, 1));
		}
		String lottoUrl = "http://nylottery.ny.gov/wps/PA_NYSLNumberCruncher/NumbersServlet?game=quick&action=winningnumbers&startSearchDate="+lottoDate+"&endSearchDate=&pageNo=&last=&perPage=999&sort=";
		String lottoPage = Util.getPage(lottoUrl);
		logger.info("Getting lottery numbers "+lottoUrl);
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		LottoResult lottoMap = null;
		try {
			lottoMap = objectMapper.readValue(lottoPage, LottoResult.class);
			LottoResult lottoMapNew = new LottoResult();			
			lottoMapNew.draw = new ArrayList<LottoDraw>();
			for (LottoDraw lottoDraw : lottoMap.draw) {
				LottoDraw lottoDrawNew = new LottoDraw();
				lottoDrawNew.dateNY = dateFormatDateTimeLotto.parse(lottoDraw.date);
				lottoDrawNew.numbersDrawn = lottoDraw.numbersDrawn;
				lottoMapNew.draw.add(lottoDrawNew);
			}
			return lottoMapNew;
		} catch (Exception e) {
			logger.error(e.toString());
		}
		return lottoMap;
	}

	public static List<BigInteger> getLottoNumbersAfterDate(Date date) {
		LottoResult lottoMap = getLottoResults(date);
		if (lottoMap != null) {
			for (LottoDraw draw : lottoMap.draw) {
				if (draw.dateNY.after(date)) {
					return draw.numbersDrawn;
				}
			}
		}
		return null;
	}

	public static void resolve() {
		//resolve bets
		logger.info("Resolving bets");

		Database db = Database.getInstance();
		ResultSet rs = db.executeQuery("select block_time,blocks.block_index as block_index,blocks.block_hash as block_hash,tx_index,tx_hash,source,destination,bet,payout,chance,cha_supply,cards,get_btc_back from bets,blocks where bets.block_index=blocks.block_index and bets.validity='valid' and bets.resolved IS NOT 'true';");

		try {
			while (rs.next()) {
				String source = rs.getString("source");
				String destination = rs.getString("destination");
				String txHash = rs.getString("tx_hash");
				String blockHash = rs.getString("block_hash");
				Integer txIndex = rs.getInt("tx_index");
				Integer blockIndex = rs.getInt("block_index");
				BigInteger bet = BigInteger.valueOf(rs.getLong("bet"));
				Double payout = rs.getDouble("payout");
				Double chance = rs.getDouble("chance");
				String cards = rs.getString("cards");
				String getBTCBack = rs.getString("get_btc_back");
				BigInteger chaSupply = BigInteger.valueOf(rs.getLong("cha_supply"));
				Date blockTime = new Date((long)rs.getLong("block_time")*1000);

				logger.info("Attempting to resolve bet "+txHash);
				ResultSet rsCouldWin = db.executeQuery("select sum(bet*(payout-1)) as total from bets,blocks where bets.block_index=blocks.block_index and bets.validity='valid' and blocks.block_index='"+blockIndex.toString()+"';");
				Double couldWin = rsCouldWin.getDouble("total");
				couldWin = couldWin / Config.unit;

				Double roll = null;
				Double rollA = null;
				Double rollB = null;
				Double rollC = 0.0;

				Integer block1 = 308500;
				Integer block2 = 311600; //PROTOCOL CHANGE: push rollC change to block 311600
				if (couldWin>20000.0 || blockIndex<block1) { //we must use NY Lottery numbers
					List<BigInteger> lottoNumbers = getLottoNumbersAfterDate(blockTime);
					if (lottoNumbers != null) {
						logger.info("Found lottery numbers we can use to resolve bet");
						BigInteger denominator = Util.combinations(BigInteger.valueOf(80),BigInteger.valueOf(20)).subtract(BigInteger.ONE);
						BigInteger n = BigInteger.ZERO;
						BigInteger i = BigInteger.ONE;
						for (BigInteger number : lottoNumbers) {
							n = n.add(Util.combinations(number.subtract(BigInteger.ONE),i));
							i = i.add(BigInteger.ONE);
						}
						rollA = n.doubleValue() / (denominator.doubleValue());
					}
				}else{ //we can just use the blockhash for randomness
					rollA = 0.0;
				}
				if (blockIndex>block2) { 
					rollC = (new BigInteger(blockHash,16)).mod(BigInteger.valueOf(1000000000)).doubleValue()/1000000000.0;					
				}

				if (rollA != null) {
					//PROTOCOL CHANGE:
					// if block is less than 308500, then rollA uses lotto numbers, rollB uses txHash, rollC is 0
					// if block is greater than 308500, then rollA uses lotto number if block could win more than 20,000 CHA, rollB uses txHash, rollC uses block hash
					rollB = (new BigInteger(txHash.substring(10, txHash.length()),16)).mod(BigInteger.valueOf(1000000000)).doubleValue()/1000000000.0;
					roll = (rollA + rollB + rollC) % 1.0;
					roll = roll * 100.0;
				}

				// Instant bets are resolved using Roll transactions
				ResultSet rsRoll = db.executeQuery("select * from rolls where roll_tx_hash='"+txHash+"' and block_index='"+blockIndex.toString()+"'");
				if (rsRoll.next()) {
					roll = rsRoll.getDouble("roll");
				}

				BigInteger chaCredit = BigInteger.ZERO;				
				if (roll != null) {
					if (cards!=null && cards.length()>0) { //poker bet
						Deck dealtSoFar = new Deck();
						dealtSoFar.cards.clear();
						Deck removedCards = new Deck();
						removedCards.cards.clear();
						for (String c : cards.split(" ")) {
							Card card = new Card(c);
							if (!card.toString().equals("??")) {
								removedCards.cards.add(card);
							}
							dealtSoFar.cards.add(card);
						}
						Deck deal = Deck.ShuffleAndDeal(roll / 100.0, removedCards.cards, dealtSoFar.cards.size()-removedCards.cards.size());
						for (int j = 0; j<dealtSoFar.cards.size(); j++) {
							if (dealtSoFar.cards.get(j).cardString().equals("??")) {
								dealtSoFar.cards.set(j, deal.cards.remove(0));
							}
						}
						logger.info("Dealt cards = "+dealtSoFar.toString());

						BigInteger profit = BigInteger.ZERO;
						if (Deck.didWin(dealtSoFar.cards)) {
							logger.info("The bet is a winner");
							//win
							profit = new BigDecimal(bet.doubleValue()*(payout.doubleValue()-1.0)*chaSupply.doubleValue()/(chaSupply.doubleValue()-bet.doubleValue()*payout.doubleValue())).toBigInteger();
							BigInteger credit = profit.add(bet);
							chaCredit = credit;
						} else {
							logger.info("The bet is a loser");
							//lose
							profit = bet.multiply(BigInteger.valueOf(-1));
						}
						db.executeUpdate("update bets set cards='"+dealtSoFar.toString()+"',profit='"+profit.toString()+"', rolla='"+(rollA*100.0)+"', rollb='"+(rollB*100.0)+"', roll='"+roll+"', resolved='true' where tx_index='"+txIndex+"';");						
					} else { //dice bet
						logger.info("Roll = "+roll.toString()+", chance = "+chance.toString());

						BigInteger profit = BigInteger.ZERO;
						if (roll<chance) {
							logger.info("The bet is a winner");
							//win
							profit = new BigDecimal(bet.doubleValue()*(payout.doubleValue()-1.0)*chaSupply.doubleValue()/(chaSupply.doubleValue()-bet.doubleValue()*payout.doubleValue())).toBigInteger();
							BigInteger credit = profit.add(bet);
							chaCredit = credit;
						} else {
							logger.info("The bet is a loser");
							//lose
							profit = bet.multiply(BigInteger.valueOf(-1));
						}
						db.executeUpdate("update bets set profit='"+profit.toString()+"', rolla='"+(rollA*100.0)+"', rollb='"+(rollB*100.0)+"', roll='"+roll+"', resolved='true' where tx_index='"+txIndex+"';");
					}
					if (chaCredit.compareTo(BigInteger.ZERO)>0) {
						//						if (getBTCBack.equals("true")) {
						//							ResultSet rsAvailableBTCInCHA = db.executeQuery("select sum(btc_remaining/(btc_amount/cha_amount*(1-width/2))) as available_btc_in_cha from quotes where validity='valid' and destination='"+destination+"' and btc_remaining>0 order by btc_amount/cha_amount*(1-width/2) desc, tx_index asc");
						//							if (rsAvailableBTCInCHA.next() && BigInteger.valueOf(rsAvailableBTCInCHA.getLong("available_btc_in_cha")).compareTo(chaCredit)>=0) {
						//								ResultSet rsQuotes = db.executeQuery("select * from quotes where validity='valid' and destination='"+destination+"' and btc_remaining>0 order by btc_amount/cha_amount*(1-width/2) desc, tx_index asc");
						//								BigInteger btcFilled = BigInteger.ZERO;
						//								BigInteger chaFilled = BigInteger.ZERO;
						//								try {
						//									while (rsQuotes.next()) {
						//										String quoteTxIndex = rsQuotes.getString("tx_index");
						//										String quoteTxHash = rsQuotes.getString("tx_hash");
						//										String quoteSource = rsQuotes.getString("source");
						//										String quoteDestination = rsQuotes.getString("destination");
						//										BigInteger btcRemaining = BigInteger.valueOf(rs.getLong("btc_remaining"));
						//										BigInteger btcQuote = BigInteger.valueOf(rs.getLong("btc_amount"));
						//										BigInteger chaQuote = BigInteger.valueOf(rs.getLong("cha_amount"));
						//										Double width = rs.getDouble("width");
						//										Double price = btcQuote.doubleValue() / chaQuote.doubleValue() * (1-width/2);
						//										BigInteger availableBTCInCHA = new BigDecimal(btcRemaining.doubleValue() / price).toBigInteger();
						//										if (availableBTCInCHA.compareTo(chaCredit.subtract(chaFilled))<0) {
						//											//we take down all of the quote
						//											BigInteger btcToFill = btcRemaining;
						//											BigInteger chaToFill = availableBTCInCHA;
						//											btcFilled = btcFilled.add(btcToFill);
						//											chaFilled = chaFilled.add(chaToFill);
						//											btcRemaining = BigInteger.ZERO;
						//											db.executeUpdate("update quotes set btc_available='"+btcRemaining+"' where tx_index = '"+quoteTxIndex+"'");
						//											Util.credit(quoteSource, "CHA", chaToFill, "Quote fill", txHash, blockIndex);
						//											Util.debit(quoteDestination, "CHA", chaToFill, "Quote fill", txHash, blockIndex);
						//											String quotePayId = quoteTxHash + txHash;
						//											db.executeUpdate("insert into quotepays (id,tx_hash_buy,tx_hash_sell,buysell,source,destination,btc_amount,validity) values('"+quotePayId+"','"+quoteTxHash+"','"+txHash+"','"+destination+"','"+source+"','"+btcToFill+"','pending')");
						//										} else {
						//											//we take down a fraction of the quote, and we are done
						//											BigInteger chaToFill = chaCredit.subtract(chaFilled);
						//											BigInteger btcToFill = new BigDecimal(price * chaToFill.doubleValue()).toBigInteger();
						//											btcFilled = btcFilled.add(btcToFill);
						//											chaFilled = chaFilled.add(chaToFill);
						//											btcRemaining = btcRemaining.subtract(btcToFill);
						//											db.executeUpdate("update quotes set btc_available='"+btcRemaining+"' where tx_index = '"+quoteTxIndex+"'");
						//											Util.credit(quoteSource, "CHA", chaToFill, "Quote fill", txHash, blockIndex);
						//											Util.debit(quoteDestination, "CHA", chaToFill, "Quote fill", txHash, blockIndex);
						//											String quotePayId = quoteTxHash + txHash;
						//											db.executeUpdate("insert into quotepays (id,tx_hash_buy,tx_hash_sell,buysell,source,destination,btc_amount,validity) values('"+quotePayId+"','"+quoteTxHash+"','"+txHash+"','"+destination+"','"+quoteSource+"','"+btcToFill+"','pending')");
						//											break;
						//										}
						//									}
						//								} catch (SQLException e) {	
						//								}
						//							} else {
						//								//not enough available BTC on quotes, must credit CHA instead
						//								Util.credit(source, "CHA", chaCredit, "Bet won", txHash, blockIndex);
						//							}
						//						} else {
						Util.credit(source, "CHA", chaCredit, "Bet won", txHash, blockIndex);
						//						}
					}
				}
			}
		} catch (SQLException e) {
			logger.error(e.toString());
		}		
	}
}

class LottoResult {
	public List<LottoDraw> draw;
}
class LottoDraw {
	public String date;
	public Date dateNY;
	public List<BigInteger> numbersDrawn;
}

class BetInfo {
	public String betType;
	public String source;
	public BigInteger bet;
	public Double chance;
	public Double payout;
	public String txHash;
	public Boolean valid;
	public Boolean resolved;
	public Double profit;
	public Double roll;
	public String cards;
}