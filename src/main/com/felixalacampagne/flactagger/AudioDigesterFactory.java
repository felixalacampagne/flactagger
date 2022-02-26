package com.felixalacampagne.flactagger;

import java.io.File;

public class AudioDigesterFactory {

	public static AudioDigester getAudioDigester(File audiofile)
	{
		AudioDigester fd = null;
		if(audiofile.getName().matches("(?i)^.*\\.flac$"))
		{
			fd = new FLACdigester();
		}
		else if(audiofile.getName().matches("(?i)^.*\\.mp3$"))
		{	
			fd = new JMP3digester();
			//fd = new DelthasMP3digester();
		}
		return fd;
	}
}
