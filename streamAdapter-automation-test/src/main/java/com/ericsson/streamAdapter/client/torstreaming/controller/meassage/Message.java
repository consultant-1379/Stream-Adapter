package com.ericsson.streamAdapter.client.torstreaming.controller.meassage;

import com.ericsson.streamAdapter.client.torstreaming.controller.Source;

public interface Message {
	public byte[] createMessage(Source source);
}
