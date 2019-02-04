package com.felixalacampagne.flactagger.gui;

import java.util.logging.ErrorManager;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;


public class CaptureLogHandler extends Handler
{
private final CaptureLogFormatter fmt = new CaptureLogFormatter();
private final CaptureLogPublisher publishTo;
protected Level logLevel;
   
   public CaptureLogHandler(CaptureLogPublisher publishto)
   {
      super();
      publishTo = publishto;

      fmt.setFormat("%4$s: %5$s%6$s%n");
      setFormatter(fmt);
      setLevel(Level.INFO);
   }

   public void publish(LogRecord record) 
   {
      if (!isLoggable(record)) 
      {
         return;
      }
      String msg="";
      try 
      {
         msg = getFormatter().format(record);
      } 
      catch (Exception ex) 
      {
         // report the exception to any registered ErrorManager.
         reportError(null, ex, ErrorManager.FORMAT_FAILURE);
         msg = "ERROR: Failed to format LogRecord: " + ex.toString();
     }
     publishTo.publishMessage(msg);    

   }

   @Override
   public void flush()
   {
      return;
   }

   @Override
   public void close() throws SecurityException
   {
      return;
   }  
}
