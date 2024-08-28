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
package com.ericsson.streamAdapter.output;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.zip.GZIPOutputStream;

import com.ericsson.streamAdapter.util.StreamedRecord;
import com.ericsson.streamAdapter.util.Utils;
import com.ericsson.streamAdapter.util.config.Config;
import com.ericsson.streamAdapter.util.config.StreamAdapterConstants;
import com.ericsson.streamAdapter.util.config.StreamAdapterDefaults;

/**
 * This class is a container for outgoing channels for events for each source Id, the information about where and how to output the record is stored
 * here
 * 
 * @author esmipau
 */
public class EventCTRStoFile implements EventOutputChannel {
    // The source ID of the originating node
    private int sourceId;

    // output management
    private boolean isOpen = false;
    private File eventFilename;
    private FileOutputStream eventOutput;
    private GZIPOutputStream eventWriter;
    // rop management
    private int rop_minutes = 5;
    private int rop_length_in_seconds = 300;
    private long rop_end; // calculated when we open a file, checked when we refresh

    // output management
    private String fileName;
    private String lteesName;
    private String cfaName;
    private String ipAddress;
    private String oss;
    private String fdn;
    private String tempDir; // where the file will be written
    private String lteesDir; // where the file will be moved to when closed
    private String cfaDir; // where the file will be moved to when closed

    private boolean createSymLinks = false;
    private long rop_start;
    private ByteBuffer ctrsHeader;
    private static final DateFormat dateFormat1 = new SimpleDateFormat("yyyyMMdd.HHmm-");
    private byte[] defaultHeader = { 0x01, (byte) 0x94, 0x00, 0x00, 0x53, 0x20, 0x20, 0x20, 0x20, 0x41, 0x42, 0x33, 0x20, 0x20 };
    private static final DateFormat dateFormat2 = new SimpleDateFormat("HHmm_");
    private byte[] fixedHeader;
    private ByteBuffer footer = ByteBuffer.allocate(20); // don't make it static because other threads use these methods
    private Config config;

    public EventCTRStoFile(Config config) {
        this.config = config;
        this.rop_minutes = config.getValue(StreamAdapterConstants.ROP_MINUTES, StreamAdapterDefaults.ROP_MINUTES);
        this.rop_length_in_seconds = (rop_minutes * 60);
    }

    @Override
    public void connect(StreamedRecord record) {
        this.sourceId = record.getSourceId();
        this.ipAddress = Utils.getIP(record.getRemoteIP());
        // Is there a header in the data?
        // Is there a header in the data?
        if (record.getDataSize() > 0) {
            prepareHeader(record.getData());
        } else {
            prepareHeader(null); // use the default
        }
        setOutputDirs(config,record.getRemoteIP()); //setup Temp and Output directories for this source
        this.isOpen = false;
    }

    @Override
    public boolean write(StreamedRecord record) {
        try {
            if (!this.isOpen) {
                mkFileName();
                eventFilename = new File(fileName);
                eventOutput = new FileOutputStream(eventFilename);
                eventWriter = new GZIPOutputStream(eventOutput);
                this.isOpen = true;
                eventWriter.write(mkHeader());
            }
        } catch (Exception e) {
            System.out.println("Open failed for :" + fileName); // oops open failed
            e.printStackTrace();
            return false; // not much point continueing...
        }
        try {

            eventWriter.write(record.getData());

        } catch (Exception e1) {
            System.out.println("Write failed:" + e1.getMessage()); // oops open failed
        }
        return true;
    }

    /*
     * refresh the output channel if needed
     */
    @Override
    public void refresh(long timeNow) {
        if (isOpen) {
            if (timeNow > rop_end) { // need to do a refresh
                this.close();
            }
        }

    }

    /*
     * close the buffer, release any resources. output buffer no longer valid
     */
    @Override
    public void close() {
        if (this.isOpen) {
            try {
                eventWriter.write(mkFooter());
                eventWriter.finish(); // this does not close underlying stream
                eventOutput.flush();
                eventOutput.close();
                if (createSymLinks) {
                    Files.createSymbolicLink(Paths.get(lteesName), Paths.get(fileName)); // this doesn't work for windows
                    Files.createSymbolicLink(Paths.get(cfaName), Paths.get(fileName));
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            this.isOpen = false;
        }
    }

    private static final byte[] spaces = { 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, //16
            0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, //32
            0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, //48
            0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, //64
    };

    private void mkFileName() {
        fileName = tempDir + "/A_error_" + sourceId;
        try {
            long now = System.currentTimeMillis(); // UTC
            rop_start = now - (now % (rop_length_in_seconds * 1000));
            rop_end = rop_start + (rop_length_in_seconds * 1000);
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(rop_start);

            String tmp1 = dateFormat1.format(cal.getTime());
            cal.setTimeInMillis(rop_end);
            String tmp2 = dateFormat2.format(cal.getTime());
            fileName = tempDir + "/A" + tmp1 + tmp2 + ipAddress + "_celltrace_3-" + sourceId + ".bin.gz";
            lteesName = lteesDir + "/A" + tmp1 + tmp2 + fdn + "_celltrace_3-" + sourceId + ".bin.gz";
            cfaName = cfaDir + "_A" + tmp1 + tmp2 + ipAddress + "_celltrace_3-" + sourceId + ".bin.gz";
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /*
     * if we get a header as part of the connect message, use it to create a file header otherwise use the default. In 14A nodes, the fiv changed from
     * 5 to 13 bytes.
     */
    private void prepareHeader(byte[] header) {
        if (header != null) {
            int len = ((header[0] & 0x00ff) * 256) + (header[1] & 0x00ff);
            ctrsHeader = ByteBuffer.allocate(len);

            int fivLen = 5;
            if (header[4] > (byte) (0x0053)) { // 14A or later
                fivLen = 13;
            }
            fixedHeader = new byte[(2 + 2 + 5 + fivLen)];
            System.arraycopy(header, 0, fixedHeader, 0, 2 + 2 + 5 + fivLen);
        } else { // use defaults
            ctrsHeader = ByteBuffer.allocate(404);
            fixedHeader = defaultHeader;
        }
    }

    private static final byte[] fixedFooter = { 0x00, 0x0c, 0x00, 0x05 };

    private byte[] mkHeader() {
        ctrsHeader.clear();
        ctrsHeader.put(fixedHeader);
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(rop_start);
        ctrsHeader.putShort((short) cal.get(Calendar.YEAR));
        ctrsHeader.put((byte) cal.get(Calendar.MONTH));
        ctrsHeader.put((byte) cal.get(Calendar.DAY_OF_MONTH));
        ctrsHeader.put((byte) cal.get(Calendar.HOUR_OF_DAY));
        ctrsHeader.put((byte) cal.get(Calendar.MINUTE));
        ctrsHeader.put((byte) cal.get(Calendar.SECOND));
        ctrsHeader.put(spaces, 0, 64); // User label 128
        ctrsHeader.put(spaces, 0, 64);
        ctrsHeader.put(spaces, 0, 64); // logical name 255
        ctrsHeader.put(spaces, 0, 64);
        ctrsHeader.put(spaces, 0, 64);
        ctrsHeader.put(spaces, 0, 63);
        return ctrsHeader.array();
    }

    private byte[] mkFooter() {
        footer.clear();

        footer.put(fixedFooter);
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(rop_end);
        footer.putShort((short) cal.get(Calendar.YEAR));
        footer.put((byte) cal.get(Calendar.MONTH));
        footer.put((byte) cal.get(Calendar.DAY_OF_MONTH));
        footer.put((byte) cal.get(Calendar.HOUR_OF_DAY));
        footer.put((byte) cal.get(Calendar.MINUTE));
        footer.put((byte) cal.get(Calendar.SECOND));
        footer.put((byte) 0x00); // padding

        return footer.array();
    }

    private void setOutputDirs(Config config, byte[] ipAddressB) {
        setFDN(config);

        // TODO - find out how this should be don1!
        int dirNum = 0;
        for (int i = 0; i < ipAddressB.length; i++) {
            dirNum += ipAddressB[i];
        }
        int numDir = config.getValue(StreamAdapterConstants.NUM_DIR, StreamAdapterDefaults.NUM_DIR); // the root of the temp dir
        dirNum = (dirNum % numDir) + 1; // a number between 1 and 50

        tempDir = config.getValue(StreamAdapterConstants.TEMP_DIR, "1"); // the root of the temp dir
        if (config.getValue(StreamAdapterConstants.MKLINKS, StreamAdapterDefaults.MKLINKS) == 1) { // create symbolic links
            createSymLinks = true;
            lteesDir = config.getValue(StreamAdapterConstants.LTE_ES_DIR, StreamAdapterDefaults.LTE_ES_DIR); // the root of the temp dir
            cfaDir = config.getValue(StreamAdapterConstants.CFA_DIR, StreamAdapterDefaults.CFA_DIR); // the root of the temp dir
        }
        if (config.getValue(StreamAdapterConstants.SIMPLE, StreamAdapterDefaults.SIMPLE) != 1) { // turn off fancy output
            tempDir = tempDir + "/" + oss + "/dir" + dirNum + "/" + rop_minutes + "min/";
            lteesDir = lteesDir + "/" + oss + "/dir" + dirNum + "/" + rop_minutes + "min/";
            cfaDir = cfaDir + "/" + rop_minutes + "min/" + "/dir" + dirNum + "/" + oss;
        }

        File outDir = new File(tempDir);
        if (!outDir.exists()) {
            outDir.mkdir();
            System.out.println("Creating directory " + tempDir);
        }
    }

    // set the oss and fdn to use
    private void setFDN(Config config) {
        String ip2fdn = config.getValue(StreamAdapterConstants.IP_TO_FDN, StreamAdapterDefaults.IP_TO_FDN);
        // format is oss,ip,fdn
        BufferedReader br = null;
        String line;
        boolean found = false;

        try {
            br = new BufferedReader(new FileReader(ip2fdn));
            while ((line = br.readLine()) != null) {
                int beginIdx = line.indexOf(',');
                int endIdx = line.indexOf(',', beginIdx + 1);
                oss = line.substring(0, beginIdx);
                String ip = line.substring(beginIdx + 1, endIdx);
                if (ip.equals(ipAddress)) {
                    fdn = line.substring(endIdx + 1);
                    found = true;
                    break;
                }
            }
        } catch (Exception e) {
            ; // not really much we can do.
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (Exception e1) {
                    ; // do nothing
                }
            }
        }
        if (!found) {
            oss = "events_oss_1";
            fdn = "unknown";
        }

    }

}
