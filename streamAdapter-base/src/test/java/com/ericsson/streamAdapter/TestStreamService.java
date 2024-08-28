package com.ericsson.streamAdapter;


import org.junit.Ignore;
import org.junit.Test;

public class TestStreamService {
    @Ignore
	@Test
	public void testStreamService() {
		StreamService objectToTest = new StreamService();
		String[] args = new String[4];
		args[0] = "-f";
		args[1] = "MyStreamAdapter.ini";
		args[2] = "-i";
		args[3] = "TorStreamCTUM";
		objectToTest.start(args);
	}

}
