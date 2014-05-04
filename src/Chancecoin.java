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
		//TODO: be able to buy CHA to bet easily
		//TODO: prepare to handle different bet types
		Blocks blocks = Blocks.getInstance();
		//blocks.reparse();
	}
}