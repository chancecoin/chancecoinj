import java.math.BigInteger;

import org.json.JSONObject;

import com.google.bitcoin.core.Transaction;


public interface JsonRpcService {
	public String getBalance(String address);
	public String send(String source, String destination, Double amount);
	public String getSends(String address);
	public String getReceives(String address);
	public String importPrivateKey(String privateKey);
	public String importPrivKey(String privateKey);
	public void reparse();
}
