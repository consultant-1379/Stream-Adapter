package com.ericsson.streamAdapter.server;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;

public class TORStreamingServerFactory implements ChannelPipelineFactory {
    @Override
    public ChannelPipeline getPipeline() throws Exception {
        // Create and configure a new pipeline for a new channel.
        return Channels.pipeline(new TORStreamingServerHandler());
    }
}
