import java.math.BigInteger;

import org.json.JSONObject;

import com.google.bitcoin.core.Transaction;


public interface JsonRpcService {
	public String getChancecoinBalance(String address);
	public String sendChancecoin(String source, String destination, Double amount);
	public String getSends(String address);
	public String getReceives(String address);
}
