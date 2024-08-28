package com.ericsson.streamAdapter.torstreaming.controller;

/**
 * An enum to hold the state of the connection to TOR streaming
 * 
 * @author eeimho
 */
public enum TORStreamingState {
    Disconnected, Connecting, Connected, Resetting, Disconnecting

}
