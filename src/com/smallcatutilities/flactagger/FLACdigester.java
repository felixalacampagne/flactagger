package com.smallcatutilities.flactagger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.kc7bfi.jflac.FLACDecoder;
import org.kc7bfi.jflac.PCMProcessor;
import org.kc7bfi.jflac.metadata.StreamInfo;
import org.kc7bfi.jflac.util.ByteData;

import com.sun.istack.internal.logging.Logger;

// Requires the "JustFLAC" library
public class FLACdigester implements PCMProcessor
{
private final Logger log = Logger.getLogger(FLACdigester.class);
protected MessageDigest md = null;
	public String getAudioDigest(File flacFile) throws IOException
	{
	FileInputStream is = null;
	String txtmd = null;
			
		try
		{
			is = new FileInputStream(flacFile);
			FLACDecoder decoder = new FLACDecoder(is);
			decoder.addPCMProcessor(this);
			decoder.decode();
          
			// Decoder returns when entire audio has been decoded
			byte[] mdbytes = md.digest();
          
			// Incredibly there isn't a simple way to get the string representation of the MD5
			// Needless to say there a billion and one wierd ways of doing it according to Google
			// but i'll stick with the simplest I can think of.
			txtmd = bytesToHex(mdbytes);
		}
		finally
		{
			if(is != null)
				is.close();
			//if(fos != null)
			//	fos.close();
		}
		return txtmd;
	}

	@Override
	public void processPCM(ByteData bd)
	{
		// Figured out why the MD5 calculated here was different to the ffmpeg audioonly md5...
		// the byte array is not filled! getLen() MUST be used to determine the real
		// amount of data in the array... and voila! the md5s are the same as for ffmpeg!
		// Obvious really, I suppose, but then isn't everything in hindsight!
		int bdlen = bd.getLen();
        md.update(bd.getData(), 0, bdlen);
	}

	@Override
	public void processStreamInfo(StreamInfo si)
	{
		// This is called at the start
	   byte[] simd5 = si.getMd5sum();
	   log.info("StreamInfo MD5: " + bytesToHex(simd5));
		try
		{
			md = MessageDigest.getInstance("MD5");
			md.reset();
		}
		catch (NoSuchAlgorithmException e)
		{
			e.printStackTrace();
		}
		
	}
	
	public String bytesToHex(byte[] bs)
	{
	   if((bs == null) || (bs.length < 1))
	   {
	      return "No MD5";
	   }
        StringBuffer sb = new StringBuffer();
        for (byte b : bs) 
        {
 			sb.append(String.format("%02x", b & 0xff));
        }          
        return sb.toString();
	}
}
