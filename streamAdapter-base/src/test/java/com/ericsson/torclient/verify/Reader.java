/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2013
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.torclient.verify;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

/**
 * This class is a container for outgoing channels for events
 * for each source Id, the information about where and how to output the record is stored here
 *
 * @author esmipau
 */
public class Reader  {
    private boolean isOpen = false;
    private FileInputStream eventIn;
    private GZIPInputStream eventReader;
       
    
    public Reader( String filename) {
    	this.isOpen = false; 
        
    	File f = new File(filename);
    	if (!f.exists()) {
    		System.out.println("File not found: "+f.getAbsolutePath());
    	}
    	try {
    		eventIn = new FileInputStream(filename);
    		eventReader = new GZIPInputStream(eventIn);
        	this.isOpen = true; 
    	} catch (Exception e) {
	    	System.out.println("Open failed for: "+ f.getAbsolutePath()+" Exception "+e.getMessage()); // oops open failed
	    	return; // not much point continuing...
	    }
    	
    }


    public byte [] getRecord () throws IOException {
    	byte [] dataB = null;
//    	try {
    		byte [] lenB = new byte[2];
    		int got = eventReader.read(lenB);
    		while (got < 2) {
    			got += eventReader.read(lenB, got , 2-got);
    		}
    		int len = ((lenB[0] & 0x00ff) << 8) | (lenB[1] & 0x00ff);
    		if (len < 2) {
    			System.out.println("Bad length "+len);
    			return null;
    		}
    		len -= 2; // record len includes the two bytes that say how long it is
    		dataB = new byte[len]; 
    		got = eventReader.read(dataB, 0, len );
    		while (got  < len ) { // if we didn't get it all, try and get the rest.
    			//System.out.println("Short read! got "+got+" expected "+(len -2 )); 
        		got +=  eventReader.read(dataB, got, len - got);
    		}
    		if (got != len) {
    			System.out.println("Short read! got "+got+" expected "+(len )); 
    		}
//    	} catch (EOFException e) {    		
//    		return null; 
//    	} catch (Exception e) {
//    		e.printStackTrace();
//	    	return null; // not much point continuing...
//	    }
    	return dataB;
    }
    
    
	public void close() {
    	if (this.isOpen) {
    		try {
				eventReader.close();
	    		eventIn.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    	// TODO to what needs doing
	    	// Example of reasonable action:
	    	// Close output channel, 
	    	// if output is to a file, move file to destination.
	    	// No need to open a fresh output channel, that will be done if there is anything to write.
        	this.isOpen = false;
    	}
    }
        
}
