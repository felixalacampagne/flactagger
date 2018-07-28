package com.smallcatutilities.flactagger;

import com.smallcatutilities.flactagger.generated.lyrics.FileType;

public class FileTypeEx extends FileType {
private String artist;
private String album;

public FileTypeEx() {
	super();
}
	public String getArtist() {
	return artist;
}

public void setArtist(String artist) {
	this.artist = artist;
}

public String getAlbum() {
	return album;
}

public void setAlbum(String album) {
	this.album = album;
}



}
