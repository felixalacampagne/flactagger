package com.felixalacampagne.utils;

import java.io.Closeable;
import java.io.File;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;

public class Utils
{

	public Utils()
	{
		// TODO Auto-generated constructor stub
	}

   public static void safeClose(AutoCloseable t)
   {
      if(t == null)
         return;
      try
      {
         t.close();
      } catch (Exception e)
      {
         e.printStackTrace();
      }      
   }
	
   public static void safeClose(Object t)
   {
      if(t == null)
         return;
      try
      {
         if (t instanceof Closeable)
         {
            // this is a sub-class of AutoCloseable as from Java 7 (but not before)
            // so call should go to the AutoCloseable version in theory
            ((Closeable)t).close();
         }
         else
         {
            Method cls = null;
            try
            {
               cls = t.getClass().getMethod("close");
            }
            catch(Exception ex)
            {
               ex.printStackTrace();;
            }
            if(cls != null)
            {
               cls.invoke(t);
            }
         }
      } 
      catch (Exception e)
      {
         /* Whole point is to be able to ignore these exceptions */
      }      
   }	
	
   public static String getTimestampFN()
   {
      return getTimestampFN(new Date());
   }

   public static String getTimestampFN(long date)
   {
      return getTimestampFN(new Date(date));
   }
   public static String getTimestampFN(Date date)
   {
   SimpleDateFormat sdf = new SimpleDateFormat("yyMMddHHmm");
      
      return sdf.format(date);
   }
   
   /**
    * Convert string to Integer. Uses the first digits up to 
    * the first non-digit (unlike valueOf, which throws an exception if there
    * are any non-digits in the string)
    * @param str
    * @return Integer value, null if str is not a valid decimal number
    */
   public static Integer str2Int(String str)
   {
   Integer i = null;
      try
      {
      	str = str.replaceFirst("[^0-9].*$", "");
         i = Integer.valueOf(str);
      }
      catch(Exception ex)
      {
         // Ignore exceptions
      }
      return i;
   }

  
	public static String safeString(String s)
	{
		return (s==null) ? "" : s;
	}

	/**
	 * Return null for a null or empty string. 
	 * 
	 * Used to avoid writing an empty tag
	 * 
	 * @param val
	 * @return
	 */
	public static String getValueOrNull(String val)
	{
		return getValueOrDefault(val, null); //  ((val == null) || (val.isEmpty())) ? null : val; 
	}

	// Returns defvalue if val is null or empty, otherwise returns val.
	// defvalue can be null
	public static String getValueOrDefault(String val, String defvalue)
	{
		return ((val == null) || (val.isEmpty())) ? defvalue : val; 
	}

	
	public static int safeValueOf(String s)
	{
	int i = 0;
		try
		{
			i = Integer.valueOf(s);
		}
		catch(Exception ex)
		{
			// Ignore exceptions
		}
		return i;
	}

}
