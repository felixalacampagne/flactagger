package com.felixalacampagne.utils;

import java.io.Closeable;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;

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

}
