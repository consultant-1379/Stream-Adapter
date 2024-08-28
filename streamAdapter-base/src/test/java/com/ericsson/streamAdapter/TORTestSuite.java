package com.ericsson.streamAdapter;

import com.ericsson.streamAdapter.output.EventOutputToMZTest;
import com.ericsson.streamAdapter.output.OutputThreadTest;
import com.ericsson.streamAdapter.util.EventMetricsTest;
import com.ericsson.streamAdapter.util.config.ConfigTest;
import com.ericsson.streamAdapter.util.statistics.StatisticsHandlerTest;
import org.junit.runners.Suite;
import org.junit.runner.RunWith;

import com.ericsson.streamAdapter.torstreaming.TORStreamingControllerTest;
import com.ericsson.streamAdapter.torstreaming.TORStreamingListenerTest;

@RunWith(Suite.class)
@Suite.SuiteClasses(value = { TORStreamingControllerTest.class, TORStreamingListenerTest.class, ConfigTest.class, StatisticsHandlerTest.class,
        EventMetricsTest.class, OutputThreadTest.class, EventOutputToMZTest.class })
public class TORTestSuite {

}
