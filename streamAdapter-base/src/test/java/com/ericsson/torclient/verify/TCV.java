package com.ericsson.torclient.verify;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


/* TorClient Verify */
public class TCV {
	private String filename = null;
	private String dirname = null;
	private int verbose = 1; // 0 - silent, higher = noisier
	private boolean isCTUM = true;
	
	
	public static void main(String[] args) {
		
		TCV tcv = new TCV();
		if (args.length == 0) {
			//String argv[] = {"me", "-f", "C:/Users/esmipau/TORClient/torclient-base/Out/ctrs/A20131030.1631-1632_10.45.232.105_celltrace_3-855.bin.gz", "-m", "CTRS", "-v", "1"};
			//String argv[] = {"me", "-d", "C:/temp/ctum/ctrs", "-m", "CTRS", "-v", "2"};
			//String argv[] = {"me", "-d", "C:/temp/ctum/ctum", "-m", "CTUM", "-v", "2"};
			//String argv[] = {"me", "-d", "C:/Users/esmipau/TORClient/torclient-base/Out/ctum/", "-m", "CTUM", "-v", "1"};
			String argv[] = {"me", "-d", "C:/Users/esmipau/TORClient/torclient-base/Out/ctrs/", "-m", "CTRS", "-v", "1"};
			args = argv;
		} 
		if (tcv.init(args))
			tcv.go();			

	}

	
	public TCV () {
		
	}
	
	public boolean init(String[] args) {
		File file;
		for (int i = 0; i < (args.length -1); i++) {
			if (args[i] == "-f") {
				filename = args[i+1];
				file = new File(filename);
				if (!file.exists() || !file.isFile()) {
					System.out.println("Error! Specified file doesn't exist or is not a file :"+file.getAbsolutePath());
					return false;
				}
			} else if (args[i] == "-d") {
				dirname = args[i+1];
				file = new File(dirname);
				if (!file.exists() || !file.isDirectory()) {
					System.out.println("Error! Specified directory doesn't exist or is not a directory :"+file.getAbsolutePath());
					return false;
				}
				
			} else if (args[i] == "-v") {
				verbose = Integer.parseInt(args[i+1]);
			} else if (args[i] == "-m") {
				isCTUM = ("CTUM".equals(args[i+1]));
			}
			
		}
		if (verbose > 0 ) {
			System.out.println("Starting with filename = "+filename+", dirname = "+dirname+", mode = "+(isCTUM?"CTUM":"CTRS")+", verbose = "+verbose);
		}
		return true;
	}
	
	private void go() {
		// user has specified file
		long recordCnt = 0;
		int fileCnt = 0;
		if (filename != null) {
			try {
				if (isCTUM) {
					recordCnt += parseCTUM(filename);
				} else {
					recordCnt += parseCTRS(filename);
				}
				fileCnt ++;
			} catch (Exception e) {
				if (verbose > 1)
					System.out.println("Failed to parse "+filename);				
			}
		}
		// user has specified a directory
		if (dirname != null) {
			File dir = new File(dirname);
			for (File file:dir.listFiles() ) {
				if (file.length() == 0) {
					if (verbose > 1)
						System.out.println("Empty file! "+file.getAbsolutePath());
				} else {
					try {
						if (isCTUM) {
							recordCnt += parseCTUM(file.getAbsolutePath());
						} else {
							recordCnt += parseCTRS(file.getAbsolutePath());
						}
						fileCnt ++;
					} catch (Exception e) {
						if (verbose > 1)
							System.out.println("Failed to parse "+filename);				
					}
				}
			}
			
		}
		if (verbose > 0) {
			System.out.println(fileCnt+" files processed, "+recordCnt+" records counted");
		}

	}
	
	public int parseCTRS(String filename) throws Exception {
		Reader r = new Reader(filename);
		byte [] recordB = r.getRecord();  // try and get header
		int [] typeCounts = new int [10];
		
		int type = ((recordB[0] & 0x00ff) << 8) | (recordB[1] & 0x00ff);
		if (type != 0) { // header
			if (verbose > 1)
				System.out.println("Invalid Header found! type is "+ type+", expected it to be 0.  "+bytesToHex(recordB));
			return 0;
		}
		typeCounts[(type%10)] ++;
		if (verbose > 2)
			decodeCTRS(recordB);
		int recCnt = -1; // subtract header and footer from count
		while ( type != 5 && ( recordB = r.getRecord()) != null ){ // type 5 is footer
			type = ((recordB[0] & 0x00ff) << 8) | (recordB[1] & 0x00ff);
			 
			if (verbose > 3)
				System.out.println(" Got record of type "+type+" and len "+recordB.length);
			if (verbose > 2)
				decodeCTRS(recordB);
			
			typeCounts[(type%10)] ++;
			recCnt++;
		}
		if (type != 5) {
			if (verbose > 2)
				System.out.println("Invalid Footer found or early EOF. Got "+ type+", expected type = 5.");
		}
		r.close();
		if (verbose > 1) {
			System.out.println("Record Count for file "+filename);
			System.out.println(" type   ");
			for (int i = 0; i < 10; i++) {
				if (typeCounts[i] != 0)
					System.out.println("   "+i+"    "+typeCounts[i]);
			}
			if (!unrecognisedEventIds.isEmpty())
				System.out.println("\n List of Event ID's that we don't recognise but tried to process ");
			for (Integer eid:unrecognisedEventIds) {
				System.out.println("Event ID :"+eid);
			}
			if (!unknownEventIds.isEmpty())
				System.out.println("\n List of Event ID's that we don't recognise and could not process ");
			for (Integer eid:unknownEventIds) {
				System.out.println("Event ID :"+eid);
			}
			if (verbose > 2)
				System.out.println(" header hour and min"+headerHour+":"+headerMin);
		}
		return recCnt;
	}

	public int parseCTUM(String filename) throws Exception {
		Reader r = new Reader(filename);
		byte [] recordB = r.getRecord();
		int [] typeCounts = new int [10];
		int type = (recordB[0] & 0x00ff) ;
		if (type != 0) {
			if (verbose > 1)
				System.out.println("Invalid Header found! type is "+ type+", expected it to be 0.  "+bytesToHex(recordB));
			return 0;
		}
		typeCounts[(type%10)] ++;
		if (verbose > 2)
			decodeCTUM(recordB);
		int recCnt = -1; // allow for header and footer
		while ( type != 3 && (recordB = r.getRecord()) != null ){
			if (verbose > 2) {
				System.out.println(" Got record of type "+type+" and len "+recordB.length);
				decodeCTUM(recordB);
			}			
			type = (recordB[0] & 0x00ff) ;
			typeCounts[(type%10)] ++;
			recCnt++;
		}
		if (type != 3) { // footer record
			if (verbose > 1)
				System.out.println("No Footer found! ");
		}
		
		r.close();
		
		if (verbose > 1) {
			System.out.println("Record Count for file "+filename);
			System.out.println(" type   ");
			for (int i = 0; i < 10; i++) {
				if (typeCounts[i] != 0)
					System.out.println("   "+i+"    "+typeCounts[i]);
			}
		}
		return recCnt;
	}

	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
	private static String bytesToHex(byte[] bytes) {
		return bytesToHex(bytes, 0, bytes.length);
	}
	public static String bytesToHex(byte[] bytes, int offset, int length) {
		int len = (length > 8 || bytes.length > 8) ? 8 : length; 
	    char[] hexChars = new char[len * 2];
	    int v;
	    for ( int j = 0; j < len; j++ ) {
	        v = bytes[offset+j] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    return new String(hexChars);
	}

	private void decodeCTUM(byte [] byteB) {
		int type = (int)byteB[0];
		System.out.println("Type = "+type);
		if (type == 0) {			
			System.out.println("Header ffv "+byteB[1]+" fiv "+byteB[2] + " bytes "+bytesToHex(byteB) + " len "+byteB.length);
		} else if (type == 1) {			
			System.out.print("Event hh="+byteB[1]+" mm "+byteB[2]+" ss "+byteB[3]+" millisec "+printBits(byteB, 4, 0, 10));
			int byteCnt = 5;
			int bitCnt = 3;
			if (isBitSet(byteB[byteCnt], bitCnt)) {
				if (verbose > 3)
					System.out.print(" macro_enodeB is "+printBits(byteB, byteCnt, bitCnt+1, 20));
				byteCnt +=2;
				bitCnt += 4; 
			} else {
				//System.out.print(" macro_enodeB not present ");
				bitCnt += 1; 
			}					
			if (isBitSet(byteB[byteCnt], bitCnt)) {
				if (verbose > 3)
					System.out.print(" home_enodeB is "+printBits(byteB, byteCnt, bitCnt+1, 28));
				byteCnt +=3;
				bitCnt += 4; 
			} else {
				//System.out.print(" home_enodeB not present ");
				bitCnt += 1; 
			}	
			if (verbose > 3)
				System.out.println(" IMSI (first 24) is "+printBits(byteB, byteCnt, bitCnt, 64) + " byte is "+byteCnt+" bit "+bitCnt);			
		} else {
			System.out.println("Other "+ bytesToHex(byteB) + " len "+byteB.length);
			
		} 
	}
	
	private List <Integer>unknownEventIds = new ArrayList<Integer>();
	private List <Integer>unrecognisedEventIds = new ArrayList<Integer>();
	private int headerHour = 0; 
	private int headerMin = 0; 
	private void decodeCTRS(byte [] byteB) {
		int type = (int)((byteB[0] &0x00ff) << 8) + ( byteB[1] & 0x00ff);
		//System.out.print("Type = "+type);
		if (type == 0) {
			StringBuffer ffv = new StringBuffer();
			StringBuffer fiv = new StringBuffer();
			for (int i = 0; i < 5; i++ ) {
				ffv.append((char)byteB[2+i]);
				fiv.append((char)byteB[7+i]);
			}
			int year = (int)((byteB[12] &0x00ff) << 8) + ( byteB[13] & 0x00ff);
			headerHour = (int)(byteB[16] &0x00ff);
			headerMin = (int)(byteB[17] &0x00ff);
			
			System.out.println(" Header ffv "+ffv.toString()+" fiv "+fiv.toString() + " year " +year );
		} else if (type == 3) {
			int scannerId = (int)((byteB[7] &0x00ff) << 16) + ((byteB[8] &0x00ff) << 8) + ( byteB[9] & 0x00ff);
			System.out.println(" Scanner Header ID "+scannerId +" status "+byteB[10]);
		} else if (type == 4) {	
			int eid = (int) ((byteB[4] & 0x00ff) + ((byteB[3] & 0x00ff) << 8) + ((byteB[2]& 0x00ff) << 16));
			int hour = (int) (byteB[5] & 0x00ff); 
			int min = (int) (byteB[6] & 0x00ff);
			switch (eid) {
			default:
				// We don't recognise the eventID, but if the hour matchs the header and the minute is less then 6 minutes ahead of the header, 
				// then we can try to decode it anyway
				if (hour != headerHour || byteB.length <= 26 || (min - headerMin) > 5) {
					System.out.println(" Unknown Event ID! "+ eid + " hh:mm "+hour+":"+min+" len "+byteB.length);
					if (!unknownEventIds.contains(eid)) {
							unknownEventIds.add(eid);
					}
					break;
				}
				//System.out.println(" Unrecognised Event ID, but we think we can handle it! "+ eid);
				if (!unrecognisedEventIds.contains(eid)) {
					unrecognisedEventIds.add(eid);
				}
					// deliberate fall through into case below
			case 3076 :
			case 3077 :
			case 3081 :
			case 4099 :
			case 4114 :
			case 5135 :
			case 5193 :
			case 4105 :
			case 4104 :
			case 4113 :
			case 4112 :
			case 4103 :
			case 4102 :
			case 4111 :
			case 4110 :
			case 4106 :
			case 4097 :
			case 4100 :
			case 4098 :	
				if (verbose > 2) {
					int scannerIDEvt = (int) (((byteB[11] & 0x00ff) << 16) + ((byteB[12] & 0x00ff) << 8) + ((byteB[13]& 0x00ff) ));
					int rbsMod = (int) (byteB[14] & 0x00ff);
					long  cellId = (long) (((byteB[15] & 0x00ff) << 24) + ((byteB[16] & 0x00ff) << 16) + ((byteB[17] & 0x00ff) << 8) + ((byteB[18]& 0x00ff) ));
					int enbs1 = (int) (((byteB[19] & 0x00ff) << 16) + ((byteB[20] & 0x00ff) << 8) + ((byteB[21]& 0x00ff) ));
					long  mmes1 = (long) (((byteB[22] & 0x00ff) << 24) + ((byteB[23] & 0x00ff) << 16) + ((byteB[24] & 0x00ff) << 8) + ((byteB[25]& 0x00ff) ));
					int isGummei = (int) (byteB[26] & 0x00ff);
					System.out.print(" Event id="+eid+" hh:mm "+hour+":"+min);
					System.out.println(" Scanner Evt "+scannerIDEvt+" rbsMod "+rbsMod+" cellID "+cellId+" enbs1 "+enbs1+" mmes1 "+mmes1+" isGummiValid "+isGummei+" "+bytesToHex(byteB, 27, 6));
				}
				break;
			}
		} else if (type == 5) {			
			System.out.println(" Footer "+ bytesToHex(byteB));
		} else {
			System.out.println(" Other "+ bytesToHex(byteB));
			
		} 
	}

	private String printBits(byte [] byteB, int byteOffset, int bitOffset, int numBits) {
		StringBuilder res = new StringBuilder();
		int bitNum = bitOffset;
		
		for (int i = 0; i < numBits; i++) {
			if (bitNum >= 8) {
				bitNum = 0;
				byteOffset ++; // move on to next byte
			}
			res.append((isBitSet(byteB[byteOffset], bitNum)?"|":"."));
			bitNum++;
		}
		return res.toString();
	}
	private static Boolean isBitSet(byte b, int bit)
	{
	    return (b & (1 << bit)) != 0;
	}
}


