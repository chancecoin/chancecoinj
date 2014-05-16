import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public class Chancecoin {
	public static void main(String[] args) {
		Blocks blocks = Blocks.getInstanceSkipVersionCheck();
		blocks.init();
		//blocks.versionCheck();
		JsonRpcServletEngine engine = new JsonRpcServletEngine();
		try {
			engine.startup();
		} catch (Exception e) {
			e.printStackTrace();
		}
		blocks.follow();
		Thread blocksThread = new Thread(blocks);
		blocksThread.setDaemon(true);
		blocksThread.start(); 
		Config.loadUserDefined();
	}
}