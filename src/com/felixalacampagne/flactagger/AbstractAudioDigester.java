package com.felixalacampagne.flactagger;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;

import com.sun.istack.internal.logging.Logger;

public abstract class AbstractAudioDigester implements AudioDigester {
	protected final Logger log = Logger.getLogger(this.getClass());
	protected String streaminfoMD5 = null;
	protected String calculatedMD5 = null;
	protected MessageDigest md = null;
	@Override
	public abstract String getAudioDigest(File audioFile) throws IOException ;

	@Override
	public String getCalculatedMD5() {
	
		return calculatedMD5;
	}

	@Override
	public String getStreaminfoMD5() {

		return streaminfoMD5;
	}

	@Override
	public String getStreamInfoMD5(File audioFile) throws IOException {
		
		return streaminfoMD5;
	}
	
	public String bytesToHex(byte[] bs)
	{
	   if((bs == null) || (bs.length < 1))
	   {
	      return null;
	   }
	   StringBuffer sb = new StringBuffer();
	   for (byte b : bs) 
	   {
	   	sb.append(String.format("%02x", b & 0xff));
	   }          
	   return sb.toString();
	}

}
