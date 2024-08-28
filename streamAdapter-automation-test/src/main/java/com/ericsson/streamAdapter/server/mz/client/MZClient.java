package com.ericsson.streamAdapter.server.mz.client;

import java.net.InetSocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

public class MZClient {

	private int port;
	ServerBootstrap bootstrap;
	public MZClient(int port)
	{
		this.port = port;
	}
	public static void main(String[] args) throws Exception {
        Executor bossPool = Executors.newCachedThreadPool();
        Executor workerPool = Executors.newCachedThreadPool();
        ChannelFactory factory = new NioServerSocketChannelFactory(bossPool, workerPool);

        ServerBootstrap bootstrap = new ServerBootstrap(factory);

        bootstrap.setPipelineFactory(new ChannelPipelineFactory(){
        	    public ChannelPipeline getPipeline() throws Exception {
        	        // Create and configure a new pipeline for a new channel.
        	        return Channels.pipeline(new MZClientHandler());
        	    }});
        bootstrap.setOption("child.tcpNoDelay", true);
        bootstrap.setOption("child.keepAlive", true);

        bootstrap.bind(new InetSocketAddress(4010));
	}
	
	public void start()
	{

        Executor bossPool = Executors.newCachedThreadPool();
        Executor workerPool = Executors.newCachedThreadPool();
        ChannelFactory factory = new NioServerSocketChannelFactory(bossPool, workerPool);

        bootstrap = new ServerBootstrap(factory);

        bootstrap.setPipelineFactory(new ChannelPipelineFactory(){
        	    public ChannelPipeline getPipeline() throws Exception {
        	        // Create and configure a new pipeline for a new channel.
        	        return Channels.pipeline(new MZClientHandler());
        	    }});
        bootstrap.setOption("child.tcpNoDelay", true);
        bootstrap.setOption("child.keepAlive", true);

        bootstrap.bind(new InetSocketAddress(port));
	}
	
	public void shutDown()
	{
		bootstrap.shutdown();
	}
}