import java.math.BigInteger;
import java.util.List;

public class UnspentOutput {
    public Double amount;
    public String txid;
    public Integer vout;
    public String type;
    public Integer confirmations;
    public ScriptPubKey scriptPubKey;
    
    public class ScriptPubKey {
    	public String asm;
    	public String hex;
    }
}
