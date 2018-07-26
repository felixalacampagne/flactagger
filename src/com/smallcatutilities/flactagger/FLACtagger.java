package com.smallcatutilities.flactagger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
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
				System.out.println("Extract not implemented yet!");
			}
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	
private final String rootDir;
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
}
