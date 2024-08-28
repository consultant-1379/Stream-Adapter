package com.ericsson.streamAdapter.util.statistics;

import com.codahale.metrics.Meter;
import com.ericsson.streamAdapter.StreamService;
import com.ericsson.streamAdapter.util.config.Config;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StatisticsHandlerTest {

    private StatisticsHandler statisticsHandler;
    private Config config;
    private StreamService service;

    private static final String EMPTY_STRING = "";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        config = mock(Config.class);
        when(config.getValue("Statistic_On","false")).thenReturn("false");
        service = mock(StreamService.class);
        statisticsHandler = new StatisticsHandler(config);
    }

    @Test
    public void testIsStatisticsOn() {
        assertTrue(!statisticsHandler.isStatisticsOn());
    }

    @Test
    public void test_null_createMeter_1() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("name must not be null or empty");

        statisticsHandler.createMeter(StatisticsHandlerTest.class,null, null);
    }

    @Test
    public void test_null_createMeter_2() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("type must not be null or empty");

        statisticsHandler.createMeter(StatisticsHandlerTest.class,"test", null);
    }

    @Test
    public void test_null_createMeter_3() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("name must not be null or empty");

        statisticsHandler.createMeter(null, null, (StreamService)null);
    }

    @Test
    public void test_null_createMeter_4() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("type must not be null or empty");

        statisticsHandler.createMeter("test", null, null);
    }

    @Test
    public void test_null_createMeter_5() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("service must not be null");

        statisticsHandler.createMeter("test", "eventsReceived", null);
    }

    @Test
    public void test_empty_createMeter_1() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("className must not be null");

        statisticsHandler.createMeter(null,EMPTY_STRING, EMPTY_STRING);
    }

    @Test
    public void test_empty_createMeter_2() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("name must not be null or empty");

        statisticsHandler.createMeter(StatisticsHandlerTest.class,EMPTY_STRING, EMPTY_STRING);
    }

    @Test
    public void test_empty_createMeter_3() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("name must not be null or empty");

        statisticsHandler.createMeter(EMPTY_STRING, EMPTY_STRING, null);
    }

    @Test
    public void test_empty_createMeter_4() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("type must not be null or empty");

        statisticsHandler.createMeter("test", EMPTY_STRING, null);
    }

    @Test
    public void test_valid_createMeter_1() {
        assertTrue(statisticsHandler.createMeter(StatisticsHandlerTest.class,"test", "eventsReceived") instanceof Meter);
    }

    @Test
    public void test_valid_createMeter_2() {
        assertTrue(statisticsHandler.createMeter("test", "eventsReceived", service) instanceof Meter);
    }
}
