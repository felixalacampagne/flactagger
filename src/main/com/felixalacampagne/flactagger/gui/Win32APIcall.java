package com.felixalacampagne.flactagger.gui;

import java.lang.reflect.Method;

/**
 * This is a lot of effort to make a single Win32 system call to keep the system from going to sleep.
 * 
 * It uses the JNA library purely via Java reflection. This means that the JNA library is not
 * required to compile the class. If JNA library is available at runtime it will be used, if
 * not then the systemIsBusy call will return false to indicate that it could not execute 
 * the api call which will allow alternative methods to stop sleep to be used.
 */
public class Win32APIcall
{
private Object K32=null; // Kernel32
private Method mthSetThreadExecutionState = null;
private boolean bInitFailed = false;
private int ES_SYSTEM_REQUIRED = 1;   // com.sun.jna.platform.win32.WinBase.ES_SYSTEM_REQUIRED
public Win32APIcall()
{
   initKernel32();
}

public void initKernel32()
{
   
   if((K32 != null) || bInitFailed)
      return;

   bInitFailed = true;
   try
   {
      Class<?> native_class = Class.forName("com.sun.jna.Native");
      Class<?> k32_class = Class.forName("com.sun.jna.platform.win32.Kernel32");
      Method load_method = native_class.getMethod("load", String.class, Class.class);
      K32 = load_method.invoke(null, "kernel32", k32_class);
      mthSetThreadExecutionState = k32_class.getMethod("SetThreadExecutionState", int.class);
      bInitFailed = false;
   }
   catch(Error err)
   {
      err.printStackTrace();
   }
   catch(NoSuchMethodException nsmex)
   {
      System.out.println("Win32APIcall.initKernel32: cannot access Win32 method: keep awake will not be possible");
   }
   catch(Exception ex)
   {

      ex.printStackTrace();
   }
}
   
public boolean systemIsBusy()
{
   if(mthSetThreadExecutionState != null)
   {
      try
      {
         mthSetThreadExecutionState.invoke(K32, ES_SYSTEM_REQUIRED);
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
      return true;
   }
   return false;
}

}
