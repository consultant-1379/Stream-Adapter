package com.ericsson.streamAdapter.util.config;

public final class StreamAdapterDefaults {

    public static final String STATISTIC_ON = "false";
    public static final String REPORTER = "JMX";
    public static final String CSV_LOCATION = "";
    public static final String STREAM_LOAD_MONITOR = "false";
    public static final int MONITOR_PERIOD = 10000;
    public static final String CTRS_PROPS = "/eniq/mediation_inter/M_E_CTRS/etc/ctrs.prop";
    public static final String INPUT_IP = "10.45.207.208";
    public static final int INPUT_PORT = 11101;
    public static final int USER_ID = 1;
    public static final int FILTER_ID = 2;
    public static final int GROUP_ID = 2;
    public static final int MIN_QUEUE_SIZE = 100000;
    public static final int REFRESH_PERIOD = 15000;
    public static final String DATA_TYPE = "CTUM";
    public static final String OUTPUT_TYPE = "MZ";
    public static final int PROVIDE_HEADERS = 0;
    public static final int RUN_FOR_MINUTES = 0;
    public static final int NUM_OF_THREADS = 5;
    public static final String MZ_IPADDRESS = "localhost";
    public static final String TEMP_DIR = "/eniq/data/pmdata/eventdata/00/lte_event_ctums_file";
    public static final int ROP_MINUTES = 5;
    public static final int SIMPLE = 0;
    public static final int MKLINKS = 0;
    public static final String OUT_DIR = "1";
    public static final int NUM_DIR = 50;
    public static final String IP_TO_FDN = "ctrs_ip2fdn.txt";
    public static final String LTE_ES_DIR = "1/es";
    public static final String CFA_DIR = "1/cfa";
    public static final String EC = "ec_st_1";

    private StreamAdapterDefaults() {
    }

}
