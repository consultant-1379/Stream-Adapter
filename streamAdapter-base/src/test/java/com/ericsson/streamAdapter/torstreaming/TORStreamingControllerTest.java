package com.ericsson.streamAdapter.torstreaming;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.ericsson.oss.mediation.engine.netty.core.Engine;
import com.ericsson.streamAdapter.output.OutputThreadHandler;
import com.ericsson.streamAdapter.util.cache.BlockingQueueCache;
import com.ericsson.streamAdapter.util.statistics.StatisticsHandler;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.ericsson.oss.mediation.engine.netty.NettyEngine;
import com.ericsson.streamAdapter.torstreaming.controller.TORStreamingController;
import com.ericsson.streamAdapter.torstreaming.controller.TORStreamingState;
import com.ericsson.streamAdapter.torstreaming.listener.TORStreamingListener;
import com.ericsson.streamAdapter.util.config.Config;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TORStreamingControllerTest {

    private static final String INSTANCE_ID = "streamAdapter";
    private static OutputThreadHandler mockHandler;
    private static TORStreamingController object_To_Test;
    private static TORStreamingListener mockListener;
    private static Config config = new Config();
    private static NettyEngine mockedNettyEngine;
    private static StatisticsHandler statisticsHandler;

    @BeforeClass
    public static void oneTimeSetUp() {
        mock(Engine.class);
        statisticsHandler = new StatisticsHandler(config);
        statisticsHandler.startStatisticsReporting();
        mockedNettyEngine = mock(NettyEngine.class);
        mockHandler = mock(OutputThreadHandler.class);
        mockListener = mock(TORStreamingListener.class);
        object_To_Test = new TORStreamingController(INSTANCE_ID, config, mockHandler) {
            public NettyEngine getNettyEngine() {
                return mockedNettyEngine;
            }

            public long getTransititionDelay() {
                return 100;
            }
        };
        object_To_Test.setListener(mockListener);
    }

    @AfterClass
    public static void oneTimeTearDown() {
        statisticsHandler.stopStatisticsReporting();
        statisticsHandler = null;
        object_To_Test = null;
        mockListener = null;
        mockedNettyEngine = null;
    }

    @Test
    public void test_StartEngine_Expect_Connected_State() throws InterruptedException {
        object_To_Test.start();
        Thread.sleep(500);
        verify(mockedNettyEngine, times(1)).start();
        verify(mockedNettyEngine, times(1)).startDataPath(any(String.class));
        assertTrue(" Expected : " + TORStreamingState.Connected + "   Got : " + object_To_Test.getEngineState(),
                object_To_Test.getEngineState() == TORStreamingState.Connected);
    }

    @Test
    public void test_ResetEngine_Expect_Connecting_State() throws InterruptedException {
        object_To_Test.reset();
        assertTrue(" Expected : " + TORStreamingState.Resetting + " Got : " + object_To_Test.getEngineState(),
                object_To_Test.getEngineState() == TORStreamingState.Resetting);
        object_To_Test.handleTransition();
        verify(mockedNettyEngine, times(1)).stopDataPath(any(String.class));
        verify(mockedNettyEngine, times(1)).stop();
        assertTrue(" Expected : " + TORStreamingState.Connecting + "   Got : " + object_To_Test.getEngineState(),
                object_To_Test.getEngineState() == TORStreamingState.Connecting);
    }

    @Test
    public void test_stopEngine_Expect_Disconnected_State() throws InterruptedException {
        object_To_Test.stop();
        Thread.sleep(500);
        assertTrue(" Expected : " + TORStreamingState.Disconnected + "   Got : " + object_To_Test.getEngineState(),
                object_To_Test.getEngineState() == TORStreamingState.Disconnected);
    }

    @Test
    public void test_instance_id() throws InterruptedException {
        assertTrue(" Expected : " + INSTANCE_ID + "   Got : " + object_To_Test.getInstanceId(), object_To_Test.getInstanceId() == INSTANCE_ID);
    }

    @Test
    public void test_Output_Metrics_Call() throws InterruptedException {
        object_To_Test.outputMetrics();
        verify(mockListener, times(1)).outputMetrics();
    }

    @Test
    public void test_Get_Config() throws InterruptedException {
        assertEquals(object_To_Test.getConfig(), config);
    }

}
