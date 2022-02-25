package com.felixalacampagne.utils;
import java.util.Properties;



public class CmdArgMgr
{
public final static String CAM_NOKEY = "#nokey";
Properties mArgs = new Properties();

   public CmdArgMgr()
   {
   }
   public CmdArgMgr(String[] a_strArgs)
   {
      parseArgs(a_strArgs);
   }

   // Parse key value arguments of the form
   // -keyx[ valx]
   // If the key is present but no value then an empty
   // string is used for val.
   // If a value is present without a preceding key then a dummy
   // key value of '#nokeyx' is used, where x is numeric and
   // increases continuously from 1
   public void parseArgs(String[] a_strArgs)
   {
   String key = null;
   String val = null;
   int nokey = 1;

      if(a_strArgs == null)
         return;

      for (int i = 0; i < a_strArgs.length; i++)
      {
         if (a_strArgs[i].startsWith("-") == true)
         {
            key = a_strArgs[i].substring(1);
            i++;
            val = "";
            if(i < a_strArgs.length)
            {
               if(a_strArgs[i].startsWith("-") == true)
               {
                  i--;
               }
               else
               {
                  val = a_strArgs[i];
               }
            }
         }
         else
         {
            key = CAM_NOKEY + (nokey++);
            val = a_strArgs[i];

         }

         java.util.logging.Logger.getLogger("Command line parameter: " + key + " = " + val);
         mArgs.setProperty(key, val);
      }
   }

   // This should return null if the arg is not present in the list

   public String getArg(String argname)
   {
      return mArgs.getProperty(argname);
   }

   public String getArg(String argname, String defvalue)
   {
      return mArgs.getProperty(argname, defvalue);
   }

   public void setArg(String argname, String value)
   {
      try
      {
         // Will replace an existing value with the new value
         mArgs.setProperty(argname, value);
      }
      catch(Exception ex)
      {
      }
   }

}
