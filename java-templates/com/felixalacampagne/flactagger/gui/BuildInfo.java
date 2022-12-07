
package com.felixalacampagne.flactagger.gui;

public class BuildInfo
{
   public static final String VERSION = "${project.version}";
   public static final String APPNAME = "${project.name}";
   //public static final String BLDTIME = "${build.localtime}";
   // Why is it that every single simple trivial thing to do with maven takes hours of
   // searching the internet to figure out how to make it work (or not even find a solution!)
   // Turns out 'build.localtime' is not actually a build property - not sure where it came
   // from, and it really should be a property because stupidly the real variable, ie. build.timestamp
   // does not use the the local system time, and there is no way to make it do so!
   // So far the only workaround to obtain the real timestamp is to use yet another
   // plugin, build-helper-maven-plugin, which allows creation of a variable for the current time
   // using a hard coded timezone, which is not ideal but will have to do.
   // I thought of putting a conversion function here to get the local time from the
   // UTC timestamp, but that's no good as the displayed time would then depend on
   // the system timezone in use where the app is running.
   public static final String BLDTIME = "${localtime}";

   public static String getAppTitle()
   {
      String title = BuildInfo.APPNAME + " - " + BuildInfo.VERSION;
      if(BuildInfo.VERSION.contains("SNAPSHOT"))
      {
         title = title + " (" + BuildInfo.BLDTIME + ")";
      }
      return title;
   }
}
