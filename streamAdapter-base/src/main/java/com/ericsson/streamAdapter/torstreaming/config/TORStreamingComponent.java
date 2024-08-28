/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2013
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.streamAdapter.torstreaming.config;

/**
 * This class defines the TOR streaming component that attaches to the end of the TOR client stream.
 * See META-INF/services/com.ericsson.oss.mediation.engine.netty.Component for the definition of this
 * component for Java SPI
 * 
 * @author eeimho
 */

import com.ericsson.oss.mediation.engine.netty.AbstractComponent;
import com.ericsson.oss.mediation.engine.netty.HandlerLifecycle;
import com.ericsson.streamAdapter.torstreaming.listener.TORStreamingListener;
import org.jboss.netty.channel.ChannelHandler;

public class TORStreamingComponent extends AbstractComponent {


    /**
     * Constructor, instantiate the component and define its life cycle
     */
    public TORStreamingComponent() {
        super("1.0", HandlerLifecycle.SHARED);
    }


    /**
     * Return the object that handles the channel interface: connects, disconnects, and message reception on the channel
     */
    @Override
    public ChannelHandler getHandler() {
        return new TORStreamingListener();
    }

}
