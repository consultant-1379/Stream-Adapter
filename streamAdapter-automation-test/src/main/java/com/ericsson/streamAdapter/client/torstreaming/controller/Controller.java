package com.ericsson.streamAdapter.client.torstreaming.controller;

import java.util.ArrayList;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.ericsson.streamAdapter.client.torstreaming.controller.sources.ENBFaultSource;
import com.ericsson.streamAdapter.client.torstreaming.controller.sources.ENBSequenceSource;
import com.ericsson.streamAdapter.client.torstreaming.controller.sources.ENBSource;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;

import com.ericsson.streamAdapter.server.logger.AutomationLogger;

import static com.ericsson.streamAdapter.server.utils.Constants.*;

public class Controller {
	Logger logger = AutomationLogger.getLogger();
	private int numberOfNodes = 0;
	private int numberOfSequenceEventsNodes = 0;
	private int numberOfFaultNodes = 0;
	private int numberOfEventsPerNode = 0;
	private boolean customEvents = false;

	private List<Source> sources = new ArrayList<Source>();
	private Channel channel;

	public Controller() {
		this.numberOfNodes = NUMBER_OF_WORKING_NODES;
		this.setNumberOfSequenceEventsNodes(NUMBER_OF_WORKING_NODES_SEQUENCE);
		this.numberOfFaultNodes = NUMBER_OF_FAULT_NODES;
		this.numberOfEventsPerNode = NUMBER_OF_EVENTS_PER_NODE;
		this.customEvents = CUSTOM_EVENT;
	}

	public void initialiseController() {
		for (int i = 1; i <= getNumberOfNodes(); i++) {
			Source source = new ENBSource(i);
			setSourceAributes(source);
		}
		for (int i = getNumberOfNodes() + 1; i <= getNumberOfNodes()
				+ getNumberOfFaultNodes(); i++) {
			Source source = new ENBFaultSource(i);
			setSourceAributes(source);
		}
		for (int i = getNumberOfNodes() + getNumberOfFaultNodes() + 1; i <= getNumberOfNodes()
				+ getNumberOfFaultNodes() + getNumberOfSequenceEventsNodes(); i++) {
			Source source = new ENBSequenceSource(i);
			setSourceAributes(source);
		}
	}

	private void setSourceAributes(Source source) {
		source.setNoOfEventsToGenerate(numberOfEventsPerNode);
		source.setChannel(channel);
		source.setCustomEvent(customEvents);
		sources.add(source);
	}

	public void startController() {
		logger.debug(Controller.class.getSimpleName()
				+ " : Number of working nodes : " + NUMBER_OF_WORKING_NODES
				+ ", Number of faulty nodes : " + NUMBER_OF_FAULT_NODES
				+ ", Number of Events per node : " + NUMBER_OF_EVENTS_PER_NODE
				+ ", Total number of Events : " + NUMBER_OF_EVENTS_PER_NODE
				* (NUMBER_OF_FAULT_NODES + NUMBER_OF_WORKING_NODES)
				+ ", Is Custom Event : " + CUSTOM_EVENT);
		ExecutorService threadPool = Executors.newFixedThreadPool(10);
		for (Source source : sources) {
			Future<?> future = threadPool.submit(source);
			source.setTask(future);
		}
	}

	public int getNumberOfNodes() {
		return numberOfNodes;
	}

	public void setNumberOfNodes(int numberOfNodes) {
		this.numberOfNodes = numberOfNodes;
	}

	public int getNumberOfEvents() {
		return numberOfEventsPerNode;
	}

	public void setNumberOfEvents(int numberOfEvents) {
		this.numberOfEventsPerNode = numberOfEvents;
	}

	public int getNumberOfFaultNodes() {
		return numberOfFaultNodes;
	}

	public void setNumberOfFaultNodes(int numberOfFaultNodes) {
		this.numberOfFaultNodes = numberOfFaultNodes;
	}

	public Channel getChannel() {
		return channel;
	}

	public void setChannel(Channel channel) {
		this.channel = channel;
	}

	/**
	 * @return the numberOfSequenceEventsNodes
	 */
	public int getNumberOfSequenceEventsNodes() {
		return numberOfSequenceEventsNodes;
	}

	/**
	 * @param numberOfSequenceEventsNodes
	 *            the numberOfSequenceEventsNodes to set
	 */
	public void setNumberOfSequenceEventsNodes(int numberOfSequenceEventsNodes) {
		this.numberOfSequenceEventsNodes = numberOfSequenceEventsNodes;
	}
}
