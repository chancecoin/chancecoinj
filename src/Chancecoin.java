import java.math.BigInteger;

import com.google.bitcoin.core.Transaction;

public class Chancecoin {
	public static void main(String[] args) {
		Blocks blocks = Blocks.getInstance();
		blocks.reparse();
		Double rawBet = 0.001;
		BigInteger bet = BigInteger.valueOf((long) (rawBet*Config.unit));
		System.out.println(bet);
		Transaction tx = Bet.create("1BckY64TE6VrjVcGMizYBE7gt22axnq6CM", bet, 50.0, 1.96);
		blocks.importTransaction(tx, null, Util.getLastBlock());
		System.out.println(tx);
		System.exit(0);
		try {
			//peerGroup.broadcastTransaction(tx).get();
		} catch (Exception e) {
		}

		//blocks.importPrivateKey("");
	}

}