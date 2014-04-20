import java.math.BigInteger;
import com.google.bitcoin.core.Transaction;


public interface JsonRpcService {
	public String getName();
	public Double getChancecoinBalance(String address);
	public Transaction sendChancecoin(String source, String destination, BigInteger amount);
}
