//TODO: encrypt wallet
//TODO: make it so when you make a transaction, it automatically shows the pending transaction like blockchain.info
//TODO: make transactions keep retrying
//TODO: more information on fees
//TODO: reduce fees for betting somehow?
//TODO: chancecoin wallet should list all types of transactions maybe?
//TODO: option to allow btcpays to be completed automatically?
//TODO: automatically buy CHA at best price and bet
//TODO: test betting with 0 bet size
//TODO: other games
//TODO: easy function to allow you to get escrow back easily
public class Test {

	public static void main(String[] args) {
		Blocks blocks = Blocks.getInstance();
		blocks.reDownloadBlockTransactions(299628);
	}
	
}
