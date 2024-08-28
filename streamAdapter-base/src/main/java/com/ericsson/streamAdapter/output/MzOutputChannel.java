package com.ericsson.streamAdapter.output;

import com.ericsson.streamAdapter.util.Utils;
import com.ericsson.streamAdapter.util.config.Config;
import com.ericsson.streamAdapter.util.config.StreamAdapterConstants;
import com.ericsson.streamAdapter.util.config.StreamAdapterDefaults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MzOutputChannel extends OutputChannel {
    private static final Logger logger = LoggerFactory.getLogger(MzOutputChannel.class);
    private int threadNum;

    public MzOutputChannel(Config config, int threadNum) {
        super(config);
        this.threadNum = threadNum;
    }

    @Override
    protected EventOutputChannel getOutputHandler() {
        EventOutputToMZ eventOutput = getEventOutputChannel();
        return eventOutput;
    }

    private EventOutputToMZ getEventOutputChannel() {
        String ipAddress = config.getValue(StreamAdapterConstants.MZ_IPADDRESS, StreamAdapterDefaults.MZ_IPADDRESS);
        String service = config.getValue(StreamAdapterConstants.EC, ipAddress);
        ipAddress = service;
        boolean isCtum = "CTUM".equalsIgnoreCase(config.getValue(StreamAdapterConstants.DATA_TYPE, StreamAdapterDefaults.DATA_TYPE));
        int port = Utils.safeParseInt(Utils.getMZPort(threadNum - 1), logger);
        return new EventOutputToMZ(ipAddress, port, isCtum);
    }

}
