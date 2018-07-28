package com.smallcatutilities.flactagger;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
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

import com.smallcatutilities.flactagger.generated.lyrics.DirectoryType;
import com.smallcatutilities.flactagger.generated.lyrics.FileType;
import com.smallcatutilities.flactagger.generated.lyrics.FilesType;
import com.smallcatutilities.flactagger.generated.lyrics.LyricsType;
import com.smallcatutilities.flactagger.generated.lyrics.ObjectFactory;
import com.smallcatutilities.utils.CmdArgMgr;

public class FLACtagger
{
private static final String USAGE="Usage: FLACtagger <-u|-x> <-l lyrics.xml> [-r FLAC file rootdir]";
private static final String FLAC_LYRICS_TAG="UNSYNCED LYRICS";
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
	LyricsType lyrics =  objFact.createLyricsType();
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
	
	public int extractFiles(File dir, LyricsType lyrics)
	{
	int flaccnt = 0;
	
		if((lyrics == null) || (lyrics.getDirectory() == null))
			return flaccnt;
	
	List<File> flacs = getFiles(dir);
	DirectoryType d = null;
	List<FileType> files = null;
		for(File f : flacs)
		{
			FileTypeEx ft = getFile(f);
			if(ft != null)
			{
				if(d == null)
				{
					d = objFact.createDirectoryType();
					d.setArtist(ft.getArtist());
					d.setAlbum(ft.getAlbum());
					d.setName(dir.getName());
					d.setFiles(objFact.createFilesType());
					files = d.getFiles().getFile();
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
	public FileTypeEx getFile(File f)
	{
	FileTypeEx ftx = null;
		System.out.println("INFO: Loading: " + f.getName());
		try 
		{
			AudioFile af = AudioFileIO.read(f);
			Tag tag = af.getTag();
			
			// Artist, album, lyric, directory name, file name
			ftx = new FileTypeEx();
			ftx.setName(f.getName());
			ftx.setArtist(tag.getFirst(FieldKey.ARTIST));
			ftx.setAlbum(tag.getFirst(FieldKey.ALBUM));
			String lyric = tag.getFirst(FLAC_LYRICS_TAG);
			if(lyric != null)
			{
				// Leaving the CRLF in results lines terminated with "&#xD;" and a normal linefeed
				// Seems the Java XML parse is stuck in the Unix world. There might be a way to
				// tell the parse to treat the data verbatim (I thought that is what CDATA meant) but
				// don't know whether that must be in the schema or the xjb or what at the moment
				lyric = lyric.replace("\r","");
				
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
	LyricsType lyrics = loadLyrics(lyricsxml);
	
		if(lyrics == null)
		{
			System.out.println("No Lyrics loaded from " + lyricsxml + "!");
			return 1;
		}
	
		for(DirectoryType d : lyrics.getDirectory())
		{
			File dir = new File(rootDir, d.getName());
			if(!dir.exists())
			{
				System.out.println("WARN:  Directory not found: " + dir.getAbsolutePath());
				continue;
			}
			System.out.println("INFO: Processing Directory: " + d.getName());

			FilesType files = d.getFiles();
			for(FileType ft : files.getFile())
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
				System.out.println("INFO: Loading: " + ft.getName());
				AudioFile af = AudioFileIO.read(f);
				
				Tag tag = af.getTag();
				if((tag != null) && (tag instanceof FlacTag ))
				{
					if(tag.hasField(FLAC_LYRICS_TAG))
					{
						String currlyric = tag.getFirst(FLAC_LYRICS_TAG);
						if(trimlyric.equals(currlyric))
						{
							System.out.println("INFO: Lyric is already present, no update required: "+ ft.getName());
							continue;
						}
						//System.out.println("INFO: Removing existing lyric from "+ ft.getName() + ":\n" + currlyric);
						System.out.println("INFO: Removing existing lyric from "+ ft.getName());
						tag.deleteField(FLAC_LYRICS_TAG);
					}
					// TagField ID: UNSYNCED LYRICS Class: org.jaudiotagger.tag.vorbiscomment.VorbisCommentTagField
					TagField lyrictf = new VorbisCommentTagField(FLAC_LYRICS_TAG, trimlyric);
					tag.addField(lyrictf);
					System.out.println("INFO: updating: " + ft.getName());
					//System.out.println("DBUG: " + trimlyric);
					af.commit();
				}
				else
				{
					System.out.println("WARN: No or none-FLAC tag, unable to update: " + ft.getName());
				}
			}
		}
		
	return 0;	
	}
	private LyricsType loadLyrics(String lyricsxml) throws JAXBException, FileNotFoundException
	{
		JAXBContext jc = JAXBContext.newInstance( "com.smallcatutilities.flactagger.generated.lyrics" );
		Unmarshaller u = jc.createUnmarshaller(); 
		JAXBElement<LyricsType> o = u.unmarshal(new StreamSource(new FileInputStream(lyricsxml)), LyricsType.class);
		LyricsType lyrics = o.getValue();
		return lyrics;
	}
	
	private void saveLyrics(String lyricsxml, LyricsType lyrics) throws JAXBException, FileNotFoundException
	{
		// Arg for JAXBContext is the package containing the ObjectFactory for the type to be Un/Marshalled
		String ctxname = LyricsType.class.getPackage().getName();
		JAXBContext jc = JAXBContext.newInstance(ctxname);	
		Marshaller m = jc.createMarshaller();
		
		JAXBElement<LyricsType> o = objFact.createLyrics(lyrics);
		m.marshal(o, new FileOutputStream(lyricsxml));
	}
}
