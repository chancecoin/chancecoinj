import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import ca.ualberta.cs.poker.Hand;
import ca.ualberta.cs.poker.HandEvaluator;

public class Deck {

	public List<Card> cards = new ArrayList<Card>();

	public Deck() {
		for (int c = 0; c<=12; c++) {
			for (int s = 0; s<=3; s++) {
				cards.add(new Card(c,s));
			}
		}
	}
	
	public Deck(String cards) {
		for (String card : cards.split(" ")){
			this.cards.add(new Card(card));
		}
	}
	
	public Deck(List<Card> cards) {
		for (Card card : cards){
			this.cards.add(card);
		}
	}

	@Override
	public String toString() {
		String str = "";
		for (int i = 0; i < cards.size(); i++) {
			str += cards.get(i).toString();
			if (i < cards.size()-1) {
				str += " ";
			}
		}
		return str;
	}

	public List<String> cardStrings() {
		List<String> cardStrings = new ArrayList<String>();
		for (Card card : cards) {
			cardStrings.add(card.toString());
		}
		return cardStrings;
	}

	public List<Deck> combinations(int k) {
		int c = (int) Util.combinations(BigInteger.valueOf(cards.size()), BigInteger.valueOf(k)).intValue();
		Card[][] res = new Card[c][Math.max(0, k)];
		int[] ind = k < 0 ? null : new int[k];
		for (int i = 0; i < k; ++i) { ind[i] = i; }
		for (int i = 0; i < c; ++i) {
			for (int j = 0; j < k; ++j) {
				res[i][j] = cards.get(ind[j]);
			}
			int x = ind.length - 1;
			boolean loop;
			do {
				loop = false;
				ind[x] = ind[x] + 1;
				if (ind[x] > cards.size() - (k - x)) {
					--x;
					loop = x >= 0;
				} else {
					for (int x1 = x + 1; x1 < ind.length; ++x1) {
						ind[x1] = ind[x1 - 1] + 1;
					}
				}
			} while (loop);
		}
		List<Deck> decks = new ArrayList<Deck>();
		for (int i = 0; i < res.length; i++) {
			Deck deck = new Deck();
			deck.cards.clear();
			for (int j = 0; j < k; j++) {
				deck.cards.add(res[i][j]);
			}
			decks.add(deck);
		}
		return decks;
	}

	public static Deck ShuffleAndDeal(Double roll, List<Card> removedCards, int nDeal) {
		//Deck deal = Deck.ShuffleAndDeal(rs.getDouble("roll")/100.0, null, 5);
		Deck deck = new Deck();
		Deck shuffled = new Deck();
		shuffled.cards.clear();
		if (removedCards != null) {
			for (Card card : removedCards) {
				deck.cards.remove(card);
			}
		}
		BigInteger nCards = BigInteger.valueOf(deck.cards.size());
		BigInteger nToDeal = BigInteger.valueOf(nDeal);
		BigInteger nLeftToDeal = BigInteger.valueOf(nDeal);
		BigInteger nMax = Util.factorial(nCards).divide(Util.factorial(nCards.subtract(nToDeal)));
		BigInteger n = BigDecimal.valueOf(nMax.doubleValue() * roll).toBigInteger();

		while (nLeftToDeal.compareTo(BigInteger.ZERO)>0) {
			BigInteger divider = Util.factorial(nCards.subtract(BigInteger.ONE).subtract(nToDeal.subtract(nLeftToDeal))).divide(Util.factorial(nCards.subtract(nToDeal)));
			int digit = n.divide(divider).intValue();
			Card card = deck.cards.remove(digit);
			shuffled.cards.add(card);
			n = n.remainder(divider);
			nLeftToDeal = nLeftToDeal.subtract(BigInteger.ONE);
		}
		return shuffled;
	}
	
	public static String result(List<Card> cards) {
		//cards must be in a list, [p1, p2, b1, b2, b3, b4, b5, o1, o2]
		if (cards.size()==9) {
			return HandEvaluator.nameHand(new Hand(new Deck(cards.subList(0, 7)).toString())) + " vs " + HandEvaluator.nameHand(new Hand(new Deck(cards.subList(2, 9)).toString()));
		}
		return null;
	}
	
	public static Boolean didWin(List<Card> cards) {
		//cards must be in a list, [p1, p2, b1, b2, b3, b4, b5, o1, o2]
		if (cards.size()==9) {
			return didWin(cards.get(0), cards.get(1), cards.get(2), cards.get(3), cards.get(4), cards.get(5), cards.get(6), cards.get(7), cards.get(8));
		} 
		return null;
	}	
	public static Boolean didWin(Card p1, Card p2, Card b1, Card b2, Card b3, Card b4, Card b5, Card o1, Card o2) {
		HandEvaluator handEval = new HandEvaluator();
		Deck pDeck = new Deck();
		pDeck.cards.clear();
		pDeck.cards.add(p1);
		pDeck.cards.add(p2);
		pDeck.cards.add(b1);
		pDeck.cards.add(b2);
		pDeck.cards.add(b3);
		pDeck.cards.add(b4);
		pDeck.cards.add(b5);
		Deck oDeck = new Deck();
		oDeck.cards.clear();
		oDeck.cards.add(o1);
		oDeck.cards.add(o2);
		oDeck.cards.add(b1);
		oDeck.cards.add(b2);
		oDeck.cards.add(b3);
		oDeck.cards.add(b4);
		oDeck.cards.add(b5);
		Hand p = new Hand(pDeck.toString());
		Hand o = new Hand(oDeck.toString());
		int result = handEval.compareHands(p, o);
		if (result==1) {
			return true;
		} else {
			return false;
		}
	}
	
	public static Double chanceOfWinning(Card p1, Card p2, Card b1, Card b2, Card b3, Card b4, Card b5, Card o1, Card o2) {
		List<Card> cards = new ArrayList<Card>();
		cards.add(p1);
		cards.add(p2);
		cards.add(b1);
		cards.add(b2);
		cards.add(b3);
		cards.add(b4);
		cards.add(b5);
		cards.add(o1);
		cards.add(o2);
		return chanceOfWinning(cards);
	}
	
	public static Double chanceOfWinning(List<Card> cards) {
		//this takes player cards p1 and p2, board cards b1-b5, and opponent card o1 and o2, and determines exact 
		//chance of the player winning. cards can be null, which means they are unknown.
		//cards must be in a list, [p1, p2, b1, b2, b3, b4, b5, o1, o2]
		Double chance = 0.0;
		Deck deck = new Deck();
		int unknown = 0;
		for (Card card : cards) {
			if (card != null && !card.toString().equals("??")) {
				deck.cards.remove(card);
			} else {
				unknown++;
			}
		}
		List<Deck> deckCombinations = deck.combinations(unknown);
		Double numerator = 0.0;
		Double denominator = 0.0;
		for (Deck deckCombination : deckCombinations) {
			denominator++;
			List<Card> cardsFilledIn = new ArrayList<Card>();
			for (Card card : cards) {
				if (card != null && !card.toString().equals("??")) {
					cardsFilledIn.add(card);
				} else {
					cardsFilledIn.add(deckCombination.cards.remove(0));			
				}
			}
			Boolean result = didWin(cardsFilledIn.get(0), cardsFilledIn.get(1), cardsFilledIn.get(2), cardsFilledIn.get(3), cardsFilledIn.get(4), cardsFilledIn.get(5), cardsFilledIn.get(6), cardsFilledIn.get(7), cardsFilledIn.get(8));
			if (result) {
				numerator++;
			}
		}
		chance = numerator/denominator;
		return chance;
	}

	public static void main(String[] args) {
		Double rollA = new Random().nextDouble();
		Double rollB = new Random().nextDouble();
		rollB = 87.7269586434694 / 100.0;
		Deck deal = Deck.ShuffleAndDeal(rollA, null, 9);
		deal = new Deck("3H 3D 6C 8C TH ?? ?? 8S KS");
		deal.cards.set(5, new Card("??"));
		deal.cards.set(6, new Card("??"));
		System.out.println(deal.toString());
		Deck deal2 = Deck.ShuffleAndDeal(rollB, deal.cards, 2);
		System.out.println(deal2.toString());
		Deck playerHand = new Deck();
		playerHand.cards.clear();
		for (int i = 0; i<5; i++) {
			playerHand.cards.add(deal.cards.get(i));
		}
		Hand playerHandForEval = new Hand(playerHand.toString());
		System.out.println(playerHandForEval.toString());
		System.out.println(HandEvaluator.nameHand(playerHandForEval));
		Double chance = chanceOfWinning(deal.cards);
		System.out.println(chance);
	}

}
