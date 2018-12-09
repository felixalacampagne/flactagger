package com.felixalacampagne.flactagger;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;

import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagField;
import org.jaudiotagger.tag.flac.FlacTag;
import org.jaudiotagger.tag.vorbiscomment.VorbisCommentTagField;

import com.felixalacampagne.flactagger.generated.flactags.Directory;
import com.felixalacampagne.flactagger.generated.flactags.FileList;
import com.felixalacampagne.flactagger.generated.flactags.FileMetadata;
import com.felixalacampagne.flactagger.generated.flactags.FlacTags;
import com.felixalacampagne.flactagger.generated.flactags.ObjectFactory;
import com.felixalacampagne.utils.CmdArgMgr;
import com.felixalacampagne.utils.Utils;

public class FLACtagger
{
private static final String USAGE="Usage: FLACtagger <-u|-x> <-l lyrics.xml> [-r FLAC file rootdir]";
private static final String FLAC_LYRICS_TAG="UNSYNCED LYRICS";
private static final Logger log = Logger.getLogger(FLACtagger.class.getName());
	public static void main(String[] args)
	{
	FLACtagger tagger = null;
	CmdArgMgr cmds = new CmdArgMgr(args);
	String lyricsxml = null;
	Logger.getLogger("org.jaudiotagger").setLevel(Level.WARNING);
	
		if(args.length < 1)
		{
			System.out.println(USAGE);
			return;
		}
		
		tagger = new FLACtagger(cmds.getArg("r", null));
		lyricsxml = cmds.getArg("l");
		if(lyricsxml == null)
		{
			System.out.println("No lyrics file specified!!!");
			System.out.println(USAGE);
			return;
		}
		
		try
		{
			if(cmds.getArg("u") != null)
			{
				tagger.update(lyricsxml);
			}
			else if(cmds.getArg("x") != null)
			{
				tagger.extract(lyricsxml);
			}
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	
private final String rootDir;
private final ObjectFactory objFact = new ObjectFactory();
private boolean md5Enabled = false;
private boolean md5fileEnabled = false;


public FLACtagger()
{
	this(null);
}

public FLACtagger(String root)
{
	if(root == null)
		rootDir = System.getProperty("user.dir"); // Current working directory, ie. random value
	else
		rootDir = root;
}
	
	
public int extract(String alyricsxml) throws Exception
{
FlacTags lyrics =  objFact.createFlacTags();
FlacTags alllyrics = null;
File root = new File(rootDir);
boolean separatelyrics = false;
boolean flacdirlyrics = false;
	// Dont want a real recurse search, only the current directory if it contains flacs or
	// the sub-directories of the current directory if there are no flacs in the current dir.
	
	// Should really just save the lyrics files as for the folderaudio files but instead...
	// Kludge upon kludge!!: If the output lyrics file is not specified then assume
	// it is should be written into the rootdir using the directory as the file name
	// Ugh! That's OK when the rootDir contains the flacs but not quite as well
	// when the rootDir contains album sub-dirs: the xml files go into the directory
	// containing the sub-dirs instead of the same directory as the flacs. And this
	// behaviour is sometimes what I want, and sometimes not what I want.
	// Maybe I can kludge it a bit more:
	// alyricsxml is present AND a directory: separate lyric files into the specified directory
	// alyricsxml is absent: separate lyrics files into the flac directory
	// alyricsxml is present AND a filename: all lyrics go into the same file

	if (alyricsxml==null || alyricsxml.isEmpty())
	{
		// this implies that separatelyrics=true assuming rootDir is a valid directory
		alyricsxml = rootDir;
		flacdirlyrics = true;
	}
   // Kludge!!: if alyricsxml is a directory then save each lyrics as
   // an individual file. The individual file names is already handled by 
   // saveLyrics, just need to create a new lyrics and save it for each
   // flac directory. This could be made into a parameter... later
   separatelyrics = (new File(alyricsxml).isDirectory());

	// extract flacs in rootDir
	if(extractFiles(root, lyrics) == 0)
	{
		// This means the root did not contain flacs and is therefore assumed to contain sub-dirs which do contain flacs
		for(File subdir : getDirs(root))
		{
			extractFiles(subdir, lyrics);
			if(separatelyrics && (lyrics.getDirectory().size()>0))
			{
			String lyricsdir = alyricsxml;
				if(flacdirlyrics)
				{
					lyricsdir = subdir.getAbsolutePath(); 
				}
			   saveLyrics(lyricsdir, lyrics);
			   if(alllyrics == null)
			   {
			   	alllyrics = objFact.createFlacTags();
			   }
			   alllyrics.getDirectory().addAll(lyrics.getDirectory());
			   lyrics = objFact.createFlacTags();
			}
		}
	}

   //if(!separatelyrics) This means that a scan of one flac containing directory does not output anything!
	if(lyrics.getDirectory().size()>0)
   {
      saveLyrics(alyricsxml, lyrics);
   }
	if(alllyrics == null)
	{
		alllyrics = lyrics;
	}

	// Makes more sense to save the flacaudio.md5 using root directory, ie. the directory
	// searched for the .flac files. So here is the most convenient place to save
	// the .md5 file(s)
	if(isMd5fileEnabled())
	{
		
		saveFlacaudioMD5(rootDir, alllyrics);
	}	
	
	
	return 0;
}
	
// returns empty list if no dirs found
public List<File> getDirs(File root)
{
	List<File> subdirs = new ArrayList<File>(); 
	File [] files =
		root.listFiles(new FileFilter(){
			@Override
			public boolean accept(File pathname) {
				return pathname.isDirectory();
			}
		});
	if(files != null)
	{
		Arrays.sort(files);
		subdirs.addAll(Arrays.asList(files));
	}
	return subdirs;
}
	
public int extractFiles(File dir, FlacTags lyrics)
{
int flaccnt = 0;

	if((lyrics == null) || (lyrics.getDirectory() == null))
		return flaccnt;

List<File> flacs = getFiles(dir);
Directory d = null;
List<FileMetadata> files = null;
	for(File f : flacs)
	{
		FileMetadata ft = getFileMetadata(f);
		if(ft != null)
		{
			if(d == null)
			{
				d = objFact.createDirectory();
				d.setName(dir.getName());
				d.setFiles(objFact.createFileList());
				files = d.getFiles().getFilemetadata();
			}
			files.add(ft);
			flaccnt++;
		}
	}
	if(d != null) 
	{
		lyrics.getDirectory().add(d);
	}
	return flaccnt;
}
	
// TODO: Artist and album should be per file instead of per directory to accommodate compilations
public FileMetadata getFileMetadata(File f)
{
	FileMetadata ftx = null;
	log.info("Loading: " + getFileDispName(f));
	try 
	{
		AudioFile af = AudioFileIO.read(f);
		Tag tag = af.getTag();

		// Artist, album, lyric, directory name, file name
		ftx = objFact.createFileMetadata();
		ftx.setName(f.getName());
		ftx.setArtist(tag.getFirst(FieldKey.ARTIST));
		ftx.setAlbum(tag.getFirst(FieldKey.ALBUM));
		String lyric = tag.getFirst(FLAC_LYRICS_TAG);
		if(lyric != null)
		{
			// Leaving the CRLF in results in lines terminated with the text "&#xD;" and a normal linefeed
			// Seems the Java XML parse is stuck in the Unix world. There might be a way to
			// tell the parse to treat the data verbatim (I thought that is what CDATA meant) but
			// don't know whether that must be in the schema or the xjb or what at the moment
			lyric = lyric.replace("\r","");
			lyric = lyric.replace("\u2018", "'"); // left single quote, for completeness
			lyric = lyric.replace("\u2019", "'"); // right single quote, frequently used for he'd, I'd, aint', etc..
			lyric = lyric.replace("\u201D", "'"); // right double quote
			lyric = lyric.replace("\u201C", "'"); // left double quote
			lyric = lyric.replace("&", "and");
			lyric = lyric.replace(">", "");
			lyric = lyric.replace("<", "");

			// Dump remaining non-ascii stuff
			lyric = lyric.replaceAll("[^\\x00-\\x7f]", "");

			// make it easier to edit the lyric ensure start and end lyric tags are not on the same line as the content
			if(!lyric.startsWith("\n"))
				lyric = "\n" + lyric;
			if(!lyric.endsWith("\n"))
				lyric = lyric + "\n";
		}
		else
		{
			lyric = "";
		}
		ftx.setLyric(lyric);
	} 
	catch (Exception e) 
	{
		e.printStackTrace();
	} 
	
	getAudioDigest(f, ftx);
	return ftx;
}

public List<File> getFiles(File dir)
{
List<File> files = new ArrayList<File>();
	if(dir == null)
	{
		return files;
	}
	
	if(!dir.isDirectory())
		return files;
	
	files.addAll(Arrays.asList(dir.listFiles(
		new FileFilter()
		{
			@Override
			public boolean accept(File pathname) {
				if(pathname.isDirectory())
					return false;
				String name = pathname.getName();
				boolean rc = name.matches("(?i)^.*\\.flac$"); 
				return rc;
			}
		})));

	return files;
}



public int update(String alyricsxml) throws Exception
{
List<File> lyricstoprocess = new ArrayList<File>();
File rootdirf = new File(rootDir);
File lyfile = null;
   // If no lyrics file specified assume it is in the rootDir with rootDir name
   if((alyricsxml==null) || alyricsxml.isEmpty())
   {
      lyfile = new File(rootDir, rootdirf.getName() + ".xml");
   }
   else
   {
      lyfile = new File(alyricsxml);
   }
   
   if(lyfile.isDirectory())
   {
      lyricstoprocess.addAll(Arrays.asList(lyfile.listFiles(
            new FileFilter()
            {
               @Override
               public boolean accept(File pathname) {
                  if(pathname.isDirectory())
                     return false;
                  String name = pathname.getName();
                  // Make big assumption that all XML files in the directory as lyrics files!!
                  boolean rc = name.matches("(?i)^.*\\.xml$"); 
                  return rc;
               }
            })));      
   }
   else
   {
      if(! lyfile.isFile())
      {
         log.severe("No Lyrics specifed or " + lyfile.getAbsolutePath() + "does not exist!");
         return 1;
      }
      lyricstoprocess.add(lyfile);   
   }
   
  for(File flyricsxml : lyricstoprocess)
  {
   
     FlacTags lyrics = loadLyrics(flyricsxml);

     if(lyrics == null)
     {
        log.severe("No Lyrics loaded from " + flyricsxml + "!");
        return 1;
     }

     String rootName = rootdirf.getName();
	
     for(Directory d : lyrics.getDirectory())
     {
        File dir;
        // If album directory was specified as root then use it
        if(rootName.equals(d.getName()))
        {
           dir = new File(rootDir);
        }
        else
        {
           // If album directory is not the same as the root then assume it is in the root
           dir = new File(rootDir, d.getName());
        }
        if(!dir.exists())
        {
           // Revert to using the rootDir in case flac files were relocated (eg. when reencoded to a subdir of
           // the original album dir). This possibly makes one of the file.exists checks below redundant unless
           // maybe when multiple directories are being processed. Anyway I'll leave it in for now.
           dir = new File(rootDir);
        }
        log.info("Processing FlacTag Directory: " + d.getName());

        FileList files = d.getFiles();
        for(FileMetadata ft : files.getFilemetadata())
        {
           String trimlyric = ft.getLyric();
           if(trimlyric == null)
              continue;
           trimlyric = trimlyric.trim();
           if(trimlyric.length() < 1)
              continue;
			
           // JAXB strips out the CRs but apparently they must be there, at least for mp3tag,
           // so must put them back before adding to the tag. The ? is supposed to avoid
           // the case where the CRs are already present... but can't figure out how that works...
           // unless ? means 0 or 1... it does!
           trimlyric = trimlyric.replaceAll("\r?\n", "\r\n");
           File f = new File(dir, ft.getName());
			
           // Above directory assumptions don't always work. Rather than break the
           // multi-directory processing, which usually works the way I want it to will try to find missing files
           // by checking for them in some other possible places, eg. the directory of flactag file or the root directory.
           if(!f.exists())
           {
              f = new File(flyricsxml.getParentFile(), ft.getName());
              if(!f.exists())
              {
                 f = new File(rootdirf, ft.getName());
                 if(!f.exists())
                 {
                    log.severe("File "+ ft.getName() + " not found in " 
                     + dir.getAbsolutePath() + " or " 
                     + flyricsxml.getParentFile().getAbsolutePath() + " or "
                     + rootdirf.getAbsolutePath());
                    continue;
                 }
              }
           }
			
           String fdisp = getFileDispName(f);
           log.info("Loading: " + fdisp);
           try
           {
              AudioFile af = AudioFileIO.read(f);
			
              Tag tag = af.getTag();
              if((tag != null) && (tag instanceof FlacTag ))
              {
                 if(tag.hasField(FLAC_LYRICS_TAG))
                 {
                    String currlyric = tag.getFirst(FLAC_LYRICS_TAG);
                    if(trimlyric.equals(currlyric))
                    {
                       log.info("Lyric is already present, no update required: "+ fdisp);
                       continue;
                    }
                    log.info("Removing existing lyric from "+ fdisp);
                    tag.deleteField(FLAC_LYRICS_TAG);
                 }
                 // TagField ID: UNSYNCED LYRICS Class: org.jaudiotagger.tag.vorbiscomment.VorbisCommentTagField
                 TagField lyrictf = new VorbisCommentTagField(FLAC_LYRICS_TAG, trimlyric);
                 tag.addField(lyrictf);
                 log.info("updating: " + fdisp);
                 af.commit();
              }
              else
              {
                 log.severe("WARN: No or none-FLAC tag, unable to update: " + fdisp);
              }
           }
           catch(Exception ex)
           {
              log.severe("Exception reading " + fdisp + ": " + ex.getMessage());
           }				
        }
     }
  }
	
return 0;	
}

private FlacTags loadLyrics(String lyricsxml) throws JAXBException, FileNotFoundException
{
   return loadLyrics(new File(lyricsxml));
}

private FlacTags loadLyrics(File lyricsxml) throws JAXBException, FileNotFoundException
{
	String ctxname = FlacTags.class.getPackage().getName();
	JAXBContext jc = JAXBContext.newInstance(ctxname);
	Unmarshaller u = jc.createUnmarshaller(); 
	AsciiFilterInputStream ascii = new AsciiFilterInputStream(new FileInputStream(lyricsxml));
	try
	{
		JAXBElement<FlacTags> o = u.unmarshal(new StreamSource(ascii), FlacTags.class);
		FlacTags lyrics = o.getValue();
		return lyrics;
	}
   catch(JAXBException xex)
   {
      log.log(Level.SEVERE, "Exception processing " + lyricsxml, xex);
      throw xex;
   }
	finally
	{
		Utils.safeClose(ascii);
	}
	
}

/**
 * 
 * @param lyricsxml path for the md5 files 
 * @param tags to be save. The StreamInfo MD5 entry for each file is saved into a flacaudio.md5 
 *        file in the given directory. If tags for multiple directories are present
 *        then one md5 will be save per directory - in theory!  
 * @throws FileNotFoundException
 */
public static final String FA_NAME="folderaudio";
public static final String FA_EXTN=".md5";
public static final String FA_FILENAME= FA_NAME + FA_EXTN;
public void saveFlacaudioMD5(String lyricsxml, FlacTags tags) throws FileNotFoundException
{
   // flacaudio.md5 writing belongs somewhere else!!
   OutputStreamWriter osw = null;
   File rootdir = null;
   try
   {
      for(Directory d : tags.getDirectory())
      {

      	if(rootdir == null)
      	{
      		rootdir = new File(lyricsxml);
      		if(rootdir.getName().equals(d.getName()))
      		{
      			rootdir = rootdir.getParentFile();
      		}
      	}
      	
      	File lxfile = new File(rootdir, d.getName());
      	if(lxfile.isDirectory())
      	{
      		lxfile = new File(lxfile, FA_FILENAME);
      	}
      	else
      	{
      		lxfile = new File(rootdir, d.getName() + "_" + FA_FILENAME);
      	}
      	
      	
      	if(lxfile.exists())
      	{
      		// Backup the previous file. Handle name with directory prefix
      		String name = lxfile.getName();
      		name = name.replace(FA_FILENAME, FA_NAME + "_" + Utils.getTimestampFN(lxfile.lastModified()) + FA_EXTN);
      	   File rnfile = new File(lxfile.getParentFile(), name);
      	   Files.move(lxfile.toPath(), rnfile.toPath(),  StandardCopyOption.REPLACE_EXISTING);
      	}
      	
      	StringBuffer md5s = new StringBuffer();
         for(FileMetadata fmd : d.getFiles().getFilemetadata())
         {
            md5s.append(fmd.getStrmpcmmd5()).append(" *");
            md5s.append(fmd.getName());
            md5s.append("\n");
         }
         
         osw = new OutputStreamWriter(new FileOutputStream(lxfile));
         osw.write(md5s.toString());
         Utils.safeClose(osw);
      }
   }
   catch (IOException e)
   {
      // TODO Auto-generated catch block
      e.printStackTrace();
   }
   finally
   {
   	Utils.safeClose(osw);
   }
	
}

private void saveLyrics(String lyricsxml, FlacTags lyrics) throws JAXBException, FileNotFoundException
{
	// Arg for JAXBContext is the package containing the ObjectFactory for the type to be Un/Marshalled
	String ctxname = FlacTags.class.getPackage().getName();
	JAXBContext jc = JAXBContext.newInstance(ctxname);	
	FileOutputStream fos = null;
	

		
	   Marshaller m = jc.createMarshaller();
	   m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
	   JAXBElement<FlacTags> o = objFact.createFlactags(lyrics);
	   try
	   {
	      File lxfile = new File(lyricsxml);
	      if(lxfile.isDirectory())
	      {
	         // If a directory is specified then create the lyrics file using the 
	         // lyric directory as the name in the directory.
	         if(lyrics.getDirectory().size() > 0)
	         {
	            lxfile = new File(lxfile, lyrics.getDirectory().get(0).getName() + ".xml");
	         }
	         else
	         {
	            lxfile = new File(lxfile, "flactaggerlyrics.xml");
	         }
	      }
	      
	      
	      fos = new FileOutputStream(lxfile);
	      m.marshal(o, fos);
	   }
	   finally
	   {
	   	Utils.safeClose(fos);
	   }
	   

}

private String getFileDispName(File f)
{
	return (new File(f.getParent()).getName()) + File.separatorChar + f.getName();
}

public String getAudioDigest(File flacfile, FileMetadata ftx)
{
	
	FLACdigester fd = new FLACdigester();
	String dig = null;
	try
	{
		if(md5Enabled)
		{
		   dig = fd.getAudioDigest(flacfile);
			
			if((dig != null) && (dig.length()>0))
			{
				log.info("Calculated MD5:" + dig + "*" + flacfile.getName());
				ftx.setCalcpcmmd5(dig);
			}
			String sdig = fd.getStreaminfoMD5();
			if((sdig != null) && (sdig.length()>0))
			{
				log.info("StreamInfo MD5:" + sdig + "*" + flacfile.getName());
				ftx.setStrmpcmmd5(sdig);
				
				if((dig != null) && !dig.equalsIgnoreCase(sdig))
				{
				   log.warning("MISMATCH! MD5s are different: " + flacfile.getName());
				}
			}
		}
		else
		{
			dig = fd.getStreamInfoMD5(flacfile);
			if((dig != null) && (dig.length()>0))
			{
				log.info("StreamInfo MD5:" + dig + "*" + flacfile.getName());
				ftx.setStrmpcmmd5(dig);
			}			
		}
	}
	catch (IOException e)
	{
		log.log(Level.SEVERE, "Failed to calculate digest for " + flacfile.getName(), e);
	}
	return dig;

}

public boolean isMd5fileEnabled()
{
	return md5fileEnabled;
}

public void setMd5fileEnabled(boolean md5fileEnabled)
{
	this.md5fileEnabled = md5fileEnabled;
}

public boolean isMd5Enabled() 
{
	return md5Enabled;
}

public void setMd5Enabled(boolean md5Enabled) 
{
	this.md5Enabled = md5Enabled;
}


} // End of class
