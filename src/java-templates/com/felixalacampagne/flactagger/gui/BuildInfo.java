
package com.felixalacampagne.flactagger.gui;

public class BuildInfo
{
private static String VERSION = "${build.localtime}";
	public BuildInfo() {}
	public String getVersion()
	{
		return VERSION;
	}
}
