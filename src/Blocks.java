import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
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

public class Blocks {
	static NetworkParameters params;
	static Logger logger = LoggerFactory.getLogger(Blocks.class);
	private static Blocks instance = null;
	static Wallet wallet;
	static PeerGroup peerGroup;
	static BlockChain blockChain;
	static BlockStore blockStore;
	
	public static Blocks getInstance() {
		if(instance == null) {
			instance = new Blocks();
			instance.follow();
		}
		return instance;
	}

	private Blocks() {

	}

	public void follow() {
		params = MainNetParams.get();
		try {
			Integer lastBlock = Util.getLastBlock();
			if (lastBlock == 0) {
				lastBlock = Config.firstBlock - 1;
			}
			Integer nextBlock = lastBlock + 1;
			Address burnAddress = new Address(params, Config.burnAddress);		    

			if ((new File("wallet")).exists()) {
				logger.info("Found wallet file");
				wallet = Wallet.loadFromFile(new File("wallet"));
			} else {
				wallet = new Wallet(params);
				ECKey newKey = new ECKey();
				newKey.setCreationTimeSeconds(Config.burnCreationTime);
				wallet.addKey(newKey);
				wallet.addWatchedAddress(burnAddress, Config.burnCreationTime);
			}
			blockStore = new H2FullPrunedBlockStore(params, Config.appName.toLowerCase(), 1000);
			blockChain = new BlockChain(params, wallet, blockStore);
			peerGroup = new PeerGroup(params, blockChain);
			peerGroup.addWallet(wallet);
			peerGroup.setFastCatchupTimeSecs(Config.burnCreationTime);
			//peerGroup.recalculateFastCatchupAndFilter(FilterRecalculateMode.FORCE_SEND);
			wallet.autosaveToFile(new File("wallet"), 1, TimeUnit.MINUTES, null);
			peerGroup.addPeerDiscovery(new DnsDiscovery(params));
			peerGroup.startAndWait();
			peerGroup.addEventListener(new ChancecoinPeerEventListener());
			peerGroup.downloadBlockChain();

			Block block = peerGroup.getDownloadPeer().getBlock(blockStore.getChainHead().getHeader().getHash()).get();
			Integer blockHeight = blockStore.getChainHead().getHeight();

			//traverse new blocks
			Database db = Database.getInstance();
			logger.info("Bitcoin block height: "+blockHeight);	
			logger.info("Chancecoin block height: "+lastBlock);	
			Integer blocksToScan = blockHeight - lastBlock;
			List<Sha256Hash> blockHashes = new ArrayList<Sha256Hash>();
			
			while (blockStore.get(block.getHash()).getHeight()>lastBlock) {
				blockHashes.add(block.getHash());
				block = blockStore.get(block.getPrevBlockHash()).getHeader();
			}
			
			for (int i = blockHashes.size()-1; i>=0; i--) { //traverse blocks in reverse order
				block = peerGroup.getDownloadPeer().getBlock(block.getPrevBlockHash()).get();
				blockHeight = blockStore.get(block.getHash()).getHeight();
				logger.info("Catching Chancecoin up to Bitcoin (block "+blockHeight.toString()+"): "+Util.format((blockHashes.size() - i)/((double) blockHashes.size())*100.0)+"%");	
				importBlock(block, blockHeight);
			}
			
			/*
			while (blockHeight > lastBlock) {
				logger.info("Catching Chancecoin up to Bitcoin: "+Util.format((blocksToScan - (blockHeight - lastBlock))/((double) blocksToScan)*100.0)+"%");	
				importBlock(block, blockHeight);
				blockHeight -= 1;
				block = peerGroup.getDownloadPeer().getBlock(block.getPrevBlockHash()).get();
			}
			*/
			
			if (db.getDBMinorVersion()<Config.minorVersionDB){
				reparse();
				db.updateMinorVersion();		    	
			}else{
				//reparse(); 
				parseFrom(nextBlock);
			}
			Bet.resolve();
			Order.expire();
			
		} catch (Exception e) {
			logger.error(e.toString());
			System.exit(0);
		}
	}

	/*
	public void reDownloadBlockTransactions(Integer blockHeight) {
		Database db = Database.getInstance();
		ResultSet rs = db.executeQuery("select * from blocks where block_index='"+blockHeight.toString()+"';");
		try {
			if (rs.next()) {
				Block block = peerGroup.getDownloadPeer().getBlock(new Sha256Hash(rs.getString("block_hash"))).get();
				db.executeUpdate("delete from transactions where block_index='"+blockHeight.toString()+"';");
				for (Transaction tx : block.getTransactions()) {
					Blocks.getInstance().importTransaction(tx, block, blockHeight);
				}
				Bet.resolve();
				Order.expire();
			}
		} catch (Exception e) {
			
		}
	}
	*/
	
	public void importBlock(Block block, Integer blockHeight) {
		logger.info("Block height: "+blockHeight);
		Database db = Database.getInstance();
		ResultSet rs = db.executeQuery("select * from blocks where block_hash='"+block.getHashAsString()+"';");
		try {
			if (!rs.next()) {
				db.executeUpdate("INSERT INTO blocks(block_index,block_hash,block_time) VALUES('"+blockHeight.toString()+"','"+block.getHashAsString()+"','"+block.getTimeSeconds()+"')");
			}
			for (Transaction tx : block.getTransactions()) {
				Blocks.getInstance().importTransaction(tx, block, blockHeight);
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
						db.executeUpdate("INSERT INTO transactions(tx_index, tx_hash, block_index, block_time, source, destination, btc_amount, fee, data) VALUES('"+(Util.getLastTxIndex()+1)+"','"+tx.getHashAsString()+"','"+blockHeight+"','"+block.getTimeSeconds()+"','"+source+"','"+destination+"','"+btcAmount.toString()+"','"+fee.toString()+"','"+dataString+"')");
					}else{
						db.executeUpdate("INSERT INTO transactions(tx_index, tx_hash, block_index, block_time, source, destination, btc_amount, fee, data) VALUES('"+(Util.getLastTxIndex()+1)+"','"+tx.getHashAsString()+"','"+blockHeight+"','','"+source+"','"+destination+"','"+btcAmount.toString()+"','"+fee.toString()+"','"+dataString+"')");						
					}
				}
			} catch (SQLException e) {
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
		Blocks.getInstance().parseFrom(0);
	}

	public void parseFrom(Integer blockNumber) {
		Database db = Database.getInstance();
		ResultSet rs = db.executeQuery("select * from blocks where block_index>="+blockNumber.toString()+" order by block_index asc;");
		try {
			while (rs.next()) {
				Integer blockIndex = rs.getInt("block_index");
				ResultSet rsTx = db.executeQuery("select * from transactions where block_index="+blockIndex.toString()+" order by tx_index asc;");
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
							//Cancel.parse(txIndex, message);
						} else if (messageType.get(3)==BTCPay.id.byteValue()) {
							//BTCPay.parse(txIndex, message);
						}						
					}
				}
			}
			Bet.resolve();
			Order.expire();
		} catch (SQLException e) {
			logger.error(e.toString());
		} catch (UnsupportedEncodingException e) {
		}
	}
	
	public void importPrivateKey(String privateKey) {
		DumpedPrivateKey dumpedPrivateKey;
		try {
			dumpedPrivateKey = new DumpedPrivateKey(params, privateKey);
			ECKey key = dumpedPrivateKey.getKey();
			logger.info("Importing address "+key.toAddress(params).toString());
			wallet.addKey(key);
		} catch (AddressFormatException e) {
		}
	}
	
	public Transaction transaction(String source, String destination, BigInteger btcAmount, BigInteger fee, String dataString) {
		Transaction tx = new Transaction(params);
		LinkedList<TransactionOutput> unspentOutputs = wallet.calculateAllSpendCandidates(true);
		if (btcAmount.compareTo(BigInteger.valueOf(Config.dustSize))>=0) {
			
			byte[] data = null;
			List<Byte> dataArrayList = new ArrayList<Byte>();
			try {
				data = dataString.getBytes("ISO-8859-1");
				dataArrayList = Util.toByteArrayList(data);
			} catch (UnsupportedEncodingException e) {
			}

			BigInteger totalOutput = BigInteger.ZERO;
			BigInteger totalInput = BigInteger.ZERO;

			for (int i = 0; i < dataArrayList.size(); i+=32) {
				totalOutput = totalOutput.add(BigInteger.valueOf(Config.dustSize));
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
			
			if (!destination.equals("") && btcAmount.compareTo(BigInteger.ZERO)>0) {
				totalOutput = totalOutput.add(btcAmount);
			}
			
			for (TransactionOutput out : unspentOutputs) {
				Script script = out.getScriptPubKey();
				Address address = script.getToAddress(params);
				if (address.toString().equals(source)) {
					if (totalOutput.compareTo(totalInput)>0) {
						totalInput.add(out.getValue());
						tx.addInput(out);
					}
				}
			}
			if (totalInput.compareTo(totalOutput)<0) {
				//return null; //not enough inputs
			}
			BigInteger totalChange = totalInput.subtract(totalOutput);
				
			try {
				if (!destination.equals("") && btcAmount.compareTo(BigInteger.ZERO)>0) {
					tx.addOutput(btcAmount, new Address(params, destination));
				}
				if (totalChange.compareTo(BigInteger.ZERO)>0) {
					tx.addOutput(totalChange, new Address(params, source));
				}
			} catch (AddressFormatException e) {
			}
		}
		return tx;
	}
}