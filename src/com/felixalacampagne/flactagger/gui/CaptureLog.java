package com.felixalacampagne.flactagger.gui;

import java.io.ByteArrayOutputStream;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

public class CaptureLog extends StreamHandler
{

private final ByteArrayOutputStream bstream = new ByteArrayOutputStream();
private final CaptureLogFormatter fmt = new CaptureLogFormatter();
private final CaptureLogPublisher publishTo;

	public CaptureLog(CaptureLogPublisher publishto)
	{
		super();
		publishTo = publishto;
		this.setOutputStream(bstream);
		this.setLevel(Level.INFO);
		fmt.setFormat("%4$s: %5$s%6$s%n");
		this.setFormatter(fmt);

	}

	public void publish(LogRecord record) 
	{
		// This probably has so many issues with multithreading that there is no point even trying to make it safe...
		bstream.reset();
		super.publish(record);
		flush();
		if(bstream.size() > 0)
		{
			publishTo.publishMessage(bstream.toString());
		}
	}

}
