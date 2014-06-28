//TODO: allow people to verify NY Lottery number calculation easily
//TODO: force locale to avoid decimals, commas issue
//TODO: encrypt wallet
//TODO: make it so when you make a transaction, it automatically shows the pending transaction
//TODO: make it so pending transactions affect the balance so people can't double bet, double btcpay, etc.
//TODO: make transactions keep retrying
//TODO: more information on fees
//TODO: chancecoin wallet should list all types of transactions maybe?
//TODO: option to allow btcpays to be completed automatically?
//TODO: automatically buy CHA at best price and bet (basically bet with BTC, it buys the CHA on the Dex, and we are always offering)
//TODO: test betting with 0 bet size
//TODO: other games
//TODO: redundancy for downloads.txt on github
//TODO: let people send BTC with Chancecoin client
//TODO: keep the db files more up to date, and let people just download them when they want to gamble.
//TODO: use NAS's blockchain download stuff if it's better than bitcoinj
//TODO: other ways to transactions/bet resolving faster?
//TODO: scratch ticket solution: lock in randomness and bet offline
//TODO: NY lottery numbers only if block can win > 20,000 CHA. otherwise, use blockhash.
//TODO: use n-of-3 multisig as per dexx7's suggestions
//TODO: make chancecoin.com live
public class Test {

	public static void main(String[] args) {
		Blocks blocks = Blocks.getInstanceAndWait();
//		blocks.reDownloadBlockTransactions(308002);
//		blocks.parseBlock(308002);
//		blocks.reDownloadBlockTransactions(308023);
//		blocks.parseBlock(308023);
	}
	
}
