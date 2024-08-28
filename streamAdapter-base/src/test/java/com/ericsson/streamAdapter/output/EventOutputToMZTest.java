package com.ericsson.streamAdapter.output;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.*;
import static com.ericsson.streamAdapter.util.TestConstants.*;
import com.ericsson.streamAdapter.util.StreamedRecord;

public class EventOutputToMZTest {

	private EventOutputToMZ eventOutputToMZ;
	private SocketChannel mockedSocket;
	private StreamedRecord record;
	private ByteBuffer buffer;

	@Before
	public void setUp() throws Exception {
		eventOutputToMZ = new StubbedEventOutputToMZ();
		record = new StreamedRecord();
		mockedSocket = mock(SocketChannel.class);
		buffer = mock(ByteBuffer.class);
	}

	@Test
	public void test_create_Connection_Expect_True() throws IOException {
		assertTrue(eventOutputToMZ
				.createConnection(DUMMY_IP_STRING, DUMMY_PORT));
		verify(mockedSocket, times(1)).connect(any(InetSocketAddress.class));
		verify(mockedSocket, times(1)).finishConnect();
	}

	@Test
	public void test_writeSocket_ExpectSuccessfulWrite() throws IOException {
		eventOutputToMZ.setSocket(mockedSocket);
		when(mockedSocket.isConnected()).thenReturn(true);
		when(mockedSocket.write(buffer)).thenReturn(1);
		assertTrue(eventOutputToMZ.writeSocket(record, 0, buffer));
	}

	@Test
	public void test_write_ExpectSuccessfulWrite() throws IOException {
		setSoruceIdAndIp();
		setStreamData();
		eventOutputToMZ.setSocket(mockedSocket);
		eventOutputToMZ.connect(record);
		when(mockedSocket.isConnected()).thenReturn(true);
		when(mockedSocket.write(any(ByteBuffer.class))).thenReturn(30);
		assertTrue(eventOutputToMZ.write(record));
	}

	private void setSoruceIdAndIp() {
		int sourceId = 1;
		byte[] remoteIP = { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
				0x00, 0x00, 0x00, 0x00, 0x75, 0x00, 0x00, 0x01 };
		record.setSourceId(sourceId);
		record.setRemoteIP(remoteIP);
	}

	private void setStreamData() {
		final byte[] byteArray = { 0x00, 0x00, 0x00, 0x00 };
		record.setData(byteArray);
	}

	public class StubbedEventOutputToMZ extends EventOutputToMZ {
		StubbedEventOutputToMZ() {
			super();
		}

		public void setConfigureBlocking() {
			System.out.println("Called Configure Blocking.");
		}
		
		public void close() {
			System.out.println("Called socket close");
		}
		
		public SocketChannel getSocketChannel() throws IOException {
			return mockedSocket;
		}
	}
}