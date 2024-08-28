package com.ericsson.streamAdapter.output;

import com.ericsson.streamAdapter.output.exception.NoConnectionException;

import com.ericsson.streamAdapter.util.cache.BlockingQueueCache;
import com.ericsson.streamAdapter.util.cache.Cache;
import com.ericsson.streamAdapter.util.config.Config;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.*;

import static org.junit.Assert.*;

import com.ericsson.streamAdapter.util.StreamedRecord;

import static com.ericsson.streamAdapter.util.TestConstants.*;

public class OutputThreadTest {


	private OutputThread MZobjectToTest;
	private OutputThread fileobjectToTest;
	private OutputChannel mockChannelSingle;
	private OutputChannelMulti mockChannelMulti;
    private String filetopic;
    private String mztopic;


	@Before
	public void setUp() throws Exception {
		String workingDirectory = System.getProperty("user.dir");
		Config fileconfig = new Config(workingDirectory
				+ "/src/test/resources/TestTORClient.ini", "TorStreamCTUM3");

        mockChannelMulti = mock(CTUMOutputChannel.class);

        fileobjectToTest = new StubbedOutputThread(mockChannelMulti,fileconfig,1);


		Config MZconfig = new Config(workingDirectory
				+ "/src/test/resources/TestTORClient.ini", "TorStreamCTUM4");
		mockChannelSingle = mock(MzOutputChannel.class);

		MZobjectToTest = new StubbedOutputThread(mockChannelSingle,fileconfig,1);

	}

	@Test
	public void test_handleConnect() throws Exception {
		StreamedRecord record = createConnectMessage();
        MZobjectToTest.processRecord(record);
        MZobjectToTest.processMessage();
		verify(mockChannelSingle, times(1)).handleConnect(record);
	}

	@Test
	public void test_handleDisconnect() throws Exception {
		StreamedRecord record = createDisconnectMessage();
        fileobjectToTest.processRecord(record);
        fileobjectToTest.processMessage();
		verify(mockChannelMulti, times(1)).handleDisconnect(record);
	}

	@Test
	public void test_handleClose() throws Exception {
		StreamedRecord record = createCloseMessage();
        MZobjectToTest.processRecord(record);
        MZobjectToTest.processMessage();
		verify(mockChannelSingle, times(1)).handleClose(record);
	}

	@Test
	public void test_handleRefresh() throws Exception {
		StreamedRecord record = createRefreshMessage();
        MZobjectToTest.processRecord(record);
        MZobjectToTest.processMessage();
		verify(mockChannelSingle, times(1)).handleRefresh(record);
	}

	@Test
	public void test_handleEvent() throws Exception {
		StreamedRecord record = creteEventMessage();
        MZobjectToTest.processRecord(record);
        MZobjectToTest.processMessage();
		verify(mockChannelSingle, times(1)).handleEvent(record);
	}

	@Test
	public void test_close() throws Exception {
        MZobjectToTest.close();
		assertEquals(StreamedRecord.Actions.CLOSE, MZobjectToTest.getQueue().take().getAction());
	}

	@Test
	public void test_refresh() throws Exception {
        MZobjectToTest.refresh();
		assertEquals(StreamedRecord.Actions.REFRESH, MZobjectToTest.getQueue().take().getAction());
	}

	@Test(expected = NoConnectionException.class)
	public void test_No_Output_Channel() throws InterruptedException,
			OutputThreadException, NoConnectionException {
        fileobjectToTest.setChannel(null);
		StreamedRecord record = creteEventMessage();
        fileobjectToTest.processRecord(record);
        fileobjectToTest.processMessage();

	}

	@Test
	public void test_Event_Metrics_Increment_Connects()
			throws InterruptedException, OutputThreadException,
			NoConnectionException {
        StreamedRecord record = createConnectMessage();
        fileobjectToTest.processRecord(record);
        fileobjectToTest.processMessage();
		assertEquals(1, fileobjectToTest.getMetrics().getNumberOfConnects());
	}

	@Test
	public void test_Event_Metrics_Increment_Disconnects()
			throws InterruptedException, OutputThreadException,
			NoConnectionException {
        fileobjectToTest.processRecord(createConnectMessage());
        fileobjectToTest.processMessage();
		// objectToTest.getEventOutput().setChannel(new
		// DummyEventOutputDummy());
        fileobjectToTest.processRecord(createDisconnectMessage());
        fileobjectToTest.processMessage();
		assertEquals(1, fileobjectToTest.getMetrics().getNumberOfDisconnects());
	}

	@Test
	public void test_Event_Metrics_Increment_Events()
			throws InterruptedException, OutputThreadException,
			NoConnectionException {
        fileobjectToTest.processRecord(createConnectMessage());
        fileobjectToTest.processMessage();
		// objectToTest.getEventOutput().setChannel(new
		// DummyEventOutputDummy());
        fileobjectToTest.processRecord(creteEventMessage());
        fileobjectToTest.processMessage();
		assertEquals(1, fileobjectToTest.getMetrics().getNumberOfEvents());
	}


	@Test
	public void test_Event_Metrics_Increment_Multiple_Events()
			throws InterruptedException, OutputThreadException,
			NoConnectionException {
        fileobjectToTest.processRecord(createConnectMessage());
        fileobjectToTest.processMessage();
		// objectToTest.getEventOutput().setChannel(new
		// DummyEventOutputDummy());
        fileobjectToTest.processRecord(creteEventMessage());
        fileobjectToTest.processMessage();
        fileobjectToTest.processRecord(creteEventMessage());
        fileobjectToTest.processMessage();
		assertEquals(2, fileobjectToTest.getMetrics().getNumberOfEvents());
	}

	@Test
	public void test_Event_Metrics_Increment_One_Connect_Multiple_Events()
			throws InterruptedException, OutputThreadException,
			NoConnectionException {
        fileobjectToTest.processRecord(createConnectMessage());
        fileobjectToTest.processMessage();
		// objectToTest.getEventOutput().setChannel(new
		// DummyEventOutputDummy());
        fileobjectToTest.processRecord(creteEventMessage());
        fileobjectToTest.processMessage();
        fileobjectToTest.processRecord(creteEventMessage());
        fileobjectToTest.processMessage();
        fileobjectToTest.processRecord(creteEventMessage());
        fileobjectToTest.processMessage();
        fileobjectToTest.processRecord(creteEventMessage());
        fileobjectToTest.processMessage();
        fileobjectToTest.processRecord(createDisconnectMessage());
        fileobjectToTest.processMessage();
		assertEquals(1, fileobjectToTest.getMetrics().getNumberOfConnects());
		assertEquals(4, fileobjectToTest.getMetrics().getNumberOfEvents());
		assertEquals(1, fileobjectToTest.getMetrics().getNumberOfDisconnects());
	}

	private StreamedRecord creteEventMessage() {
		StreamedRecord streamedRecord = new StreamedRecord();
		streamedRecord.setSourceId(DUMMY_SRC_ID);
		streamedRecord.setAction(getEventAction());
		return streamedRecord;
	}

	private StreamedRecord createConnectMessage() {
		StreamedRecord streamedRecord = new StreamedRecord();
		streamedRecord.setSourceId(DUMMY_SRC_ID);
		streamedRecord.setRemoteIP(DUMMY_IP_ADDRESS);
		streamedRecord.setAction(getConnectAction());
		return streamedRecord;
	}

	private StreamedRecord createCloseMessage() {
		StreamedRecord streamedRecord = new StreamedRecord();
		streamedRecord.setSourceId(DUMMY_SRC_ID);
		streamedRecord.setRemoteIP(DUMMY_IP_ADDRESS);
		streamedRecord.setAction(getCloseAction());
		return streamedRecord;
	}

	private StreamedRecord createDisconnectMessage() {
		StreamedRecord streamedRecord = new StreamedRecord();
		streamedRecord.setSourceId(DUMMY_SRC_ID);
		streamedRecord.setDisconnectReason(DUMMY_DISCONNECT_REASON);
		streamedRecord.setAction(getDisconnectAction());
		return streamedRecord;
	}

	private StreamedRecord createRefreshMessage() {
		StreamedRecord streamedRecord = new StreamedRecord();
		streamedRecord.setSourceId(DUMMY_SRC_ID);
		streamedRecord.setDisconnectReason(DUMMY_DISCONNECT_REASON);
		streamedRecord.setAction(getRefreshventAction());
		return streamedRecord;
	}

	private StreamedRecord.Actions getEventAction() {
		return StreamedRecord.Actions.EVENT;
	}

	private StreamedRecord.Actions getConnectAction() {
		return StreamedRecord.Actions.CONNECT;
	}

	private StreamedRecord.Actions getCloseAction() {
		return StreamedRecord.Actions.CLOSE;
	}

	private StreamedRecord.Actions getRefreshventAction() {
		return StreamedRecord.Actions.REFRESH;
	}

	private StreamedRecord.Actions getDisconnectAction() {
		return StreamedRecord.Actions.DISCONNECT;
	}
	
	public class StubbedOutputThread extends OutputThread{

		public StubbedOutputThread(OutputChannel outputChannel, Config config,int index) {
			super(outputChannel,config,index);
			// TODO Auto-generated constructor stub
		}
		
	    public boolean isWriteSuccess() {
	    	//System.out.println("Called write successful");
	        return true;
	    }
	}
}
