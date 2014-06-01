public class Card {

	public int card; //2-14 = 2-10, J, Q, K, A
	public int suit; //0-3 = D,C,H,S

	public Card(int card, int  suit) {
		this.card = card;
		this.suit = suit;
	}

	public Card(String card, String suit) {
		switch (suit) {
		case "D":  this.suit = 0;
		break;
		case "C":  this.suit = 1;
		break;
		case "H":  this.suit = 2;
		break;
		case "S":  this.suit = 3;
		break;
		}
		switch (card) {
		case "2":  this.card = 2;
		break;
		case "3":  this.card = 3;
		break;
		case "4":  this.card = 4;
		break;
		case "5":  this.card = 5;
		break;
		case "6":  this.card = 6;
		break;
		case "7":  this.card = 7;
		break;
		case "8":  this.card = 8;
		break;
		case "9":  this.card = 9;
		break;
		case "10":  this.card = 10;
		break;
		case "J":  this.card = 11;
		break;
		case "Q":  this.card = 12;
		break;
		case "K":  this.card = 13;
		break;
		case "A":  this.card = 14;
		break;
		}		
	}
	
	public String cardString() {
		String cardString = "";
		switch(card) {
		case 2: cardString+="2";
		break;
		case 3: cardString+="3";
		break;
		case 4: cardString+="4";
		break;
		case 5: cardString+="5";
		break;
		case 6: cardString+="6";
		break;
		case 7: cardString+="7";
		break;
		case 8: cardString+="8";
		break;
		case 9: cardString+="9";
		break;
		case 10: cardString+="10";
		break;
		case 11: cardString+="J";
		break;
		case 12: cardString+="Q";
		break;
		case 13: cardString+="K";
		break;
		case 14: cardString+="A";
		break;
		}
		switch(suit) {
		case 0: cardString+="D";
		break;
		case 1: cardString+="C";
		break;
		case 2: cardString+="H";
		break;
		case 3: cardString+="S";
		break;
		}
		return cardString;
	}

}
