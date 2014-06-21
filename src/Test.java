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
//TODO: keep the db files more up to date, and let people just download them when they want to gamble.
//TODO: use NAS's blockchain download stuff if it's better than bitcoinj
//TODO: other ways to transactions/bet resolving faster?
public class Test {

	public static void main(String[] args) {
		Blocks blocks = Blocks.getInstanceAndWait();
		//blocks.reDownloadBlockTransactions(302332);
		Util.getTransaction("4012d93baa2322f5c1a2f1da0ed208d0ab03800a9f081fe230fcc01810643669");
	}
	
}
