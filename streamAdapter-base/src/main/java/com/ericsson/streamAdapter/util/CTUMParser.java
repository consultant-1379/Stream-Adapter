package com.ericsson.streamAdapter.util;

import com.ericsson.streamAdapter.output.EventOutputChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import java.nio.ByteBuffer;


public class CTUMParser implements EventOutputChannel {

    private static final Logger logger = LoggerFactory.getLogger(CTUMParser.class);
    private static final int HEADER_RECORD = 0;
    private static final int EVENT_RECORD = 1;
    private static final int ERROR_RECORD = 2;
    private static final int FOOTER_RECORD = 3;
    private static final int STREAM_HEADER_RECORD = 4;
    private static final int STREAM_FOOTER_RECORD = 5;
    protected int year = 0;
    protected int month = 0;
    protected int day = 0;
    protected int hour = 0;
    protected int minute = 0;
    protected int second = 0;
    protected long utcOffset = 0;
    protected String fileFormatVersion = null;
    protected String fileInformationVersion = null;
    protected String neLogicalName = null;
    protected int recordLength;
    protected int recordType;
    // Fields used for streaming
    protected int scanner;
    protected int cause;
    protected long dropped;


    @Override
    public void connect(StreamedRecord record) {
            write(record);
    }

    @Override
    public boolean write(StreamedRecord record) {
        byte[] data = record.getData();

        recordLength = (int) getByteArrayInteger(data, 0, 2, false);
        recordType = (int) getByteArrayInteger(data, 2, 1, false);
        logger.info("Type :: {}", recordType);
        switch (recordType) {
        case HEADER_RECORD: {

            break;
        }
        case EVENT_RECORD: {
            handleRecord(data,record.getSourceId());
            break;
        }
        case ERROR_RECORD: {

            break;
        }
        case FOOTER_RECORD: {

            break;
        }
        case STREAM_HEADER_RECORD: {
            handleStreamHeader(data,record.getSourceId());
            break;
        }
        case STREAM_FOOTER_RECORD: {
            handleStreamFooter(data);
            break;
        }
        default: {

        }

        }
        return true;
    }

    @Override
    public void refresh(long timeNowInMilliSecs) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void close() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    private void handleStreamFooter(byte[] data) {
    }

    private void handleRecord(byte[] data,int sourceId) {
        int offset = 3;
        byte TIMESTAMP_BYTE_HOUR = (byte) getByteArrayUnsignedInteger(data, offset, 1);
        offset += 1;
        byte TIMESTAMP_BYTE_MINUTE = (byte) getByteArrayUnsignedInteger(data, offset, 1);
        offset += 1;
        byte TIMESTAMP_BYTE_SECOND = (byte) getByteArrayUnsignedInteger(data, offset, 1);
        offset += 1;
        int TIME_MILLISECOND = (int) getByteArrayUnsignedInteger(data, offset, 2);
        offset += 2;
        long ENODEB_ID_MACRO_ENODEB_ID = (byte) getByteArrayUnsignedInteger(data, offset, 3);
        offset += 3;
        long ENODEB_ID_HOME_ENODEB_ID = (byte) getByteArrayUnsignedInteger(data, offset, 4);
        offset += 4;
        String IMSI = (String) getByteArrayTBCDString(data, offset, 8);
        offset += 8;
        String IMEISV = (String) getByteArrayTBCDString(data, offset, 8);
        offset += 8;
        String GUMMEI_PLMN_IDENTITY = (String) getByteArrayTBCDString(data, offset, 3);
        offset += 3;
        int GUMMEI_MME_GROUP_ID = (int) getByteArrayUnsignedInteger(data, offset, 2);
        offset += 2;
        int GUMMEI_MME_CODE = (int) getByteArrayUnsignedInteger(data, offset, 1);
        offset += 1;
        long MME_UE_S1AP_ID = (long) getByteArrayUnsignedInteger(data, offset, 4);
        offset += 4;
        long ENB_UE_S1AP_ID = (long) getByteArrayUnsignedInteger(data, offset, 3);
        offset += 3;
        logger.info(" ================================== CTUM Record {} ================================== ",sourceId);
        logger.info(
                "HOUR : {},MINUTE : {},SECOND : {},MILLISECONDS : {},EnB MACRO ID : {},EnB HOME ID : {},IMSI : {}, PLMN ID : {}, MME GROUP ID : {},MME CODE : {},MME S1AP ID : {}, EnB S1AP ID : {}",
                TIMESTAMP_BYTE_HOUR, TIMESTAMP_BYTE_MINUTE, TIMESTAMP_BYTE_SECOND, TIME_MILLISECOND, ENODEB_ID_MACRO_ENODEB_ID, ENODEB_ID_HOME_ENODEB_ID, IMSI, IMEISV, GUMMEI_PLMN_IDENTITY,
                GUMMEI_MME_GROUP_ID, GUMMEI_MME_CODE, MME_UE_S1AP_ID, ENB_UE_S1AP_ID);
        logger.info(" ================================================================================= ");

    }

    private void handleStreamHeader(byte[] data,int sourceId) {
        fileFormatVersion = Integer.toString(data[3]);
        fileInformationVersion = Integer.toString(data[4]);
        year = (int) getByteArrayInteger(data, 5, 2, false);
        month = data[7];
        day = data[8];
        hour = data[9];
        minute = data[10];
        second = data[11];
        cause = data[12];
        neLogicalName = new String(data, 13, (data.length >= 30 ? 20 : data.length - 10)).replace('\0', ' ').trim();
        logger.info(" ================================== Stream Header Record {} ================================== ",sourceId);
        logger.info(",file Format Version :{},file Information Version :{},Network Element Name :{}", fileFormatVersion, fileInformationVersion, neLogicalName);
        logger.info("Year {},Month {}, Day {}, Hour {}, Minute {}, Second {}", year, month, day, hour, minute, second);
        logger.info(" ========================================================================================== ");
    }

    private long getByteArrayInteger(final byte[] data, final int offset, final int bytes, final boolean signed) {
        // Get the integer value of the parameter
        long paramValue = 0;

        // Iterate over the bytes to be decoded
        for (int i = offset; i < offset + bytes; i++) {
            paramValue = paramValue << 8 | (0x00000000000000ff & data[i]);
        }

        // For integers of length less than 8 bytes, check if this integer value is signed
        // Integers of length 8 bytes are full longs and will be handled automatically
        if (bytes < 8 && signed) {
            // Check if the uppermost bit for the number of bytes in question is set, then we have a negative value
            if ((paramValue >> ((bytes * 8) - 1)) != 0) {
                // Uppermost bit is set, we set all high bytes to 1s for the length of the long
                for (int i = bytes; i < 8; i++) {
                    // Set high bits
                    long maskValue = ((long) 0xff) << (i * 8);
                    paramValue = paramValue | maskValue;
                }
            }
        }

        return paramValue;
    }

    private String getByteArrayTBCDString(final byte[] data, final int offset, final int bytes) {
        // Declare a byte array to hold the TBCD value
        byte[] tbcdValue = getByteArrayTBCD(data, offset, bytes);

        // Return the value with hexadecimal 'f' values stripped
        return getByteArrayHexString(tbcdValue, 0, tbcdValue.length).replace("f", "");
    }

    private byte[] getByteArrayTBCD(final byte[] data, final int offset, final int bytes) {
        // Declare a byte array to hold the TBCD value
        byte[] tbcdValue = new byte[bytes];

        // Reverse the order of the nibbles in each byte
        for (int i = offset, j = 0; i < bytes + offset; i++, j++) {
            // Store least significant nibble first
            int value = (data[i] & 0x0000000f) << 4;

            // Store most significant nibble last
            value |= (data[i] & 0x000000f0) >> 4;

            // Store the value into the array
            tbcdValue[j] = (byte) value;
        }

        // Return the value
        return tbcdValue;
    }

    private String getByteArrayHexString(final byte[] data, final int offset, final int bytes) {
        // These are the characters we can have in the output
        char[] hexCharArray = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

        // This array will hold the hex string as a character array
        char[] hexChars = new char[bytes * 2];

        // The current byte value
        int value;

        // Iterate over the byte array
        for (int i = 0; i < bytes; i++) {
            // Get the current byte
            value = data[i + offset] & 0xff;

            // Add in the character representation of each character of the current byte
            hexChars[i * 2] = hexCharArray[value / 16];
            hexChars[i * 2 + 1] = hexCharArray[value % 16];
        }

        return new String(hexChars);
    }

    private long getByteArrayUnsignedInteger(final byte[] data, final int offset, final int bytes) {
        return getByteArrayInteger(data, offset, bytes, false);
    }

}
