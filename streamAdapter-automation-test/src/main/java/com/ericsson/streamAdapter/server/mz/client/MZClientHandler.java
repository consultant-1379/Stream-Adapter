package com.ericsson.streamAdapter.server.mz.client;

import com.ericsson.streamAdapter.server.logger.AutomationLogger;
import com.ericsson.streamAdapter.server.mz.client.reponsehandler.ResponseHandler;
import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;

public class MZClientHandler extends SimpleChannelHandler {
	Logger  logger = AutomationLogger.getLogger();
	ResponseHandler response;
	@Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
		logger.info(MZClientHandler.class.getSimpleName()+ " : Channel created between TOR Client and MZ Client");
        ChannelBuffer buf = (ChannelBuffer) e.getMessage();
    	response = ResponseHandler.getInstance();
        response.handleResponse(buf);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
    	logger.error(MZClientHandler.class.getSimpleName()+ " : Exception from TOR Cleint "+e.getCause());
    }
}
