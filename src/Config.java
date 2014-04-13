
public class Config {
	//name
	public static String appName = "Chancecoin";
	public static String burnAddress = "1ChancecoinXXXXXXXXXXXXXXXXXZELUFD";
	public static Boolean testNet = false;
	public static String prefix = "CHANCECO";
	public static String log = appName+".log";
	
	//version
	public static Integer majorVersion = 1;
	public static Integer minorVersion = 2;
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
	
	//casino
	public static Double houseEdge = 0.02;
	public static Double maxProfit = 0.01;
	
	//bitcoin
	public static Integer dustSize = 5430*2;
	public static Integer minFee = 10000;
	public static Integer dataValue = 0;
	
	//protocol
	public static String txTypeFormat = ">I";
	public static Integer unit = 100000000;
	
	//etc.
	public static Integer twoWeeks = 2*7*24*3600;
	public static Integer maxExpiration = 4*2016;
	public static Integer maxInt = ((int) Math.pow(2.0,63.0))-1;
	public static Double feeRequiredDefault = 0.01;
	public static Double feeProvidedDefault = 0.01;
}
