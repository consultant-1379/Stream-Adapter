package com.ericsson.streamAdapter.server;

import org.junit.Before; 
import org.junit.Test;

import junit.framework.TestCase;

public class MultiSourceProviderTest extends TestCase {
	
	MultiSourceIPProvider object_To_Test;
	@Before
	protected void setUp() throws Exception {
		object_To_Test = new MultiSourceIPProvider();
	}
	@Test
	public void test_convertIntToByteArray_Expect_1()
	{
		byte[] bytes = MultiSourceIPProvider.generateThreeBytesSource(1);
		assertTrue("Expected : 1 \n Got : "+bytes[2], bytes[2]==1);
	}
	@Test
	public void test_convertIntToByteArray_Expect_39_16()
	{
		byte[] bytes = MultiSourceIPProvider.generateThreeBytesSource(10000);
		assertTrue("Expected : 0 \n Got : "+bytes[0], bytes[0]==0);
		assertTrue("Expected : 39 \n Got : "+bytes[1], bytes[1]==39);
		assertTrue("Expected : 16 \n Got : "+bytes[2], bytes[2]==16);
	}
}
