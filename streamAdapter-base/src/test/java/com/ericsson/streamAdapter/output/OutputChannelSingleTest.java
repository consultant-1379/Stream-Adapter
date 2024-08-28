package com.ericsson.streamAdapter.output;

import static com.ericsson.streamAdapter.util.TestConstants.DUMMY_IP_ADDRESS;
import static com.ericsson.streamAdapter.util.TestConstants.DUMMY_SRC_ID;
import static com.ericsson.streamAdapter.util.TestConstants.DUMMY_THREAD_ID;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.ericsson.streamAdapter.util.StreamedRecord;
import com.ericsson.streamAdapter.util.config.Config;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class OutputChannelSingleTest {

	private OutputChannel object_To_Test;
	private Config testConfig;
	private EventOutputToMZ mockedEventOutputToMZ;

	@Before
	public void setUp() throws Exception {
		String workingDirectory = System.getProperty("user.dir");
		testConfig = new Config(workingDirectory
				+ "/src/test/resources/TestTORClient.ini", "TorStreamCTUM2");
		mockedEventOutputToMZ = mock(EventOutputToMZ.class);
		object_To_Test = new StubbedOutputChannelSingle(testConfig,DUMMY_THREAD_ID);
        StreamedRecord streamedRecord = new StreamedRecord();
        streamedRecord.setSourceId(DUMMY_SRC_ID);
        streamedRecord.setRemoteIP(DUMMY_IP_ADDRESS);
        object_To_Test.handleConnect(streamedRecord);
	}

	@Test
	public void test1_HandleConnect_ExpectSourceAddedToMapWithChannel()
			throws Exception {
		StreamedRecord streamedRecord = new StreamedRecord();
		streamedRecord.setSourceId(DUMMY_SRC_ID);
		streamedRecord.setRemoteIP(DUMMY_IP_ADDRESS);
		object_To_Test.handleConnect(streamedRecord);
		verify(mockedEventOutputToMZ, times(2)).connect(any(StreamedRecord.class));
	}

	@Test
	public void test2_HandleDisconnect_ExpectSourceRemovedFromMap()
			throws Exception {
		StreamedRecord streamedRecord = new StreamedRecord();
		streamedRecord.setSourceId(DUMMY_SRC_ID);
		streamedRecord.setRemoteIP(DUMMY_IP_ADDRESS);
		object_To_Test.handleDisconnect(streamedRecord);
		verify(mockedEventOutputToMZ, times(1)).close();
	}

	@Test
	public void test3_HandleEvent_ExpectWriteOnChannel() throws Exception {
		StreamedRecord streamedRecord = new StreamedRecord();
		streamedRecord.setSourceId(DUMMY_SRC_ID);
		streamedRecord.setRemoteIP(DUMMY_IP_ADDRESS);
		object_To_Test.handleEvent(streamedRecord);
		verify(mockedEventOutputToMZ, times(1))
				.write(any(StreamedRecord.class));
	}

	@Test
	public void test4_HandleRefresh_ExpectRefreshOnChannel() throws Exception {
		StreamedRecord streamedRecord = new StreamedRecord();
		streamedRecord.setSourceId(DUMMY_SRC_ID);
		streamedRecord.setRemoteIP(DUMMY_IP_ADDRESS);
		object_To_Test.handleRefresh(streamedRecord);
		verify(mockedEventOutputToMZ, times(1)).refresh(any(Integer.class));
	}

	@Test
	public void test5_HandleClose_ExpectCloseOnChannel() throws Exception {
		StreamedRecord streamedRecord = new StreamedRecord();
		streamedRecord.setSourceId(DUMMY_SRC_ID);
		streamedRecord.setRemoteIP(DUMMY_IP_ADDRESS);
		object_To_Test.handleClose(streamedRecord);
		verify(mockedEventOutputToMZ, times(1)).close();
	}

	public class StubbedOutputChannelSingle extends MzOutputChannel {

		public StubbedOutputChannelSingle(Config testConfig,String section) {
			super(testConfig,1);
		}

        @Override
        protected EventOutputChannel getOutputHandler() {
            return mockedEventOutputToMZ;
        }
	}
}
