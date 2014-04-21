import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.google.bitcoin.core.Block;
import com.google.bitcoin.core.Sha256Hash;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.Transaction.SigHash;
import com.google.bitcoin.net.NioClientManager;
import com.google.bitcoin.store.BlockStoreException;

public class Chancecoin {
	public static void main(String[] args) {
		Blocks blocks = Blocks.getInstance();
		//blocks.reparse();
		//Transaction txSend = Send.create("1BckY64TE6VrjVcGMizYBE7gt22axnq6CM", "1JSgzenaqrRtdscWvurhbPgFBt1YMBuFDj", "CHA", BigInteger.ONE);
		//System.out.println(txSend);
		//System.out.println("done");
		//System.exit(0);
	}

}