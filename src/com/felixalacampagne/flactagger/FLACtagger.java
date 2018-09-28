package com.felixalacampagne.flactagger;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
public boolean isMd5Enabled() 
{
	return md5Enabled;
}

public void setMd5Enabled(boolean md5Enabled) 
{
	this.md5Enabled = md5Enabled;
}

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
	
	
public int extract(String lyricsxml) throws Exception
{
FlacTags lyrics =  objFact.createFlacTags();
File root = new File(rootDir);
	// Dont want a real recurse search, only the current directory if it containts flacs or
	// the sub-directories of the current directory if there are no flacs.
	
	// extract flacs in rootDir
	if(extractFiles(root, lyrics) == 0)
	{
		for(File subdir : getDirs(root))
		{
			extractFiles(subdir, lyrics);
		}
	}
	
	// marshal the lyric object
	// save XML to lyricsxml as UTF-8
	saveLyrics(lyricsxml, lyrics);
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



public int update(String lyricsxml) throws Exception
{
FlacTags lyrics = loadLyrics(lyricsxml);

	if(lyrics == null)
	{
		log.severe("No Lyrics loaded from " + lyricsxml + "!");
		return 1;
	}
	String rootName = new File(rootDir).getName();
	
	for(Directory d : lyrics.getDirectory())
	{
	   File dir;
	   if(rootName.equals(d.getName()))
	   {
	      dir = new File(rootDir);
	   }
	   else
	   {
	      dir = new File(rootDir, d.getName());
	   }
		if(!dir.exists())
		{
			log.severe("Directory not found: " + dir.getAbsolutePath());
			continue;
		}
		log.info("Processing Directory: " + d.getName());

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
	
return 0;	
}

private FlacTags loadLyrics(String lyricsxml) throws JAXBException, FileNotFoundException
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
		try{ ascii.close(); } catch (IOException e) {}
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
	         // lyric directory in the directory.
	         // Eventually it might be nicer to write each directories lyric to a 
	         // separate file, but that's maybe something for the Gui to handle
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
	      if(fos != null)
	      {
	         try {fos.close(); } catch (Exception e) {}
	      }
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


} // End of class
