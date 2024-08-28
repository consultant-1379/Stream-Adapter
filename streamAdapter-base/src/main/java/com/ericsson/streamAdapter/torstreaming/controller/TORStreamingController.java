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

package com.ericsson.streamAdapter.torstreaming.controller;

import com.ericsson.oss.mediation.engine.netty.NettyEngine;
import com.ericsson.oss.mediation.engine.netty.core.Engine;
import com.ericsson.streamAdapter.output.OutputThreadHandler;
import com.ericsson.streamAdapter.torstreaming.listener.TORStreamingListener;
import com.ericsson.streamAdapter.torstreaming.config.TORStreamingConfigurationProvider;
import com.ericsson.streamAdapter.util.config.Config;

import com.ericsson.streamAdapter.util.config.StreamAdapterConstants;
import com.ericsson.streamAdapter.util.config.StreamAdapterDefaults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.streamAdapter.util.Metrics;
import com.ericsson.streamAdapter.util.cache.Cache;


/**
 * This class is responsible for controlling a TOR Streaming client.
 *
 * @author eeimho
 *
 */

public class TORStreamingController implements Runnable, Metrics {

    private static final Logger logger = LoggerFactory.getLogger(TORStreamingController.class);
    private static final long STATE_TRANSITION_DELAY = 10000;
    private NettyEngine engine; // The Netty engine reference
    private TORStreamingListener torStreamingListener = null; // The listener that will receive events on the Netty channel
    private String datapathName;
    private TORStreamingState torStreamingState = TORStreamingState.Disconnected;
    private String host;
    private int port;
    private Config config;
    private String instanceId;
    private OutputThreadHandler handler;

    /**
     * Constructor, initialize the TOR streaming controller
     *
     * @param instanceId
     * @param config
     * @param handler
     */
    public TORStreamingController(String instanceId, Config config, OutputThreadHandler handler) {
        this.instanceId = instanceId;
        this.handler = handler;
        this.config = config;
        this.host = config.getValue(StreamAdapterConstants.INPUT_IP, StreamAdapterDefaults.INPUT_IP);
        this.port = config.getValue(StreamAdapterConstants.INPUT_PORT, StreamAdapterDefaults.INPUT_PORT);
        // Set the data path name to be unique
        datapathName = instanceId + "_" + this.host + ':' + this.port;
        TORStreamingListener.setTorStreamingController(this);
        TORStreamingConfigurationProvider.setTorStreamingController(this);

    }

    /**
     * Start the TOR streaming client, this runs the TOR streaming client handling state machine
     */
    public void start() {
        logger.debug("TOR streaming [{}] client starting . . .", instanceId);
        // Connect to TOR streaming
        torStreamingState = TORStreamingState.Connecting;
        logger.debug("[{}] : DataPath {}: -->Connecting", instanceId, datapathName);
        // Run the TOR streaming state machine as a separate thread
        new Thread(this).start();
    }

    /**
     * Resetting the TOR streaming client.
     */
    public void reset() {
        logger.debug("Ordering TOR streaming [{}] client reset {}", instanceId, datapathName);
        torStreamingState = TORStreamingState.Resetting;
        logger.debug("[{}] : DataPath {}: -->Resetting", instanceId, datapathName);
    }

    /**
     * Stops the TOR streaming client.
     */
    public void stop() {
        logger.debug("Ordering TOR streaming [{}] client stop {}", instanceId, datapathName);
        torStreamingState = TORStreamingState.Disconnecting;
        logger.debug("[{}] : DataPath {}: -->Disconnecting", instanceId, datapathName);
    }

    /**
     * Return the Client Config details.
     *
     * @return
     */
    public Config getConfig() {
        return config;
    }

    /**
     * This method runs the TOR streaming state machine in a separate thread
     */
    @Override
    public void run() {
        // Run in an endless loop
        while (true) {
            handleTransition();
        }

    }

    /**
     * Set the TOR streaming client listener, called back by the listener when it is started by the TOR streaming client
     *
     * @param torStreamingListener
     */
    public void setListener(TORStreamingListener torStreamingListener) {
        this.torStreamingListener = torStreamingListener;
    }

    /**
     * Called to output metrics on incoming events
     */
    @Override
    public void outputMetrics() {
        if (torStreamingListener != null) {
            torStreamingListener.outputMetrics();
        }
    }


    /**
     * @return
     */
    public OutputThreadHandler getHandler() {
        return handler;
    }

    public TORStreamingState getEngineState() {
        return torStreamingState;
    }

    public NettyEngine getNettyEngine() {
        return Engine.getInstance();
    }

    public long getTransititionDelay() {
        return STATE_TRANSITION_DELAY;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void handleTransition() {

        // Handle the transitions in the machine
        // eprabab: Added this for junit tcs verification to avoid thread run method call
        switch (torStreamingState) {
            case Disconnected: {
                logger.debug("[{}] : DataPath {}: Disconnected", instanceId, datapathName);
                return;
            }

            case Connecting: {
                logger.debug("[{}] : DataPath {}: Connecting . . .", instanceId, datapathName);
                engine = getNettyEngine();
                engine.start();
                torStreamingState = TORStreamingState.Connected;
                //Believe or not this is the correct place to set the connection state.
                //The reason being, if the connection fails then the exceptionCaught() method is invoked in the listener class.
                //This will reset the connection state. If you set the connection state to connected after startDataPath this will override
                // the reset state if an exception occurred.
                engine.startDataPath(datapathName);

                logger.debug("[{}] : DataPath {}: -->Connected", instanceId, datapathName);
                break;
            }

            case Connected: {
                logger.debug("[{}] : DataPath {}: Connected", instanceId, datapathName);
                // Do nothing, everything is good!
                break;
            }

            case Resetting: {
                logger.debug("[{}] : DataPath {}: Resetting", instanceId, datapathName);

                // Engine is resetting, stop the engine
                engine = getNettyEngine(); // eprabab: Added this for junit tcs verification;
                engine.stopDataPath(datapathName);
                engine.stop();

                // Order a reconnect
                torStreamingState = TORStreamingState.Connecting;
                logger.debug("[{}] : DataPath {}: -->Connecting", instanceId, datapathName);
                break;
            }

            case Disconnecting: {
                logger.debug("[{}] : DataPath {}: Disconnecting", instanceId, datapathName);
                engine = getNettyEngine(); // eprabab: Added this for junit tcs verification;
                // Engine is disconnecting, stop the engine
                engine.stopDataPath(datapathName);
                engine.stop();

                // Set to disconnected
                torStreamingState = TORStreamingState.Disconnected;
                logger.debug("[{}] : DataPath {}: -->Disconnected", instanceId, datapathName);

                break;
            }

            default: {
                logger.warn("[{}] : DataPath {}: invalid state", instanceId, datapathName);
                break;
            }
        }

        // Sleep for the specified interval between transitions
        try {
            Thread.sleep(getTransititionDelay());
        } catch (InterruptedException e) {
            // Interrupt, so disconnect
            logger.debug("TOR streaming [{}] client interrupt ordered", instanceId);
            torStreamingState = TORStreamingState.Disconnecting;
        }

    }
}
