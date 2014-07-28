import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;


public class Poloniex {
	private static final String API_URL = "https://poloniex.com/tradingApi";
	private static final String USER_AGENT = "Mozilla/5.0 (compatible; BTCE-API/1.0; MSIE 6.0 compatible; +https://github.com/abwaters/btce-api)" ;
	
	private static long auth_last_request = 0 ;
	private static long auth_request_limit = 1000 ;	// request limit in milliseconds
	private static long last_request = 0 ;
	private static long request_limit = 15000 ;	// request limit in milliseconds for non-auth calls...defaults to 15 seconds
	private boolean initialized = false;	
	private Mac mac ;
	
	private static Poloniex instance = null;

	private Poloniex() {}
	
	public static Poloniex getInstance() {
		if(instance == null) {
			instance = new Poloniex();
		}
		return instance;
	}

	public void setAuthKeys() throws Exception {
		SecretKeySpec keyspec = null ;
		try {
			keyspec = new SecretKeySpec(Config.poloniexSecret.getBytes("UTF-8"), "HmacSHA512") ;
		} catch (UnsupportedEncodingException uee) {
			throw new Exception("HMAC-SHA512 doesn't seem to be installed",uee) ;
		}

		try {
			mac = Mac.getInstance("HmacSHA512") ;
		} catch (NoSuchAlgorithmException nsae) {
			throw new Exception("HMAC-SHA512 doesn't seem to be installed",nsae) ;
		}

		try {
			mac.init(keyspec) ;
		} catch (InvalidKeyException ike) {
			throw new Exception("Invalid key for signing request",ike) ;
		}
		initialized = true ;
	}
	

	private final void preAuth() {
		long elapsed = System.currentTimeMillis()-auth_last_request ;
		if( elapsed < auth_request_limit ) {
			try {
				Thread.currentThread().sleep(auth_request_limit-elapsed) ;
			} catch (InterruptedException e) {

			}
		}
		auth_last_request = System.currentTimeMillis() ;
	}
	
	private String toHex(byte[] b) throws UnsupportedEncodingException {
	    return String.format("%040x", new BigInteger(1,b));
	}
	
	private final String authrequest(String method, Map<String,String> args) throws Exception {
		setAuthKeys();
		if(!initialized) {
			throw new Exception("Poloniex connection not initialized.") ;
		}
		
		// prep the call
		preAuth() ;

		// add method and nonce to args
		if (args == null) {
			args = new HashMap<String,String>() ;
		}
		long nonce = System.currentTimeMillis();
		args.put("method", method) ;
		args.put("nonce",Long.toString(nonce)) ;
		

		// create url form encoded post data
		String postData = "" ;
		for (Iterator<String> iter = args.keySet().iterator(); iter.hasNext();) {
			String arg = iter.next() ;
			if (postData.length() > 0) {
				postData += "&" ;
			}
			postData += arg + "=" + URLEncoder.encode(args.get(arg)) ;
		}

		// create connection
		URLConnection conn = null ;
		StringBuffer response = new StringBuffer() ;
		try {
			URL url = new URL(API_URL);
			conn = url.openConnection() ;
			conn.setUseCaches(false) ;
			conn.setDoOutput(true) ;
			conn.setRequestProperty("Key",Config.poloniexKey) ;
			conn.setRequestProperty("Sign",toHex(mac.doFinal(postData.getBytes("UTF-8")))) ;
			conn.setRequestProperty("Content-Type","application/x-www-form-urlencoded") ;
			conn.setRequestProperty("User-Agent",USER_AGENT) ;

			// write post data
			OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
			out.write(postData) ;
			out.close() ;

			// read response
			BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String line = null ;
			while ((line = in.readLine()) != null)
				response.append(line) ;
			in.close() ;
		} catch (MalformedURLException e) {
			throw new Exception("Internal error.",e) ;
		} catch (IOException e) {
			throw new Exception("Error connecting to BTC-E.",e) ;
		}
		System.out.println(response.toString());
		return response.toString() ;
	}
	
	public void buy(Double rate, Double amount) {
		HashMap<String, String> args = new HashMap<String, String>();
		args.put("command", "buy");
		args.put("currencyPair", "BTC_CHA");
		args.put("rate", rate.toString());
		args.put("amount", amount.toString());
		try {
			authrequest("buy", args);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return;
	}
	
	public void sell(Double rate, Double amount) {
		HashMap<String, String> args = new HashMap<String, String>();
		args.put("command", "sell");
		args.put("currencyPair", "BTC_CHA");
		args.put("rate", rate.toString());
		args.put("amount", amount.toString());
		try {
			authrequest("sell", args);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return;
	}

	public void cancelOrder(String orderNumber) {
		HashMap<String, String> args = new HashMap<String, String>();
		args.put("command", "cancelOrder");
		args.put("currencyPair", "BTC_CHA");
		args.put("orderNumber", orderNumber);
		
		try {
			authrequest("cancelOrder", args);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return;
	}
	
	public void getBalance() {
		HashMap<String, String> args = new HashMap<String, String>();
		args.put("command", "returnBalances");
		
		try {
			authrequest("returnBalances", args);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return;
	}
	
	public void getOpenOrders() {
		HashMap<String, String> args = new HashMap<String, String>();
		args.put("command", "returnOpenOrders");
		args.put("currencyPair", "BTC_CHA");
		
		try {
			authrequest("returnOpenOrders", args);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return;
	}	
	
	public void getTradeHistory() {
		HashMap<String, String> args = new HashMap<String, String>();
		args.put("command", "returnTradeHistory");
		args.put("currencyPair", "BTC_CHA");
		
		try {
			authrequest("returnTradeHistory", args);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return;
	}	

	public static void main(String args[]) {
		Config.loadUserDefined();
		Poloniex polo = Poloniex.getInstance();
		//polo.buy(0.00052, 0.2);
		polo.getBalance();
		System.out.println(Config.poloniexKey);
		System.out.println(Config.poloniexSecret);
		//polo.cancelOrder("13589199");
	}
}
