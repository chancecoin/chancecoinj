import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.Block;
import com.google.bitcoin.core.BlockChain;
import com.google.bitcoin.core.DumpedPrivateKey;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.InsufficientMoneyException;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.PeerGroup;
import com.google.bitcoin.core.PeerGroup.FilterRecalculateMode;
import com.google.bitcoin.core.Transaction.SigHash;
import com.google.bitcoin.core.ScriptException;
import com.google.bitcoin.core.Sha256Hash;
import com.google.bitcoin.core.StoredBlock;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionInput;
import com.google.bitcoin.core.TransactionOutput;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.net.discovery.DnsDiscovery;
import com.google.bitcoin.params.MainNetParams;
import com.google.bitcoin.script.Script;
import com.google.bitcoin.script.ScriptBuilder;
import com.google.bitcoin.script.ScriptChunk;
import com.google.bitcoin.script.ScriptOpCodes;
import com.google.bitcoin.store.BlockStore;
import com.google.bitcoin.store.BlockStoreException;
import com.google.bitcoin.store.H2FullPrunedBlockStore;
import com.google.bitcoin.wallet.WalletTransaction;
import com.sun.org.apache.xpath.internal.compiler.OpCodes;

public class Blocks implements Runnable {
	public NetworkParameters params;
	public Logger logger = LoggerFactory.getLogger(Blocks.class);
	private static Blocks instance = null;
	public Wallet wallet;
	public String walletFile = "resources/db/wallet";
	public PeerGroup peerGroup;
	public BlockChain blockChain;
	public BlockStore blockStore;
	public Boolean working = false;
	public Boolean parsing = false;
	public Integer parsingBlock = 0;
	
	public static Blocks getInstance() {
		if(instance == null) {
			instance = new Blocks();
			instance.init();
			new Thread() { public void run() {instance.follow();}}.start();
		} else {
			new Thread() { public void run() {instance.follow();}}.start();
		}
		return instance;
	}

	public static Blocks getInstance(Boolean dbExists) {
		if(instance == null) {
			instance = new Blocks();
			if (!dbExists) {
				instance.createTables();
			}
			instance.init();
			new Thread() { public void run() {instance.follow();}}.start();
		} else {
			if (!dbExists) {
				instance.createTables();				
			}
			new Thread() { public void run() {instance.follow();}}.start();
		}
		return instance;
	}

	@Override
	public void run() {
		while (true) {
			Blocks.getInstance();
			try {
				logger.info("looping Blocks");
				Thread.sleep(1000*60); //once a minute, we run blocks.follow()
			} catch (InterruptedException e) {
				System.out.println(e.toString());
				System.exit(0);
			}
		}
	}

	public void init() {
		params = MainNetParams.get();
		try {
			if ((new File(walletFile)).exists()) {
				logger.info("Found wallet file");
				wallet = Wallet.loadFromFile(new File(walletFile));
			} else {
				wallet = new Wallet(params);
				ECKey newKey = new ECKey();
				newKey.setCreationTimeSeconds(Config.burnCreationTime);
				wallet.addKey(newKey);
			}
			if (!(new File(Config.dbPath+Config.appName.toLowerCase()+".h2.db").exists())) {
				new File(Config.dbPath+Config.appName.toLowerCase()+".h2.db")
			}
			if (!(new File(Database.dbFile).exists())) {
				
			}
			blockStore = new H2FullPrunedBlockStore(params, Config.dbPath+Config.appName.toLowerCase(), 2000);
			blockChain = new BlockChain(params, wallet, blockStore);
			peerGroup = new PeerGroup(params, blockChain);
			peerGroup.addWallet(wallet);
			peerGroup.setFastCatchupTimeSecs(Config.burnCreationTime);
			wallet.autosaveToFile(new File(walletFile), 1, TimeUnit.MINUTES, null);
			peerGroup.addPeerDiscovery(new DnsDiscovery(params));
			peerGroup.startAndWait();
			peerGroup.addEventListener(new ChancecoinPeerEventListener());
			peerGroup.downloadBlockChain();
		} catch (Exception e) {
			logger.error(e.toString());
			System.exit(0);
		}
	}
	
	public void follow() {
		if (!working) {
			working = true;
			try {
				Integer blockHeight = blockStore.getChainHead().getHeight();
				Integer lastBlock = Util.getLastBlock();
				if (lastBlock == 0) {
					lastBlock = Config.firstBlock - 1;
				}
				Integer nextBlock = lastBlock + 1;
				
				if (lastBlock < blockHeight) {
					//traverse new blocks
					Database db = Database.getInstance();
					logger.info("Bitcoin block height: "+blockHeight);	
					logger.info("Chancecoin block height: "+lastBlock);	
					Integer blocksToScan = blockHeight - lastBlock;
					List<Sha256Hash> blockHashes = new ArrayList<Sha256Hash>();
					
					Block block = peerGroup.getDownloadPeer().getBlock(blockStore.getChainHead().getHeader().getHash()).get();
					while (blockStore.get(block.getHash()).getHeight()>lastBlock) {
						blockHashes.add(block.getHash());
						block = blockStore.get(block.getPrevBlockHash()).getHeader();
					}
					
					for (int i = blockHashes.size()-1; i>=0; i--) { //traverse blocks in reverse order
						block = peerGroup.getDownloadPeer().getBlock(blockHashes.get(i)).get();
						blockHeight = blockStore.get(block.getHash()).getHeight();
						logger.info("Catching Chancecoin up to Bitcoin (block "+blockHeight.toString()+"): "+Util.format((blockHashes.size() - i)/((double) blockHashes.size())*100.0)+"%");	
						importBlock(block, blockHeight);
					}
					
					if (getDBMinorVersion()<Config.minorVersionDB){
						reparse();
						updateMinorVersion();		    	
					}else{
						parseFrom(nextBlock);
					}
					Bet.resolve();
					Order.expire();
				}
			} catch (Exception e) {
				logger.error(e.toString());
				System.exit(0);
			}	
			working = false;
		}
	}

	public void reDownloadBlockTransactions(Integer blockHeight) {
		Database db = Database.getInstance();
		ResultSet rs = db.executeQuery("select * from blocks where block_index='"+blockHeight.toString()+"';");
		try {
			if (rs.next()) {
				Block block = peerGroup.getDownloadPeer().getBlock(new Sha256Hash(rs.getString("block_hash"))).get();
				db.executeUpdate("delete from transactions where block_index='"+blockHeight.toString()+"';");
				for (Transaction tx : block.getTransactions()) {
					importTransaction(tx, block, blockHeight);
				}
			}
		} catch (Exception e) {
			
		}
	}
	
	public void importBlock(Block block, Integer blockHeight) {
		logger.info("Block height: "+blockHeight);
		Database db = Database.getInstance();
		ResultSet rs = db.executeQuery("select * from blocks where block_hash='"+block.getHashAsString()+"';");
		try {
			if (!rs.next()) {
				db.executeUpdate("INSERT INTO blocks(block_index,block_hash,block_time) VALUES('"+blockHeight.toString()+"','"+block.getHashAsString()+"','"+block.getTimeSeconds()+"')");
			}
			for (Transaction tx : block.getTransactions()) {
				importTransaction(tx, block, blockHeight);
			}
			Bet.resolve();
			Order.expire();
		} catch (SQLException e) {
		}
	}

	public void importTransaction(Transaction tx, Block block, Integer blockHeight) {
		BigInteger fee = BigInteger.ZERO;
		String destination = "";
		BigInteger btcAmount = BigInteger.ZERO;
		List<Byte> dataArrayList = new ArrayList<Byte>();
		byte[] data = null;
		String source = "";

		Database db = Database.getInstance();

		//check to see if this is a burn
		for (TransactionOutput out : tx.getOutputs()) {
			try {
				Script script = out.getScriptPubKey();
				Address address = script.getToAddress(params);
				if (address.toString().equals(Config.burnAddress)) {
					destination = address.toString();
					btcAmount = out.getValue();
				}
			} catch(ScriptException e) {				
			}
		}

		for (TransactionOutput out : tx.getOutputs()) {
			//fee = fee.subtract(out.getValue()); //TODO, turn this on
			try {
				Script script = out.getScriptPubKey();
				List<ScriptChunk> asm = script.getChunks();
				if (asm.size()==2 && asm.get(0).equalsOpCode(106)) { //OP_RETURN
					for (byte b : asm.get(1).data) dataArrayList.add(b);
				} else if (asm.size()>=5 && asm.get(0).equalsOpCode(81) && asm.get(3).equalsOpCode(82) && asm.get(4).equalsOpCode(174)) { //MULTISIG
					for (int i=1; i<asm.get(2).data[0]+1; i++) dataArrayList.add(asm.get(2).data[i]);
				}

				if (destination.equals("") && btcAmount==BigInteger.ZERO && dataArrayList.size()==0) {
					Address address = script.getToAddress(params);
					destination = address.toString();
					btcAmount = out.getValue();					
				}
			} catch(ScriptException e) {				
			}
		}
		if (destination.equals(Config.burnAddress)) {
		} else if (dataArrayList.size()>Config.prefix.length()) {
			byte[] prefixBytes = Config.prefix.getBytes();
			byte[] dataPrefixBytes = Util.toByteArray(dataArrayList.subList(0, Config.prefix.length()));
			dataArrayList = dataArrayList.subList(Config.prefix.length(), dataArrayList.size());
			data = Util.toByteArray(dataArrayList);
			if (!Arrays.equals(prefixBytes,dataPrefixBytes)) {
				return;
			}
		} else {
			return;
		}
		for (TransactionInput in : tx.getInputs()) {
			if (in.isCoinBase()) return;
			try {
				Script script = in.getScriptSig();
				//fee = fee.add(in.getValue()); //TODO, turn this on
				Address address = script.getFromAddress(params);
				if (source.equals("")) {
					source = address.toString();
				}else if (!source.equals(address.toString()) && !destination.equals(Config.burnAddress)){ //require all sources to be the same unless this is a burn
					return;
				}
			} catch(ScriptException e) {
			}
		}

		logger.info("Incoming transaction from "+source+" to "+destination+" ("+tx.getHashAsString()+")");

		if (!source.equals("") && (destination.equals(Config.burnAddress) || dataArrayList.size()>0)) {
			String dataString = "";
			if (destination.equals(Config.burnAddress)) {
			}else{
				try {
					dataString = new String(data,"ISO-8859-1");
				} catch (UnsupportedEncodingException e) {
				}
			}
			ResultSet rs = db.executeQuery("select * from transactions where tx_hash='"+tx.getHashAsString()+"';");
			try {
				if (!rs.next()) {
					if (block!=null) {
						PreparedStatement ps = db.connection.prepareStatement("INSERT INTO transactions(tx_index, tx_hash, block_index, block_time, source, destination, btc_amount, fee, data) VALUES('"+(Util.getLastTxIndex()+1)+"','"+tx.getHashAsString()+"','"+blockHeight+"','"+block.getTimeSeconds()+"','"+source+"','"+destination+"','"+btcAmount.toString()+"','"+fee.toString()+"',?)");
						ps.setString(1, dataString);
						ps.execute();
						//db.executeUpdate("INSERT INTO transactions(tx_index, tx_hash, block_index, block_time, source, destination, btc_amount, fee, data) VALUES('"+(Util.getLastTxIndex()+1)+"','"+tx.getHashAsString()+"','"+blockHeight+"','"+block.getTimeSeconds()+"','"+source+"','"+destination+"','"+btcAmount.toString()+"','"+fee.toString()+"','"+dataString+"')");
					}else{
						PreparedStatement ps = db.connection.prepareStatement("INSERT INTO transactions(tx_index, tx_hash, block_index, block_time, source, destination, btc_amount, fee, data) VALUES('"+(Util.getLastTxIndex()+1)+"','"+tx.getHashAsString()+"','"+blockHeight+"','','"+source+"','"+destination+"','"+btcAmount.toString()+"','"+fee.toString()+"',?)");
						ps.setString(1, dataString);
						ps.execute();
						//db.executeUpdate("INSERT INTO transactions(tx_index, tx_hash, block_index, block_time, source, destination, btc_amount, fee, data) VALUES('"+(Util.getLastTxIndex()+1)+"','"+tx.getHashAsString()+"','"+blockHeight+"','','"+source+"','"+destination+"','"+btcAmount.toString()+"','"+fee.toString()+"','"+dataString+"')");						
					}
				}
			} catch (SQLException e) {
				logger.error(e.toString());
			}
		}
	}

	public void reparse() {
		Database db = Database.getInstance();
		db.executeUpdate("delete from debits;");
		db.executeUpdate("delete from credits;");
		db.executeUpdate("delete from balances;");
		db.executeUpdate("delete from sends;");
		db.executeUpdate("delete from orders;");
		db.executeUpdate("delete from order_matches;");
		db.executeUpdate("delete from btcpays;");
		db.executeUpdate("delete from bets;");
		db.executeUpdate("delete from burns;");
		db.executeUpdate("delete from cancels;");
		db.executeUpdate("delete from order_expirations;");
		db.executeUpdate("delete from order_match_expirations;");
		db.executeUpdate("delete from messages;");
		new Thread() { public void run() {parseFrom(0);}}.start();
	}

	public void parseFrom(Integer blockNumber) {
		if (!working) {
			working = true;
			parsing = true;
			Database db = Database.getInstance();
			ResultSet rs = db.executeQuery("select * from blocks where block_index>="+blockNumber.toString()+" order by block_index asc;");
			try {
				while (rs.next()) {
					Integer blockIndex = rs.getInt("block_index");
					ResultSet rsTx = db.executeQuery("select * from transactions where block_index="+blockIndex.toString()+" order by tx_index asc;");
					parsingBlock = blockIndex;
					while (rsTx.next()) {
						Integer txIndex = rsTx.getInt("tx_index");
						String source = rsTx.getString("source");
						String destination = rsTx.getString("destination");
						BigInteger btcAmount = BigInteger.valueOf(rsTx.getInt("btc_amount"));
						byte[] data = rsTx.getString("data").getBytes("ISO-8859-1");
	
						if (destination.equals(Config.burnAddress)) {
							//parse Burn
							Burn.parse(txIndex);
						} else {
							List<Byte> dataArrayList = Util.toByteArrayList(data);
	
							List<Byte> messageType = dataArrayList.subList(0, 4);
							List<Byte> message = dataArrayList.subList(4, dataArrayList.size());
	
							if (messageType.get(3)==Bet.id.byteValue()) {
								Bet.parse(txIndex, message);
							} else if (messageType.get(3)==Send.id.byteValue()) {
								Send.parse(txIndex, message);
							} else if (messageType.get(3)==Order.id.byteValue()) {
								Order.parse(txIndex, message);
							} else if (messageType.get(3)==Cancel.id.byteValue()) {
								Cancel.parse(txIndex, message);
							} else if (messageType.get(3)==BTCPay.id.byteValue()) {
								BTCPay.parse(txIndex, message);
							}						
						}
					}
					Bet.resolve();
					Order.expire(blockIndex);
				}
			} catch (SQLException e) {
				logger.error(e.toString());
			} catch (UnsupportedEncodingException e) {
			}
			parsing = false;
			working = false;
		}
	}
		
	public void recreateDatabase() {
		Database db = Database.getInstance();
		try {
			db.connection.close();
		} catch (SQLException e) {
		}
		File dbFile = new File(db.dbFile);
		dbFile.renameTo(new File("resources/db/" + Config.appName.toLowerCase()+"-"+Config.majorVersionDB.toString()+(Long.toString(System.currentTimeMillis()))+".db"));		
		db.init();
		createTables();
	}
	
	public void createTables() {
		Database db = Database.getInstance();
		try {
			// Blocks
			db.executeUpdate("CREATE TABLE IF NOT EXISTS blocks(block_index INTEGER PRIMARY KEY, block_hash TEXT UNIQUE, block_time INTEGER)");
			db.executeUpdate("CREATE INDEX IF NOT EXISTS blocks_block_index_idx ON blocks (block_index)");

			// Transactions
			db.executeUpdate("CREATE TABLE IF NOT EXISTS transactions(tx_index INTEGER PRIMARY KEY, tx_hash TEXT UNIQUE, block_index INTEGER, block_time INTEGER, source TEXT, destination TEXT, btc_amount INTEGER, fee INTEGER, data BLOB, supported BOOL DEFAULT 1)");
			db.executeUpdate("CREATE INDEX IF NOT EXISTS transactions_block_index_idx ON transactions (block_index)");
			db.executeUpdate("CREATE INDEX IF NOT EXISTS transactions_tx_index_idx ON transactions (tx_index)");
			db.executeUpdate("CREATE INDEX IF NOT EXISTS transactions_tx_hash_idx ON transactions (tx_hash)");

			// (Valid) debits
			db.executeUpdate("CREATE TABLE IF NOT EXISTS debits(block_index INTEGER, address TEXT, asset TEXT, amount INTEGER, calling_function TEXT, event TEXT)");
			db.executeUpdate("CREATE INDEX IF NOT EXISTS debits_address_idx ON debits (address)");

			// (Valid) credits
			db.executeUpdate("CREATE TABLE IF NOT EXISTS credits(block_index INTEGER, address TEXT, asset TEXT, amount INTEGER, calling_function TEXT, event TEXT)");
			db.executeUpdate("CREATE INDEX IF NOT EXISTS credits_address_idx ON credits (address)");

			// Balances
			db.executeUpdate("CREATE TABLE IF NOT EXISTS balances(address TEXT, asset TEXT, amount INTEGER)");
			db.executeUpdate("CREATE INDEX IF NOT EXISTS address_idx ON balances (address)");
			db.executeUpdate("CREATE INDEX IF NOT EXISTS asset_idx ON balances (asset)");

			// Sends
			db.executeUpdate("CREATE TABLE IF NOT EXISTS sends(tx_index INTEGER PRIMARY KEY, tx_hash TEXT UNIQUE, block_index INTEGER, source TEXT, destination TEXT, asset TEXT, amount INTEGER, validity TEXT)");
			db.executeUpdate("CREATE INDEX IF NOT EXISTS sends_block_index_idx ON sends (block_index)");

			// Orders
			db.executeUpdate("CREATE TABLE IF NOT EXISTS orders(tx_index INTEGER PRIMARY KEY, tx_hash TEXT UNIQUE, block_index INTEGER, source TEXT, give_asset TEXT, give_amount INTEGER, give_remaining INTEGER, get_asset TEXT, get_amount INTEGER, get_remaining INTEGER, expiration INTEGER, expire_index INTEGER, fee_required INTEGER, fee_provided INTEGER, fee_remaining INTEGER, validity TEXT)");
			db.executeUpdate("CREATE INDEX IF NOT EXISTS block_index_idx ON orders (block_index)");
			db.executeUpdate("CREATE INDEX IF NOT EXISTS expire_index_idx ON orders (expire_index)");

			// Order Matches
			db.executeUpdate("CREATE TABLE IF NOT EXISTS order_matches(id TEXT PRIMARY KEY, tx0_index INTEGER, tx0_hash TEXT, tx0_address TEXT, tx1_index INTEGER, tx1_hash TEXT, tx1_address TEXT, forward_asset TEXT, forward_amount INTEGER, backward_asset TEXT, backward_amount INTEGER, tx0_block_index INTEGER, tx1_block_index INTEGER, tx0_expiration INTEGER, tx1_expiration INTEGER, match_expire_index INTEGER, validity TEXT)");
			db.executeUpdate("CREATE INDEX IF NOT EXISTS match_expire_index_idx ON order_matches (match_expire_index)");

			// BTCpays
			db.executeUpdate("CREATE TABLE IF NOT EXISTS btcpays(tx_index INTEGER PRIMARY KEY, tx_hash TEXT UNIQUE, block_index INTEGER, source TEXT, destination TEXT, btc_amount INTEGER, order_match_id TEXT, validity TEXT)");
			db.executeUpdate("CREATE INDEX IF NOT EXISTS block_index_idx ON btcpays (block_index)");

			// Bets
			db.executeUpdate("CREATE TABLE IF NOT EXISTS bets(tx_index INTEGER PRIMARY KEY, tx_hash TEXT UNIQUE, block_index INTEGER, source TEXT, bet INTEGER, chance REAL, payout REAL, profit INTEGER, cha_supply INTEGER, rolla REAL, rollb REAL, roll REAL, resolved TEXT, validity TEXT)");
			db.executeUpdate("CREATE INDEX IF NOT EXISTS block_index_idx ON bets (block_index)");

			// Burns
			db.executeUpdate("CREATE TABLE IF NOT EXISTS burns(tx_index INTEGER PRIMARY KEY, tx_hash TEXT UNIQUE, block_index INTEGER, source TEXT, burned INTEGER, earned INTEGER, validity TEXT)");
			db.executeUpdate("CREATE INDEX IF NOT EXISTS validity_idx ON burns (validity)");
			db.executeUpdate("CREATE INDEX IF NOT EXISTS address_idx ON burns (address)");

			// Cancels
			db.executeUpdate("CREATE TABLE IF NOT EXISTS cancels(tx_index INTEGER PRIMARY KEY, tx_hash TEXT UNIQUE, block_index INTEGER, source TEXT, offer_hash TEXT, validity TEXT)");
			db.executeUpdate("CREATE INDEX IF NOT EXISTS cancels_block_index_idx ON cancels (block_index)");

			// Order Expirations
			db.executeUpdate("CREATE TABLE IF NOT EXISTS order_expirations(order_index INTEGER PRIMARY KEY, order_hash TEXT UNIQUE, source TEXT, block_index INTEGER)");
			db.executeUpdate("CREATE INDEX IF NOT EXISTS block_index_idx ON order_expirations (block_index)");

			// Order Match Expirations
			db.executeUpdate("CREATE TABLE IF NOT EXISTS order_match_expirations(order_match_id TEXT PRIMARY KEY, tx0_address TEXT, tx1_address TEXT, block_index INTEGER)");
			db.executeUpdate("CREATE INDEX IF NOT EXISTS block_index_idx ON order_match_expirations (block_index)");

			// Messages
			db.executeUpdate("CREATE TABLE IF NOT EXISTS messages(message_index INTEGER PRIMARY KEY, block_index INTEGER, command TEXT, category TEXT, bindings TEXT)");
			db.executeUpdate("CREATE INDEX IF NOT EXISTS block_index_idx ON messages (block_index)");
			
			updateMinorVersion();
		} catch (Exception e) {
			logger.error(e.toString());
			System.exit(0);			
		}
	}
	
	public Integer getDBMinorVersion() {
		Database db = Database.getInstance();
		ResultSet rs = db.executeQuery("PRAGMA user_version;");
		try {
			while(rs.next()) {
			    return rs.getInt("user_version");
			}
		} catch (SQLException e) {
		}	
		return 0;
	}

	public void updateMinorVersion() {
		// Update minor version
		Database db = Database.getInstance();
		db.executeUpdate("PRAGMA user_version = "+Config.minorVersionDB.toString());
	}
	
	public Integer getHeight() {
		try {
			Integer height = blockStore.getChainHead().getHeight();
			return height;
		} catch (BlockStoreException e) {
		}
		return 0;
	}

	public String importPrivateKey(String privateKey) {
		DumpedPrivateKey dumpedPrivateKey;
		try {
			dumpedPrivateKey = new DumpedPrivateKey(params, privateKey);
			ECKey key = dumpedPrivateKey.getKey();
			String address = key.toAddress(params).toString();
			logger.info("Importing address "+address);
			wallet.addKey(key);
			//recreateBlockchainDatabase();
			List<Map.Entry<String,String>> txsInfo = Util.infoGetTransactions(address);
			BigInteger balance = BigInteger.ZERO;
			BigInteger balanceSent = BigInteger.ZERO;
			BigInteger balanceReceived = BigInteger.ZERO;
			Integer transactionCount = 0;
			for (Map.Entry<String,String> txHashBlockHash : txsInfo) {
				String txHash = txHashBlockHash.getKey();
				String blockHash = txHashBlockHash.getValue();
				try {
					Block block = peerGroup.getDownloadPeer().getBlock(new Sha256Hash(blockHash)).get();
					List<Transaction> txs = block.getTransactions();
					for (Transaction tx : txs) {
						if (tx.getHashAsString().equals(txHash)){// && wallet.isPendingTransactionRelevant(tx)) {
							transactionCount ++;
							wallet.receivePending(tx, peerGroup.getDownloadPeer().downloadDependencies(tx).get());
							balanceReceived = balanceReceived.add(tx.getValueSentToMe(wallet));
							balanceSent = balanceSent.add(tx.getValueSentFromMe(wallet));
							balance = balance.add(tx.getValueSentToMe(wallet));
							balance = balance.subtract(tx.getValueSentFromMe(wallet));
						}
					}
				} catch (InterruptedException e) {
				} catch (ExecutionException e) {				
				}
			}	
			return address;
		} catch (AddressFormatException e) {
		}
		return null;
	}
	
	public Transaction transaction(String source, String destination, BigInteger btcAmount, BigInteger fee, String dataString) {
		Transaction tx = new Transaction(params);
		LinkedList<TransactionOutput> unspentOutputs = wallet.calculateAllSpendCandidates(true);
		if (destination.equals("") || btcAmount.compareTo(BigInteger.valueOf(Config.dustSize))>=0) {
			
			byte[] data = null;
			List<Byte> dataArrayList = new ArrayList<Byte>();
			try {
				data = dataString.getBytes("ISO-8859-1");
				dataArrayList = Util.toByteArrayList(data);
			} catch (UnsupportedEncodingException e) {
			}

			BigInteger totalOutput = fee;
			BigInteger totalInput = BigInteger.ZERO;

			try {
				if (!destination.equals("") && btcAmount.compareTo(BigInteger.ZERO)>0) {
					totalOutput = totalOutput.add(btcAmount);
					tx.addOutput(btcAmount, new Address(params, destination));
				}
			} catch (AddressFormatException e) {
			}

			for (int i = 0; i < dataArrayList.size(); i+=32) {
				List<Byte> chunk = new ArrayList<Byte>(dataArrayList.subList(i, Math.min(i+32, dataArrayList.size())));
				chunk.add(0, (byte) chunk.size());
				while (chunk.size()<32+1) {
					chunk.add((byte) 0);
				}
				List<ECKey> keys = new ArrayList<ECKey>();
				for (ECKey key : wallet.getKeys()) {
					try {
						if (key.toAddress(params).equals(new Address(params, source))) {
							keys.add(key);
							break;
						}
					} catch (AddressFormatException e) {
					}
				}
				keys.add(new ECKey(null, Util.toByteArray(chunk)));
				Script script = ScriptBuilder.createMultiSigOutputScript(1, keys);
				tx.addOutput(BigInteger.valueOf(Config.dustSize), script);
				totalOutput = totalOutput.add(BigInteger.valueOf(Config.dustSize));
			}
			
			for (TransactionOutput out : unspentOutputs) {
				Script script = out.getScriptPubKey();
				Address address = script.getToAddress(params);
				if (address.toString().equals(source)) {
					if (totalOutput.compareTo(totalInput)>0) {
						totalInput = totalInput.add(out.getValue());
						tx.addInput(out);
					}
				}
			}
			if (totalInput.compareTo(totalOutput)<0) {
				return null; //not enough inputs
			}
			BigInteger totalChange = totalInput.subtract(totalOutput);
				
			try {
				if (totalChange.compareTo(BigInteger.ZERO)>0) {
					tx.addOutput(totalChange, new Address(params, source));
				}
			} catch (AddressFormatException e) {
			}
		}
		return tx;
	}
	
	public Boolean sendTransaction(Transaction tx) {
		try {
			tx.signInputs(SigHash.ALL, wallet);
			//System.out.println(tx);
			//blocks.wallet.commitTx(txBet);
			peerGroup.broadcastTransaction(tx).get(60, TimeUnit.SECONDS);
			return true;
			/*
			byte[] rawTxBytes = tx.bitcoinSerialize();
			String rawTx = new BigInteger(1, rawTxBytes).toString(16);
			rawTx = "0" + rawTx;
			System.out.println(rawTx);
			*/
		} catch (Exception e) {
			logger.error(e.toString());
			return false;
		}		
	}
}