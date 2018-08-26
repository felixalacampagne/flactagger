FLACtagger Readme

This is a text file, not a message digest, so why does it need to have the ".md" extension?

I created FLACtagger because I could not find a way to easily export the lyrics from my
FLAC files and then re-import them having made changes. Editing the lyrics directly in
my chosen FLAC processing programs, foobar2k and mp3tag, is a particularly painful experience
especially when large numbers of files are concerned, ie. more than 1.

mp3tag provides a mechanism to export tags using a customizable format but provides no way, AFAIK,
to re-import the tags. However mp3tag provided the inspiration for FLACtagger, which was originally
only going to provide the re-import support I needed, as it allowed me to create an XML format file
which I could then be processed by my own application. Once I'd got the import working 
providing the export was relatively simple. On the way I discovered a way to create digests of the
raw audio to match the digests created by ffmpeg, and also discovered that most FLAC files also 
contain the same digest embedded in them. This provides a handy way of checking that the audio
hasn't been changed by the various operations applied to the files between creation and the final
listening format, which just happens to be ALAC, for me, also also ensuring that backups on optical
media haven't become corrupt.

FLACtagger relies on;
- the "jaudiotagger" library which can be found here: https://bitbucket.org/ijabz/jaudiotagger.git
- the "JustFLAC" library which can be found here: https://github.com/drogatkin/JustFLAC.git
  NB. I needed to modify the JustFLAC library to expose the STREAMINFO digest. I haven't figured out 
  a way to make the git sub-modules work in GitHub and also haven't figured out how to store my
  change. For anyone who may be interested, the change is to expose the md5sum byte array in
  org.kc7bfi.jflac.metadata.StreamInfo with a public method called getMD5sum().
