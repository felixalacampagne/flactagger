package com.felixalacampagne.flactagger;


import java.io.File;
import java.io.FileNotFoundException;
import java.util.logging.Logger;
import com.felixalacampagne.utils.Utils;

public class FLACtaggerUtils 
{
public static final String EMPTY_LYRIC = "<blank>";


protected final Logger log = Logger.getLogger(this.getClass().getName());

	
	/**
	 * Reformat the lyric string parsed from the FileMetaData XML
	 * for the lyric tag
	 * 
	 * @param fmdLyric - lyric string parsed from the FileMetaData XML
	 * @return - lyric string for the lyric tag.
	 */
	public String fmtLyricForTag(String fmdLyric)
	{
		String trimlyric = Utils.safeString(fmdLyric);
	   
	   // If there is no lyric defined then add a default one indicating it is not defined.
	   
	   if(trimlyric == null)
	 	  trimlyric = EMPTY_LYRIC;
	   trimlyric = trimlyric.trim();
	   
	   if(trimlyric.length() < 1)
	 	  trimlyric = EMPTY_LYRIC ;
		
	   // JAXB strips out the CRs but apparently they must be there, at least for mp3tag,
	   // so must put them back before adding to the tag. The ? is supposed to avoid
	   // the case where the CRs are already present... but can't figure out how that works...
	   // unless ? means 0 or 1... it does!
	   trimlyric = trimlyric.replaceAll("\r?\n", "\r\n");
	   return trimlyric;
	}

	/**
	 * Search for a matching file in various directories.
	 * @param rootdir
	 * @param xmldir
	 * @param auddir
	 * @param audname
	 * @return
	 * @throws FileNotFoundException
	 */
	public File findFileToTag(File rootdir, File xmldir, File auddir, String audname) throws FileNotFoundException
	{
	   File f = new File(auddir, audname);
		
	   if(!f.exists())
	   {
	      f = new File(xmldir, audname);
	      if(!f.exists())
	      {
	         f = new File(rootdir, audname);
	         if(!f.exists())
	         {
	            log.severe("File "+ audname + " not found in " 
	             + auddir.getAbsolutePath() + " or " 
	             + xmldir.getAbsolutePath() + " or "
	             + rootdir.getAbsolutePath());
	            throw new FileNotFoundException(audname);
	         }
	      }
	   }
	   return f;
	}
	
   public String getFileDispName(File f)
   {
   	return (f==null) ? "<null>" : f.getParentFile().getName() + File.separator + f.getName();
   }
 	
}
