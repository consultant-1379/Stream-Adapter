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
package com.ericsson.streamAdapter.util;

import java.text.DecimalFormat;

/**
 * This class is used to report metrics on events
 * 
 * @author eeimho
 * 
 */
public class EventMetrics {
	// Variables for counting records and files
	boolean aggregated = false; // aggregated metrics use record totals but no
								// details.
	private long events = 0;
    private long events_per_interval = 0;
	private long records = 0;
	private long invalidRecords = 0;
    private long lostRecords = 0;
    private long inits = 0;
	private long connects = 0;
	private long disconnects = 0;
    private long queueFull = 0;
	private long drops = 0;
	// Messages where we do not recognise the source ID.
	private long noSrc = 0;
	// Variables for times
	private long startTime = 0;
	private long toTime = 0;
	private long events_Failed_To_Send_Over_Socket = 0;

	public EventMetrics(boolean useAggregated) {
		aggregated = useAggregated;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		DecimalFormat threePlaces = new DecimalFormat("0.000");

		double interval = ((double) (toTime - startTime)) / 1000.0;

		if (!aggregated) {
            builder.append("TotalRecords=");
            builder.append(records);
			builder.append(";TotalEvents=");
			builder.append(events);
			builder.append(";Connects=");
			builder.append(connects);
			builder.append(";Disconnects=");
			builder.append(disconnects);
            builder.append(";ActiveConnections=");
            builder.append(connects - disconnects);
		    builder.append(";Drops=");
			builder.append(drops);
            builder.append(";QueueFull=");
            builder.append(queueFull);
            builder.append(";LostEvents=");
            builder.append(lostRecords);
            builder.append(";Invalid=");
            builder.append(invalidRecords);
            builder.append(";NoSourceId=");
            builder.append(noSrc);
            builder.append(";FailedSocket=");
            builder.append(events_Failed_To_Send_Over_Socket);
		}
//		builder.append(", Total:[Per " + interval + " sec interval] ");
//		builder.append(events_per_interval);
		builder.append(";EventsPerSec=");
		builder.append(interval > 0 ? threePlaces.format(events_per_interval / interval)
				: 0);

		if (!aggregated
				&& records != (inits + connects + disconnects + events + drops)) {
			// Warn anybody who knows that the record counts don't add up!
			// This could be harmless such as when a number rolls over.
			// Or it could require investigating...
			// TODO builder.append(" ** "); // add this back in when system
			// stabilises
			records = inits + connects + disconnects + events + invalidRecords;
		}
		reset();

		return builder.toString();
	}

	public void incrementRecords() {
		records++;
	}

	public void incrementInvalidRecords(long incrementAmount) {
		invalidRecords += incrementAmount;
	}

    public void incrementLostRecords(long incrementAmount) {
        lostRecords += incrementAmount;
    }

	public void incrementEvents() {
        events++;
        events_per_interval++;
	}

	public long getNumberOfEvents() {
		return events;
	}

	// Messages where we do not recognise the source ID.
	public void incrementNoSrc() {
		noSrc++;
	}

	public void incrementConnects() {
		connects++;
	}

	public long getNumberOfConnects() {
		return connects;
	}

	public void incrementDisconnects() {
		disconnects++;
	}
	
	public long getNumberOfDisconnects() {
		return disconnects;
	}

    public void incrementInits() {
        inits++;
    }

    public void incrementQueueFull(){
        queueFull++;
    }

	public void incrementDrops() {
		drops++;
	}

	public long getStartTime() {
		return startTime;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	public long getToTime() {
		return toTime;
	}

	public void setToTime(long toTime) {
		this.toTime = toTime;
	}

	private void reset() {
        events_per_interval = 0;
	}
	
    public void incrementSocketWriteFail(){
    	events_Failed_To_Send_Over_Socket++;
    }
}
