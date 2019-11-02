package com.felixalacampagne.flactagger;

import java.io.BufferedInputStream;

import java.io.File;
import java.io.FileInputStream;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import com.felixalacampagne.utils.Utils;


public class JMP3digester extends AbstractAudioDigester {

	/**
	 * Too much PCM data is produced compared to the decoding performed by FFMPEG.
	 * This was confirmed by generating a raw PCM file with FFMPEG using
	 * the following:
	 *   ffmpeg -i "01 Hello Boys.mp3" -f s16le -acodec pcm_s16le output.raw
	 * The md5sum of the output.raw file matches the MD5 produced by the command
	 * used to generate the checksums in my folderaudio.md5 files, ie.
	 *   ffmpeg -i "01 Hello Boys.mp3" -map 0:a -f hash -hash md5 -v 0 -
	 * The output.raw file is smaller. It appears to have fewer nul bytes
	 * at the start. 
	 * 
	 * In my test the Java PCMs have an additional ca.9000 nul bytes at the beginning.
	 * Not sure if this is connected to the 'start' reported by FFMPEG when it decodes
	 * to raw PCM.
	 * 
	 * The Delthas decoder generates different PCM audio with an MD5 different to
	 * both FFMPEG and jl decoder.
	 * 
	 * So it seems that different decoders generate different PCM, which makes the
	 * PCM md5 checksum pretty useless. I'll stick to the ffmpeg version since that is
	 * used by the checker I most frequently use.
	 * 
	 * I'll keep this version of the decoder since it doesn't require the libraries to be
	 * present at runtime - if they are missing AudioSystem will throw an exception for
	 * an unsupported format. 
	 * 
	 * To use this decoder requires the following to be in the classpath
	 * jl1.0.1.jar
	 * mp3spi1.9.5.jar - http://www.javazoom.net/mp3spi/mp3spi.html
	 * tritonus_share.jar
	 * 
	 * All jars are included in the mp2pi download (when I downloaded my copy) 
	 */
	@Override
	public String getAudioDigest(File audioFile) throws IOException 
	{
		AudioInputStream mp3stream = null;
		AudioInputStream pcmstream = null;
		
		
		try
		{
			mp3stream = AudioSystem.getAudioInputStream(new BufferedInputStream(new FileInputStream(audioFile)));
			
			// This is a java standard so no way it would be strightforward and sensible to use...
			AudioFormat baseFormat = mp3stream.getFormat();
			AudioFormat decodedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 
			                                            baseFormat.getSampleRate(),
			                                            16,
			                                            baseFormat.getChannels(),
			                                            baseFormat.getChannels() * 2,
			                                            baseFormat.getSampleRate(),
			                                            false);			
			
			pcmstream = AudioSystem.getAudioInputStream(decodedFormat, mp3stream); //AudioSystem.getAudioInputStream(AudioFormat.Encoding.PCM_SIGNED, mp3stream);

//	      Maybe details in the properties can give an idea why the difference in size between FFMPEG and JMP3,
//       but I don't see how at the moment.
//			AudioFileFormat baseFileFormat = AudioSystem.getAudioFileFormat(audioFile);
//			Map<String, Object> audiopros = baseFileFormat.properties();
//			for(String s : audiopros.keySet())
//			{ 
//				System.out.println("Property " + s + ": " + audiopros.get(s));
//			}
		}
		catch(Exception ex)
		{
			log.info("Cannot create digest", ex);
			return null;
		}
		
		
		
	
		try
		{
			md = MessageDigest.getInstance("MD5");
			md.reset();
		}
		catch (NoSuchAlgorithmException e)
		{
			e.printStackTrace();
		}
		
		
		byte[] pcmbuffer = new byte[5*1024*2014];
		int bytesread = 0;
		int totbytes = 0;
		
		//FileOutputStream fos = new FileOutputStream(audioFile.getAbsolutePath() + "_J.raw");
		
		while((bytesread = pcmstream.read(pcmbuffer)) > 0)
		{
			totbytes += bytesread;
			md.update(pcmbuffer, 0, bytesread);
			//fos.write(pcmbuffer, 0, bytesread);
		}
		//Utils.safeClose(fos);
		Utils.safeClose(pcmstream);
		Utils.safeClose(mp3stream);
		byte[] mdbytes = md.digest();
		calculatedMD5 = bytesToHex(mdbytes);		
		return calculatedMD5;
	}



}
