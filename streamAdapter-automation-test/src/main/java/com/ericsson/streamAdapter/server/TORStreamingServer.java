package com.ericsson.streamAdapter.server;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


public class TORStreamingServer {
	 ServerBootstrap bootstrap;
    private final int port;

    public TORStreamingServer(int port) {
        this.port = port;

    }

    public static void main(String[] args) throws Exception {
        new TORStreamingServer(10898).start();
    }

    public void start() {
        Executor bossPool = Executors.newCachedThreadPool();
        Executor workerPool = Executors.newCachedThreadPool();
        ChannelFactory factory = new NioServerSocketChannelFactory(bossPool, workerPool);

        bootstrap = new ServerBootstrap(factory);

        bootstrap.setPipelineFactory(new TORStreamingServerFactory());
        bootstrap.setOption("child.tcpNoDelay", true);
        bootstrap.setOption("child.keepAlive", true);

        bootstrap.bind(new InetSocketAddress(port));
    }
    
    public void shutDown(){
    	bootstrap.shutdown();
    }
    
}
