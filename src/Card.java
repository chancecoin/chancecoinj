public class Card {

	public int card; //0-12 = 2-10, J, Q, K, A
	public int suit; //0-3 = C,D,H,S

	public Card(int card, int  suit) {
		this.card = card;
		this.suit = suit;
	}
	
	public Card(int card) {
		this.card = card % 4;
		this.suit = card/4;
		if (card>51 || card<0) {
			this.suit = -1;
			this.card = -1;
		}
	}
	
	public Card(String card, String suit) {
		switch (suit) {
		case "C":  this.suit = 0;
		break;
		case "D":  this.suit = 1;
		break;
		case "H":  this.suit = 2;
		break;
		case "S":  this.suit = 3;
		break;
		default:
		this.suit = -1;
		break;
		}
		switch (card) {
		case "2":  this.card = 0;
		break;
		case "3":  this.card = 1;
		break;
		case "4":  this.card = 2;
		break;
		case "5":  this.card = 3;
		break;
		case "6":  this.card = 4;
		break;
		case "7":  this.card = 5;
		break;
		case "8":  this.card = 6;
		break;
		case "9":  this.card = 7;
		break;
		case "10":  this.card = 8;
		break;
		case "J":  this.card = 9;
		break;
		case "Q":  this.card = 10;
		break;
		case "K":  this.card = 11;
		break;
		case "A":  this.card = 12;
		break;
		default:
		this.suit = -1;
		break;		
		}		
	}
	
	public Card(String cardString) {
		String card = cardString.substring(0, cardString.length()-1);
		String suit = cardString.substring(cardString.length()-1, cardString.length());
		new Card(card, suit);
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
		case 8: cardString+="10";
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
    	return suit*4+card;
	}
	
    @Override
    public int hashCode() {
    	return suit*4+card;
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
}
