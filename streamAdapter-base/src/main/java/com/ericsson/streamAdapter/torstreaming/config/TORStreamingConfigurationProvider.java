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

import com.ericsson.oss.mediation.component.multiplex.client.StreamMultiplexInitServerComponent;
import com.ericsson.oss.mediation.component.multiplex.client.config.MultiplexClientConfiguration;
import com.ericsson.oss.mediation.component.muxdecoder.MultiplexDecoderComponent;
import com.ericsson.oss.mediation.engine.netty.ConfigurationProvider;
import com.ericsson.oss.mediation.engine.netty.DatapathProvider;
import com.ericsson.oss.mediation.engine.netty.EngineConfigurationException;
import com.ericsson.oss.mediation.engine.netty.configuration.Components;
import com.ericsson.oss.mediation.engine.netty.configuration.DatapathConfiguration;
import com.ericsson.oss.mediation.engine.netty.configuration.EngineConfiguration;
import com.ericsson.oss.mediation.engine.netty.extension.configuration.ExtensionContext;
import com.ericsson.streamAdapter.torstreaming.controller.TORStreamingController;
import com.ericsson.streamAdapter.util.config.Config;
import com.ericsson.streamAdapter.util.config.StreamAdapterConstants;
import com.ericsson.streamAdapter.util.config.StreamAdapterDefaults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

/**
 * @author eeimho
 */
public class TORStreamingConfigurationProvider implements ConfigurationProvider, DatapathProvider {

    private static final Logger logger = LoggerFactory.getLogger(TORStreamingConfigurationProvider.class);
    private static final String MULTIPLEX_DECODER = MultiplexDecoderComponent.class.getName();
    private static final String STREAM_MULTIPLX = StreamMultiplexInitServerComponent.class.getName();
    private static final String TOR_STREAMING = TORStreamingComponent.class.getName();
    private static final String[] componentArray = { MULTIPLEX_DECODER, STREAM_MULTIPLX, TOR_STREAMING };
    private static TORStreamingController torStreamingController;

    public static void setTorStreamingController(TORStreamingController theTorStreamingController) {
        torStreamingController = theTorStreamingController;
    }

    @Override
    public <T extends Serializable> T getComponentConfiguration(final String dataPathName, final int componentId, final Class<T> t) {
        // We only provide specific configuration for the multiplexing component, we must specify a filter, a group ID and and user id
        if (!t.equals(MultiplexClientConfiguration.class)) {
            return getDefaultConfiguration(t);
        }

        // Set the filter, group Id, and user id
        MultiplexClientConfiguration mcc = new MultiplexClientConfiguration();

        try {
            Config config = torStreamingController.getConfig();
            short userId = config.getValue(StreamAdapterConstants.USER_ID, StreamAdapterDefaults.USER_ID).shortValue();
            if(userId == 9999){
                userId = (short)(Math.random() * 10000);
            }
            mcc.setUid(userId);
            mcc.setFid(config.getValue(StreamAdapterConstants.FILTER_ID, StreamAdapterDefaults.FILTER_ID).shortValue());
            mcc.setGid(config.getValue(StreamAdapterConstants.GROUP_ID, StreamAdapterDefaults.GROUP_ID));

        } catch (Exception e) {
            logger.warn("could not set filter, group, and user properties from configuration for TOR streaming", e);
        }

        logger.debug("multiplex client definition:" + mcc.getFid() + "," + mcc.getGid() + "," + mcc.getUid());
        // known error wit hthe way Java handles generics
        return (T) mcc;
    }

    /**
     * Allows Subscription for reconfiguration events on the data path, subscriptions are not supported here
     *
     * @param dataPathName
     *            : The data path the component is on
     * @param component
     *            ID: The ID of the component instance
     * @param type
     *            : The type of configuration class to return
     */
    @Override
    public <T extends Serializable> void subscribeForReconfiguration(String datapathName, int componentId, Class<T> type) {
    }

    /**
     * Gets the engine configuration, the default engine configuration is returned
     *
     * @return THe default engine configuration
     */
    @Override
    public EngineConfiguration getEngineConfiguration() {
        return this.getDefaultConfiguration(EngineConfiguration.class);
    }

    /**
     * Gets the extension context, the default extension context is returned
     *
     * @return The default extension context
     */
    @Override
    public ExtensionContext getExtensionContext() {
        return this.getDefaultConfiguration(ExtensionContext.class);
    }

    /**
     * This method is called to get the data path that the TOR streaming client will connect to
     *
     * @param datapath
     *            : The data path name
     * @return The data path configuration
     */
    @Override
    public DatapathConfiguration getDatapathDefinition(String datapath) {
        DatapathConfiguration datapathConfiguration = new DatapathConfiguration();
        datapathConfiguration.setName(datapath);

        // Set the components for this data path
        Components components = new Components();
        components.setComponent(componentArray);
        datapathConfiguration.setComponents(components);

        try {
            // Set the data path parameters using xStream configuration parameters
            Config config = torStreamingController.getConfig();
            datapathConfiguration.setAddress(config.getValue(StreamAdapterConstants.INPUT_IP, StreamAdapterDefaults.INPUT_IP));
            datapathConfiguration.setPort(config.getValue(StreamAdapterConstants.INPUT_PORT, StreamAdapterDefaults.INPUT_PORT));
            datapathConfiguration.setSocketFactory(DatapathConfiguration.SocketFactory.NIO_SOCKET_CLIENT);
        } catch (Exception e) {
            logger.error("Could not set connection properties from configuration for TOR streaming", e);
        }
        return datapathConfiguration;
    }

    /**
     * Method to return the default configuration (default constructor on the type) for the given class type
     *
     * @param type
     *            The configuration class type
     * @return The default configuration
     */
    private <T> T getDefaultConfiguration(final Class<T> type) {
        try {
            return type.newInstance();
        } catch (Exception e) {
            throw new EngineConfigurationException("Can't create configuration for " + type + " reason: " + e.getMessage());
        }
    }
}
