FLACtagger Readme

I created FLACtagger because I could not find a way to easily export the lyrics from my
FLAC files and then re-import them having made changes. Editing the lyrics directly in
my chosen FLAC processing programs, ie. foobar2k and mp3tag, is a particularly painful experience
especially when large numbers of files are concerned, ie. more than 1.

'mp3tag' provides a mechanism to export tags using a customizable format but provides no way, AFAIK,
to re-import the tags. However mp3tag provided the inspiration for FLACtagger, which was originally
only going to provide the re-import support I needed, as it allowed me to create an XML format file
which could then be processed by my own application. Once I'd got the import working 
providing the export was relatively simple. On the way I discovered a way to generate digests of the
raw audio in FLACtagger which match the digests created by ffmpeg. I also discovered that most FLAC files
also contain the same raw audio digest embedded in them. This provides a handy way of checking that the audio
hasn't been changed by the various tagging operations applied to the files between creation, tagging and 
the final version, which for me just happens to be ALAC, and also ensuring that backups on optical
media haven't become corrupt.

FLACtagger relies on;
- the "jaudiotagger" library which can be found here: https://bitbucket.org/ijabz/jaudiotagger.git
- the "JustFLAC" library which can be found here: https://github.com/drogatkin/JustFLAC.git
- the "jNA" library found here: https://github.com/java-native-access/jna
- since we are going back to the future it now relies on JAXB which has become an external dependency
  
  To keep FLACtagger a standalone (aka executable) jar the dependent libraries are included in the FLACtagger
  jar file when it is built using the any script 'mkjar.xml'. 
  
  The dependent libraries can be downloaded with maven and the 'pom.xml' and specifying 
  'dependency:copy-dependencies' as the goal. I haven't found a way to use maven to get the jar files
  for the FLAC and audiotagger libraries so the sources are included in the project as git sub-modules.
  This is not ideal as git sub-modules suck. Eventually I guess the project will need to be converted to
  maven where these libraries can exist as sub-projects in parallel to the FLAGtagger project... or something...
  but that is for another day.

When I set up the FLACtagger GitHub project I was asked about the license to be applied. I can't find this
information anywhere, and can't find licenses for the dependencies either.


