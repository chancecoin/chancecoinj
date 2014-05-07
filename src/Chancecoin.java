//TODO: encrypt wallet
//TODO: easier upgrade process
//TODO: finish RPC calls
//TODO: better splash screen message so user knows what's happening
//TODO: make it so when you make a transaction, it automatically shows the pending transaction like blockchain.info
//TODO: better error messages for transactions
//TODO: make transactions keep retrying
//TODO: more information on fees
//TODO: every address should be a link to a chancecoin wallet
//TODO: chancecoin wallet should list all types of transactions maybe?
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