public class Card {

	public int card; //0-12 = 2-9, T, J, Q, K, A
	public int suit; //0-3 = C,D,H,S

	public Card(int card, int  suit) {
		this.card = card;
		this.suit = suit;
	}
	
	public Card(int card) {
		this.card = card % 13;
		this.suit = card/13;
		if (card>51 || card<0) {
			this.suit = -1;
			this.card = -1;
		}
	}
	
	public Card(String card, String suit) {
		if (suit == "C") {
			this.suit = 0;
		} else if (suit == "D") {
			this.suit = 1;
		} else if (suit == "H") {
			this.suit = 2;
		} else if (suit == "S") {
			this.suit = 3;
		} else {
			this.suit = -1;
		}
		if (card == "2") {
			this.card = 0;
		} else if (card == "3") {
			this.card = 1;
		} else if (card == "4") {
			this.card = 2;
		} else if (card == "5") {
			this.card = 3;
		} else if (card == "6") {
			this.card = 4;
		} else if (card == "7") {
			this.card = 5;
		} else if (card == "8") {
			this.card = 6;
		} else if (card == "9") {
			this.card = 7;
		} else if (card == "T") {
			this.card = 8;
		} else if (card == "J") {
			this.card = 9;
		} else if (card == "Q") {
			this.card = 10;
		} else if (card == "K") {
			this.card = 11;
		} else if (card == "A") {
			this.card = 12;
		} else {
			this.card = -1;
		}
	}
	
	public Card(String cardString) {
		String card = cardString.substring(0, cardString.length()-1);
		String suit = cardString.substring(cardString.length()-1, cardString.length());
		if (suit == "C") {
			this.suit = 0;
		} else if (suit == "D") {
			this.suit = 1;
		} else if (suit == "H") {
			this.suit = 2;
		} else if (suit == "S") {
			this.suit = 3;
		} else {
			this.suit = -1;
		}
		if (card == "2") {
			this.card = 0;
		} else if (card == "3") {
			this.card = 1;
		} else if (card == "4") {
			this.card = 2;
		} else if (card == "5") {
			this.card = 3;
		} else if (card == "6") {
			this.card = 4;
		} else if (card == "7") {
			this.card = 5;
		} else if (card == "8") {
			this.card = 6;
		} else if (card == "9") {
			this.card = 7;
		} else if (card == "T") {
			this.card = 8;
		} else if (card == "J") {
			this.card = 9;
		} else if (card == "Q") {
			this.card = 10;
		} else if (card == "K") {
			this.card = 11;
		} else if (card == "A") {
			this.card = 12;
		} else {
			this.card = -1;
		}
	}
	
	public String cardString() {
		String cardString = "";
		switch(card) {
		case 0: cardString+="2";
		break;
		case 1: cardString+="3";
		break;
		case 2: cardString+="4";
		break;
		case 3: cardString+="5";
		break;
		case 4: cardString+="6";
		break;
		case 5: cardString+="7";
		break;
		case 6: cardString+="8";
		break;
		case 7: cardString+="9";
		break;
		case 8: cardString+="T";
		break;
		case 9: cardString+="J";
		break;
		case 10: cardString+="Q";
		break;
		case 11: cardString+="K";
		break;
		case 12: cardString+="A";
		break;
		default: cardString+="?";
		break;
		}
		switch(suit) {
		case 0: cardString+="C";
		break;
		case 1: cardString+="D";
		break;
		case 2: cardString+="H";
		break;
		case 3: cardString+="S";
		break;
		default: cardString+="?";
		break;		
		}
		return cardString;
	}

	public int cardValue() {
		if (suit>=0 && suit<=4 && card>=0 && card<=12) {
			return suit*13+card;
		}else{
			return -1;
		}
	}
	
    @Override
    public int hashCode() {
    	return cardValue();
    }
    
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Card))
            return false;
        if (o == this)
            return true;
        Card c = (Card) o;
	    return (c.card==this.card && c.suit==this.suit);
	}
	
	@Override
	public String toString() {
		return cardString();
	}
	
	public static void main(String[] args) {
	}
}
