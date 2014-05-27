//TODO: allow people to verify NY Lottery number calculation easily
//TODO: force locale to avoid decimals, commas issue
//TODO: encrypt wallet
//TODO: make it so when you make a transaction, it automatically shows the pending transaction
//TODO: make it so pending transactions affect the balance so people can't double bet, double btcpay, etc.
//TODO: make transactions keep retrying
//TODO: more information on fees
//TODO: chancecoin wallet should list all types of transactions maybe?
//TODO: option to allow btcpays to be completed automatically?
//TODO: automatically buy CHA at best price and bet
//TODO: test betting with 0 bet size
//TODO: other games
//TODO: redundancy for downloads.txt on github
//TODO: let people send BTC with Chancecoin client
public class Test {

	public static void main(String[] args) {
		Blocks blocks = Blocks.getInstanceAndWait();
		blocks.reDownloadBlockTransactions(302332);
	}
	
}
