package com.ericsson.streamAdapter.server;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.*;

import static com.ericsson.streamAdapter.server.StreamOutProtocol.*;

public class TORStreamingServerHandler extends SimpleChannelHandler {
    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        System.out.println("channelConnected");
        Channel ch = e.getChannel();
        ChannelBuffer channelBuffer = ChannelBuffers.wrappedBuffer(getInitilizationMessage());
        ch.write(channelBuffer);

        Thread.sleep(1000);
        channelBuffer = ChannelBuffers.wrappedBuffer(getConnectionMessage());
        ch.write(channelBuffer);

        Thread.sleep(1000);
        channelBuffer = ChannelBuffers.wrappedBuffer(getEventMessage());
        ch.write(channelBuffer);

        Thread.sleep(1000);
        channelBuffer = ChannelBuffers.wrappedBuffer(getDisconnectMessage());
        ch.write(channelBuffer);

    }

    @Override
    public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        super.channelDisconnected(ctx, e);
        System.out.println("channelDisconnected");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {

        e.getCause().printStackTrace();

        Channel ch = e.getChannel();
        ch.close();
    }


}
