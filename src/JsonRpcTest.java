import java.net.MalformedURLException;
import java.net.URL;

import com.googlecode.jsonrpc4j.JsonRpcHttpClient;
import com.googlecode.jsonrpc4j.ProxyUtil;

public class JsonRpcTest {

	private JsonRpcServletEngine JsonRpcServletEngine;

	public void setup() throws Exception {
		JsonRpcServletEngine = new JsonRpcServletEngine();
		JsonRpcServletEngine.startup();
	}

	public void runJsonRpcHttpClient() throws MalformedURLException {
		JsonRpcHttpClient jsonRpcHttpClient = new JsonRpcHttpClient(new URL(
				"http://127.0.0.1:" + JsonRpcServletEngine.PORT + "/servlet"));
		JsonRpcService service = ProxyUtil.createClientProxy(
				JsonRpcService.class.getClassLoader(), JsonRpcService.class,
				jsonRpcHttpClient);

		
		System.out.println(service.getName());
		System.out.println(service.getChancecoinBalance("1Nbxq7n5fKife6NYeMKfSGgCqiyd84ewA8"));
		System.out.println(service.getChancecoinBalance("1Nbxq7n5fKife6NYeMKfSGgCqiyd84ewA8"));
		
	}

	public void teardown() throws Exception {
		JsonRpcServletEngine.stop();
	}
	
	public static void main(String args[]) {
		JsonRpcTest test = new JsonRpcTest();
		try {
			//test.setup();
			test.runJsonRpcHttpClient();
			//test.teardown();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}

}