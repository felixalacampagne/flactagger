package com.smallcatutilities.flactagger.gui;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;



public class CaptureLogFormatter extends SimpleFormatter
{
	protected final static String DEFAULT_FORMAT = "%1$tk:%1$tM:%1$tS %4$s: %5$s%n"; //"%1$tY%1$tm%1$td %1$tk:%1$tM:%1$tS %4$s: %5$s%n";
	private String format = DEFAULT_FORMAT;
	@Override
   public String format(LogRecord logrecord)
   {  
      Date dat = new Date();
      dat.setTime(logrecord.getMillis());
      String s;
      if(logrecord.getSourceClassName() != null)
      {
         s = logrecord.getSourceClassName();
         if(logrecord.getSourceMethodName() != null)
            s += "." + logrecord.getSourceMethodName().toString();
      } 
      else
      {
         s = logrecord.getLoggerName();
      }
      String s1 = formatMessage(logrecord);
      String s2 = "";
      if(logrecord.getThrown() != null)
      {
           StringWriter stringwriter = new StringWriter();
           PrintWriter printwriter = new PrintWriter(stringwriter);
           printwriter.println();
           logrecord.getThrown().printStackTrace(printwriter);
           printwriter.close();
           s2 = stringwriter.toString();
       }
       return String.format(format, new Object[] {
           dat, s, logrecord.getLoggerName(), logrecord.getLevel().getLocalizedName(), s1, s2
       });
   }
   
   /**
    * The format string follows the java.util.logging.SimpleFormatter convention: 
    * Param 1: date - a Date object representing event time of the log record.
    * Param 2: source - a string representing the caller, if available; otherwise, the logger's name.
    * Param 3: logger - the logger's name.
    * Param 4: level - the log level.
    * Param 5: message - the formatted log message returned from the Formatter.formatMessage(LogRecord) method. It uses java.text formatting and does not use the java.util.Formatter format argument.
    * Param 6: thrown - a string representing the throwable associated with the log record and its backtrace beginning with a newline character, if any; otherwise, an empty string.
    *
    * The default format is: "%1$tk:%1$tM:%1$tS %4$s: %5$s%n"
    * which prints a line like;
    * 17:30:01 WARNING: Time to put down the tools
    * @param newformat
    */
   public void setFormat(String newformat)
   {
      format = newformat;
   }		
}

