package com.felixalacampagne.flactagger;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldDataInvalidException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagField;
import org.jaudiotagger.tag.flac.FlacTag;
import org.jaudiotagger.tag.id3.AbstractID3v2Frame;
import org.jaudiotagger.tag.id3.AbstractID3v2Tag;
import org.jaudiotagger.tag.id3.framebody.FrameBodyUSLT;
import org.jaudiotagger.tag.images.Artwork;
import org.jaudiotagger.tag.images.ArtworkFactory;
import org.xml.sax.SAXParseException;

import com.felixalacampagne.flactagger.generated.flactags.Directory;
import com.felixalacampagne.flactagger.generated.flactags.FileList;
import com.felixalacampagne.flactagger.generated.flactags.FileMetadata;
import com.felixalacampagne.flactagger.generated.flactags.FlacTags;
import com.felixalacampagne.flactagger.generated.flactags.ObjectFactory;
import com.felixalacampagne.utils.CmdArgMgr;
import com.felixalacampagne.utils.Utils;
import static com.felixalacampagne.utils.Utils.getValueOrNull;
import static com.felixalacampagne.utils.Utils.getValueOrDefault;

public class FLACtagger
{
private static final String USAGE="Usage: FLACtagger <-u|-x> <-l lyrics.xml> [-r FLAC file rootdir]";
private static final String FLAC_LYRICS_TAG="UNSYNCED LYRICS";
private static final Logger log = Logger.getLogger(FLACtagger.class.getName());


// Need to keep a reference to the JAT logger to avoid it being garbage collected before
// any real JAT loggers are created, which I think is what causes the INFO level
// messages to pollute the output sometimes.
private static final Logger jatlog = Logger.getLogger("org.jaudiotagger");

public static void main(String[] args)
{
FLACtagger tagger = null;
CmdArgMgr cmds = new CmdArgMgr(args);
String lyricsxml = null;
jatlog.setLevel(Level.WARNING);


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
		log.severe("Failed to process " + lyricsxml + ": " + e.getMessage());
	}
}

	
private final String rootDir;
private final ObjectFactory objFact = new ObjectFactory();
private boolean md5Enabled = false;
private boolean md5fileEnabled = false;
private FLACtaggerUtils ftu = new FLACtaggerUtils();


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

	alyricsxml = Utils.safeString(alyricsxml);

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

	if (alyricsxml.isEmpty())
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
		saveFolderaudioMD5(rootDir, alllyrics);
		saveCuesheet(rootDir, alllyrics); // TODO: give this it's own option
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



// This should work for FLAC and MP3 if the abstract Fieldkey tag names are mapped correctly
public FileMetadata getFileMetadata(File f)
{
	FileMetadata ftx = null;
	log.info("Loading: " + ftu.getFileDispName(f));
	try 
	{
		AudioFile af = AudioFileIO.read(f);
		Tag tag = af.getTag();

		// Artist, album, lyric, directory name, file name
		ftx = objFact.createFileMetadata();
		ftx.setName(f.getName());
		
		// NB Tracknumber can be of the form [track]/[disc]. The disc number
		// is intentionally ignored by str2Int since I do not want the disc number
		// to contaminate the track number.
		// TODO: Add a field for disc number and use the disc number
		// embedded in the tracknumber.
		ftx.setTracknumber(Utils.str2Int(tag.getFirst(FieldKey.TRACK)));
		ftx.setArtist(tag.getFirst(FieldKey.ARTIST));
		ftx.setAlbum(tag.getFirst(FieldKey.ALBUM));
		ftx.setTitle(tag.getFirst(FieldKey.TITLE));
		ftx.setAlbumartist(getValueOrNull(tag.getFirst(FieldKey.ALBUM_ARTIST)));
		ftx.setComposer(getValueOrNull(tag.getFirst(FieldKey.COMPOSER)));
		ftx.setComment(getValueOrNull(tag.getFirst(FieldKey.COMMENT)));
		ftx.setYear(getValueOrNull(tag.getFirst(FieldKey.YEAR))); // TODO: Use a (normal) Integer
		ftx.setGenre(getValueOrDefault(tag.getFirst(FieldKey.GENRE), "Pop"));
		
		// Not sure whether the tag library supports the compilation flag
		String s = tag.getFirst(FieldKey.IS_COMPILATION);
		Boolean b = (!isEmptyString(s)) && (s.equals("1")) ? true : null;
		ftx.setCompilation(b);
		
		String lyric = null;
		if(tag instanceof FlacTag)
		{
			// Unfortunately lyric is empty when FieldKey.LYRICS is used for a FLAC file
			lyric = tag.getFirst(FLAC_LYRICS_TAG);
		}
		else
		{
			lyric = tag.getFirst(FieldKey.LYRICS);
		}
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
			lyric = lyric.replace("&quot;", "'");
			lyric = lyric.replace("&amp;", "and");
			
			lyric = lyric.replace("&", "and");
			lyric = lyric.replace(">", "");
			lyric = lyric.replace("<", "");
			
			// Some lyrics have the quotes replaced with ?, extremely annoying for
			// expressions like I'll, I'm, you're, etc.
			// Restore the quote assuming ?s should only come at the end of
			// sentences. The regex wont catch cases where the quote comes at end of the word, eg.
			// Chris? regex is good enough, but will get most of them.
			lyric = lyric.replaceAll("(?<=\\p{Alpha})\\?(?=\\p{Alpha})", "'");

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
		log.severe("Failed to read TAG data from " + ftu.getFileDispName(f) + ": " + e.getMessage());
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
				boolean rc = name.matches("(?i)^.*\\.(flac|mp3)$"); 
				return rc;
			}
		})));

	return files;
}

private List<File> buildLyricsList (String alyricsxml) throws Exception
{
List<File> lyricstoprocess = new ArrayList<File>();
File rootdirf = new File(rootDir);
File lyfile = null;

   // Need to reproduce behaviour of extract when only base directory is specified:
   // - if dir contains flacs then generate the xml in the directory named after directory
   // - if no flacs then search one level of sub-dirs for flacs and generate the xml in the sub-directory
   //   named after the sub-directory.
   
   // If no lyrics file specified assume it is in the rootDir with rootDir name
   if((alyricsxml==null) || alyricsxml.isEmpty())
   {
      lyfile = new File(rootDir, rootdirf.getName() + ".xml");
      if(!lyfile.exists())
         lyfile = new File(rootDir);
   }
   else
   {
      lyfile = new File(alyricsxml);
   }
   
   if(lyfile.isDirectory())
   {
      FileFilter xmlfilter = new FileFilter()
      {
         @Override
         public boolean accept(File pathname) {
            if(pathname.isDirectory())
               return false;
            String name = pathname.getName();
            // Make big assumption that all XML files in the directory are lyrics files!!
            boolean rc = name.matches("(?i)^.*\\.xml$"); 
            return rc;
         }
      };
      lyricstoprocess.addAll(Arrays.asList(lyfile.listFiles(xmlfilter)));   
      
      // If there are no lyrics files in root dir then maybe they are in the sub-dirs...
      if(lyricstoprocess.size() == 0)
      {
         for(File subf: lyfile.listFiles())
         {
            if(subf.isDirectory())
            {
               lyricstoprocess.addAll(Arrays.asList(subf.listFiles(xmlfilter))); 
            }
         }
      }
   }
   else
   {
      if(! lyfile.isFile())
      {
         log.severe("Lyrics file " + lyfile.getAbsolutePath() + "does not exist!");
      }
      else
      {
      	lyricstoprocess.add(lyfile);
      }
   }
	return lyricstoprocess;
}



public int update(String alyricsxml) throws Exception
{
List<File> lyricstoprocess = buildLyricsList(alyricsxml);
File rootdirf = new File(rootDir);
String rootName = rootdirf.getName();

  // List failed files at the end as it is too easy to miss one or two failures
  // in a long list of processed files
  List<String> fails = new ArrayList<String>();
   
  for(File flyricsxml : lyricstoprocess)
  {
     FlacTags lyrics = loadLyrics(flyricsxml);
     if(lyrics == null)
     {
        log.severe("No tags loaded from " + flyricsxml + "!");
        return 1;
     }

	
     for(Directory d : lyrics.getDirectory())
     {
        File dir;
        // If album directory was specified as root then use it
        if(rootName.equals(d.getName()))
        {
           dir = rootdirf;
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
           dir = rootdirf;
        }
        log.info("Processing FlacTag Directory: " + d.getName());

        Artwork folderjpg = getFolderJPG(dir);
        FileList files = d.getFiles();  // Files from the XML
        for(FileMetadata ft : files.getFilemetadata())
        {
           File f = null;
           try
           {
         	  f = ftu.findFileToTag(rootdirf, flyricsxml.getParentFile(), dir, ft.getName());
           }
           catch(FileNotFoundException filex)
           {
         	  fails.add(ft.getName() + ": missing file");
         	  continue;
           }
			
           String fdisp = ftu.getFileDispName(f);
           log.info("Loading: " + fdisp);
           try
           {
              AudioFile af = AudioFileIO.read(f);
              boolean updated = false;
              Tag tag = af.getTag();

              if(tag != null)
              {
            	  updated = updateTagFields(tag, ft, folderjpg, fdisp);
            	  if(updated)
            	  {
            		  log.info("Updating tag: " + fdisp);
            		  af.commit();
            	  }
            	  else
            	  {
            		  log.info("No change to tag required"); 
            	  }
              }
              else
              {
            	  // TODO: Should create a new tag instead of ignoring!
                 log.severe("WARN: No metadata tag present in file, unable to update: " + fdisp);
              }
           }
           catch(Exception ex)
           {
              log.severe("Exception reading " + fdisp + ": " + ex.getMessage());
              fails.add(fdisp + ": " + ex.getMessage());
           }				
        }
     }
     
     // List failed files
     if(fails.size() > 0)
     {
   	  StringBuilder sb = new StringBuilder("The following files could not be processed:");
   	  fails.forEach(fail -> sb.append(fail).append("\n"));
   	  log.severe(sb.toString());
     }
  } 
	
return 0;	
}






public static final String FOLDER_JPG = "folder.jpg"; 
private Artwork getFolderJPG(File dir) 
{
	Artwork bjpg = null;
	File fjpg = new File(dir, FOLDER_JPG);
	
	if(fjpg.isFile())
	{
		try 
		{
			Artwork tmpart = ArtworkFactory.createArtworkFromFile(fjpg);
			bjpg = tmpart;
		} 
		catch (Exception e) 
		{
			log.severe("Exception reading " + fjpg + ": " + e.getMessage());
		}
	}
	return bjpg;
}

private boolean isEmptyString(String s)
{
	return ((s==null) || (s.isEmpty()));
}

private boolean updateTagFields(Tag tag, FileMetadata ft, Artwork folderjpg, String fdisp)
{
boolean updated = false;
String trimlyric = ftu.fmtLyricForTag(ft.getLyric());
	if(tag instanceof FlacTag )
	{
		  updated = updateLyricTag((FlacTag) tag, trimlyric, fdisp);
	}
	else if(tag instanceof AbstractID3v2Tag)
	{
		updated = updateLyricTag((AbstractID3v2Tag)tag, trimlyric, fdisp);
	}

	updated = updated | updateFieldTag(tag, FieldKey.IS_COMPILATION, ft.isCompilation(), fdisp);            	  
	updated = updated | updateFieldTag(tag, FieldKey.TITLE, ft.getTitle(), fdisp); 
	updated = updated | updateFieldTag(tag, FieldKey.ALBUM, ft.getAlbum(), fdisp);
	updated = updated | updateFieldTag(tag, FieldKey.ARTIST, ft.getArtist(), fdisp);
  
	// Keep ALBUM_ARTIST and COMPOSER equal to ARTIST because some Apple apps
	// choose the wrong field for display. 
	updated = updated | updateFieldTag(tag, FieldKey.ALBUM_ARTIST, ft.getAlbumartist(), ft.getArtist(), fdisp);
	updated = updated | updateFieldTag(tag, FieldKey.COMPOSER, ft.getComposer(), ft.getArtist(), fdisp);
	updated = updated | updateFieldTag(tag, FieldKey.COMMENT, ft.getComment(), fdisp);
	updated = updated | updateFieldTag(tag, FieldKey.YEAR, ft.getYear(), fdisp);
	updated = updated | updateFieldTag(tag, FieldKey.GENRE, ft.getGenre(), "Pop", fdisp);

	updated = updated | updateCoverTag(tag, folderjpg, fdisp);
	updated = updated | updateTracknumberTag(tag, ft.getTracknumber(), fdisp);
	updated = updated | updateFieldTag(tag, FieldKey.DISC_NO, "1", fdisp);
	
	return updated;
}


private boolean updateFieldTag(Tag tag, FieldKey fld, String newvalue, String defaultvalue, String fname)
{
	boolean updated = false;
	
	String updvalue = isEmptyString(newvalue) ? defaultvalue : newvalue;
	if(isEmptyString(updvalue))
		return updated;
	
	
	String oldvalue = FLACtaggerUtils.EMPTY_LYRIC;
	if(tag.hasField(fld))
	{
		oldvalue = tag.getFirst(fld);
		if(updvalue.equals(oldvalue))
		{
			log.fine("Value is unchanged for field '" + fld.name() + "', no update required: "+ fname);
			return updated;
		}
		log.fine("Removing existing value for field '" + fld.name() + "':"   + fname);
		tag.deleteField(fld);
	}
	TagField tagf;
	try {
		tagf = tag.createField(fld, updvalue);
		tag.addField(tagf);
		log.info("Updated '"  + fld.name() + "' to '" + updvalue + "' from '" + oldvalue + "'");
		updated = true;
	} 
	catch (Exception e) 
	{
		log.log(Level.SEVERE, "Failed to add value '" + fld.name() + "' to: "+ fname, e);
	} 
	return updated;
}

private boolean updateFieldTag(Tag tag, FieldKey fld, String newvalue, String fname)
{
	return updateFieldTag(tag, fld, newvalue, null, fname);
}

private boolean updateFieldTag(Tag tag, FieldKey fld, Boolean newvalue, String fname)
{
	if(newvalue == null)
		return false;

	return updateFieldTag(tag, fld, newvalue ? "1" : "0", null, fname);
}


private boolean updateLyricTag(FlacTag tag, String newlyric, String fname)
{
	// Can't use the generic FieldKey.LYRICS because it is mapped to FLAC tag 'LYRICS' instead of 'UNSYNCED LYRICS'
	// and I don't see a way to override this.	
	
	boolean updated = false;
	if((newlyric==null) || (newlyric.isEmpty() || FLACtaggerUtils.EMPTY_LYRIC.equals(newlyric)))
		return updated;
	if(tag.hasField(FLAC_LYRICS_TAG))
	{
		  String currlyric = tag.getFirst(FLAC_LYRICS_TAG);
		  if(newlyric.equals(currlyric))
		  {
			  log.fine("Lyric is unchanged, no update required: "+ fname);
			  return updated;
		  }
		  log.fine("Removing existing lyric from "+ fname);
		  tag.deleteField(FLAC_LYRICS_TAG);
	}
	
	try {

		//TagField lyrictf = tag.createField(FieldKey.LYRICS, newlyric);
		TagField lyrictf = tag.createField(FLAC_LYRICS_TAG, newlyric);
		tag.addField(lyrictf);
		log.info("Updated lyric in "+ fname);

		updated = true;
	} catch (FieldDataInvalidException e) {
	
		log.log(Level.SEVERE, "Failed to add lyric to: "+ fname, e);
	}
	return updated;
}

private boolean updateTracknumberTag(Tag tag, Integer fmdtrack, String fdisp) 
{
	boolean updated = false;
	try
	{
		String fldval = tag.getFirst(FieldKey.TRACK);
		
		// I want the track nos. to be minimum 2 digit zero padded so need to compare
		// the string values of the track no., assuming we have a valid no. in the fmd.
		// Would also like to get rid of the t/d formatted track nos. and with a bit of luck
		// this should cause them to be replaced even if the track no. itself is the same
		// as the fmd no. Previously the /d was just ignored so no update was done as the
		// integer nos. were the same.
		if(fmdtrack>0)
		{
			String fmdfldval = String.format("%02d", fmdtrack);
			if(!fmdfldval.equals(fldval))
			{
				tag.setField(FieldKey.TRACK, fmdfldval);
				log.info("Updated track number to '" + fmdfldval +"'");
				updated = true;				
			}
		}
	}
	catch(Exception ex)
	{
		log.log(Level.SEVERE, "Failed to update track number to " + fmdtrack + ": " + fdisp, ex);
	}
	return updated;
}


// Works for both MP3 and FLAC
private boolean updateCoverTag(Tag tag, Artwork folderjpg, String fdisp) 
{
boolean updated = false;
	
	Artwork coverart = folderjpg;
	if(coverart != null)
	{
		List<Artwork> covers = tag.getArtworkList();
		if(covers.size() == 1)
		{
			byte[] newbytes = coverart.getBinaryData();
			byte[] origbytes = covers.get(0).getBinaryData();
			if(origbytes.length == newbytes.length)
			{
				if(Arrays.equals(origbytes, newbytes))
				{
					log.log(Level.FINE, "Cover art is unchanged, no update required: "+ fdisp);
					return updated;
				}
			}
		}
		log.fine("Deleting all existing artwork from "+ fdisp);
		tag.deleteArtworkField();

		try 
		{
			tag.setField(coverart);
			updated = true;
			log.log(Level.INFO, "Cover art updated: "+ fdisp);
		} 
		catch (FieldDataInvalidException e) 
		{
			log.log(Level.SEVERE, "Failed to add cover art to: "+ fdisp, e);
		}
	}
	
	return updated; 
}

private boolean updateLyricTag(AbstractID3v2Tag tag, String newlyric, String fname)
{
boolean updated = false;
	if((newlyric==null) || (newlyric.isEmpty()))
		return updated;
	if(tag.hasField(FieldKey.LYRICS))
	{
		  String currlyric = tag.getFirst(FieldKey.LYRICS);
		  if(newlyric.equals(currlyric))
		  {
			  log.fine("Lyric is unchanged, no update required: "+ fname);
			  return updated;
		  }
		  log.fine("Removing existing lyric from "+ fname);
		  tag.deleteField(FieldKey.LYRICS);
	}
	TagField lyrictf;
	try {
		lyrictf = tag.createField(FieldKey.LYRICS, newlyric);

		if(lyrictf instanceof AbstractID3v2Frame)
		{
			setLyricLanguage((AbstractID3v2Frame) lyrictf);
		}
		tag.addField(lyrictf);
		log.info("Updated lyric in "+ fname);
		updated = true;
	} 
	catch (Exception e) 
	{
		log.log(Level.SEVERE, "Failed to add lyric to: "+ fname, e);
	} 
	

	return updated;
}

// This is ugly but apparently a language is required by the tag library and the default value
// is invalid. Not sure why the language is required. When MP3TAG displays the lyrics it
// includes the language which is weird. Remains to be seen what is displayed in iTunes.
private void setLyricLanguage(AbstractID3v2Frame lyrf)
{
	((FrameBodyUSLT) lyrf.getBody()).setLanguage("eng");
}

private FlacTags loadLyrics(File lyricsxml) throws JAXBException, FileNotFoundException
{
	String ctxname = FlacTags.class.getPackage().getName();
	JAXBContext jc = JAXBContext.newInstance(ctxname);
	Unmarshaller u = jc.createUnmarshaller(); 
	AsciiFilterInputStream ascii = new AsciiFilterInputStream(new FileInputStream(lyricsxml));
	FlacTags lyrics = null;
	try
	{
		JAXBElement<FlacTags> o = u.unmarshal(new StreamSource(ascii), FlacTags.class);
		lyrics = o.getValue();
		
	}
	catch(UnmarshalException uex)
	{
		decodeUnmarshalException(uex, lyricsxml);
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
	return lyrics;
}

private void decodeUnmarshalException(UnmarshalException uex, File file) {
	Throwable lex = uex.getLinkedException();
	if(lex instanceof SAXParseException)
	{
		SAXParseException sex = (SAXParseException) lex;
		// Would be nice to align Line under Parse but the console font is not fixed width
		// alignment is done by tweaking tab size to match width of "SEVERE:" prefix

		log.log(Level.SEVERE, "Parse fail: " + file.getName()+"\n\tLine " + sex.getLineNumber() + "(" + sex.getColumnNumber() + ") " + sex.getMessage());

	}
	else
	{
		log.log(Level.SEVERE, "Exception processing " + file, uex);
	}

	
}

/**
 * Save a dummy cuesheet a la CUETools but without the annoying file extensions.
 * Directory/Filename handling copied from saveFlacaudioMD5 since the cuesheet should
 * always go into the directory containing the flacs.
 * 
 * @param lyricsxml
 * @param tags
 * @throws FileNotFoundException
 */
public void saveCuesheet(String lyricsxml, FlacTags tags) throws FileNotFoundException
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
         if(!lxfile.isDirectory())
         {
            lxfile = rootdir;
         }
         lxfile = new File(lxfile, d.getName() + ".cue");
         
         // Easier to backup existing file than to check whether the
         // filename are still correct and it's possible the existing
         // file contains something important, eg. if it's a real cuesheet
         // for a single file flac then dont want to loose the track indexes!
         if(lxfile.exists())
         {
            // Backup the previous file.
            String name = lxfile.getName();
            name = name.replace(".cue", "_" + Utils.getTimestampFN(lxfile.lastModified()) + ".cue");
            File rnfile = new File(lxfile.getParentFile(), name);
            Files.move(lxfile.toPath(), rnfile.toPath(),  StandardCopyOption.REPLACE_EXISTING);
         }         
         

         StringBuffer cue = new StringBuffer();
         cue.append("REM COMMENT \"FLACtagger generated CUE sheet\"\n");

         int i=0;
         for(FileMetadata fmd : d.getFiles().getFilemetadata())
         {
         	// i is used to provide the track number in case it is not present in the metadata
         	// but can also use it to indicate when to write the top level cue data  
            if(i == 0)
            {
               cue.append("PERFORMER \"").append(fmd.getArtist()).append("\"\n");
               cue.append("TITLE \"").append(fmd.getAlbum()).append("\"\n"); 
            }
            i++;
            cue.append("FILE \"").append(fmd.getName()).append("\" WAVE\n");

            Integer iT = fmd.getTracknumber();
            
            // If tracknumber is missing fallback to using the i count
            // which assumes that tracknumber is going to be missing for all tracks.
            if(iT == null)
            {
               iT = Integer.valueOf(i);
            }

            cue.append(String.format("   TRACK %02d AUDIO\n", iT.intValue()));
            cue.append("   INDEX 01 00:00:00\n");
         }
         
         osw = new OutputStreamWriter(new FileOutputStream(lxfile));
         osw.write(cue.toString());
         Utils.safeClose(osw);
      }
   }
   catch (IOException e)
   {
   	log.severe("Failed to save cuesheet for " + lyricsxml + ": " + e.getMessage());
   }
   finally
   {
      Utils.safeClose(osw);
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
public void saveFolderaudioMD5(String lyricsxml, FlacTags tags) throws FileNotFoundException
{
   // flacaudio.md5 writing belongs somewhere else!!
   OutputStreamWriter osw = null;
   File rootdir = null;
   try
   {
      for(Directory d : tags.getDirectory())
      {

      	// Only write folderaudio if all entries have an MD5, either from streaminfo or calculated.
      	boolean skipfamd5 = false;
         for(FileMetadata fmd : d.getFiles().getFilemetadata())
         {
         	String md5trk = fmd.getStrmpcmmd5();
         	if(md5trk == null)
         		md5trk = fmd.getCalcpcmmd5();
         	if(md5trk == null)
         	{
         		skipfamd5 = true;
         		break;
         	}
         }
         if(skipfamd5)
         	continue;
         
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
         	// Use a calculated MD5 if there is no streaminfo value (always the case for MP3)
         	String md5trk = fmd.getStrmpcmmd5();
         	if(md5trk == null)
         		md5trk = fmd.getCalcpcmmd5();
            md5s.append(md5trk).append(" *");
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
   	log.severe("Failed to save lyric file: " + lyricsxml + ": " + e.getMessage());
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
	
		sortTags(lyrics);
		
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


// Sort the files in each directory according to the sort criteria.
// Default is track order - this is the same as filename order for 
// 'normal' albums, ie. with filenames like '<track> <title>', but not
// for compilations, which do not include the track in the filename. 
// Compilations consist of multiple albums merged together. Sorting on the
// filename mixes the albums in the output file, sorting on the track 
// number keeps the albums together in the output file which makes
// looking for lyrics easier.
// NICETOHAVE: make sort order configurable, ie. allow the Comparator to be
// defined externally.
private void sortTags(FlacTags lyrics) {
	
	Comparator<FileMetadata> compareByTrack = 
			(FileMetadata tag1, FileMetadata tag2) -> 
	{
		if((tag1 == null) || (tag1.getTracknumber() == null))
			return -1;
		if(tag2 == null)
			return -1;

		
		return tag1.getTracknumber().compareTo( tag2.getTracknumber());
   };
   
   Comparator<FileMetadata> tagsorter = compareByTrack;
	for(Directory dir : lyrics.getDirectory())
	{
		List<FileMetadata> tags = dir.getFiles().getFilemetadata();
		tags.sort(tagsorter);
	}
	
}

public String getAudioDigest(File flacfile, FileMetadata ftx)
{
	AudioDigester fd = AudioDigesterFactory.getAudioDigester(flacfile);

	if(fd == null)
		return null; 
	//AudioDigester fd = new FLACdigester();
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

