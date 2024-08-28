package com.ericsson.streamAdapter.server.mz.client.reponsehandler;

import com.ericsson.streamAdapter.server.logger.AutomationLogger;
import com.ericsson.streamAdapter.server.utils.EventDetails;
import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;

import java.util.*;

public class ProcessRecord implements Runnable {
	Logger  logger = AutomationLogger.getLogger();
	private int eventsLength;
	private static Map<String,List<EventDetails>> eventsInformation = Collections.synchronizedMap(new HashMap<String,List<EventDetails>>());
	private ChannelBuffer buf;
	public ProcessRecord(ChannelBuffer buf) {
		this.buf = buf;
	}

	@Override
	public void run() {
		logger.debug(ProcessRecord.class.getSimpleName() +" : Total buffer Length processed by current thread : "+buf.array().length);
		int eventByteCount = getNumberOfEvents(buf);
		logger.debug(ProcessRecord.class.getSimpleName() +" : Total events processed : "+eventByteCount);
		for(int i = 0 ; i < eventByteCount ; i++){
			ChannelBuffer buffer = buf.readBytes(eventsLength);
			EventDetails event = EventDetails.createEventDetails(buffer);
			String ipAdress = event.getIpAddressAsString();
			if(getEventsInformationMap().get(ipAdress)==null)
			{
				List<EventDetails> eventsDetailsList = new ArrayList<EventDetails>();
				eventsDetailsList.add(event);
				getEventsInformationMap().put(ipAdress, eventsDetailsList);
			}
			else{
				List<EventDetails> eventsDetailsList = getEventsInformationMap().get(ipAdress);
				eventsDetailsList.add(event);
			}
			logger.debug(ProcessRecord.class.getSimpleName() +
					" Event received for source : "+ ipAdress +
							" Total number of events received : " +getEventsInformationMap().get(ipAdress).size() +
									" Event details "+event);
		}
	}

	private int getNumberOfEvents(ChannelBuffer buffer) {	
		eventsLength = buffer.getShort(0);
		logger.debug(ProcessRecord.class.getSimpleName() +" : Per event length value from message : "+eventsLength);
		int eventCount = buffer.array().length/eventsLength;
		return eventCount;
	}

	public static Map<String,List<EventDetails>> getEventsInformationMap() {
		return eventsInformation;
	}
	
}
