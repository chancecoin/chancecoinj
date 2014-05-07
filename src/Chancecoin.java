//TODO: encrypt wallet
//TODO: easier upgrade process
//TODO: finish RPC calls
//TODO: better splash screen message so user knows what's happening
//TODO: make it so when you make a transaction, it automatically shows the pending transaction like blockchain.info
//TODO: better error messages for transactions
public class Chancecoin {
	public static void main(String[] args) {
		Blocks blocks = Blocks.getInstance();
		JsonRpcServletEngine engine = new JsonRpcServletEngine();
		try {
			engine.startup();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}