package com.felixalacampagne.flactagger;

import java.io.File;
import java.io.IOException;

public interface AudioDigester {
	
	public String getAudioDigest(File audioFile) throws IOException;
	public String getCalculatedMD5();
	public String getStreaminfoMD5();
	public String getStreamInfoMD5(File audioFile) throws IOException;
}
