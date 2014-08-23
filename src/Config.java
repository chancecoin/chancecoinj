import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;


public class Config {
	//name
	public static String appName = "Chancecoin";
	public static String burnAddress = "1ChancecoinXXXXXXXXXXXXXXXXXZELUFD";
	public static Boolean testNet = false;
	public static String prefix = "CHANCECO";
	public static String log = appName+".log";
	public static String minVersionPage = "https://raw2.github.com/chancecoin/chancecoinj/master/min_version.txt";
	public static String minVersionPage2 = "http://chancecoin.com/downloads/min_version.txt";
	public static String dbPath = "resources/db/";
	public static String downloadUrl = "http://chancecoin.com/downloads/";
	public static String downloadZipUrl = "https://raw2.github.com/chancecoin/chancecoinj/master/download.txt";
	public static String downloadZipUrl2 = "http://chancecoin.com/downloads/download.txt";
	public static Integer RPCPort = 54121;
	public static String RPCUsername = "";
	public static String RPCPassword = "";
	public static Integer serverPort = 8080;
	public static String ConfigFile = "./resources/chancecoin.conf";
	public static String customConfigFile = "./resources/custom.conf";
	public static Boolean readOnly = false;
	public static String donationAddress = "1BckY64TE6VrjVcGMizYBE7gt22axnq6CM";
	public static Integer redownloadDatabase = 144;
	public static Double burnPrice = 0.001;
	
	//version
	public static Integer majorVersion = 2;
	public static Integer minorVersion = 8;
	public static String version = Integer.toString(majorVersion)+"."+Integer.toString(minorVersion);
	public static Integer majorVersionDB = 1;
	public static Integer minorVersionDB = 2;
	public static String versionDB = Integer.toString(majorVersionDB)+"."+Integer.toString(minorVersionDB);	
	
	//burn
	public static Integer firstBlock = 291860;
	public static Integer startBlock = firstBlock;
	public static Integer endBlock = startBlock+6*24*45;
	public static Integer maxBurn = 1000000;
	public static Integer multiplier = 1000;
	public static Integer multiplierInitial = 1500;
	public static long burnCreationTime = 1395508676-1;
	
	//market making address
	public static String marketMakingAddress = "1CHABTCsWGp25zjfDWjYrhejgP5V97Zv9r";
	//public static Double maxWidth = 0.05; //5%
	
	//fee address
	public static String feeAddress = "1CHACHAGuuxTr8Yo9b9SQmUGLg9X5iSeKX";
	//public static Integer feeAddressFee = 12560; //12560-780*2-10000 leaves 1000 as a fee
	public static Integer feeAddressFee = 3560; //780*2+1000+1000
	
	//casino
	public static Double houseEdge = 0.01; //was 0.02 before block 298340
	public static Double maxProfit = 0.01;
	
	//bitcoin
	public static Integer dustSize = 780;
	public static Integer minFee = 1000;
	public static Integer dataValue = 0;
	
	//protocol
	public static String txTypeFormat = ">I";
	public static Integer unit = 100000000;
	
	//etc.
	public static Integer maxExpiration = 4*2016;
	public static Integer maxInt = ((int) Math.pow(2.0,63.0))-1;
	
	//exchanges
	public static String poloniexKey = "";
	public static String poloniexSecret = "";
	
	public static void loadUserDefined() {
		FileInputStream input;
		try {
			input = new FileInputStream(ConfigFile);
			Properties prop = new Properties();
			prop.load(input);
			if (prop.getProperty("RPCUsername")!=null && prop.getProperty("RPCPassword")!=null) {
				RPCUsername = prop.getProperty("RPCUsername");
				RPCPassword = prop.getProperty("RPCPassword");				
			}
			if (prop.getProperty("serverPort")!=null) {
				serverPort = Integer.parseInt(prop.getProperty("serverPort"));
			}
			if (prop.getProperty("readOnly")!=null) {
				if (prop.getProperty("readOnly").equals("true")) {
					readOnly = true;
				}
			}
			if (prop.getProperty("redownloadDatabase")!=null) {
				redownloadDatabase = Integer.parseInt(prop.getProperty("redownloadDatabase"));
			}
			
			if ((new File(customConfigFile)).exists()) {
				input = new FileInputStream(customConfigFile);
				prop = new Properties();
				prop.load(input);
				if (prop.getProperty("poloniexKey")!=null && prop.getProperty("poloniexSecret")!=null) {
					poloniexKey = prop.getProperty("poloniexKey");
					poloniexSecret = prop.getProperty("poloniexSecret");				
				}
			}
		} catch (IOException e) {
		}		
	}
}
