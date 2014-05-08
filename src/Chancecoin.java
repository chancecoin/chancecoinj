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