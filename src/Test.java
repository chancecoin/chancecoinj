import java.util.List;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import javax.xml.bind.DatatypeConverter;

import com.google.bitcoin.script.Script;
import com.google.bitcoin.script.ScriptBuilder;
import com.google.bitcoin.script.ScriptOpCodes;
import com.lambdaworks.crypto.SCrypt;
import com.lambdaworks.crypto.SCryptUtil;

//TODO: allow people to verify NY Lottery number calculation easily
//TODO: encrypt wallet
//TODO: chancecoin wallet should list all types of transactions maybe?
//TODO: option to allow btcpays to be completed automatically?
//TODO: test betting with 0 bet size
//TODO: other games
//TODO: redundancy for downloads.txt on github
//TODO: use NAS's blockchain download stuff if it's better than bitcoinj
//TODO: use n-of-3 multisig as per dexx7's suggestions

//TODO next:
//TODO: document fees, new user documentation
//TODO: BTC bet is default for user with no CHA
//TODO: make a JS wallet that just connects to chancecoin.com api instead of downloading blockchain
//TODO: make it so pending transactions affect the balance so people can't double bet
public class Test {

	public static String solveScryptPuzzle(String betDescription) {
		Date start = new Date();
		String password = betDescription;
		MessageDigest digest;
		BigInteger bestValue = BigInteger.valueOf(16).pow(64);
		BigInteger goal = bestValue.divide(BigInteger.valueOf(25000));
		String scrypt = "";
		while (bestValue.compareTo(goal)>=0) {
			try {
				scrypt = SCryptUtil.scrypt(password, 2048, 1, 1);
				//System.out.println(scrypt);
				digest = MessageDigest.getInstance("SHA-256");
				byte[] hashBytes = digest.digest(scrypt.getBytes("UTF-8"));
				BigInteger hashInteger = new BigInteger(1, hashBytes);
				if (hashInteger.compareTo(bestValue)<0) {
					bestValue = hashInteger;
				}
				String hash = hashInteger.toString(16);
			} catch (Exception e1) {
			}
		}
		Date end = new Date();
		long timeDifference = end.getTime()-start.getTime();
		System.out.println(timeDifference/1000);
		return scrypt;
	}

	public static Boolean checkScryptPuzzle(String scrypt, String betDescription) {
		String password = betDescription;
		Boolean result = SCryptUtil.check(password, scrypt);
		return result;
	}

	public static void main(String[] args) {
//		Blocks blocks = Blocks.getInstanceAndWait();
//		for (int block = 311738; block<=311738; block++) {
//			blocks.reDownloadBlockTransactions(block);
//			blocks.parseBlock(block);
//		}
		
		Util.getBestOfferOnExchanges(1.0);

		//		Database db = Database.getInstance();
		//		ResultSet rs = db.executeQuery("select block_hash from blocks;");
		//		try {
		//			while (rs.next()) {
		//				Double rollA = (new BigInteger(rs.getString("block_hash"),16)).mod(BigInteger.valueOf(1000000000)).doubleValue()/1000000000.0;
		//				System.out.println(rollA);			
		//			}
		//		} catch (SQLException e) {
		//			// TODO Auto-generated catch block
		//			e.printStackTrace();
		//		}
		//		Blocks blocks = Blocks.getInstanceAndWait();
		//		for (Integer block : Arrays.asList(308218,308023,308002,308318)) {
		//			blocks.reDownloadBlockTransactions(block);
		//			blocks.parseBlock(block);
		//		}
		//		String betDescription = "hi";
		//		String scrypt = solveScryptPuzzle(betDescription);
		//		System.out.println(scrypt);
		//		System.out.println(checkScryptPuzzle(scrypt,betDescription));
	}

}
