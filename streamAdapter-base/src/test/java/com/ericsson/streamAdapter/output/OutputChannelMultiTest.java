package com.ericsson.streamAdapter.output;

import static com.ericsson.streamAdapter.util.TestConstants.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.ericsson.streamAdapter.util.StreamedRecord;
import com.ericsson.streamAdapter.util.config.Config;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class OutputChannelMultiTest {

	private OutputChannelMulti object_To_Test;
	private Config testConfig;
	private EventCTUMtoFile mockedEventCTUMtoFile;

	@Before
	public void setUp() throws Exception {
		String workingDirectory = System.getProperty("user.dir");
		testConfig = new Config(workingDirectory
				+ "/src/test/resources/TestTORClient.ini", "TorStreamCTUM");
		mockedEventCTUMtoFile = mock(EventCTUMtoFile.class);
		object_To_Test = new StubbedOutputChannelMulti(testConfig);
	}

	@Test
	public void test1_HandleConnect_ExpectSourceAddedToMapWithChannel()
			throws Exception {
		StreamedRecord streamedRecord = new StreamedRecord();
		streamedRecord.setSourceId(DUMMY_SRC_ID);
		streamedRecord.setRemoteIP(DUMMY_IP_ADDRESS);
		object_To_Test.handleConnect(streamedRecord);
		Object eventCTUMToFileObj = object_To_Test.getSrcMap()
				.get(DUMMY_SRC_ID);
		assertTrue(eventCTUMToFileObj instanceof EventCTUMtoFile);
	}

	@Test
	public void test2_HandleDisconnect_ExpectSourceRemovedFromMap()
			throws Exception {
		StreamedRecord streamedRecord = new StreamedRecord();
		streamedRecord.setSourceId(DUMMY_SRC_ID);
		streamedRecord.setRemoteIP(DUMMY_IP_ADDRESS);
		object_To_Test.handleConnect(streamedRecord);
		object_To_Test.handleDisconnect(streamedRecord);
		verify(mockedEventCTUMtoFile, times(1)).close();
		Object eventCTUMToFileObj = object_To_Test.getSrcMap()
				.get(DUMMY_SRC_ID);
		assertTrue(eventCTUMToFileObj == null);
	}

	@Test
	public void test3_HandleEvent_ExpectWriteOnChannel() throws Exception {
		StreamedRecord streamedRecord = new StreamedRecord();
		streamedRecord.setSourceId(DUMMY_SRC_ID);
		streamedRecord.setRemoteIP(DUMMY_IP_ADDRESS);
		object_To_Test.handleConnect(streamedRecord);
		object_To_Test.handleEvent(streamedRecord);
		verify(mockedEventCTUMtoFile, times(1))
				.write(any(StreamedRecord.class));
	}

	@Test
	public void test4_HandleRefresh_ExpectRefreshOnChannel() throws Exception {
		StreamedRecord streamedRecord = new StreamedRecord();
		streamedRecord.setSourceId(DUMMY_SRC_ID);
		streamedRecord.setRemoteIP(DUMMY_IP_ADDRESS);
		object_To_Test.handleConnect(streamedRecord);
		object_To_Test.handleRefresh(streamedRecord);
		verify(mockedEventCTUMtoFile, times(1)).refresh(any(Integer.class));
	}

	@Test
	public void test5_HandleClose_ExpectCloseOnChannel() throws Exception {
		StreamedRecord streamedRecord = new StreamedRecord();
		streamedRecord.setSourceId(DUMMY_SRC_ID);
		streamedRecord.setRemoteIP(DUMMY_IP_ADDRESS);
		object_To_Test.handleConnect(streamedRecord);
		object_To_Test.handleClose(streamedRecord);
		verify(mockedEventCTUMtoFile, times(1)).close();
	}

	public class StubbedOutputChannelMulti extends CTUMOutputChannel {

		public StubbedOutputChannelMulti(Config testConfig) {
			super(testConfig);
		}

        @Override
        protected EventOutputChannel getOutputHandler() {
            return mockedEventCTUMtoFile;
        }
	}
}
