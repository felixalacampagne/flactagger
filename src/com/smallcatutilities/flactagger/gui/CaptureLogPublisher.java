package com.smallcatutilities.flactagger.gui;

/**
 * Interface to allow CaptureLog to write a message to the SwiingWorker 
 * publish queue, which turns out to be protected.
 */
public interface CaptureLogPublisher
{
	public void publishMessage(String logmessage);
}
