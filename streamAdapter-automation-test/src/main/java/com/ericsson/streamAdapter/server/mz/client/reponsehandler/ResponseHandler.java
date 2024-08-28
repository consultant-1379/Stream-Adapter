package com.ericsson.streamAdapter.server.mz.client.reponsehandler;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import org.jboss.netty.buffer.ChannelBuffer;

public class ResponseHandler implements Runnable {
	private BlockingQueue<ChannelBuffer> recordQueue;
	private ExecutorService threadPool;
	public static ResponseHandler instance = null;
	
	private ResponseHandler() {
		threadPool = Executors.newFixedThreadPool(10);
		recordQueue = new LinkedBlockingQueue<ChannelBuffer>(100000);
	}
	public void handleResponse(ChannelBuffer buf) {
		try {
			recordQueue.offer(buf);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	@Override
	public void run() {
		while(true)
		{
			try {
				ChannelBuffer buf = recordQueue.take();
				threadPool.execute(new ProcessRecord(buf));
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public static ResponseHandler getInstance() {
		if(instance == null){
			instance = new ResponseHandler();
			Thread handler = new Thread(instance);
			handler.start();
		}
		return instance;
	}
}
