import java.math.BigInteger;

import com.google.bitcoin.core.Transaction;

public class Chancecoin {
	public static void main(String[] args) {
		Blocks blocks = Blocks.getInstance();
		Transaction tx = Bet.createBet("1BckY64TE6VrjVcGMizYBE7gt22axnq6CM", BigInteger.ONE, 50.0, 1.96);
		System.out.println(tx);
		System.exit(0);
		try {
			//peerGroup.broadcastTransaction(tx).get();
		} catch (Exception e) {
		}

		//blocks.importPrivateKey("");
	}

}