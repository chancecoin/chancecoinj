import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.google.bitcoin.core.Block;
import com.google.bitcoin.core.Sha256Hash;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.net.NioClientManager;
import com.google.bitcoin.store.BlockStoreException;

public class Chancecoin {
	public static void main(String[] args) {
		Blocks blocks = Blocks.getInstance();
		//blocks.reparse();
		Double rawBet = 0.001;
		BigInteger bet = BigInteger.valueOf((long) (rawBet*Config.unit));
		Transaction txBet = Bet.create("1BckY64TE6VrjVcGMizYBE7gt22axnq6CM", bet, 50.0, 1.96);
		System.out.println(txBet);
		Transaction txSend = Send.create("1BckY64TE6VrjVcGMizYBE7gt22axnq6CM", "1JSgzenaqrRtdscWvurhbPgFBt1YMBuFDj", "CHA", BigInteger.ONE);
		System.out.println(txSend);
		//blocks.importPrivateKey("");
		System.out.println(Util.getMinVersion());
		System.out.println(Util.getMinMajorVersion());
		System.out.println(Util.getMinMinorVersion());
		System.out.println("done");
		System.exit(0);
		//blocks.importTransaction(txBet, null, Util.getLastBlock());
		blocks.recreateBlockchainDatabase();
		try {
			//peerGroup.broadcastTransaction(txBet).get();
		} catch (Exception e) {
		}
	}

}