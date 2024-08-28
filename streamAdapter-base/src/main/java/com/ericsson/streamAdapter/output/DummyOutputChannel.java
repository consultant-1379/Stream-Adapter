package com.ericsson.streamAdapter.output;

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

import com.ericsson.streamAdapter.util.config.Config;

public class DummyOutputChannel extends OutputChannel {

    public DummyOutputChannel(Config config) {
        super(config);
    }

    @Override
    protected EventOutputChannel getOutputHandler() {
        return new EventOutputDummy();
    }
}
