package com.ericsson.torclient.automation;

import static com.ericsson.streamAdapter.server.utils.Constants.EMULATOR_PORT_TO_WRITE;
import static com.ericsson.streamAdapter.server.utils.Constants.MZ_PORT_TO_LISTEN;
import static com.ericsson.streamAdapter.server.utils.Constants.NUMBER_OF_FAULT_NODES;
import static com.ericsson.streamAdapter.server.utils.Constants.NUMBER_OF_WORKING_NODES_SEQUENCE;
import static com.ericsson.streamAdapter.server.utils.Constants.NUMBER_OF_WORKING_NODES;
import static com.ericsson.streamAdapter.server.utils.Constants.NUMBER_OF_EVENTS_PER_NODE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.ericsson.streamAdapter.StreamService;
import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.junit.*;
import org.junit.runners.MethodSorters;

import com.ericsson.streamAdapter.client.torstreaming.controller.Source;
import com.ericsson.streamAdapter.server.TORStreamingServer;
import com.ericsson.streamAdapter.server.logger.AutomationLogger;
import com.ericsson.streamAdapter.server.mz.client.MZClient;
import com.ericsson.streamAdapter.server.mz.client.reponsehandler.ProcessRecord;
import com.ericsson.streamAdapter.server.utils.Constants;
import com.ericsson.streamAdapter.server.utils.EventDetails;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TORClientAutomationTest {
	static Logger logger = AutomationLogger.getLogger();
	static ExecutorService threadPool = Executors.newFixedThreadPool(3);
	final static TORStreamingServer torSimulator = new TORStreamingServer(
			EMULATOR_PORT_TO_WRITE);
	final static MZClient mzClient = new MZClient(MZ_PORT_TO_LISTEN);
	final static StreamService torClient = new StreamService();
	Map<String, List<EventDetails>> eventsReceivedByTorClient;
	Map<String, List<EventDetails>> eventsReceivedByMZClient;
	static Future<?> torEmulator;
	static Future<?> torclient;
	static Future<?> mzClientExecutor;

	@BeforeClass
	public static void setUpTorClient() {
		startMZClient();
		startTORClient();
	}

	@Test
	// EQEV-5045-1-6,EQEV-5045-1-7
	public void test1_SourceWithValidConnection_ExceptAllSouceSentToMZ() {
		setSimulatorPropertiesAndStart("1", "0", "0", "1");
		waitForSimulatorToSendAllSources();
		waitForMZToReceiveAllSources();
		assertTrue(logFailure(1, eventsReceivedByMZClient.keySet().size()),
				(1 == eventsReceivedByMZClient.keySet().size()));
	}

	@Test
	public void test2_SourcesWithValidAndInvalidConnections_ExceptValidSouceSentToMZ() {
		setSimulatorPropertiesAndStart("3", "0", "2", "2");
		waitForSimulatorToSendAllSources();
		waitForMZToReceiveAllSources();
		assertTrue(logFailure(3, eventsReceivedByMZClient.keySet().size()),
				(3 == eventsReceivedByMZClient.keySet().size()));
	}

	@Test
	// EQEV-5045-1-8 ,EQEV-5045-1-9
	public void test3_TORClientReconnectionToTORStreaming_ExpectSucessfulReconnect() {
		setSimulatorPropertiesAndStart("1", "0", "0", "1");
		waitForSimulatorToSendAllSources();
		waitForMZToReceiveAllSources();
		assertTrue(logFailure(1, eventsReceivedByMZClient.keySet().size()),
				(1 == eventsReceivedByMZClient.keySet().size()));
		cleanUp();
		setSimulatorPropertiesAndStart("3", "0", "0", "1");
		waitForSimulatorToSendAllSources();
		waitForMZToReceiveAllSources();
		assertTrue(logFailure(3, eventsReceivedByMZClient.keySet().size()),
				(3 == eventsReceivedByMZClient.keySet().size()));
	}

	@Test
	// EQEV-5045-1-10
	public void test4_NumberOfMessagesReceivedByTORClient_EqualsNumberOfMessagesReceivedByMZClient() {
		setSimulatorPropertiesAndStart("1", "0", "0", "3");
		waitForSimulatorToSendAllSources();
		waitForMZToReceiveAllSources();
		for (String ip : eventsReceivedByTorClient.keySet()) {
			waitForSimulatorToSendAllData(ip);
			waitForMZToReceiveAllData(ip);
			List<EventDetails> eventListReceivedByMZClient = eventsReceivedByMZClient
					.get(ip);
			assertEquals(logFailure(4, eventListReceivedByMZClient.size()), 4,
					eventListReceivedByMZClient.size());
		}
	}

	@Test
	// EQEV-5045-1-11
	public void test5_EventMessagePayloadReceivedByTORClient_EqualsEventMessagePayloadReceivedByMZClient() {
		setSimulatorPropertiesAndStart("2", "0", "0", "4");
		waitForSimulatorToSendAllSources();
		waitForMZToReceiveAllSources();
		for (String ip : eventsReceivedByTorClient.keySet()) {
			waitForSimulatorToSendAllData(ip);
			waitForMZToReceiveAllData(ip);
			for (int i = 0; i < eventsReceivedByTorClient.get(ip).size(); i++) {
				List<Byte> eventMessagePayloadReceivedByTORClient = channelBufferToList(eventsReceivedByTorClient
						.get(ip).get(i).getData());
				List<Byte> eventMessagePayloadReceivedByMZClient = channelBufferToList(eventsReceivedByMZClient
						.get(ip).get(i).getData());
				List<Byte> dataBytes = eventMessagePayloadReceivedByMZClient
						.subList(0,
								eventMessagePayloadReceivedByTORClient.size());
				assertTrue(eventMessagePayloadReceivedByTORClient
						.equals(dataBytes));
				List<Byte> paddingBits = eventMessagePayloadReceivedByMZClient
						.subList(eventMessagePayloadReceivedByTORClient.size(),
								eventMessagePayloadReceivedByMZClient.size());
				assertTrue(checkAllZerosInPaddingBits(paddingBits));
			}
		}
	}

	@Test
	// EQEV-5045-1-12
	public void test6_SourceWithValidConnectDisconnectForEachEvent_ExpectAllEventsReceivedByMZClient() {
		setSimulatorPropertiesAndStart("0", "1", "0", "3");
		waitForSimulatorToSendAllSources();
		waitForMZToReceiveAllSources();
		for (String ip : eventsReceivedByTorClient.keySet()) {
			waitForSimulatorToSendAllData(ip);
			waitForMZToReceiveAllData(ip);
			List<EventDetails> eventListReceivedByMZClient = eventsReceivedByMZClient
					.get(ip);
			assertEquals(logFailure(3, eventListReceivedByMZClient.size()), 3,
					eventListReceivedByMZClient.size());
		}
	}

	private static void startTORSimulator() {
		torEmulator = threadPool.submit(new Thread() {
			public void run() {
				logger.info(TORClientAutomationTest.class.getSimpleName()
						+ " : Starting TOR Client Emulator");
				torSimulator.start();
				logger.info(TORClientAutomationTest.class.getSimpleName()
						+ " : Started TOR Client Emulator");
			}
		});
		sleep(5000);
	}

	private static void startMZClient() {
		mzClientExecutor = threadPool.submit(new Thread() {
			public void run() {
				logger.info(TORClientAutomationTest.class.getSimpleName()
						+ " : Starting MZ Agents");
				mzClient.start();
				logger.info(TORClientAutomationTest.class.getSimpleName()
						+ " : Started MZ Agents");
			}
		});
	}

	private static void startTORClient() {
		torclient = threadPool.submit(new Thread() {
			public void run() {
				logger.info(TORClientAutomationTest.class.getSimpleName()
						+ " : Starting TOR Client");

				torClient.start(argumentToTORClient());

				logger.info(TORClientAutomationTest.class.getSimpleName()
						+ " : Started TOR Client");
			}
		});
	}

	private static String[] argumentToTORClient() {
		logger.debug("Input ini file to TOR Client - Automation.ini");
		logger.debug("Configuration read from ini  - CTUMInput1");
		String[] inputArgsToTorClient = new String[4];
		inputArgsToTorClient[0] = "-f";
		inputArgsToTorClient[1] = "Automation.ini";
		inputArgsToTorClient[2] = "-i";
		inputArgsToTorClient[3] = "CTUMInput1";
		return inputArgsToTorClient;
	}

	private void setSimulatorPropertiesAndStart(String workingNodes,
			String sequenceEventsWorkingNodes, String faultNodes,
			String numberOfEventsPerNode) {
		setWorkingNodes(workingNodes);
		setSequenceEventsWorkingNodes(sequenceEventsWorkingNodes);
		setFaultyNodes(faultNodes);
		setNumberOfEvents(numberOfEventsPerNode);
		Constants.reLoad();
		startTORSimulator();
		sleep(5000);
	}

	private void setWorkingNodes(String numberOfEvents) {
		System.setProperty("NUMBER_OF_WORKING_NODES", numberOfEvents);
	}

	private void setSequenceEventsWorkingNodes(String sequenceEventsWorkingNodes) {
		System.setProperty("NUMBER_OF_WORKING_NODES_SEQUENCE",
				sequenceEventsWorkingNodes);
	}

	private void setFaultyNodes(String numberOfFaultyNodes) {
		System.setProperty("NUMBER_OF_FAULT_NODES", numberOfFaultyNodes);
	}

	private void setNumberOfEvents(String numberOfWorkingNodes) {
		System.setProperty("NUMBER_OF_EVENTS_PER_NODE", numberOfWorkingNodes);
	}

	private void waitForMZToReceiveAllSources() {
		int counter = 0;
		eventsReceivedByMZClient = ProcessRecord.getEventsInformationMap();
		while (eventsReceivedByMZClient.keySet().size() != (NUMBER_OF_WORKING_NODES
				+ NUMBER_OF_FAULT_NODES + NUMBER_OF_WORKING_NODES_SEQUENCE)
				&& counter != 15) {
			sleep(1000);
			counter++;
		}
	}

	private void waitForSimulatorToSendAllSources() {
		int counter = 0;
		eventsReceivedByTorClient = Source.getEventDetailsMap();
		while (eventsReceivedByTorClient.keySet().size() != (NUMBER_OF_WORKING_NODES
				+ NUMBER_OF_FAULT_NODES + NUMBER_OF_WORKING_NODES_SEQUENCE)
				&& counter != 15) {
			sleep(1000);
			counter++;
		}
	}

	private void waitForSimulatorToSendAllData(String ip) {
		int counter = 0;
		eventsReceivedByTorClient = Source.getEventDetailsMap();
		while (eventsReceivedByTorClient.get(ip).size() != (NUMBER_OF_EVENTS_PER_NODE)
				&& counter != 15) {
			sleep(1000);
			counter++;
		}
	}

	private void waitForMZToReceiveAllData(String ip) {
		int counter = 0;
		eventsReceivedByMZClient = ProcessRecord.getEventsInformationMap();
		while (eventsReceivedByMZClient.get(ip).size() != (NUMBER_OF_EVENTS_PER_NODE)
				&& counter != 15) {
			sleep(1000);
			counter++;
		}
	}

	private static void shutDownSimulator() {
		torSimulator.shutDown();
	}

	private static void shutDownMZlient() {
		mzClient.shutDown();
	}

	private static void shutDownTORClient() {
		torClient.terminate();
	}

	private static void sleep(int milliSeconds) {
		try {
			Thread.sleep(milliSeconds);
		} catch (InterruptedException e) {
			logger.error(TORClientAutomationTest.class.getSimpleName()
					+ " Thread Interuppted : " + e.getMessage());
		}
	}

	private List<Byte> channelBufferToList(ChannelBuffer channelBuffer) {
		int bytes = channelBuffer.readableBytes();
		List<Byte> list = new ArrayList<Byte>();
		for (int i = 0; i < bytes; i++) {
			list.add(channelBuffer.getByte(i));
		}
		return list;
	}

	private boolean checkAllZerosInPaddingBits(List<Byte> paddingBits) {
		for (Byte dataPaddingByte : paddingBits) {
			if (dataPaddingByte != 0)
				return false;
		}
		return true;
	}

	private String logFailure(int expected, int got) {
		String message = "Expected is " + expected + " but Got " + got;
		logger.error(message);
		return message;
	}

	@After
	public void cleanUp() {
		shutDownSimulator();
		sleep(5000);
		eventsReceivedByTorClient.clear();
		eventsReceivedByMZClient.clear();
		Source.getEventDetailsMap().clear();
		ProcessRecord.getEventsInformationMap().clear();
	}

	@AfterClass
	public static void shutDown() {
		shutDownMZlient();
		shutDownTORClient();
	}
}