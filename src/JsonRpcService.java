import java.math.BigInteger;

import org.json.JSONObject;

import com.google.bitcoin.core.Transaction;


public interface JsonRpcService {
	public Double getChancecoinBalance(String address);
	public Transaction sendChancecoin(String source, String destination, BigInteger amount);
	public String getSends(String address);
	public String getReceives(String address);
}
