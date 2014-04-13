import java.math.BigInteger;

public class Chancecoin {
	public static void main(String[] args) {
		Blocks blocks = Blocks.getInstance();
		
		blocks.importPrivateKey("");
		blocks.transaction(Config.burnAddress, "", BigInteger.ZERO, BigInteger.ZERO, "");
	}

}