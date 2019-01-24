package com.felixalacampagne.flactagger.gui;

import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Robot;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinBase;

/**
 * Attempts to prevent the system from entering sleep mode while a lengthy operation is being performed.
 * On Windows the system will enter sleep/hibernate mode even when the CPU is at 100%. There is a Win32 API
 * call which will stop sleep (SetThreadExecutionState) but the JNI call would add an external dependency 
 * that I want to avoid, same for the JNA library in github.
 * 
 * Since I don't want the noise associated with the solution requiring a mouse on a subwoofer, and 
 * my Dyson cool fan doesn't have any fan blades to strap the mouse to I will have to resort to 
 * moving the mouse programmatically. Don't know if it will work but a similar thing done in Perl
 * did help.
 *
 * The threading stuff didn't turn out like I intended as I didn't realise that a thread that has
 * finished it's run() cannot simply be re-started. I wanted to avoid multiple mouse moving threads
 * from being started. I think I've managed that with the restricted access constructor and by not
 * exposing the static instance of the class.
 * 
 * @author carmstro
 *
 */

public class KeepOnTruckin extends Thread
{
private static KeepOnTruckin mInstance = null;
private int mInterval = 10; //120; // seconds
private boolean mRun = false;
private Point mLastPos = null;
private int mDir = -2;
private Kernel32 K32=null;

	public static synchronized void startTruckin()
	{
		if(mInstance == null)
		{
			mInstance = new KeepOnTruckin();
			mInstance.start();
		}
		
		return;
	}

	// To be called by the caller to terminate the running thread
	public static synchronized void endOfTheRoad()
	{
		if(mInstance == null)
			return;

		mInstance.setRun(false);
		//System.out.println("terminate: interrupting...");
		mInstance.interrupt();
		try
		{
			//System.out.println("KOTtest: waiting for the end...");
			mInstance.join();
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
		finally
		{
			mInstance = null;
		}
	}
	
	
	private KeepOnTruckin()
	{
		super("KeepOnTruckin");
		// TODO: dynamically load the JNA stuff so it isn't required.
      try
      {
         System.out.println("Loading kernel32");
         K32 = Native.load("kernel32", Kernel32.class);
         System.out.println("Loaded kernel32");
      }
      catch(Error er)
      {
          er.printStackTrace();
      }
		
	}
	
	public void run()
	{
		setRun(true);

		//System.out.println("KeepOnTruckin starting");
		while(isRun())
		{
			// Jiggle the mouse a bit - the only thing that can be done with no external dependencies -
			// to keep the system from going to sleep.
		   keepSystemAwake();
			try
			{
				Thread.sleep(mInterval * 1000);
			}
			catch(InterruptedException intex)
			{
				//System.out.println("KeepOnTruckin interrupted");
			}
		}
		setRun(false);
		//System.out.println("KeepOnTruckin stopping");
	}

	private void simulateUserActivity()
	{
		Point pos = null;
		try
		{
			pos = MouseInfo.getPointerInfo().getLocation();
			if(pos.equals(mLastPos))
			{
				// According to some posts using the same location is actually enough to prevent sleep.
				pos.translate(mDir, mDir);
				
				if(( pos.x<0) || (pos.y<0) )   // This doesn't allow for moving too far...
				{
					mDir *= -1;
					pos.translate(mDir, mDir);
				}
				mDir *= -1;
				//System.out.println("simulateUserActivity: mouse did not move, giving it a little jiggle...");
				Robot roby = new Robot();
				roby.mouseMove(pos.x,pos.y);
				pos = MouseInfo.getPointerInfo().getLocation();
			}
			else
			{
				//System.out.println("simulateUserActivity: mouse movement detected, no need to jiggle...");
			}
			mLastPos = pos;				
		}
		catch(Throwable th)
		{
			// Ignore everything
			//th.printStackTrace();
		}
		
	}

	private void keepSystemAwake()
   {
	   if(K32 != null)
	   {
	      System.out.println("Keeping system busy using Win32 API");
	      K32.SetThreadExecutionState(WinBase.ES_SYSTEM_REQUIRED);
	   }
	   else
	   {
	      simulateUserActivity();
	   }
   }

	
	public synchronized boolean isRun()
	{
		return mRun;
	}


	public synchronized void setRun(boolean run)
	{
		mRun = run;
	}

	
	static public void main(String []args)
	{
		System.out.println("KOTtest: starting");
		KeepOnTruckin.startTruckin();
		try
		{
			sleep(1*60*1000);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
		System.out.println("KOTtest: terminating...");
		KeepOnTruckin.endOfTheRoad();

		System.out.println("KOTtest: done");
	}
}
