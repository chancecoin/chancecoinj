import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.bitcoin.core.Block;
import com.google.bitcoin.core.GetDataMessage;
import com.google.bitcoin.core.Message;
import com.google.bitcoin.core.Peer;
import com.google.bitcoin.core.PeerEventListener;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.store.BlockStoreException;

public class ChancecoinPeerEventListener implements PeerEventListener {
    Logger logger = LoggerFactory.getLogger(ChancecoinPeerEventListener.class);

	@Override
	public List<Message> getData(Peer peer, GetDataMessage message) {
		return null;
	}

	@Override
	public void onBlocksDownloaded(Peer peer, Block block, int blocksLeft) {
		logger.info("Block downloaded: "+blocksLeft);
		//TODO: for some reason, inside importBlock, the transaction grabbing doesn't work, so this ends up failing
		Blocks blocks = Blocks.getInstance();
		try {
			blocks.importBlock(block, blocks.blockStore.get(block.getHash()).getHeight());
		} catch (BlockStoreException e) {
		}
	}

	@Override
	public void onChainDownloadStarted(Peer peer, int blocksLeft) {
		logger.info("Chain download started: "+blocksLeft);
	}

	@Override
	public void onPeerConnected(Peer peer, int peerCount) {
		logger.info("Peer connected: "+peerCount);
	}

	@Override
	public void onPeerDisconnected(Peer peer, int peerCount) {
		logger.info("Peer disconnected: "+peerCount);
	}

	@Override
	public Message onPreMessageReceived(Peer peer, Message message) {
		return null;
	}

	@Override
	public void onTransaction(Peer peer, Transaction tx) {
		logger.info("Got transaction");		
	}
	
}