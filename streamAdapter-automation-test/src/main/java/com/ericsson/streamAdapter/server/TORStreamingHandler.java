package com.ericsson.streamAdapter.server;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;

import com.ericsson.streamAdapter.client.torstreaming.controller.Controller;
import com.ericsson.streamAdapter.server.logger.AutomationLogger;

public class TORStreamingHandler extends TORStreamingServerHandler {
	Logger logger = AutomationLogger.getLogger();
    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
    	logger.info(TORStreamingHandler.class.getName()+" :"+" Channel created betweeen TOR Client and MZ");
        Channel ch = e.getChannel();

		Controller control = new Controller();
		control.setChannel(ch);
		control.initialiseController();
		control.startController();
    }
}
