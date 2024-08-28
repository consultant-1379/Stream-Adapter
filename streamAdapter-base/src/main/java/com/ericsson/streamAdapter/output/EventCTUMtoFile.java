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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
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
public class EventCTUMtoFile implements EventOutputChannel {
    private static final DateFormat dateFormat1 = new SimpleDateFormat("yyyyMMdd.HHmm-");
    private static final DateFormat dateFormat2 = new SimpleDateFormat("yyyyMMdd.HHmm");
    private static final byte[] fixedFooter = { 0x00, 0x04, 0x03, 0x00 };
    private static byte[] fixedHeader = { 0x00, 0x0c, 0x00, 0x04, 0x0c }; // record len(12),type (0), ffv(4), fiv(13) 
    // The source ID of the originating node
    private int sourceId;
    // output management
    private String ipAddress;
    private String outputDir;
    private String tempDir;
    private boolean isOpen = false;
    private File eventFilename;
    private FileOutputStream eventOutput;
    private GZIPOutputStream eventWriter;
    // rop management
    private int rop_minutes;
    private int rop_length_in_seconds = 300;
    private long rop_end; // calculated when we open a file, checked when we refresh  
    private long rop_start;
    private ByteBuffer header = ByteBuffer.allocate(12); // don't make it static because other threads use these methods
    private ByteBuffer footer = ByteBuffer.allocate(4); // don't make it static because other threads use these methods

    public EventCTUMtoFile(Config config) {
        this.rop_minutes = config.getValue(StreamAdapterConstants.ROP_MINUTES, StreamAdapterDefaults.ROP_MINUTES);
        this.rop_length_in_seconds = (rop_minutes * 60);
        setOutputDirs(config);
    }

    @Override
    public void connect(StreamedRecord record) {
        this.sourceId = record.getSourceId();
        this.ipAddress = Utils.getIP(record.getRemoteIP());
        // Is there a header in the data?
        if (record.getDataSize() > 0) {
            prepareHeader(record.getData());
        } else {
            prepareHeader(null); // use the default
        }
        this.isOpen = false;
    }

    @Override
    public boolean write(StreamedRecord record) {

        String fn = "unset";
        try {
            if (!this.isOpen) {
                fn = mkFileName();
                eventFilename = new File(fn);
                eventOutput = new FileOutputStream(eventFilename);
                eventWriter = new GZIPOutputStream(eventOutput);
                this.isOpen = true;
                eventWriter.write(mkHeader());
            }
        } catch (Exception e) {
            System.out.println("Open failed for :" + fn + "  Exception : " + e.getMessage()); // oops open failed
            //e.printStackTrace();
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

    private String mkFileName() {
        String fileName = tempDir + "/A" + sourceId;
        try {
            long now = System.currentTimeMillis(); // UTC
            rop_start = now - (now % (rop_length_in_seconds * 1000));
            rop_end = rop_start + (rop_length_in_seconds * 1000);
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(rop_start);

            String tmp1 = dateFormat1.format(cal.getTime());
            cal.setTimeInMillis(rop_end);
            String tmp2 = dateFormat2.format(cal.getTime());
            fileName = tempDir + "/A" + tmp1 + tmp2 + ".ctum-" + this.sourceId + ".1.gz"; //TODO do this right

            // output file
            // /eniq/data/pmdata/eventdata/00/lte_event_ctums_file/5min/192.168.1.140/A20131022.1430-20131022.1435_1_ctum-1382452573952.1.gz

        } catch (Exception e) {
            e.printStackTrace();
        }
        return fileName;
    }

    // Make a header for a CTUM file
    private byte[] mkHeader() {
        header.clear();
        header.put(fixedHeader);
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(rop_start);
        header.putShort((short) cal.get(Calendar.YEAR));
        header.put((byte) cal.get(Calendar.MONTH));
        header.put((byte) cal.get(Calendar.DAY_OF_MONTH));
        header.put((byte) cal.get(Calendar.HOUR_OF_DAY));
        header.put((byte) cal.get(Calendar.MINUTE));
        header.put((byte) cal.get(Calendar.SECOND));
        return header.array();
    }

    private void prepareHeader(byte[] header) {
        if (header != null) {
            // extract the ffiv/fiv from the header we have been given
            fixedHeader[3] = header[3];
            fixedHeader[4] = header[4];
        }
    }

    private byte[] mkFooter() {
        footer.clear();
        footer.put(fixedFooter);

        return footer.array();
    }

    private void setOutputDirs(Config config) {
        // setFDN(config); CTUM doesn't use oss or fdn

        // output file
        // /eniq/data/pmdata/eventdata/00/lte_event_ctums_file/5min/192.168.1.140/A20131022.1430-20131022.1435_1_ctum-1382452573952.1.gz

        tempDir = config.getValue(StreamAdapterConstants.TEMP_DIR, StreamAdapterDefaults.TEMP_DIR); // the root of the temp dir
        outputDir = config.getValue(StreamAdapterConstants.OUT_DIR, StreamAdapterDefaults.OUT_DIR); // the root of the temp dir
        if (config.getValue(StreamAdapterConstants.SIMPLE, StreamAdapterDefaults.SIMPLE) != 1) { // turn off fancy output
            tempDir = tempDir + "/" + rop_minutes + "min/" + ipAddress + "/";
            outputDir = outputDir + "/" + rop_minutes + "min/" + ipAddress + "_";
        }

        File outDir = new File(tempDir);
        if (!outDir.exists()) {
            outDir.mkdir();
            System.out.println("Creating directory " + tempDir);
        }
    }

}
